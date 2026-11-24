package vn.edu.codelearn.platform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return problem(HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedError(Exception exception, WebRequest request) {
        LOG.error("Unhandled API error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            LOG.error("Unhandled API error", exception);
        }
        return super.handleExceptionInternal(exception, problem(status, request), headers, status, request);
    }

    private ProblemDetail problem(HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        String code = switch (status.value()) {
            case 400 -> "VALIDATION_ERROR";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 500 -> "INTERNAL_ERROR";
            default -> "HTTP_" + status.value();
        };
        problem.setProperty("code", code);
        problem.setProperty("traceId", request.getAttribute(RequestIdFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
        return problem;
    }
}
