package io.github.benxincaomu.oa.base.web;

import com.github.benxincaomu.notry.code.ResponseCode;

public enum OaResponseCode implements ResponseCode {
    USERNAME_NOT_EXIST(10001,"用户名不存在"),
    TOKEN_NOT_EXIST(10002,"token不存在"),
    ;
    private int code;
    private String message;


    
    private OaResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
    
}
