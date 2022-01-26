package io.resttestgen.core.datatype;

import java.util.Objects;

public class HTTPStatusCode {

    private Integer code;

    public HTTPStatusCode(Integer code) {
        this.code = code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public boolean isSuccessful() {
        return code != null && code >= 200 && code < 300;
    }

    public boolean isClientError() {
        return code != null && code >= 400 && code < 500;
    }

    public boolean isServerError() {
        return code != null && code >= 500 && code < 600;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTTPStatusCode that = (HTTPStatusCode) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
