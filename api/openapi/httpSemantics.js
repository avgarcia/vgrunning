const HTTP_METHODS = new Set(["get", "put", "post", "delete", "options", "head", "patch", "trace"]);
const SAFE_METHODS = new Set(["get", "head", "options"]);
const SUCCESS_RESPONSES = {
  get: ["200"],
  post: ["201", "202"],
  put: ["200", "204"],
  patch: ["200", "204"],
  delete: ["204"],
};

function resolveLocalReference(document, candidate) {
  if (typeof candidate?.$ref !== "string" || !candidate.$ref.startsWith("#/")) {
    return candidate;
  }

  return candidate.$ref
    .slice(2)
    .split("/")
    .map((segment) => segment.replace(/~1/g, "/").replace(/~0/g, "~"))
    .reduce((value, segment) => value?.[segment], document);
}

function operationParameters(document, pathItem, operation) {
  return [...(pathItem.parameters ?? []), ...(operation.parameters ?? [])]
    .map((parameter) => resolveLocalReference(document, parameter))
    .filter((parameter) => typeof parameter === "object" && parameter !== null);
}

function hasSecurityScheme(security, scheme) {
  return Array.isArray(security) && security.some((requirement) => Object.hasOwn(requirement ?? {}, scheme));
}

function hasCombinedSecurity(security, ...schemes) {
  return Array.isArray(security) && security.some((requirement) =>
    schemes.every((scheme) => Object.hasOwn(requirement ?? {}, scheme)));
}

function parameterSchema(document, parameter) {
  return resolveLocalReference(document, parameter?.schema);
}

function isCollectionResponse(document, responseCandidate) {
  const response = resolveLocalReference(document, responseCandidate);
  const schemaCandidate = response?.content?.["application/json"]?.schema;
  const schema = resolveLocalReference(document, schemaCandidate);
  if (schema?.type === "array") {
    return true;
  }
  const items = resolveLocalReference(document, schema?.properties?.items);
  return schema?.type === "object" && items?.type === "array";
}

function isProblemDetailsSchema(document, schemaCandidate) {
  const schema = resolveLocalReference(document, schemaCandidate);
  const required = new Set(schema?.required ?? []);
  return schema?.type === "object"
    && required.has("type")
    && required.has("title")
    && required.has("status")
    && schema.properties?.status?.type === "integer";
}

module.exports = (document) => {
  const results = [];

  for (const [path, pathItem] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!HTTP_METHODS.has(method) || typeof operation !== "object" || operation === null) {
        continue;
      }

      const operationPath = ["paths", path, method];
      const responses = operation.responses ?? {};
      const allowedSuccesses = SUCCESS_RESPONSES[method];
      if (allowedSuccesses && !allowedSuccesses.some((status) => Object.hasOwn(responses, status))) {
        results.push({
          message: `${method.toUpperCase()} debe declarar una respuesta de éxito ${allowedSuccesses.join(" o ")}.`,
          path: [...operationPath, "responses"],
        });
      }

      const errorResponses = Object.entries(responses)
        .filter(([status]) => status === "default" || /^[45][0-9]{2}$/.test(status));
      if (errorResponses.length === 0) {
        results.push({
          message: "La operación debe declarar al menos una respuesta de error.",
          path: [...operationPath, "responses"],
        });
      }
      for (const [status, responseCandidate] of errorResponses) {
        const response = resolveLocalReference(document, responseCandidate);
        const problemSchema = response?.content?.["application/problem+json"]?.schema;
        if (!isProblemDetailsSchema(document, problemSchema)) {
          results.push({
            message: `La respuesta ${status} debe usar application/problem+json con un esquema Problem Details.`,
            path: [...operationPath, "responses", status],
          });
        }
      }

      const parameters = operationParameters(document, pathItem, operation);
      const pathParameterNames = [...path.matchAll(/\{([^}]+)\}/g)].map((match) => match[1]);
      for (const name of pathParameterNames) {
        const parameter = parameters.find((candidate) => candidate.in === "path" && candidate.name === name);
        if (!parameter) {
          results.push({
            message: `El parámetro de ruta {${name}} debe estar declarado.`,
            path: operationPath,
          });
        } else if (parameter.required !== true) {
          results.push({
            message: `El parámetro de ruta {${name}} debe ser obligatorio.`,
            path: [...operationPath, "parameters"],
          });
        }
      }

      const effectiveSecurity = operation.security ?? document.security;
      const anonymous = Array.isArray(effectiveSecurity) && effectiveSecurity.length === 0;
      const sessionProtected = hasSecurityScheme(effectiveSecurity, "opaqueSession");
      const anonymousWithCsrf = !sessionProtected
        && hasCombinedSecurity(effectiveSecurity, "csrfCookie", "csrfToken");
      if (!anonymous && !sessionProtected && !anonymousWithCsrf) {
        results.push({
          message: "La operación debe declarar opaqueSession o el doble envío CSRF anónimo.",
          path: [...operationPath, "security"],
        });
      }
      if (SAFE_METHODS.has(method)
        && (hasSecurityScheme(effectiveSecurity, "csrfCookie")
          || hasSecurityScheme(effectiveSecurity, "csrfToken"))) {
        results.push({
          message: `${method.toUpperCase()} no debe exigir CSRF.`,
          path: [...operationPath, "security"],
        });
      }
      if (!SAFE_METHODS.has(method)) {
        const validCsrf = sessionProtected
          ? hasCombinedSecurity(effectiveSecurity, "opaqueSession", "csrfCookie", "csrfToken")
          : hasCombinedSecurity(effectiveSecurity, "csrfCookie", "csrfToken");
        if (!validCsrf) {
          results.push({
            message: sessionProtected
              ? `${method.toUpperCase()} protegido debe exigir opaqueSession, csrfCookie y csrfToken conjuntamente.`
              : `${method.toUpperCase()} anónimo debe exigir csrfCookie y csrfToken conjuntamente.`,
            path: [...operationPath, "security"],
          });
        }
      }

      if (method === "get" && isCollectionResponse(document, responses["200"])) {
        const cursor = parameters.find((parameter) => parameter.in === "query" && parameter.name === "cursor");
        const limit = parameters.find((parameter) => parameter.in === "query" && parameter.name === "limit");
        const limitSchema = parameterSchema(document, limit);
        if (!cursor || !limit || typeof limitSchema?.maximum !== "number" || limitSchema.maximum <= 0) {
          results.push({
            message: "Una colección debe declarar cursor y un limit con máximo positivo.",
            path: [...operationPath, "parameters"],
          });
        }
      }
    }
  }

  return results;
};
