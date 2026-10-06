// 全局异常处理器
package com.example.campussecondhand.exception;

import com.example.campussecondhand.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 缺少 Authorization 请求头：属于客户端未登录，不应伪装成服务端错误。
     * 注意：该异常在进入 Controller 方法体之前抛出，因此方法内部的 try/catch 无法捕获。
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("缺少请求头: {}", e.getHeaderName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "请先登录"));
    }

    /**
     * @Valid 校验失败：把具体字段提示透出，而不是笼统的 500。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage() == null ? f.getField() : f.getDefaultMessage())
                .collect(Collectors.joining("；"));
        if (detail.isEmpty()) {
            detail = "请求参数不合法";
        }
        log.warn("参数校验失败: {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        // 必须打印堆栈，否则线上 500 无法定位（此前 register 的 500 就是被这里静默吞掉）
        log.error("未处理异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }
}
