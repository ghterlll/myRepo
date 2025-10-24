
package com.mobile.aura.support;

import com.mobile.aura.constant.CommonStatusEnum;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(CommonStatusEnum e) {
        super(e.getValue());
        this.code = e.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
