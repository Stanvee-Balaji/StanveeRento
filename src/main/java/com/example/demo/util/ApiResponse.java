package com.example.demo.util;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private int statusCode;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    public boolean isSuccess(){return success;}
    public void setSuccess(boolean v){success=v;}
    public String getMessage(){return message;}
    public void setMessage(String v){message=v;}
    public T getData(){return data;}
    public void setData(T v){data=v;}
    public int getStatusCode(){return statusCode;}
    public void setStatusCode(int v){statusCode=v;}

    public static <T> ApiResponse<T> success(String message, T data, int status) {
        return new ApiResponse<>(true,message,data,status);
    }

    public static <T> ApiResponse<T> error(String message, T data, int status) {
        return new ApiResponse<>(false,message,data,status);
    }
}
