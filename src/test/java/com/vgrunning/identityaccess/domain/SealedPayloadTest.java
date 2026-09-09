package com.vgrunning.identityaccess.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SealedPayloadTest {

    @Test
    void exposesTheSameContentSuppliedToTheConstructor() {
        byte[] nonce = {1, 2, 3};
        byte[] ciphertext = {4, 5, 6};

        SealedPayload payload = new SealedPayload("key-1", nonce, ciphertext);

        assertThat(payload.keyId()).isEqualTo("key-1");
        assertThat(payload.nonce()).containsExactly(1, 2, 3);
        assertThat(payload.ciphertext()).containsExactly(4, 5, 6);
    }

    @Test
    void clonesTheArraysSoCallersCannotMutateStateAfterConstruction() {
        byte[] nonce = {1, 2, 3};
        SealedPayload payload = new SealedPayload("key-1", nonce, new byte[] {4, 5, 6});

        nonce[0] = 9;
        payload.nonce()[0] = 9;

        assertThat(payload.nonce()).containsExactly(1, 2, 3);
    }

    @Test
    void equalsAndHashCodeCompareArrayContentNotReference() {
        SealedPayload first = new SealedPayload("key-1", new byte[] {1, 2}, new byte[] {3, 4});
        SealedPayload same = new SealedPayload("key-1", new byte[] {1, 2}, new byte[] {3, 4});
        SealedPayload differentNonce =
                new SealedPayload("key-1", new byte[] {9, 9}, new byte[] {3, 4});
        SealedPayload differentCiphertext =
                new SealedPayload("key-1", new byte[] {1, 2}, new byte[] {9, 9});
        SealedPayload differentKeyId =
                new SealedPayload("key-2", new byte[] {1, 2}, new byte[] {3, 4});

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first).isNotEqualTo(differentNonce);
        assertThat(first).isNotEqualTo(differentCiphertext);
        assertThat(first).isNotEqualTo(differentKeyId);
        assertThat(first).isNotEqualTo("not-a-sealed-payload");
    }

    @Test
    void rejectsNullFields() {
        assertThatThrownBy(() -> new SealedPayload(null, new byte[] {1}, new byte[] {2}))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SealedPayload("key-1", null, new byte[] {2}))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SealedPayload("key-1", new byte[] {1}, null))
                .isInstanceOf(NullPointerException.class);
    }
}
