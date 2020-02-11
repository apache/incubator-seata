package com.vergilyn.examples.response;

import java.io.Serializable;

import com.vergilyn.examples.enums.RspStatusEnum;

import lombok.ToString;

@ToString
public class ObjectResponse<T> extends BaseResponse implements Serializable {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ObjectResponse<T> result(RspStatusEnum status){
        this.setStatus(status.getCode());
        this.setMessage(status.getMessage());

        return this;
    }
    public static ObjectResponse<Void> success(){
        return success(null);
    }

    public static <T> ObjectResponse<T> success(T data){
        ObjectResponse<T> response = new ObjectResponse<>();
        response.setData(data);
        response.result(RspStatusEnum.SUCCESS);

        return response;
    }

    public static ObjectResponse<Void> failure(RspStatusEnum status){
        ObjectResponse<Void> response = new ObjectResponse<>();
        response.setStatus(status.getCode());
        response.setMessage(status.getMessage());

        return response;
    }

    public static ObjectResponse<Void> failure(){
        return failure(null);
    }

    public static <T> ObjectResponse<T> failure(T data){
        ObjectResponse<T> response = new ObjectResponse<>();
        response.setData(data);
        response.result(RspStatusEnum.FAIL);

        return response;
    }
}
