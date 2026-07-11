package com.chm.aiagent.exception;

import com.chm.aiagent.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截 Controller 层抛出的异常，转换为 Result 响应。
 * 优先级：具体异常 > BusinessException > Exception
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 自定义业务异常 —— 所有 BusinessException 子类统一走这里
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] path={}, code={}, message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验 ====================

    /**
     * JSR-303 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] path={}, detail={}", request.getRequestURI(), detail);
        return Result.fail(ErrorCode.VALIDATION_ERROR, detail);
    }

    /**
     * 缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[缺少参数] path={}, param={}", request.getRequestURI(), e.getParameterName());
        return Result.fail(ErrorCode.BAD_REQUEST, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("[参数类型错误] path={}, name={}, value={}", request.getRequestURI(), e.getName(), e.getValue());
        return Result.fail(ErrorCode.BAD_REQUEST, "参数类型错误: " + e.getName());
    }

    /**
     * 请求体不可读
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("[请求体解析失败] path={}", request.getRequestURI());
        return Result.fail(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    // ==================== 资源 ====================

    /**
     * 404 —— 静态资源 / 不存在的接口
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("[资源不存在] path={}", request.getRequestURI());
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    // ==================== 兜底 ====================

    /**
     * 未知异常兜底
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] path={}", request.getRequestURI(), e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
