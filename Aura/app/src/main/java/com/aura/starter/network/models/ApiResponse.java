package com.aura.starter.network.models;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public boolean isSuccess() { return code == 1; } // SUCCESS code is 1 per backend runtime
    
    @Override
    public String toString() {
        return "ApiResponse{code=" + code + ", message='" + message + "', data=" + data + "}";
    }
}

