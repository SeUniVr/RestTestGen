package io.resttestgen.core.datatype;

import java.util.Objects;

public class HttpStatusCode {

    private int code;

    /**
     * Constructor.
     * @param code the status code.
     * @throws IllegalArgumentException if the provided status code is not in the range 100-599.
     */
    public HttpStatusCode(int code) throws IllegalArgumentException {
        setCode(code);
    }

    /**
     * Set the status code.
     * @param code the status code to set.
     * @throws IllegalArgumentException if the provided status code is not in the range 100-599.
     */
    public void setCode(int code) throws IllegalArgumentException {
        if (code >= 100 && code < 600) {
            this.code = code;
        } else {
            throw new IllegalArgumentException("A status code must be between 100 and 599.");
        }
    }

    public int getCode() {
        return code;
    }

    /**
     * A status code is informational if it ranges between 100 and 199.
     * @return true if the status code is in range 100-199.
     */
    public boolean isInformational() {
        return code >= 100 && code < 200;
    }

    /**
     * A status code is successful if it ranges between 200 and 299.
     * @return true if the status code is in range 200-299.
     */
    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    /**
     * A status code represents a redirection if it ranges between 300 and 399.
     * @return true if the status code is in range 300-399.
     */
    public boolean isRedirection() {
        return code >= 300 && code < 400;
    }

    /**
     * A status code represents a client error if it ranges between 400 and 499.
     * @return true if the status code is in range 400-499.
     */
    public boolean isClientError() {
        return code >= 400 && code < 500;
    }

    /**
     * A status code represents a server error if it ranges between 500 and 599.
     * @return true if the status code is in range 500-599.
     */
    public boolean isServerError() {
        return code >= 500 && code < 600;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpStatusCode that = (HttpStatusCode) o;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return Integer.toString(code);
    }
}
