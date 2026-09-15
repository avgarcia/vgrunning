package com.vgrunning;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Construye la representación Problem Details común a todos los módulos. */
public final class ProblemDetails {
    private ProblemDetails() {}

    public static ProblemDetail detail(HttpStatus status, String code, String title) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, title);
        detail.setType(URI.create("/problems/" + code.replace('_', '-')));
        detail.setTitle(title);
        detail.setProperty("code", code);
        return detail;
    }
}
