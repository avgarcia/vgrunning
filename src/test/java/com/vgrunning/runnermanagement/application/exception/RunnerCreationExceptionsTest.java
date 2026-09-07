package com.vgrunning.runnermanagement.application.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RunnerCreationExceptionsTest {
    @Test
    void exposesOnlyTheStablePublicRunnerCreationFailures() {
        assertThat(new IdempotencyKeyReusedException())
                .extracting(IdempotencyKeyReusedException::code, Throwable::getMessage)
                .containsExactly(
                        "idempotency_key_reused",
                        "La clave de idempotencia ya se ha usado con otra solicitud.");
        assertThat(new InvalidRunnerCreationException())
                .extracting(InvalidRunnerCreationException::code, Throwable::getMessage)
                .containsExactly("invalid_request", "La solicitud no es válida.");
        assertThat(new RunnerCreationForbiddenException())
                .extracting(RunnerCreationForbiddenException::code, Throwable::getMessage)
                .containsExactly(
                        "runner_creation_forbidden", "No tienes permiso para crear corredores.");
    }
}
