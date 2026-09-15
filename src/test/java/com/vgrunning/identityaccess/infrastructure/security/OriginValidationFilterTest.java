package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

class OriginValidationFilterTest {

    private static final String CANONICAL_ORIGIN = "https://running-coach.local";

    @Test
    void filtersOnlyTheApprovedUnsafeSessionMethods() {
        OriginValidationFilter filter = filter(new AtomicReference<>());

        assertThat(filter.shouldNotFilter(request("GET", "http", "localhost", 8080))).isTrue();
        assertThat(filter.shouldNotFilter(request("PUT", "http", "localhost", 8080))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "http", "localhost", 8080))).isFalse();
        assertThat(filter.shouldNotFilter(request("DELETE", "http", "localhost", 8080))).isFalse();
    }

    /**
     * La petición llega tal como la ve el contenedor detrás de un proxy TLS-terminating: HTTP, host
     * y puerto internos. El Origin público que envía el navegador es el que debe decidir, no esos
     * datos de la conexión interna.
     */
    @ParameterizedTest
    @MethodSource("matchingOrigins")
    void acceptsAbsentOrExactlyMatchingOriginsBehindATlsTerminatingProxy(String origin)
            throws ServletException, IOException {
        AtomicReference<Exception> resolved = new AtomicReference<>();
        OriginValidationFilter filter = filter(resolved);
        MockHttpServletRequest request = request("POST", "http", "localhost", 8080);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(resolved).hasValue(null);
    }

    @ParameterizedTest
    @MethodSource("rejectedOrigins")
    void rejectsMalformedOrCrossOriginValues(String origin) throws ServletException, IOException {
        AtomicReference<Exception> resolved = new AtomicReference<>();
        OriginValidationFilter filter = filter(resolved);
        MockHttpServletRequest request = request("DELETE", "http", "localhost", 8080);
        request.addHeader("Origin", origin);
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(resolved.get()).isInstanceOf(CsrfValidationException.class);
    }

    @Test
    void rejectsAMissingOrInvalidPublicOriginAtConstruction() {
        HandlerExceptionResolver resolver = (request, response, handler, exception) -> null;

        assertThatThrownBy(() -> new OriginValidationFilter(resolver, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new OriginValidationFilter(resolver, "  "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new OriginValidationFilter(resolver, "ftp://running-coach.local"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new OriginValidationFilter(resolver, "https:opaque"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Stream<Arguments> matchingOrigins() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of("https://running-coach.local"),
                Arguments.of("HTTPS://RUNNING-COACH.LOCAL"),
                Arguments.of("https://running-coach.local:443"));
    }

    private static Stream<Arguments> rejectedOrigins() {
        return Stream.of(
                Arguments.of("https://user@running-coach.local"),
                Arguments.of("https://running-coach.local/path"),
                Arguments.of("https://running-coach.local?query=true"),
                Arguments.of("https://running-coach.local#fragment"),
                Arguments.of("http://running-coach.local"),
                Arguments.of("https://other.invalid"),
                Arguments.of("https://running-coach.local:8443"),
                Arguments.of("https://[invalid"),
                Arguments.of("https:invalid"),
                Arguments.of("ftp://running-coach.local"));
    }

    private static OriginValidationFilter filter(AtomicReference<Exception> resolved) {
        HandlerExceptionResolver resolver =
                (request, response, handler, exception) -> {
                    resolved.set(exception);
                    return null;
                };
        return new OriginValidationFilter(resolver, CANONICAL_ORIGIN);
    }

    private static MockHttpServletRequest request(
            String method, String scheme, String host, int port) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/sessions");
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        return request;
    }
}
