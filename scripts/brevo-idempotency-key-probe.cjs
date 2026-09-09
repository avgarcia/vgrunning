#!/usr/bin/env node
// Prueba sintética de ADR-0031: ¿el header idempotencyKey de POST /v3/smtp/email
// suprime el envío físico duplicado, o solo devuelve un error de correlación?
//
// Uso:
//   BREVO_API_KEY=xxx BREVO_TEST_SENDER_EMAIL=verificado@tu-dominio \
//   BREVO_TEST_RECIPIENT_EMAIL=tu-correo@ejemplo.com node scripts/brevo-idempotency-key-probe.cjs
//
// BREVO_TEST_SENDER_EMAIL debe ser un remitente verificado en la cuenta de Brevo.
// No escribas la API key en este fichero ni en el historial de shell persistente.

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const apiKey = process.env.BREVO_API_KEY;
const senderEmail = process.env.BREVO_TEST_SENDER_EMAIL;
const senderName = process.env.BREVO_TEST_SENDER_NAME || "Running Coach - Prueba";
const recipientEmail = process.env.BREVO_TEST_RECIPIENT_EMAIL;

if (!apiKey || !senderEmail || !recipientEmail) {
  console.error(
    "Faltan variables de entorno. Requeridas: BREVO_API_KEY, BREVO_TEST_SENDER_EMAIL, BREVO_TEST_RECIPIENT_EMAIL.",
  );
  process.exit(1);
}

const idempotencyKey = crypto.randomUUID();

function buildPayload(callNumber) {
  return {
    sender: { name: senderName, email: senderEmail },
    to: [{ email: recipientEmail }],
    headers: { idempotencyKey },
    subject: `[Prueba ADR-0031] idempotencyKey ${idempotencyKey} — llamada ${callNumber}/2`,
    htmlContent: `<p>Contenido distinguible de la llamada ${callNumber} de 2. Misma idempotencyKey: ${idempotencyKey}.</p>`,
  };
}

async function sendOnce(callNumber) {
  const response = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      "api-key": apiKey,
    },
    body: JSON.stringify(buildPayload(callNumber)),
  });
  const text = await response.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = text;
  }
  return { callNumber, status: response.status, body };
}

async function main() {
  console.log(`idempotencyKey de la prueba: ${idempotencyKey}`);

  const first = await sendOnce(1);
  console.log(`Llamada 1/2 -> HTTP ${first.status}`, first.body);

  const second = await sendOnce(2);
  console.log(`Llamada 2/2 -> HTTP ${second.status}`, second.body);

  const record = { idempotencyKey, recipientEmail, first, second, ranAt: new Date().toISOString() };
  const outDir = path.join(__dirname, "..", ".local", "brevo-idempotency-probe");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${Date.now()}.json`);
  fs.writeFileSync(outFile, JSON.stringify(record, null, 2));
  console.log(`\nRegistro completo guardado en ${outFile} (no versionado en git).`);

  console.log(
    "\nSiguiente paso manual: revisa la bandeja de " +
      recipientEmail +
      ". Si llega UN solo correo, anota qué número de llamada indica el asunto " +
      "(confirma si Brevo sirvió la 1ª respuesta cacheada o dejó pasar solo la 2ª). " +
      "Si llegan DOS correos, idempotencyKey no suprimió el envío físico duplicado: " +
      "activar la política de fallo cerrado descrita en ADR-0031 y en " +
      "phase-2-detailed-design-notification-delivery.md (Reintentos y resultados inciertos).",
  );
}

main().catch((error) => {
  console.error("La prueba falló antes de completarse:", error);
  process.exit(1);
});
