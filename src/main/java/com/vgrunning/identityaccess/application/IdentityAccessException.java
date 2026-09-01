package com.vgrunning.identityaccess.application;

/** Fallo semántico de identity-access que no conoce su representación HTTP. */
public abstract class IdentityAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;

    protected IdentityAccessException(String code, String title) {
        super(title);
        this.code = code;
    }

    public final String code() {
        return code;
    }
}
