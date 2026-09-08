package com.vgrunning.runnermanagement.domain;

import java.text.Normalizer;

/** Nombre o apellido de un corredor, normalizado y no vacío. */
public record RunnerName(String value) {

    public RunnerName {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
    }

    /** Normaliza NFC y recorta espacios antes de aceptar el nombre. */
    public static RunnerName from(String suppliedName) {
        if (suppliedName == null) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        return new RunnerName(Normalizer.normalize(suppliedName, Normalizer.Form.NFC).trim());
    }
}
