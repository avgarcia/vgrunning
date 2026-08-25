const SENSITIVE_NAME = /(?:password|secret|token|api[-_]?key|authorization|credential|session|csrf)/i;
const SAFE_PLACEHOLDER = /^(?:<[^>]+>|\*+|synthetic(?:[-_ ].*)?|example(?:[-_ ].*)?|placeholder(?:[-_ ].*)?|test(?:[-_ ].*)?)$/i;
const SECRET_VALUE = /^(?:Bearer\s+|Basic\s+|sk-|-----BEGIN\s+|eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.)/i;

function inspectExample(value, path, results, sensitiveContext = false) {
  if (typeof value === "string") {
    if (SECRET_VALUE.test(value) || (sensitiveContext && !SAFE_PLACEHOLDER.test(value))) {
      results.push({
        message: "El ejemplo contiene una credencial o secreto plausible; usa un marcador sintético.",
        path,
      });
    }
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => inspectExample(item, [...path, index], results, sensitiveContext));
    return;
  }
  if (typeof value !== "object" || value === null) {
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    inspectExample(child, [...path, key], results, sensitiveContext || SENSITIVE_NAME.test(key));
  }
}

function walk(node, path, results) {
  if (typeof node !== "object" || node === null) {
    return;
  }
  if (Array.isArray(node)) {
    node.forEach((item, index) => walk(item, [...path, index], results));
    return;
  }
  for (const [key, value] of Object.entries(node)) {
    if (key === "example") {
      const sensitiveContext = path.some((segment) => typeof segment === "string" && SENSITIVE_NAME.test(segment));
      inspectExample(value, [...path, key], results, sensitiveContext);
    } else if (key === "examples" && typeof value === "object" && value !== null) {
      for (const [name, example] of Object.entries(value)) {
        const payload = typeof example === "object" && example !== null && Object.hasOwn(example, "value")
          ? example.value
          : example;
        inspectExample(payload, [...path, key, name], results);
      }
    } else {
      walk(value, [...path, key], results);
    }
  }
}

module.exports = (document) => {
  const results = [];
  walk(document, [], results);
  return results;
};
