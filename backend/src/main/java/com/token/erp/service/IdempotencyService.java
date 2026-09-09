package com.token.erp.service;

import com.token.erp.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 幂等控制服务（文档 20.1：入库、出库、领料、退料必须防止重复提交，建议使用幂等键）
 *
 * 方案：前端提交时生成 requestId（UUID），后端用 Redis setIfAbsent 保证同一 requestId 只处理一次。
 * 键保留 24 小时（覆盖可能的网络重试窗口）。
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "idempotent:";
    private static final Duration TTL = Duration.ofHours(24);

    /**
     * 校验并占用幂等键
     * @param requestId 前端生成的唯一请求标识
     * @param biz 业务类型（如 purchase_inbound），用于拼接键
     */
    public void checkAndOccupy(String requestId, String biz) {
        // requestId 必传：缺失则拒绝，防止绕过幂等保护导致重复入账
        if (requestId == null || requestId.isBlank()) {
            throw new BizException(400, "缺少请求标识 requestId，无法完成防重复校验");
        }
        String key = KEY_PREFIX + biz + ":" + requestId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.FALSE.equals(success)) {
            throw new BizException(409, "请勿重复提交");
        }
    }
}
