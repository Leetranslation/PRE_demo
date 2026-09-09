package com.token.erp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 系统参数读取服务（sys_config 表）
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final JdbcTemplate jdbcTemplate;

    /** 读取字符串配置，不存在返回 null */
    public String getString(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT config_value FROM sys_config WHERE config_key=?", String.class, key);
        } catch (Exception e) {
            return null;
        }
    }

    /** 读取布尔配置，不存在返回 false */
    public boolean getBoolean(String key) {
        String v = getString(key);
        return "true".equalsIgnoreCase(v);
    }
}
