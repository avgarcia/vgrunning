package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void filtersOnlyTheApprovedUnsafeSessionMethods() {
        OriginValidationFilter filter = filter(new AtomicReference<>());

        assertThat(filter.shouldNotFilter(request("GET", "http", "localhost", 80))).isTrue();
        assertThat(filter.shouldNotFilter(request("PUT", "http", "localhost", 80))).isTrue();
        assertThat(filter.shouldNotFilter(request("POST", "http", "localhost", 80))).isFalse();
        assertThat(filter.shouldNotFilter(request("DELETE", "http", "localhost", 80))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("matchingOrigins")
    void acceptsAbsentOrExactlyMatchingOrigins(String scheme, int port, String origin)
            throws ServletException, IOException {
        AtomicReference<Exception> resolved = new AtomicReference<>();
        OriginValidationFilter filter = filter(resolved);
        MockHttpServletRequest request = request("POST", scheme, "running-coach.local", port);
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
        MockHttpServletRequest request = request("DELETE", "https", "running-coach.local", 443);
        request.addHeader("Origin", origin);
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isFalse();
        assertThat(resolved.get()).isInstanceOf(CsrfValidationException.class);
    }

    private static Stream<Arguments> matchingOrigins() {
        return Stream.of(
                Arguments.of("http", 80, null),
                Arguments.of("http", 80, "http://running-coach.local"),
                Arguments.of("https", 443, "https://running-coach.local"),
                Arguments.of("https", 8443, "https://RUNNING-COACH.LOCAL:8443"));
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
                Arguments.of("https://[invalid"));
    }

    private static OriginValidationFilter filter(AtomicReference<Exception> resolved) {
        HandlerExceptionResolver resolver =
                (request, response, handler, exception) -> {
                    resolved.set(exception);
                    return null;
                };
        return new OriginValidationFilter(resolver);
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
