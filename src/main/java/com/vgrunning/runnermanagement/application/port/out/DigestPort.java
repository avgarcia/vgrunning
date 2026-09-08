package com.vgrunning.runnermanagement.application.port.out;

/** Calcula una huella criptográfica estable de un valor en claro. */
public interface DigestPort {
    byte[] sha256(String value);
}
