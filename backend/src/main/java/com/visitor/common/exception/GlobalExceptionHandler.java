package com.visitor.common.exception;

import com.visitor.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("数据完整性异常", e);
        String msg = e.getMessage();
        if (msg != null && msg.contains("Data too long")) {
            return Result.error("输入内容过长，请缩短");
        }
        return Result.error("数据保存失败，请检查必填字段是否完整");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleJsonError(HttpMessageNotReadableException e) {
        log.error("请求数据格式错误", e);
        return Result.error(400, "请求数据格式错误，请检查输入内容");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 详细异常日志记文件，便于排查
        log.error("系统异常", e);
        // 用户侧返回通用提示，避免泄露内部实现细节（SQL/堆栈/路径）
        return Result.error("系统繁忙，请稍后重试");
    }
}
