package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.token.erp.common.BizException;
import com.token.erp.dto.DeliveryRiskRow;
import com.token.erp.entity.*;
import com.token.erp.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI 交期风险智能预警 —— 运算核心。
 *
 * ## 定位
 * 这是**确定性规则计算**，不训练模型。AI 只负责把这里算出的结构化结果
 * 翻译成人话（报告），不参与计算，避免「模型编日期」。
 *
 * ## 算法（对齐《AI交期风险智能预警分析运算》）
 * 1. 可用库存 ≥ 基本数量 → 可准时交付
 * 2. 否则按交付日期倒推：最早行动日 = 交付日 − 需要天数；
 *    最早行动日 − 今天 ≥ 0 → 可准时（紧迫）；< 0 → 有逾期风险
 * 3. 逾期时顺推：建议交付日 = 今天 + 需要天数
 * 4. 需要天数：
 *    - 无 BOM 的成品 → 按**采购周期**（整批天）
 *    - 有 BOM 的成品 → 自身生产天数（数量 × 单位生产周期）+ 子件最长链路
 *    - 物料 → 采购周期 / 外协周期（整批天）
 *    子件按模拟 MRP 算净需求，净需求 ≤ 0 的子件不构成约束。
 *
 * ## 口径说明（文档自身有矛盾，这里定死）
 * - 取数窗口取 **365 天**（文档一处写「最近 3 个月」、一处写「近一年」，取一年）
 * - 生产周期 = 工单时长 ÷ 已生产良品数，单位「天/件」，倒推时乘数量
 * - 采购/外协周期不考虑数量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryRiskService {

    /** 历史周期取数窗口（天） */
    private static final int CYCLE_WINDOW_DAYS = 365;
    private static final int MAX_BOM_DEPTH = 6;

    /** 兜底周期：没有任何历史数据、也没人工录入时用 */
    private static final BigDecimal DEFAULT_PURCHASE_DAYS = new BigDecimal("10");
    private static final BigDecimal DEFAULT_OUTSOURCE_DAYS = new BigDecimal("10");
    /** 天/件 兜底：按「一天能出 20 件」估 */
    private static final BigDecimal DEFAULT_PRODUCTION_PER_UNIT = new BigDecimal("0.05");

    private static final String T_PRODUCT = "product";
    private static final String T_MATERIAL = "material";

    private final SalesOrderItemMapper salesOrderItemMapper;
    private final QuotationItemMapper quotationItemMapper;
    private final ContractItemMapper contractItemMapper;
    private final ProductMapper productMapper;
    private final MaterialMapper materialMapper;
    private final BomMapper bomMapper;
    private final BomItemMapper bomItemMapper;
    private final InventoryMapper inventoryMapper;
    private final ItemCycleStatMapper cycleStatMapper;
    private final DeliveryRiskMapper riskMapper;

    /**
     * ⚠️ 两个都必须加：
     *  - `findAndRegisterModules()`：自己 new 的 ObjectMapper 默认不认 `LocalDate`，
     *    序列化直接抛「Java 8 date/time type not supported」，SSE 一片空白。
     *  - `disable(WRITE_DATES_AS_TIMESTAMPS)`：不加的话日期会变成 `[2026,11,18]` 这种数组，
     *    前端拿到没法显示。
     */
    private final ObjectMapper json = new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ==================== 对外入口 ====================

    /** 评估一张单据的全部明细行（并覆盖写结果表） */
    @Transactional
    public List<DeliveryRiskRow> evaluate(String bizType, Long bizId) {
        List<DeliveryRiskRow> rows = new ArrayList<>();
        for (ItemRef ref : loadItems(bizType, bizId)) {
            DeliveryRiskRow row = evaluateOne(ref, bizType, bizId);
            if (row != null) rows.add(row);
        }
        rows.forEach(r -> persist(bizType, bizId, r));
        return rows;
    }

    /** 采纳建议：把建议交付日期写回明细行 */
    @Transactional
    public int apply(String bizType, Long bizId, List<Long> itemIds) {
        LambdaQueryWrapper<DeliveryRisk> qw = new LambdaQueryWrapper<DeliveryRisk>()
                .eq(DeliveryRisk::getBizType, bizType)
                .eq(DeliveryRisk::getBizId, bizId)
                .isNotNull(DeliveryRisk::getSuggestDate);
        if (itemIds != null && !itemIds.isEmpty()) qw.in(DeliveryRisk::getItemId, itemIds);

        List<DeliveryRisk> risks = riskMapper.selectList(qw);
        int n = 0;
        for (DeliveryRisk r : risks) {
            switch (bizType) {
                case DeliveryRisk.BIZ_SALES_ORDER -> {
                    SalesOrderItem it = salesOrderItemMapper.selectById(r.getItemId());
                    if (it != null) {
                        it.setDeliveryDate(r.getSuggestDate());
                        salesOrderItemMapper.updateById(it);
                        n++;
                    }
                }
                case DeliveryRisk.BIZ_QUOTATION -> {
                    QuotationItem it = quotationItemMapper.selectById(r.getItemId());
                    if (it != null) {
                        it.setDeliveryDate(r.getSuggestDate());
                        quotationItemMapper.updateById(it);
                        n++;
                    }
                }
                case DeliveryRisk.BIZ_CONTRACT -> {
                    ContractItem it = contractItemMapper.selectById(r.getItemId());
                    if (it != null) {
                        it.setDeliveryDate(r.getSuggestDate());
                        contractItemMapper.updateById(it);
                        n++;
                    }
                }
                default -> { }
            }
        }
        return n;
    }

    /** 读取已存的评估结果（列表/刷新前先用，避免重复算） */
    public List<DeliveryRisk> listRisk(String bizType, Long bizId) {
        return riskMapper.selectList(new LambdaQueryWrapper<DeliveryRisk>()
                .eq(DeliveryRisk::getBizType, bizType)
                .eq(DeliveryRisk::getBizId, bizId));
    }

    // ==================== 无状态评估（新建单据还没保存时用） ====================

    /**
     * 直接在内存明细上评估，不查库、不落库。
     *
     * 为什么需要：新建销售订单时明细行还没入库、没有 id，
     * 而「AI交期评估」按钮就在新建页上（参考图 A1）。
     * 所以进度也走这条：返回的 itemId 用**行下标（从 1 开始）**占位，
     * 前端采纳建议时按下标写回内存表单即可。
     */
    public List<DeliveryRiskRow> evaluateInline(List<InlineItem> items) {
        List<DeliveryRiskRow> rows = new ArrayList<>();
        if (items == null) return rows;
        int idx = 0;
        for (InlineItem it : items) {
            idx++;
            if (it.getProductId() == null) continue;
            BigDecimal qty = nz(it.getQuantity());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;   // 前置条件：数量 > 0
            ItemRef ref = new ItemRef((long) idx, it.getProductId(), qty, it.getDeliveryDate(), it.getSpec(), it.getUnit());
            DeliveryRiskRow row = evaluateOne(ref, DeliveryRisk.BIZ_SALES_ORDER, 0L);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    /** 无状态流式分析：明细来自请求体，不落库 */
    public void streamAnalyzeInline(List<InlineItem> items, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                streamFromRows(evaluateInline(items), emitter);
            } catch (Exception e) {
                log.error("交期风险流式分析失败", e);
                try {
                    emit(emitter, "error", Map.of("message", String.valueOf(e.getMessage())));
                    emitter.complete();
                } catch (Exception ignore) {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    /** 明细项（无状态评估的入参） */
    @Data
    public static class InlineItem {
        private Long productId;
        private BigDecimal quantity;
        private LocalDate deliveryDate;
        private String spec;
        private String unit;
    }

    // ==================== SSE：逐步推送分析过程 ====================

    /**
     * 「查看分析」的流式接口。
     *
     * 计算本身是毫秒级的，但一次性弹出一大坨结果没人看得懂，所以按阶段逐条推：
     * 规划任务 → 意图理解 → 输出清单 → BOM 解析 → MRP 净需求 → 库存分析
     * → 周期计算 → 风险判定 → 报告。
     *
     * ⚠️ 每条推送里的数字都是**真实算出来的**，只是分批给前端看；不是假进度条。
     */
    public void streamAnalyze(String bizType, Long bizId, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> {
            try {
                streamFromRows(evaluate(bizType, bizId), emitter);
            } catch (Exception e) {
                log.error("交期风险流式分析失败", e);
                try {
                    emit(emitter, "error", Map.of("message", String.valueOf(e.getMessage())));
                    emitter.complete();
                } catch (Exception ignore) {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    /** 真正做「分阶段推送」的部分：有单据和内存两种来源，共用这套 */
    private void streamFromRows(List<DeliveryRiskRow> rows, SseEmitter emitter) throws Exception {
                emit(emitter, "step", Map.of(
                        "key", "plan", "name", "正在规划任务", "status", "running",
                        "message", "收到交期评估请求，正在解析单据明细…"));
                sleep(260);
                emit(emitter, "step", Map.of(
                        "key", "plan", "name", "正在规划任务", "status", "ok",
                        "message", "已识别业务场景：交期风险预警"));

                // ---- 意图理解：把这张单的要素摆出来 ----
                StringBuilder intent = new StringBuilder();
                intent.append("当前日期：").append(LocalDate.now()).append('\n');
                intent.append("待评估明细：").append(rows.size()).append(" 行");
                for (DeliveryRiskRow r : rows) {
                    intent.append('\n').append("· ").append(r.getProductName())
                            .append("  数量 ").append(plain(r.getQuantity()))
                            .append("  期望交付 ").append(r.getDeliveryDate());
                }
                emit(emitter, "step", Map.of(
                        "key", "intent", "name", "用户意图理解", "status", "ok",
                        "message", intent.toString()));
                sleep(260);

                emit(emitter, "step", Map.of(
                        "key", "outputs", "name", "需要输出内容", "status", "ok",
                        "message", "1. BOM表解析\n2. MRP需求计算\n3. 库存分析\n4. 周期计算\n"
                                + "5. 是否存在逾期风险\n6. 风险原因分析\n7. AI预计交付日期"));
                sleep(240);

                // ---- BOM 解析 ----
                int bomLinks = 0;
                for (DeliveryRiskRow r : rows) {
                    Bom b = r.getProductId() == null ? null : activeBom(r.getProductId());
                    if (b != null) {
                        bomLinks += bomItemMapper.selectCount(
                                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, b.getId())).intValue();
                    }
                }
                emit(emitter, "step", Map.of(
                        "key", "bom", "name", "正在获取BOM表", "status", "ok",
                        "message", bomLinks > 0
                                ? "已获取到 BOM 表，共关联 " + bomLinks + " 条物料"
                                : "未找到有效 BOM，按「成品采购」口径评估"));
                sleep(240);

                // ---- MRP 净需求 ----
                List<Map<String, Object>> allShortage = new ArrayList<>();
                for (DeliveryRiskRow r : rows) {
                    if (r.getShortageItems() != null) allShortage.addAll(r.getShortageItems());
                }
                long zeroStock = rows.stream().filter(r -> nz(r.getStockQty()).compareTo(BigDecimal.ZERO) <= 0).count();
                emit(emitter, "step", Map.of(
                        "key", "mrp", "name", "正在计算MRP需求", "status", "ok",
                        "message", allShortage.isEmpty()
                                ? "各层物料净需求均为 0，无缺料"
                                : "净需求计算完成，共 " + allShortage.size() + " 项物料存在缺口"));
                sleep(240);

                emit(emitter, "step", Map.of(
                        "key", "stock", "name", "库存分析", "status", "ok",
                        "message", "可用库存（现存量 − 已占用）：" + rows.size() + " 行中 "
                                + zeroStock + " 行库存为 0"));
                sleep(240);

                // ---- 周期计算 ----
                StringBuilder cyc = new StringBuilder();
                for (DeliveryRiskRow r : rows) {
                    cyc.append("· ").append(r.getProductName()).append("：")
                            .append(plain(r.getCycleDays())).append(" 天\n");
                }
                emit(emitter, "step", Map.of(
                        "key", "cycle", "name", "周期计算", "status", "ok",
                        "message", cyc.toString().trim()));
                sleep(240);

                long late = rows.stream()
                        .filter(r -> !DeliveryRisk.LEVEL_ON_TIME.equals(r.getRiskLevel())).count();
                emit(emitter, "step", Map.of(
                        "key", "risk", "name", "风险判定", "status", "ok",
                        "message", late == 0 ? "全部可准时交付" : late + " 行存在交付风险"));
                sleep(200);

                emit(emitter, "result", rows);

                // ---- 报告 ----
                Map<String, Object> report = buildReport(rows);
                emit(emitter, "report", report);
                emit(emitter, "done", Map.of("ok", true));

                emitter.complete();
    }

    private void emit(SseEmitter emitter, String event, Object data) throws Exception {
        emitter.send(SseEmitter.event().name(event).data(json.writeValueAsString(data)));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String bizLabel(String bizType) {
        return switch (bizType) {
            case DeliveryRisk.BIZ_SALES_ORDER -> "销售订单";
            case DeliveryRisk.BIZ_QUOTATION -> "报价单";
            case DeliveryRisk.BIZ_CONTRACT -> "合同";
            default -> bizType;
        };
    }

    /**
     * 生成 4 段式风险报告。
     *
     * ⚠️ 这一版是**模板拼装**（数字全部来自真实计算），不调大模型：
     * 确定性、零成本、不会编日期。以后要更自然的措辞，把这里换成调 AI 服务即可，
     * 入参就是这四段的结构化数据。
     */
    private Map<String, Object> buildReport(List<DeliveryRiskRow> rows) {
        List<Map<String, Object>> sections = new ArrayList<>();
        if (rows.isEmpty()) {
            sections.add(Map.of("title", "1. 是否存在于逾期风险", "body", "本单据没有可评估的明细行（需数量 > 0 且已填交付日期）。"));
            return Map.of("sections", sections);
        }

        List<DeliveryRiskRow> risky = rows.stream()
                .filter(r -> !DeliveryRisk.LEVEL_ON_TIME.equals(r.getRiskLevel())).toList();

        // 1) 是否逾期
        StringBuilder s1 = new StringBuilder();
        if (risky.isEmpty()) {
            s1.append("预计可准时交付。");
        } else {
            DeliveryRiskRow worst = risky.get(0);
            for (DeliveryRiskRow r : risky) {
                if (r.getSuggestDate() != null && worst.getSuggestDate() != null
                        && r.getSuggestDate().isAfter(worst.getSuggestDate())) worst = r;
            }
            long maxDelay = worst.getSuggestDate() == null || worst.getDeliveryDate() == null ? 0
                    : ChronoUnit.DAYS.between(worst.getDeliveryDate(), worst.getSuggestDate());
            s1.append("存在明确逾期风险。整单 ").append(rows.size()).append(" 行明细中，")
                    .append(risky.size()).append(" 行无法按期望日期交付。");
            s1.append("\n\n最晚的一行是「").append(worst.getProductName()).append("」：期望交付 ")
                    .append(worst.getDeliveryDate()).append("，按当前库存与补货周期推算，最早只能 ")
                    .append(worst.getSuggestDate()).append(" 交付");
            if (maxDelay > 0) s1.append("，晚 ").append(maxDelay).append(" 天");
            s1.append("。");
        }
        sections.add(Map.of("title", "1. 是否存在于逾期风险", "body", s1.toString()));

        // 2) 主要原因
        StringBuilder s2 = new StringBuilder();
        if (risky.isEmpty()) {
            s2.append("可用库存覆盖了需求，或剩余天数足以完成补货，未触发风险。");
        } else {
            List<String> reasons = new ArrayList<>();
            for (DeliveryRiskRow r : risky) {
                StringBuilder one = new StringBuilder();
                one.append("· 「").append(r.getProductName()).append("」");
                if (nz(r.getStockQty()).compareTo(BigDecimal.ZERO) <= 0) {
                    one.append("可用库存为 0，必须走完整的补货/生产流程");
                } else {
                    one.append("可用库存 ").append(plain(r.getStockQty()))
                            .append(" 不足需求 ").append(plain(r.getQuantity()))
                            .append("，缺口 ").append(plain(r.getShortageQty())).append(" 触发补货");
                }
                one.append("，该链路周期约 ").append(plain(r.getCycleDays())).append(" 天");
                if (r.getEarliestActionDate() != null) {
                    long gap = ChronoUnit.DAYS.between(LocalDate.now(), r.getEarliestActionDate());
                    one.append("，最早应在 ").append(r.getEarliestActionDate()).append(" 开工");
                    if (gap < 0) one.append("，实际已晚 ").append(Math.abs(gap)).append(" 天");
                }
                reasons.add(one.toString());
            }
            s2.append(String.join("\n", reasons));
        }
        sections.add(Map.of("title", "2. 延期主要原因", "body", s2.toString()));

        // 3) 影响最大的物料
        StringBuilder s3 = new StringBuilder();
        List<Map<String, Object>> shortage = new ArrayList<>();
        for (DeliveryRiskRow r : risky) {
            if (r.getShortageItems() != null) shortage.addAll(r.getShortageItems());
        }
        if (shortage.isEmpty()) {
            s3.append(risky.isEmpty() ? "无缺料物料。" : "缺口全部来自成品自身库存不足，没有下一级物料瓶颈。");
        } else {
            Map<String, Object> top = null;
            for (Map<String, Object> m : shortage) {
                if (top == null) { top = m; continue; }
                BigDecimal a = new BigDecimal(String.valueOf(m.get("cycleDays")));
                BigDecimal b = new BigDecimal(String.valueOf(top.get("cycleDays")));
                if (a.compareTo(b) > 0) top = m;
            }
            s3.append("关键瓶颈物料：「").append(top.get("name")).append("」（")
                    .append(top.get("code")).append("）。净需求 ")
                    .append(plain(new BigDecimal(String.valueOf(top.get("netQty")))))
                    .append("，自身周期 ").append(plain(new BigDecimal(String.valueOf(top.get("cycleDays")))))
                    .append(" 天，是整条链路里耗时最长、直接决定交付节奏的一环。");
            if (shortage.size() > 1) s3.append("\n\n其余缺料物料共 ").append(shortage.size() - 1).append(" 项，周期均不超过它。");
        }
        sections.add(Map.of("title", "3. 影响最大的物料", "body", s3.toString()));

        // 4) 建议措施
        StringBuilder s4 = new StringBuilder();
        if (risky.isEmpty()) {
            s4.append("按计划推进即可，无需额外动作。");
        } else {
            s4.append("1. 优先核实缺料的实际在途/待到货时间，确认能否在最早行动日之前补齐；")
                    .append("\n2. 与客户沟通分批交付：先发可用库存部分，缺口部分按 AI 建议日期补发；")
                    .append("\n3. 若无法接受延期，评估替代料或外协加急，压缩上述瓶颈物料周期；")
                    .append("\n4. 复盘本次缺口对应的安全库存设置，缺口很小的场景可适当上调安全库存避免「因小失大」。");
        }
        sections.add(Map.of("title", "4. 建议采取的措施", "body", s4.toString()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sections", sections);
        out.put("summary", Map.of(
                "total", rows.size(),
                "risky", risky.size(),
                "latestSuggestDate", risky.stream().map(DeliveryRiskRow::getSuggestDate)
                        .filter(Objects::nonNull).max(LocalDate::compareTo).map(LocalDate::toString).orElse("")));
        return out;
    }

    // ==================== 单据明细装载 ====================

    private record ItemRef(Long itemId, Long productId, BigDecimal qty, LocalDate deliveryDate, String spec, String unit) {}

    private List<ItemRef> loadItems(String bizType, Long bizId) {
        List<ItemRef> list = new ArrayList<>();
        switch (bizType) {
            case DeliveryRisk.BIZ_SALES_ORDER -> salesOrderItemMapper
                    .selectList(new LambdaQueryWrapper<SalesOrderItem>()
                            .eq(SalesOrderItem::getOrderId, bizId))
                    .forEach(i -> list.add(new ItemRef(i.getId(), i.getProductId(), i.getQuantity(),
                            i.getDeliveryDate(), i.getSpec(), i.getUnit())));
            case DeliveryRisk.BIZ_QUOTATION -> quotationItemMapper
                    .selectList(new LambdaQueryWrapper<QuotationItem>()
                            .eq(QuotationItem::getQuotationId, bizId))
                    .forEach(i -> list.add(new ItemRef(i.getId(), i.getProductId(), i.getQuantity(),
                            i.getDeliveryDate(), i.getSpec(), i.getUnit())));
            case DeliveryRisk.BIZ_CONTRACT -> contractItemMapper
                    .selectList(new LambdaQueryWrapper<ContractItem>()
                            .eq(ContractItem::getContractId, bizId))
                    .forEach(i -> list.add(new ItemRef(i.getId(), i.getProductId(), i.getQuantity(),
                            i.getDeliveryDate(), i.getSpec(), i.getUnit())));
            default -> throw new BizException("不支持的单据类型：" + bizType);
        }
        return list;
    }

    // ==================== 单行评估 ====================

    private DeliveryRiskRow evaluateOne(ItemRef ref, String bizType, Long bizId) {
        if (ref.productId() == null) return null;
        // 前置条件（文档 4）：数量 > 0 且已填交付日期
        BigDecimal qty = nz(ref.qty());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) return null;

        Product p = productMapper.selectById(ref.productId());
        if (p == null) return null;

        LocalDate today = LocalDate.now();
        LocalDate delivery = ref.deliveryDate() != null ? ref.deliveryDate() : today;

        DeliveryRiskRow row = new DeliveryRiskRow();
        row.setItemId(ref.itemId());
        row.setProductId(p.getId());
        row.setProductCode(p.getCode());
        row.setProductName(p.getName());
        row.setSpec(ref.spec());
        row.setUnit(ref.unit());
        row.setQuantity(qty);
        row.setDeliveryDate(ref.deliveryDate());

        BigDecimal stock = availableQty(T_PRODUCT, p.getId());
        row.setStockQty(stock);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("bizType", bizType);
        detail.put("bizId", bizId);
        detail.put("today", today.toString());
        detail.put("qty", qty);
        detail.put("stock", stock);

        // 1) 库存够 → 可准时
        if (stock.compareTo(qty) >= 0) {
            row.setRiskLevel(DeliveryRisk.LEVEL_ON_TIME);
            row.setRiskText("可准时交付");
            row.setShortageQty(BigDecimal.ZERO);
            row.setCycleDays(BigDecimal.ZERO);
            detail.put("reason", "可用库存 " + plain(stock) + " ≥ 需求 " + plain(qty) + "，无需启动补货");
            row.setDetail(detail);
            row.setShortageItems(List.of());
            return row;
        }

        // 2) 算需要天数（含 BOM 递归 / 缺料）
        List<Map<String, Object>> shortage = new ArrayList<>();
        BigDecimal days = leadTimeOf(T_PRODUCT, p.getId(), qty, 0, shortage);

        LocalDate earliest = delivery.minusDays(days.setScale(0, RoundingMode.CEILING).longValue());
        long diff = ChronoUnit.DAYS.between(today, earliest);

        row.setCycleDays(days);
        row.setEarliestActionDate(earliest);
        row.setShortageQty(qty.subtract(stock).max(BigDecimal.ZERO));
        row.setShortageItems(shortage);

        detail.put("needDays", days);
        detail.put("earliestActionDate", earliest.toString());
        detail.put("daysGap", diff);
        detail.put("shortageQty", qty.subtract(stock).max(BigDecimal.ZERO));
        detail.put("shortageItems", shortage);

        if (diff >= 0) {
            row.setRiskLevel(DeliveryRisk.LEVEL_ON_TIME);
            row.setRiskText("可准时交付");
            detail.put("reason", "最早必须行动日 " + earliest + "，距今天还有 " + diff + " 天，来得及");
        } else {
            LocalDate suggest = today.plusDays(days.setScale(0, RoundingMode.CEILING).longValue());
            row.setRiskLevel(delivery.isBefore(today) ? DeliveryRisk.LEVEL_LATE : DeliveryRisk.LEVEL_AT_RISK);
            row.setRiskText(delivery.isBefore(today) ? "已逾期" : "有逾期风险");
            row.setSuggestDate(suggest);
            detail.put("suggestDate", suggest.toString());
            detail.put("reason",
                    "需要 " + plain(days) + " 天，最早必须 " + earliest + " 开工，但今天已是 " + today
                            + "，已晚 " + Math.abs(diff) + " 天 → 建议交付日 " + suggest);
        }
        row.setDetail(detail);
        return row;
    }

    // ==================== 期量计算 ====================

    /**
     * 递归算「从今天起，要交付 qty 需要多少天」。
     *
     * @param shortage 收集净缺料的层级明细，供报告「影响最大的物料」段使用
     */
    private BigDecimal leadTimeOf(String itemType, Long itemId, BigDecimal qty, int depth,
                                  List<Map<String, Object>> shortage) {
        if (depth > MAX_BOM_DEPTH) return BigDecimal.ZERO;

        if (T_MATERIAL.equals(itemType)) {
            Material m = materialMapper.selectById(itemId);
            if (m == null) return DEFAULT_PURCHASE_DAYS;
            boolean outsource = "outsource".equalsIgnoreCase(m.getSupplyType());
            BigDecimal days = outsource ? outsourceCycleOfMaterial(m) : purchaseCycleOfMaterial(m);
            recordShortage(shortage, itemType, itemId, m.getCode(), m.getName(), qty, days, "采购/外协");
            return days;
        }

        Product p = productMapper.selectById(itemId);
        if (p == null) return DEFAULT_PURCHASE_DAYS;

        // 自身生产时间：单位周期 × 数量
        BigDecimal perUnit = productionCyclePerUnit(p);
        BigDecimal selfDays = qty.multiply(perUnit).setScale(2, RoundingMode.HALF_UP);

        Bom bom = activeBom(p.getId());
        if (bom == null) {
            // 文档：无关联标准 BOM → 默认按缺货采购
            BigDecimal days = purchaseCycleOfProduct(p);
            recordShortage(shortage, T_PRODUCT, itemId, p.getCode(), p.getName(), qty, days, "采购");
            return days;
        }

        List<BomItem> children = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bom.getId()));
        if (children.isEmpty()) {
            BigDecimal days = purchaseCycleOfProduct(p);
            recordShortage(shortage, T_PRODUCT, itemId, p.getCode(), p.getName(), qty, days, "采购");
            return days;
        }

        BigDecimal longest = BigDecimal.ZERO;
        for (BomItem c : children) {
            if (c.getChildId() == null) continue;
            String childType = T_MATERIAL.equalsIgnoreCase(c.getChildType()) ? T_MATERIAL : T_PRODUCT;
            BigDecimal gross = qty.multiply(nz(c.getQuantity()));
            // 净需求 = 毛需求 × (1 + 损耗率) − 可用库存 + 安全库存
            BigDecimal loss = nz(c.getLossRate());
            BigDecimal net = gross.multiply(BigDecimal.ONE.add(loss))
                    .subtract(availableQty(childType, c.getChildId()))
                    .add(safetyStockOf(childType, c.getChildId()))
                    .max(BigDecimal.ZERO);
            if (net.compareTo(BigDecimal.ZERO) <= 0) continue;   // 库存够，不构成约束
            BigDecimal childDays = leadTimeOf(childType, c.getChildId(), net, depth + 1, shortage);
            if (childDays.compareTo(longest) > 0) longest = childDays;
        }
        // 自身生产 + 子件最长链路（串行开工）
        return selfDays.add(longest);
    }

    private void recordShortage(List<Map<String, Object>> shortage, String itemType, Long itemId,
                                String code, String name, BigDecimal qty, BigDecimal days, String way) {
        if (shortage == null) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemType", itemType);
        m.put("itemId", itemId);
        m.put("code", code);
        m.put("name", name);
        m.put("netQty", qty);
        m.put("cycleDays", days);
        m.put("way", way);
        shortage.add(m);
    }

    // ==================== 周期取数（人工 > 历史统计 > 已有提前期 > 兜底） ====================

    private BigDecimal purchaseCycleOfProduct(Product p) {
        if (p.getPurchaseCycleDays() != null) return p.getPurchaseCycleDays();
        BigDecimal stat = statDays(T_PRODUCT, p.getId(), ItemCycleStat.TYPE_PURCHASE);
        return stat != null ? stat : DEFAULT_PURCHASE_DAYS;
    }

    private BigDecimal outsourceCycleOfProduct(Product p) {
        if (p.getOutsourceCycleDays() != null) return p.getOutsourceCycleDays();
        BigDecimal stat = statDays(T_PRODUCT, p.getId(), ItemCycleStat.TYPE_OUTSOURCE);
        return stat != null ? stat : DEFAULT_OUTSOURCE_DAYS;
    }

    private BigDecimal purchaseCycleOfMaterial(Material m) {
        BigDecimal stat = statDays(T_MATERIAL, m.getId(), ItemCycleStat.TYPE_PURCHASE);
        if (stat != null) return stat;
        if (m.getPurchaseLeadDays() != null && m.getPurchaseLeadDays() > 0) {
            return BigDecimal.valueOf(m.getPurchaseLeadDays());
        }
        return DEFAULT_PURCHASE_DAYS;
    }

    private BigDecimal outsourceCycleOfMaterial(Material m) {
        if (m.getOutsourceCycleDays() != null) return m.getOutsourceCycleDays();
        BigDecimal stat = statDays(T_MATERIAL, m.getId(), ItemCycleStat.TYPE_OUTSOURCE);
        return stat != null ? stat : DEFAULT_OUTSOURCE_DAYS;
    }

    /** 生产单位周期（天/件）：material 上的手工值优先，其次历史统计，最后按产品提前期折算 */
    private BigDecimal productionCyclePerUnit(Product p) {
        BigDecimal stat = statDays(T_PRODUCT, p.getId(), ItemCycleStat.TYPE_PRODUCTION);
        if (stat != null && stat.compareTo(BigDecimal.ZERO) > 0) return stat;
        if (p.getProductionLeadDays() != null && p.getProductionLeadDays() > 0) {
            // 已有字段是「整批提前期」，按 1 件折算会失真，这里当作「每 100 件的天数」估一个单位值
            return BigDecimal.valueOf(p.getProductionLeadDays())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
        return DEFAULT_PRODUCTION_PER_UNIT;
    }

    private BigDecimal statDays(String itemType, Long itemId, String cycleType) {
        ItemCycleStat s = cycleStatMapper.selectOne(new LambdaQueryWrapper<ItemCycleStat>()
                .eq(ItemCycleStat::getItemType, itemType)
                .eq(ItemCycleStat::getItemId, itemId)
                .eq(ItemCycleStat::getCycleType, cycleType)
                .last("limit 1"));
        return s == null ? null : s.getAvgDays();
    }

    // ==================== 库存 / BOM ====================

    /** 可用库存 = 现存量 − 已占用 */
    private BigDecimal availableQty(String itemType, Long itemId) {
        List<Inventory> list = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getItemId, itemId)
                .eq(Inventory::getItemType, itemType));
        return list.stream()
                .map(i -> nz(i.getQuantity()).subtract(nz(i.getReservedQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safetyStockOf(String itemType, Long itemId) {
        if (T_MATERIAL.equals(itemType)) {
            Material m = materialMapper.selectById(itemId);
            return m == null ? BigDecimal.ZERO : nz(m.getSafetyStock());
        }
        Product p = productMapper.selectById(itemId);
        return p == null ? BigDecimal.ZERO : nz(p.getSafetyStock());
    }

    private Bom activeBom(Long productId) {
        return bomMapper.selectOne(new LambdaQueryWrapper<Bom>()
                .eq(Bom::getProductId, productId)
                .eq(Bom::getStatus, 1)
                .orderByDesc(Bom::getId)
                .last("limit 1"));
    }

    // ==================== 落库 ====================

    private void persist(String bizType, Long bizId, DeliveryRiskRow row) {
        DeliveryRisk old = riskMapper.selectOne(new LambdaQueryWrapper<DeliveryRisk>()
                .eq(DeliveryRisk::getBizType, bizType)
                .eq(DeliveryRisk::getBizId, bizId)
                .eq(DeliveryRisk::getItemId, row.getItemId())
                .last("limit 1"));

        DeliveryRisk dr = old != null ? old : new DeliveryRisk();
        dr.setBizType(bizType);
        dr.setBizId(bizId);
        dr.setItemId(row.getItemId());
        dr.setItemType(T_PRODUCT);
        dr.setQty(row.getQuantity());
        dr.setDeliveryDate(row.getDeliveryDate());
        dr.setRiskLevel(row.getRiskLevel());
        dr.setEarliestActionDate(row.getEarliestActionDate());
        dr.setSuggestDate(row.getSuggestDate());
        dr.setStockQty(row.getStockQty());
        dr.setShortageQty(row.getShortageQty());
        dr.setCycleDays(row.getCycleDays());
        dr.setCalcTime(LocalDateTime.now());
        try {
            Map<String, Object> payload = new LinkedHashMap<>(row.getDetail() == null ? Map.of() : row.getDetail());
            payload.put("shortageItems", row.getShortageItems());
            dr.setDetailJson(json.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("交期风险评估明细序列化失败：{}", e.getMessage());
        }
        if (old == null) riskMapper.insert(dr);
        else riskMapper.updateById(dr);
    }

    // ==================== 小工具 ====================

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
