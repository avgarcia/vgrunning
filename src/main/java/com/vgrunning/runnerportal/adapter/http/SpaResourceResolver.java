package com.vgrunning.runnerportal.adapter.http;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Sirve recursos reales y limita el fallback de la SPA a rutas de cliente. */
final class SpaResourceResolver extends PathResourceResolver {

    private static final String INDEX = "index.html";

    @Override
    @Nullable
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        if (resourcePath.isBlank()) {
            return super.getResource(INDEX, location);
        }

        Resource resource = super.getResource(resourcePath, location);
        if (resource != null) {
            return resource;
        }
        if (isClientRoute(resourcePath)) {
            return super.getResource(INDEX, location);
        }
        return null;
    }

    private static boolean isClientRoute(String resourcePath) {
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        if (isReservedPath(normalizedPath)) {
            return false;
        }
        String finalSegment = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        return !finalSegment.contains(".");
    }

    private static boolean isReservedPath(String path) {
        return path.equals("api") || path.startsWith("api/")
            || path.equals("actuator") || path.startsWith("actuator/")
            || path.equals("assets") || path.startsWith("assets/")
            || path.equals("error") || path.startsWith("error/")
            || path.equals("webjars") || path.startsWith("webjars/");
    }
}
