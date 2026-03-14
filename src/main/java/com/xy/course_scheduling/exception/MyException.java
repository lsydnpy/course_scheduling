package com.xy.course_scheduling.exception;

public class MyException extends RuntimeException{
    private String message;
    private int code;

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public MyException(int code, String message) {
        super(message);
        this.code = code;
    }
}
