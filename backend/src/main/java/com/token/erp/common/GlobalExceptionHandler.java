package com.token.erp.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException manve) {
            if (manve.getBindingResult().getFieldError() != null) {
                msg = manve.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (e instanceof BindException be) {
            if (be.getBindingResult().getFieldError() != null) {
                msg = be.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return Result.error(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        // 返回真正的 HTTP 403，前端据此提示"无权限访问"（区别于 401 未登录）
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(403, "无权限访问"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器异常：" + e.getMessage());
    }
}
