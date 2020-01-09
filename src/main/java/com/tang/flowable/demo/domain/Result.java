package com.tang.flowable.demo.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@ApiModel(value = "Result",description = "返回结果集")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 8514961063379279722L;

    private static final int SUCCESS_CODE = 200;

    private static final String OK = "OK";

    @ApiModelProperty("状态码:200成功")
    private int code = SUCCESS_CODE;

    @ApiModelProperty("状态消息")
    private String message = OK;

    @ApiModelProperty(value = "数据")
    private T data;

    public Result() {
    }

    public Result(T data) {
        this.data = data;
    }

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Result<T> withCode(int code) {
        this.code = code;
        return this;
    }

    public Result<T> withStatus(HttpStatus status) {
        this.code = status.value();
        return this;
    }

    public Result<T> withMessage(String message) {
        this.message = message;
        return this;
    }

    public Result<T> withData(T data) {
        this.data = data;
        return this;
    }

    public Result<T> ok() {
        this.code = SUCCESS_CODE;
        this.message = OK;
        return this;
    }

    @Override
	public String toString() {
		return "Result [code=" + code + ", message=" + message + ", data="
				+ data + "]";
	}
    
}
