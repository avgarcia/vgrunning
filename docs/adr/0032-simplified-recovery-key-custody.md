# ADR-0032: Custodia simplificada de la clave privada de recuperación

**Estado:** Aceptado
**Fecha:** 2026-09-08
**Responsable de revisión:** Revisor de arquitectura
**Refina parcialmente:** [ADR-0023](0023-recovery-objectives-independent-key-custody.md)

## Contexto

`ADR-0023` exige dos copias protegidas de la clave privada de recuperación en ubicaciones físicas separadas: una bajo custodia del propietario del servicio y otra bajo una **persona custodio de recuperación designada**, con acceso, sustitución y prueba documentados fuera del repositorio, y participación registrada en cada simulacro.

Esa persona custodio no existe hoy y es el único bloqueante de producción de `ADR-0023` sin evidencia de que vaya a resolverse por la vía prevista: el proyecto tiene un único mantenedor y no hay una segunda persona técnica dispuesta a sostener un rol de custodia activa —aceptar la designación, participar en simulacros semestrales, mantener disponibilidad de acceso de emergencia— de forma indefinida para un club de running de un solo operador.

La propiedad de seguridad que `ADR-0023` protege con la doble custodia es doble:

1. **No recuperabilidad exclusiva desde Azure.** La clave privada nunca debe depender de Azure, GitHub o Scaleway para poder recuperarse. Esta propiedad es independiente de cuántas personas la custodien.
2. **Ausencia de punto único de fallo físico.** Que un incendio, robo, pérdida o fallo de un único dispositivo o ubicación no deje irrecuperable la única copia. Esto sí exige más de una copia, pero no exige más de una persona: una misma persona puede sostener dos copias en ubicaciones y mecanismos independientes.

`ADR-0023` (Alternativa E) descartó la custodia de una única persona porque *"incendio, pérdida, indisponibilidad o error de una sola persona convertirían el backup externo en irrecuperable"*. Ese razonamiento cubre dos riesgos distintos: pérdida física de una copia (mitigable con más ubicaciones, sin más personas) e indisponibilidad del propio operador (que sí exigía, en el diseño original, una segunda persona activa).

Este ADR adopta la mitad de esa alternativa —fragmentar entre ubicaciones/mecanismos independientes— y **renuncia deliberadamente** a la otra: que alguien distinto del operador pueda ejecutar la recuperación si el operador está incapacitado o no localizable. El proyecto no tiene una obligación de continuidad de negocio declarada (no hay SLA formal, `phase-1-requirements.md:100`) que justifique sostener esa capacidad con el coste de mantener un segundo custodio técnico activo. Se acepta ese riesgo residual y se cubre de forma parcial y pasiva mediante un contacto de emergencia sin acceso técnico rutinario, según se describe más abajo — no es una mitigación equivalente a una persona custodio activa, es una renuncia consciente a esa propiedad.

## Decisión

Sustituye, exclusivamente, el mecanismo de custodia de la clave privada descrito en `ADR-0023` (sección *Cifrado y custodia*, párrafo de las dos copias) y su bloqueante de designar una persona custodio. El resto de `ADR-0023` —escenarios, objetivos, separación de permisos, cadencia y contenido de los simulacros— sigue vigente sin cambios.

### Custodia por fragmentación, no por segunda persona

La clave privada de recuperación se divide mediante **Shamir Secret Sharing en umbral 2 de 2** en dos fragmentos. El operador conserva ambos fragmentos, cada uno en un mecanismo y ubicación física independientes entre sí y respecto de Azure, GitHub y Scaleway:

- **Fragmento A:** almacenado en un gestor de secretos personal de terceros con cifrado de conocimiento cero (por ejemplo, Bitwarden o 1Password), en una cuenta distinta de cualquier proveedor ya usado por la plataforma (Azure, GitHub, Scaleway, Brevo). El propio vendor no puede leer el contenido del fragmento.
- **Fragmento B:** copia física impresa o en soporte offline (por ejemplo, tarjeta metálica o USB cifrado), guardada en una ubicación física distinta a la del equipo de trabajo habitual del operador (por ejemplo, una caja de seguridad bancaria o el domicilio de un familiar de confianza, sin acceso digital).

Ningún fragmento por separado permite reconstruir la clave. La pérdida de un único fragmento no impide la recuperación mientras el otro siga accesible; solo la pérdida simultánea de ambos hace irrecuperable el backup, riesgo que `ADR-0023` ya acepta explícitamente para su propio esquema de doble custodia y que aquí no empeora: sigue habiendo dos copias en ubicaciones independientes, cambia solo quién las sostiene.

### Contacto de emergencia, sin custodia técnica activa

El gestor de secretos personal usado para el Fragmento A declarará un **contacto de emergencia** mediante la función nativa del propio vendor (acceso de emergencia con periodo de espera configurable). Esa persona no participa en simulacros, no necesita conocimientos técnicos ni acceso rutinario, y solo obtiene el fragmento si solicita el acceso de emergencia y el operador no lo deniega dentro del plazo configurado. Esto cubre a coste marginal el escenario de indisponibilidad del operador, sin las obligaciones de una persona custodio designada (aceptación formal, simulacros, disponibilidad de acceso continuo).

