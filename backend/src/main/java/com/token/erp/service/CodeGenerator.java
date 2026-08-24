package com.token.erp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务编号生成服务
 * 规则：业务前缀 + YYYYMMDD + 4位流水号，按日连续自增
 * 例如：WL202608230001
 */
@Service
@RequiredArgsConstructor
public class CodeGenerator {

    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成编号：prefix + 日期 + 4位流水
     */
    public String next(String prefix) {
        String date = LocalDate.now().format(DF);
        String key = "code:seq:" + prefix + ":" + date;
        // 按日自增，每日从 1 开始
        Long seq = redisTemplate.opsForValue().increment(key);
        // 首次设置过期时间（跨天自动过期）
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, java.time.Duration.ofHours(48));
        }
        return prefix + date + String.format("%04d", seq);
    }
}
