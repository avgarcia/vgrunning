package com.vgrunning.identityaccess.adapter.security;

/** Fallo técnico de origen o CSRF que el adaptador HTTP traduce a Problem Details. */
public final class CsrfValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
