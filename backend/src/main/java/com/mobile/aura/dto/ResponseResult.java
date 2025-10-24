package com.mobile.aura.dto;

import com.mobile.aura.constant.CommonStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResponseResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResponseResult<T> success() {
        return new ResponseResult<T>()
                .setCode(CommonStatusEnum.SUCCESS.getCode())
                .setMessage(CommonStatusEnum.SUCCESS.getValue());
    }

    public static <T> ResponseResult<T> success (T data) {
        return new ResponseResult<T>()
                .setCode(CommonStatusEnum.SUCCESS.getCode())
                .setMessage(CommonStatusEnum.SUCCESS.getValue())
                .setData(data);
    }

    public static <T> ResponseResult<T> fail (T data) {
        return new ResponseResult<T>()
                .setCode(CommonStatusEnum.FAIL.getCode())
                .setMessage(CommonStatusEnum.FAIL.getValue())
                .setData(data);
    }

    public static <T> ResponseResult<T> fail (int code, String message) {
        return new ResponseResult<T>().setCode(code).setMessage(message);
    }

    public static <T> ResponseResult<T> fail (int code, String message, T data) {
        return new ResponseResult<T>().setCode(code).setMessage(message).setData(data);
    }
}
