package com.chm.aiagent.common;

import com.chm.aiagent.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应封装
 *
 * @param <T> 响应数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private Long timestamp;

    private Result() {
    }

    // ========== 静态工厂方法 ==========

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String detail) {
        return fail(errorCode.getCode(), errorCode.getMessage() + ": " + detail);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    // ========== 便捷判断 ==========

    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
