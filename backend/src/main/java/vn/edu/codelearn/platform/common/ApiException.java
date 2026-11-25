package vn.edu.codelearn.platform.common;

import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {

    private final HttpStatusCode status;
    private final String code;

    public ApiException(HttpStatusCode status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public HttpStatusCode status() {
        return status;
    }

    public String code() {
        return code;
    }
}
