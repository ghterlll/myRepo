// support/GlobalExceptionHandler.java
package com.mobile.aura.support;

import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.ResponseResult;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseResult<?> handleBiz(BizException e) {
        return ResponseResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<?> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null ?
                e.getBindingResult().getFieldError().getDefaultMessage() :
                CommonStatusEnum.INVALID_PARAM.getValue();
        return ResponseResult.fail(CommonStatusEnum.INVALID_PARAM.getCode(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult<?> handleIllegalArg() {
        return ResponseResult.fail(CommonStatusEnum.UNAUTHORIZED.getCode(), CommonStatusEnum.UNAUTHORIZED.getValue());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseResult<?> handleExpired() {
        return ResponseResult.fail(CommonStatusEnum.TOKEN_EXPIRED.getCode(), CommonStatusEnum.TOKEN_EXPIRED.getValue());
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseResult<?> handleJwt() {
        return ResponseResult.fail(CommonStatusEnum.TOKEN_INVALID.getCode(), CommonStatusEnum.TOKEN_INVALID.getValue());
    }
}
