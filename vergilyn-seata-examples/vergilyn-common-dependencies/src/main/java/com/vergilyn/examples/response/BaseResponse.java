package com.vergilyn.examples.response;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BaseResponse implements Serializable {

    private int status = 200;

    private String message;
}
