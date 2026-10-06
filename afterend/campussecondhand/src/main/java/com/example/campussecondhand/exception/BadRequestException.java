package com.example.campussecondhand.exception;

/**
 * 客户端传入的参数不合法（业务层面可预期的错误）。
 *
 * <p>与 {@code IllegalArgumentException} 的区别是：本异常代表"请求本身有问题"，
 * 需要以 HTTP 400 返回给调用方，而不是落到兜底分支变成 500。
 * Controller 内不再各自拼 400 响应体，统一抛出本异常，
 * 由 {@code GlobalExceptionHandler} 渲染。</p>
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}