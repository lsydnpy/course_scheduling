package com.xy.course_scheduling.advice;


import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.exception.MyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MyControllerAdivce {
    @ExceptionHandler(MyException.class)
    public Result<String> handleMyException(MyException e) {
        e.printStackTrace();
        return Result.fail(e.getCode(),e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail(500,"服务器内部错误！");
    }
}
