package com.vgrunning.runnermanagement.domain;

import java.util.UUID;

/** Identidad de un corredor tal como se persiste y se devuelve al crearlo. */
public record Runner(UUID id, RunnerName givenName, RunnerName familyName, String status) {}
