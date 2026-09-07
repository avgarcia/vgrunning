package com.vgrunning.runnermanagement.infrastructure.input.web;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.RunnersApi;
import org.vgrunning.generated.openapi.server.model.Runner;
import org.vgrunning.generated.openapi.server.model.RunnerCreation;

/** Entrada HTTP del alta administrativa de un corredor pendiente de activación. */
@RestController
@RequiredArgsConstructor
public class RunnerHttpController implements RunnersApi {
    private final CreateRunnerUseCase createRunner;
    private final RunnerWebMapper mapper;
    private final HttpServletRequest request;

    @Override
    public ResponseEntity<Runner> createRunner(UUID idempotencyKey, RunnerCreation runnerCreation) {
        ActorContext actor = (ActorContext) request.getAttribute(ActorContext.class.getName());
        var runner = createRunner.create(actor, idempotencyKey, mapper.toCommand(runnerCreation));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/runners/" + runner.id())
                .body(mapper.toResponse(runner));
    }
}
