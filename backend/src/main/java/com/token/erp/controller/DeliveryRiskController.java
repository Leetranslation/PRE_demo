package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.DeliveryRiskRow;
import com.token.erp.entity.DeliveryRisk;
import com.token.erp.service.DeliveryRiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 交期风险智能预警。
 *
 * 计算全在 {@link DeliveryRiskService}（确定性规则），这里只做接口暴露；
 * AI 服务只消费这里的结构化结果生成报告文字。
 */
@Tag(name = "AI交期风险预警")
@RestController
@RequestMapping("/api/sales/delivery-risk")
@RequiredArgsConstructor
public class DeliveryRiskController {

    private final DeliveryRiskService service;

    @Operation(summary = "评估单据交期风险（实时重算并覆盖缓存）")
    @PostMapping("/evaluate")
    public Result<List<DeliveryRiskRow>> evaluate(@RequestParam String bizType,
                                                  @RequestParam Long bizId) {
        return Result.success(service.evaluate(bizType, bizId));
    }

    @Operation(summary = "采纳 AI 建议交付日期（写回明细行）")
    @PostMapping("/apply")
    public Result<Integer> apply(@RequestBody ApplyRequest req) {
        return Result.success(service.apply(req.getBizType(), req.getBizId(), req.getItemIds()));
    }

    @Operation(summary = "交期风险分析（SSE 逐步推送分析过程 + 报告）")
    @GetMapping("/analyze-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter analyzeStream(
            @RequestParam String bizType,
            @RequestParam Long bizId) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(180_000L);
        service.streamAnalyze(bizType, bizId, emitter);
        return emitter;
    }

    @Operation(summary = "评估内存明细（新建单据未保存时用，不落库）")
    @PostMapping("/evaluate-inline")
    public Result<List<DeliveryRiskRow>> evaluateInline(
            @RequestBody List<DeliveryRiskService.InlineItem> items) {
        return Result.success(service.evaluateInline(items));
    }

    @Operation(summary = "内存明细流式分析（新建单据未保存时用）")
    @PostMapping("/analyze-inline")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter analyzeInline(
            @RequestBody List<DeliveryRiskService.InlineItem> items) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(180_000L);
        service.streamAnalyzeInline(items, emitter);
        return emitter;
    }

    @Operation(summary = "读取已缓存的评估结果")
    @GetMapping("/list")
    public Result<List<DeliveryRisk>> list(@RequestParam String bizType,
                                           @RequestParam Long bizId) {
        return Result.success(service.listRisk(bizType, bizId));
    }

    @Data
    public static class ApplyRequest {
        /** sales_order / quotation / contract */
        private String bizType;
        private Long bizId;
        /** 不传则全部有建议日期的行都替换 */
        private List<Long> itemIds;
    }
}
