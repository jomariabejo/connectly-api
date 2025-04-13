package com.jomariabejo.connectly_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericResponse<T> {
    private String message;
    private T data;

    public GenericResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }
}
