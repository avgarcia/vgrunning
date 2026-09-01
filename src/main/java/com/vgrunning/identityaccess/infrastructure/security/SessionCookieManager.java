package com.vgrunning.identityaccess.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/** Emite y elimina exclusivamente la cookie opaca aprobada para la sesión. */
public final class SessionCookieManager {
    public static final String SESSION_COOKIE = "__Host-pmv_session";

    public void write(String rawToken, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(rawToken).toString());
    }

    public void expire(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie("").mutate().maxAge(Duration.ZERO).build().toString());
    }

    private static ResponseCookie cookie(String value) {
        return ResponseCookie.from(SESSION_COOKIE, value)
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .build();
    }
}
