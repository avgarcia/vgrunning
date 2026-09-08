package com.vgrunning.runnermanagement.application.service;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.runnermanagement.application.exception.IdempotencyKeyReusedException;
import com.vgrunning.runnermanagement.application.exception.InvalidRunnerCreationException;
import com.vgrunning.runnermanagement.application.exception.RunnerCreationForbiddenException;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.DigestPort;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import com.vgrunning.runnermanagement.domain.Runner;
import com.vgrunning.runnermanagement.domain.RunnerName;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Coordina el alta atómica sin filtrar HTTP o SQL a la aplicación. */
@RequiredArgsConstructor
public class CreateRunnerService implements CreateRunnerUseCase {
    private final AccountProvisioningApi accounts;
    private final RunnerCreationRepository runners;
    private final DigestPort digest;

    @Override
    @Transactional
    public Runner create(ActorContext actor, UUID idempotencyKey, CreateRunner command) {
        if (!actor.isAdministrator()) {
            throw new RunnerCreationForbiddenException();
        }
        CreateRunner normalized = normalize(command);
        byte[] fingerprint = fingerprint(normalized);
        return runners.reserve(actor.accountId(), idempotencyKey, fingerprint)
                .map(existing -> replay(existing, fingerprint))
                .orElseGet(() -> createNew(actor, idempotencyKey, normalized));
    }

    private Runner createNew(ActorContext actor, UUID idempotencyKey, CreateRunner command) {
        UUID runnerId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        ProvisionedRunnerAccount account =
                accounts.provision(
                        new ProvisionRunnerAccount(command.email(), actor, correlationId));
        Runner stored =
                runners.create(
                        new RunnerCreationRepository.NewRunner(
                                runnerId,
                                account.accountId(),
                                command.givenName(),
                                command.familyName(),
                                actor.accountId(),
                                correlationId,
                                account.activationExpiresAt()));
        runners.complete(actor.accountId(), idempotencyKey, stored);
        return stored;
    }

    private Runner replay(RunnerCreationRepository.StoredCreation existing, byte[] fingerprint) {
        if (!MessageDigest.isEqual(existing.fingerprint(), fingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return existing.runner();
    }

    private static CreateRunner normalize(CreateRunner command) {
        if (!Boolean.TRUE.equals(command.adultDeclarationConfirmed())) {
            throw new InvalidRunnerCreationException();
        }
        return new CreateRunner(
                normalizedName(command.givenName()).value(),
                normalizedName(command.familyName()).value(),
                normalizedText(command.email()).toLowerCase(Locale.ROOT),
                true);
    }

    private static RunnerName normalizedName(String value) {
        try {
            return RunnerName.from(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRunnerCreationException();
        }
    }

    private static String normalizedText(String value) {
        if (value == null) {
            throw new InvalidRunnerCreationException();
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (normalized.isEmpty()) {
            throw new InvalidRunnerCreationException();
        }
        return normalized;
    }

    private byte[] fingerprint(CreateRunner command) {
        return digest.sha256(
                command.givenName() + "\n" + command.familyName() + "\n" + command.email());
    }
}
