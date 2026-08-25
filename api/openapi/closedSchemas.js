const HTTP_METHODS = new Set(["post", "put", "patch", "delete"]);

function inspectSchema(schema, path, results, visited) {
  if (typeof schema !== "object" || schema === null || visited.has(schema)) {
    return;
  }
  visited.add(schema);

  if (schema.type === "object" && schema.additionalProperties !== false) {
    results.push({
      message: "Los esquemas de objeto deben declarar additionalProperties: false.",
      path,
    });
  }

  for (const [name, property] of Object.entries(schema.properties ?? {})) {
    inspectSchema(property, [...path, "properties", name], results, visited);
  }
  if (schema.items) {
    inspectSchema(schema.items, [...path, "items"], results, visited);
  }
  for (const composition of ["allOf", "anyOf", "oneOf"]) {
    for (const [index, member] of (schema[composition] ?? []).entries()) {
      inspectSchema(member, [...path, composition, index], results, visited);
    }
  }
}

module.exports = (document) => {
  const results = [];
  const visited = new WeakSet();

  for (const [name, schema] of Object.entries(document.components?.schemas ?? {})) {
    inspectSchema(schema, ["components", "schemas", name], results, visited);
  }

  for (const [path, pathItem] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!HTTP_METHODS.has(method) || typeof operation !== "object" || operation === null) {
        continue;
      }
      for (const [mediaType, media] of Object.entries(operation.requestBody?.content ?? {})) {
        inspectSchema(media?.schema, ["paths", path, method, "requestBody", "content", mediaType, "schema"], results, visited);
      }
    }
  }

  return results;
};