### Simulacros

Los simulacros de `ADR-0023` (PITR trimestral, restauración externa semestral, salida de proveedor anual) se mantienen sin cambio de cadencia. La restauración externa deberá demostrar, además de lo ya exigido, la reconstrucción de la clave a partir de los dos fragmentos desde sus ubicaciones reales, sin que ninguno haya estado nunca junto al otro ni junto al backup cifrado.

## Alternativas consideradas

### Alternativa A: Mantener la persona custodio designada de `ADR-0023`

Se descarta porque el bloqueante lleva abierto desde la aceptación de `ADR-0023` sin vía de resolución realista para un proyecto de un único mantenedor, y porque la propiedad de seguridad que de verdad importa (dos copias, ninguna solo en Azure) no requiere una segunda persona técnica.

### Alternativa B: Clave completa (sin fragmentar) en un único gestor de secretos personal con acceso de emergencia

Se descarta como único mecanismo porque reintroduce el punto único de fallo que `ADR-0023` (Alternativa E) ya rechazó: la disponibilidad, integridad o compromiso de una sola cuenta de un solo vendor deja la clave completa expuesta o irrecuperable. Fragmentarla entre dos mecanismos independientes mitiga ambos riesgos sin coste adicional relevante.

### Alternativa C: Umbral 2 de 3 con un tercer fragmento en un segundo gestor de secretos

Se descarta por ahora por no aportar una propiedad adicional que el escenario real requiera: el riesgo dominante para un operador único es la pérdida de un fragmento, no la necesidad de tolerar la pérdida de dos de tres. Se revisará si el número de operadores o la superficie de riesgo cambian.

## Consecuencias

- Desaparece el bloqueante de producción de `ADR-0023` que exigía designar una persona custodio; se sustituye por dos bloqueantes más simples de resolver por el propio operador (ver `docs/adr/README.md`).
- La restauración externa deja de depender de la disponibilidad de una segunda persona para reconstruir la clave en el caso ordinario (operador disponible); solo la necesita en el caso excepcional de indisponibilidad del operador, y de forma pasiva (acceso de emergencia, no participación activa).
- Se introduce una dependencia adicional, aunque acotada: el vendor del gestor de secretos personal. Se mitiga eligiendo uno con cifrado de conocimiento cero y evitando reutilizar cualquier proveedor ya presente en la cadena de custodia (Azure, GitHub, Scaleway, Brevo).
- La fragmentación Shamir añade una herramienta más al procedimiento de recuperación (dividir y reconstruir), que debe probarse en el simulacro igual que el resto del procedimiento.
- Se renuncia deliberadamente a que un tercero pueda ejecutar la recuperación de forma activa si el operador está incapacitado; se acepta ese riesgo residual dado que no existe una obligación de continuidad de negocio declarada para el proyecto.

## Requisitos relacionados

- Los mismos que `ADR-0023`: todos los `RF-01` a `RF-21`; requisitos no funcionales de disponibilidad, seguridad, datos y privacidad.

## Decisiones de Fase 1 relacionadas

- Ninguna adicional a las ya citadas en `ADR-0023`.

## Validación prevista

- Ejecutar la fragmentación Shamir 2 de 2 sobre una clave de prueba y verificar que cualquiera de los dos fragmentos por separado no permite reconstruirla.
- Verificar que el gestor de secretos personal elegido cifra en el cliente y que el vendor no puede acceder al contenido del fragmento en texto claro.
- Configurar el acceso de emergencia con el contacto designado y probar el flujo completo (solicitud, periodo de espera, denegación y concesión) sin exponer el fragmento fuera del escenario de prueba.
- Incorporar la reconstrucción desde los dos fragmentos reales al guion de la restauración externa semestral de `ADR-0023`.

## Decisiones pendientes

- **Bloqueante para producción:** elegir el gestor de secretos personal concreto y el soporte físico del segundo fragmento, y documentar el procedimiento de fragmentación/reconstrucción fuera del repositorio. Responsable: Propietario del servicio. Tratamiento: seleccionar herramienta y soporte, y probarlos antes de producción.
- **Bloqueante para producción:** designar al contacto de emergencia y configurar el acceso de emergencia en el gestor de secretos elegido. Responsable: Propietario del servicio. Tratamiento: configuración y prueba del flujo de acceso de emergencia antes de producción.

## Referencias oficiales

- [Shamir's Secret Sharing — esquema original (Adi Shamir, 1979)](https://dl.acm.org/doi/10.1145/359168.359176).
- [Bitwarden: Emergency Access](https://bitwarden.com/help/emergency-access/).
- [1Password: Emergency Kit y recuperación de cuenta](https://support.1password.com/emergency-kit/).
