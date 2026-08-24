package com.vgrunning.modulithfixture.invalid.consumer;

import com.vgrunning.modulithfixture.invalid.source.internal.InternalSourceType;

final class ForbiddenInternalDependency {

    private final InternalSourceType source = new InternalSourceType();

    InternalSourceType source() {
        return source;
    }
}
