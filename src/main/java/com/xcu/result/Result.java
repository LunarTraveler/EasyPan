package com.xcu.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private String status;
    private Integer code; //编码：200成功，500和其它数字为失败（这里就比较笼统了）
    private String info; //错误信息（这里是比较详细的）
    private T data; //数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = 200;
        result.status = "success";
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 200;
        result.status = "success";
        return result;
    }

    public static <T> Result<T> fail(String msg) {
        Result result = new Result();
        result.info = msg;
        result.code = 500;
        result.status = "failed";
        return result;
    }

}
