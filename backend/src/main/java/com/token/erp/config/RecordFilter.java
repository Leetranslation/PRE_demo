package com.token.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流量录制过滤器 —— 把每个 /api/** 的请求与响应**追加写到一个 JSONL 文件**，
 * 由自动化测试平台读取该文件（docker 卷挂载到宿主机），不依赖任何网络推送。
 *
 * <p>为什么不用 HTTP 推送：容器里 Java 的 HttpClient 连 host.docker.internal 会卡死
 * （IPv4/IPv6 + 内网代理的坑），写文件最稳。
 *
 * <p>铁律：绝不影响业务。写文件失败静默；响应体必须 copyBodyToResponse 回写。
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RecordFilter extends OncePerRequestFilter {

    @Value("${erp.record.enabled:true}")
    private boolean enabled;

    /** 容器内路径，compose 把它挂到宿主机的 D:\\erp\\_record 目录 */
    @Value("${erp.record.file:/app/record/record.jsonl}")
    private String recordFile;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Object lock = new Object();

    @jakarta.annotation.PostConstruct
    void init() {
        log.warn("[RECORD] filter init: enabled={}, file={}", enabled, recordFile);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !enabled || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper reqW = new ContentCachingRequestWrapper(req);
        ContentCachingResponseWrapper resW = new ContentCachingResponseWrapper(res);
        long t0 = System.currentTimeMillis();
        try {
            chain.doFilter(reqW, resW);
        } finally {
            long cost = System.currentTimeMillis() - t0;
            try {
                append(reqW, resW, cost);
            } catch (Exception e) {
                log.warn("[RECORD] append error {}", e.toString());
            }
            resW.copyBodyToResponse();
        }
    }

    private void append(ContentCachingRequestWrapper req, ContentCachingResponseWrapper res, long cost) {
        String reqBody = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
        String resBody = new String(res.getContentAsByteArray(), StandardCharsets.UTF_8);

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("method", req.getMethod());
        p.put("path", req.getRequestURI());
        p.put("query", req.getQueryString() == null ? "" : req.getQueryString());
        p.put("requestBody", reqBody);
        p.put("status", res.getStatus());
        p.put("responseBody", resBody);
        p.put("costMs", cost);
        p.put("at", System.currentTimeMillis());

        Map<String, String> headers = new LinkedHashMap<>();
        String auth = req.getHeader("Authorization");
        if (auth != null) {
            headers.put("Authorization", auth);
        }
        p.put("headers", headers);

        final String json;
        try {
            json = mapper.writeValueAsString(p);
        } catch (Exception e) {
            return;
        }

        synchronized (lock) {
            try {
                Path path = Paths.get(recordFile);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                Files.writeString(path, json + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                log.warn("[RECORD] write error {}", e.toString());
            }
        }
    }
}
