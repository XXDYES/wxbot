package org.example.wxbot.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{} ", e.getMsg());
        return Result.error(e.getMsg());
    }

    // 兜底：其他所有未处理异常统一返回友好提示，避免直接暴露 500
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未处理的异常", e);
        return Result.error("服务器开小差了");
    }
}
