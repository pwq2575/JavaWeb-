package com.ers.common;

/**
 * 业务异常：由 service 层抛出，GlobalExceptionHandler 统一转换为 ApiResponse。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 默认 42200（业务规则不满足） */
    public BizException(String message) {
        this(ApiCode.UNPROCESSABLE, message);
    }

    public int getCode() {
        return code;
    }

    public static BizException badRequest(String message) {
        return new BizException(ApiCode.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ApiCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ApiCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ApiCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ApiCode.CONFLICT, message);
    }
}
