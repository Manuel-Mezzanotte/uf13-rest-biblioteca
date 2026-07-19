package it.marconi.biblioteca.domain.response;

import com.fasterxml.jackson.annotation.JsonValue;

public enum APIResponseStatus {
    SUCCESS("success"),
    FAIL("fail"),
    ERROR("error");

    private final String value;

    APIResponseStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
