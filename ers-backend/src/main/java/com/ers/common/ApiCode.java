package com.ers.common;

/**
 * 业务错误码（与《API 接口契约》§1.6 一一对应）。
 */
public final class ApiCode {

    private ApiCode() {
    }

    /** 成功 */
    public static final int SUCCESS = 0;
    /** 参数校验失败 */
    public static final int BAD_REQUEST = 40001;
    /** 未登录 / 令牌失效 */
    public static final int UNAUTHORIZED = 40100;
    /** 无权限 */
    public static final int FORBIDDEN = 40300;
    /** 信誉分不足 */
    public static final int CREDIT_INSUFFICIENT = 40301;
    /** 资源不存在 */
    public static final int NOT_FOUND = 40400;
    /** 状态冲突 */
    public static final int CONFLICT = 40900;
    /** 业务规则不满足 */
    public static final int UNPROCESSABLE = 42200;
    /** 服务器内部错误 */
    public static final int SERVER_ERROR = 50000;

    /** 业务码 -> HTTP 状态码 */
    public static int httpStatus(int code) {
        return switch (code) {
            case SUCCESS -> 200;
            case BAD_REQUEST -> 400;
            case UNAUTHORIZED -> 401;
            case FORBIDDEN, CREDIT_INSUFFICIENT -> 403;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case UNPROCESSABLE -> 422;
            default -> 500;
        };
    }
}
