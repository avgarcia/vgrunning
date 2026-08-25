const HTTP_METHODS = new Set(["get", "put", "post", "delete", "options", "head", "patch", "trace"]);

module.exports = (document) => {
  const seen = new Map();
  const results = [];

  for (const [path, pathItem] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!HTTP_METHODS.has(method) || typeof operation !== "object" || operation === null) {
        continue;
      }
      if (typeof operation.operationId !== "string" || operation.operationId.length === 0) {
        continue;
      }
      if (seen.has(operation.operationId)) {
        results.push({
          message: `operationId duplicado: ${operation.operationId}.`,
          path: ["paths", path, method, "operationId"],
        });
      } else {
        seen.set(operation.operationId, [path, method]);
      }
    }
  }

  return results;
};
