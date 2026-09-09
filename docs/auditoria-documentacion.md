# Auditoría de la carpeta `docs/` — Running Coach

**Corpus auditado:** rama `main`. ADR-0001 a ADR-0028 + `README.md` y plantilla, 7 documentos de Fase 0/1 y transversales, 12 documentos de Fase 2.

**Nota de alcance:** `ADR-0029` (CQRS para queries de solo lectura) **no forma parte de `main`**; vive en `origin/feature/global-config-and-security`. Los hallazgos que le afectan van marcados como fuera del corpus principal.

Los hallazgos marcados con **[V]** se han verificado leyendo el fichero citado. El resto se apoya en lectura delegada y se indica como tal.

---

## 1. Incoherencias Detectadas

1. **[V] Cabecera desactualizada: un ADR aceptado citado como «Propuesto».** `phase-2-detailed-design-identity-access.md:28` lista *"`ADR-0025` (Propuesto): Spring Session JDBC y Bucket4j local"*, cuando `ADR-0025` está **Aceptado** desde 2026-09-02 (así lo declaran el propio ADR y `docs/adr/README.md:62`). En la misma cabecera, la línea 17 sigue resumiendo `ADR-0003` como *"...recuperación, **sesiones opacas** y primer administrador"*, mecanismo que `ADR-0025:22` derogó.
   El cuerpo del documento **sí** está actualizado —línea 61 *"sesión HTTP de Spring Session JDBC"*, línea 141 *"Cada inicio correcto crea una sesión HTTP independiente de Spring Session JDBC"*, línea 143 con la cookie correcta—, así que el riesgo no es implementar el modelo derogado, sino que el estado normativo que el documento declara es incorrecto: quien audite la trazabilidad concluirá que el diseño se apoya en una decisión aún no aceptada.

2. **[V] Corregido tras verificación — la retención de 24 y 12 meses no es una divergencia injustificada.** La versión original de este hallazgo afirmaba que `phase-2-detailed-design-classification-segmentation.md:327` (24 meses) y `phase-2-detailed-design-planning.md:413` (12 meses) fijaban plazos distintos para el mismo tipo de dato sin que `ADR-0010` distinguiera esas categorías. Es falso: la matriz de `ADR-0010:74-81` sí tiene categorías separadas con esos plazos exactos —«Planes publicados y seguimiento» (24 meses, evento inicial: fecha del entrenamiento) y «Auditoría y seguridad» (12 meses, evento inicial: fecha del evento)—. Ambos diseños ya citan explícitamente su categoría correspondiente.

3. **[V] «Miembro efectivo» es un homónimo con tres referentes distintos.** `phase-1-acceptance-criteria.md:42` lo usa como *miembro efectivo del segmento*; `phase-2-detailed-design-planning.md:81` lo define como *"corredor `active` que pertenece al resultado de un **grupo** `active`"*; `ADR-0021:65` acuña *miembro efectivo de la publicación* para la pertenencia histórica congelada. Los tres se cualifican en su definición, y donde se usan sin cualificar (`phase-2-detailed-design-notification-delivery.md:130`, `phase-2-high-level-design.md:40`) el contexto de publicación desambigua. Aun así, un mismo término con tres referentes distintos —miembros de segmento, de grupo y destinatarios congelados— es una trampa al escribir consultas o controles de acceso, y choca con la puerta de calidad que el propio `phase-2-closure.md:67` da por superada: *"Terminología: ... miembro efectivo ... usados sin significados incompatibles"*.

4. **[V] Un comando privilegiado existe en el diseño detallado pero no en la línea base de seguridad.** `phase-2-access-security-baseline.md:50` documenta únicamente el comando de bootstrap del primer administrador. `phase-2-detailed-design-identity-access.md:281` añade un segundo comando de despliegue, *"La recuperación excepcional del único administrador"*, que revoca sesiones, fuerza reactivación y cambia el correo de una cuenta administradora existente. Siendo la baseline el documento que concreta los parámetros de seguridad de `ADR-0003`, omitir un comando de recuperación privilegiada es una laguna de gobernanza, no solo de redacción.

5. **[V] La política de sesión no está trazada a Fase 1.** `ADR-0025:21` y `phase-2-access-security-baseline.md:42-43` fijan la cookie `__Host-pmv_session` y la caducidad a 12 horas de inactividad. La tabla NFR de `phase-1-requirements.md:102` solo dice *"Autenticación por correo electrónico y contraseña; acceso por rol y aislamiento de los datos del corredor"*, y no existe ningún `CA-RFxx` que cubra la caducidad. Un parámetro de seguridad observable queda fuera de la cadena de trazabilidad que `documentation-quality-gates.md` declara controlar.

6. **[V] RF-20 impone un SLA que la tabla NFR niega tener.** `phase-1-requirements.md:100` afirma *"Disponibilidad: Nivel normal de SaaS; no se ha definido un SLA formal"*, mientras RF-20/D-06 fijan una ventana dura (`createdAt + 120 minutos`) con estados terminales irreversibles. Es un compromiso temporal verificable —es decir, un SLA funcional— presentado como si no lo fuera.

7. **Conflictos entre ADRs aceptados, detectados tarde** *(lectura delegada)*. `ADR-0018` reconoce que `ADR-0004` (entrenador con gestión global) y `ADR-0010` (retención de 30 días) chocaban con la reactivación real de corredores, y los sustituye parcialmente. Que quede declarado es buena práctica; que se detectara después de aceptar ambos indica que se cerraron sin recorrer un caso de uso central.

8. **Referencias hacia adelante en un documento de Fase 1** *(lectura delegada)*. `future-improvements.md` cita módulos (`runner-management`, `classification-segmentation`, `planning`) y ADRs de Fase 2 que ningún documento de Fase 1 introduce, precomprometiendo la descomposición modular desde un documento que antecede a esa decisión.

9. **Ambigüedad interna en RF-20** *(lectura delegada)*. RF-20 cierra con *"No se incluyen otros eventos de notificación"*, pero su sección «Deseable» añade *"Registro básico del estado de entrega del correo"*, sin aclarar si eso es el evento prohibido o una capa de observabilidad distinta.

10. **[V] Hallazgo no detectado en la primera versión de esta auditoría — un ADR aceptado fue reescrito en el sitio.** El commit `b36d9fc` ("Apply-hexagonal-package-structure"), al aceptar `ADR-0026`, modificó retroactivamente el árbol de paquetes de `ADR-0014` (ya aceptado), sustituyendo su estructura original `adapter/in`/`adapter/out` por la de `infrastructure/input`/`infrastructure/output` que `ADR-0026` introduce. Es exactamente la práctica que la propia convención de ADRs pretende evitar: un ADR aceptado deja de ser fiable como historia de decisiones si se edita para reflejar decisiones posteriores en lugar de registrarlas como refinamiento. Corregido durante la ejecución de este plan: se restauró en `ADR-0014` el árbol tal como se aceptó, con nota de refinamiento explícita hacia `ADR-0026`, y se añadió a `docs/adr/README.md` la norma que prohíbe este tipo de edición.

11. **Fuera del corpus de `main` — `ADR-0029`.** Dos defectos, aplicables solo a `origin/feature/global-config-and-security`: (a) enlaza como decisiones de Fase 1 relacionadas `D-10` (mayoría de edad) y `D-11` (accesibilidad), ninguna vinculada a consultas de solo lectura; (b) autoriza proyectar DTOs con jOOQ sin declarar si esa proyección sigue respetando la frontera de esquema por módulo que `ADR-0014` impone —ambigüedad crítica para vistas compuestas como `runner-portal`, que no tiene esquema propio. Conviene resolverlos antes de mergear.

---

## 2. Alertas de Sobreingeniería (con alternativa simple)

Vara de medir, tomada de los propios documentos: **club único, >500 corredores registrados, picos iniciales <100 usuarios concurrentes, un solo mantenedor que opera y revisa todo.**

**Contexto que matiza esta sección:** el corpus ya demuestra capacidad de autocorrección. `ADR-0025` elimina el token de sesión propio, el verificador SHA-256, el repositorio de sesiones y los buckets persistentes de `ADR-0003`/`0013`/`0015` con el argumento correcto —*"duplica capacidades maduras del framework... sin una necesidad funcional o de escala demostrada"*— y `ADR-0026` prohíbe explícitamente *"crear esos subpaquetes vacíos"* y *"aplicar DDD táctico por plantilla"*, además de rechazar *"interfaces ceremoniales"* entre controlador e infraestructura. Las alertas que siguen son las que ese mecanismo aún no ha alcanzado.

1. **[V] `publication`: 9 tablas de instantánea que replican el esquema vivo de `planning`.** `phase-2-detailed-design-publication.md:205-213` descompone cada versión publicada en `published_plan`, `published_plan_recipient`, `published_plan_version`, `published_version_recipient`, `published_workout`, `published_phase_duration`, `published_workout_block`, `published_workout_recovery` y `published_version_changed_day`. El propio documento reconoce la trivialidad del objeto: *"El plan contiene como máximo siete entrenamientos completos, por lo que sustituir su representación completa es razonable"*. Son datos inmutables, de escritura única y lectura íntegra, sin consultas relacionales por fila.
   **Alternativa:** `published_plan_version` con una columna `JSONB` para el contenido congelado (fases, bloques, objetivos), conservando como tablas solo lo que necesita índice o restricción relacional: `published_plan`, `published_plan_recipient` y `published_version_recipient` (unicidad corredor-semana y congelación de destinatarios). Pasa de 9 tablas a 4 sin perder ninguna garantía.

2. **[V] `notification-delivery`: 7 tablas y una reimplementación parcial de un ESP.** `phase-2-detailed-design-notification-delivery.md:191-197` define `notification_request`, `notification_attempt`, `delivery_event_inbox`, `delivery_transition`, `suppressed_destination`, `delivery_control` y `delivery_operation_audit`, con leases `SKIP LOCKED`, cifrado AEAD con rotación en Key Vault, supresión por huella HMAC versionada y ventana de reconciliación de resultados inciertos. El proveedor es Brevo, que ya gestiona rebotes, quejas y supresión de forma nativa. El propio documento admite el coste de la huella HMAC: *"Una versión de clave HMAC no puede retirarse mientras existan supresiones calculadas con ella"*.
   **Alternativa:** un outbox único (`notification_request` + `notification_attempt`) con columna de prioridad, backoff exponencial con máximo de reintentos, y el webhook actualizando el estado directamente sobre esas tablas —sin `delivery_event_inbox` ni el ledger `delivery_transition`—, delegando la supresión en la API de Brevo en lugar de replicarla con claves rotables. De 7 tablas a 2-3.

3. **[V] Calendarios de reintento diferenciados por tipo de correo** (`ADR-0011`), con hasta 7 escalones distintos y estados terminales específicos (`omitido-inactivo`, `fallo-definitivo/elegibilidad-no-resuelta`, `fallo-definitivo/resultado-desconocido`, `fallo-definitivo/reemplazado`), para un evento de publicación semanal a ~500 destinatarios.
   **Alternativa:** una única política de backoff con jitter y tope de reintentos, y un solo estado `fallo-definitivo` con campo de motivo. La única diferencia que sí es una regla de negocio real —la caducidad del secreto de invitación/recuperación— se modela como expiración, no como calendario propio.

4. **Custodia dual e independiente de claves de backup** (`ADR-0023`) *(lectura delegada)*: exige designar una segunda persona custodio —que el propio ADR admite no existir todavía—, dos copias físicas separadas, identidades cloud independientes con MFA propio y simulacros trimestrales/semestrales/anuales, para un club de running operado por una persona.
   **Alternativa:** clave de cifrado en un gestor de secretos personal con recuperación por contacto de emergencia, o Shamir con 2 fragmentos del propio operador en ubicaciones distintas. Mantiene la propiedad de "no recuperable solo desde Azure" sin depender de reclutar y sostener un rol humano que hoy no existe.

5. **WCAG 2.2 AA con auditoría automática + manual como puerta de cierre de fase** (D-11, RF-16) *(lectura delegada)*, sin ningún detonante legal, contractual o de usuario declarado en `phase-0-problem-statement.md`.
   **Alternativa:** buenas prácticas exigibles y comprobables barato (HTML semántico, contraste, foco y navegación por teclado, diseño responsive), verificadas con el linter de accesibilidad ya presente en el pipeline, sin elevar la conformidad normativa completa a criterio de bloqueo.

6. **Gobernanza documental con roles ficticios** (`documentation-quality-gates.md`) *(lectura delegada)*: 9 controles, cada uno con su Skill y su "rol revisor responsable" (Revisor de arquitectura, Revisor de producto, DPO), cuando el propio documento admite que *"en este proyecto de un único mantenedor, el autor asume los roles de revisión aplicables"*. La segregación de funciones simulada no aporta la garantía que la segregación real daría, pero sí su coste.
   **Alternativa:** una checklist única de cierre de fase, sin reparto de roles ni la ceremonia de declarar en cada PR la ausencia de revisión independiente (basta declararlo una vez, en el documento de proceso).

7. **Gobierno de API de producto público multi-equipo** (`api-design-guidelines.md`) *(lectura delegada)*: Spectral + `oasdiff` bloqueando cambios incompatibles + pruebas de contrato + generación de servidor y cliente, para una API con un único consumidor, que es el propio frontend del repo.
   **Alternativa:** OpenAPI contract-first con Spectral (que sí aporta consistencia barata) y generación de tipos del cliente; posponer el gate de compatibilidad `oasdiff` a cuando exista un consumidor que no se despliegue en el mismo artefacto.

8. **[V] Corregido tras verificación — el exceso real es PIT en cada PR, no ZAP/Schemathesis.** La versión original de esta alerta afirmaba que OWASP ZAP y Schemathesis eran gates de cada PR junto con PIT. Es falso: `ADR-0013:90` los sitúa *"antes de producción"*, y `build.gradle.kts` y `.github/workflows/` no contienen ninguna referencia a `zap` ni a `schemathesis` — no están configurados en ningún punto del pipeline. Son deuda pendiente, no sobreingeniería. Lo que sí corre en cada PR, vía `fastGate`, es mutation testing PIT con umbral 70 % sobre un build que ya requiere Docker para jOOQ/Flyway efímeros.
   **Alternativa:** restringir PIT a los paquetes de reglas transaccionales y de autorización en `fastGate`, y trasladar el umbral 70 % completo a `qualityGate` (push a main / nocturno). Configurar ZAP y Schemathesis —hoy ausentes— directamente en el pipeline nocturno o previo a release, nunca por PR, siguiendo el mismo criterio que `ADR-0024` ya aplica al resto de la validación (*"El inventario integral seguirá ejecutándose en `main`, de forma nocturna y antes de release"*).

9. **[V] Fuera del corpus de `main` — `ADR-0029` acuña «CQRS» para una regla que `ADR-0014` ya permitía** (*"catálogos y proyecciones de lectura podrán permanecer como modelos simples"*). No hay desacoplo real: misma base, mismo esquema, mismo módulo. El riesgo es que el nombre invite después a construir CQRS de verdad (buses de comandos, proyecciones asíncronas).
   **Alternativa:** una regla de una frase en la guía de codificación o en `ADR-0026` —"las consultas de solo lectura pueden proyectar DTOs con jOOQ sin hidratar el agregado, dentro del esquema del módulo propietario"—, sin ADR propio ni el término CQRS.

---

## 3. Mejoras de Calidad

- **[V] Referencias de cabecera que envejecen en silencio.** El caso de `identity-access.md:17` (incoherencia 1) es sistémico: los diseños detallados listan sus ADRs base en cabecera, pero nada obliga a revisarla cuando un ADR posterior refina esa base. **Propuesta concreta:** que el `README.md` de ADRs —que ya mantiene una tabla de refinamientos correcta y al día— sea la única fuente de esa relación, y que las cabeceras de los diseños enlacen a ella en vez de replicarla.

- **[V] `ADR-0024` a `ADR-0028` no están integrados en la documentación de Fase 2.** Son ADRs posteriores al cierre de fase y de calidad notablemente alta (decisiones concretas, alternativas reales, validación verificable), pero ningún documento de Fase 2 los refleja salvo puntualmente. Quien lea `docs/` en orden encontrará el diseño derogado antes que la decisión vigente.

- **[V] Un ADR aceptado arrastra bloqueantes de implementación no consolidados.** `ADR-0025:82` marca como *"Bloqueante para recuperación, cambio de contraseña y desactivación"* la invalidación de todas las sesiones de una cuenta. Es correcto que se declare, pero estos bloqueantes viven dispersos en la sección «Decisiones pendientes» de al menos 7 ADRs. **Propuesta:** una tabla única de bloqueantes activos en el `README.md` de ADRs; hoy reconstruir el estado real de bloqueo exige releer los ADRs completos.

- **Disclaimers en lugar de criterios verificables** *(lectura delegada)*. Fórmulas como *"sin que ello prometa entrega física o lectura"* o *"no garantiza lectura humana"* se repiten casi literales en varios RF y CA. Son cláusulas de exención: no describen comportamiento observable y no se pueden probar, justo lo que `documentation-quality-gates.md` exige de un requisito.

- **[V] RF-12 mal titulado.** *"Objetivos por frecuencia cardiaca o ritmo relativo al corredor"* sugiere captura de FC, cuando el requisito dice lo contrario (no se almacena, calcula ni valida ningún dato deportivo personal). Sobre un punto sensible de privacidad, el título contradice en apariencia al cuerpo.

- **[V] Métrica sin origen.** *"picos iniciales inferiores a 100 usuarios concurrentes"* sostiene decisiones de arquitectura en media docena de documentos, pero no es trazable a `phase-0` ni declara metodología. Debería marcarse como supuesto, con su fecha de revisión.

- **Granularidad inconsistente** *(lectura delegada)*. La tabla NFR de `phase-1-requirements.md` resuelve la seguridad en una línea, mientras RF-20, en el mismo documento, especifica minutos y estados terminales nombrados. No hay criterio declarado sobre qué merece cada nivel de detalle.

- **Boilerplate estructural en los 8 diseños de Fase 2** *(lectura delegada)*. Las secciones «Decisiones pendientes / Alternativas descartadas / Validación prevista» y los avisos de privacidad se repiten casi literalmente en documentos de 350-586 líneas, diluyendo lo que cada módulo tiene de específico. Un aviso central referenciado bastaría.

- **[V] Gates presentados con rigor desigual.** `documentation-quality-gates.md` sitúa en la misma tabla controles deterministas (Spectral, `oasdiff`) y revisiones asistidas por Skills. `ADR-0024` ya resuelve esta cuestión correctamente en el plano técnico —*"Las Skills y los agentes no podrán aprobar, omitir ni declarar innecesario un gate aplicable"*—; falta trasladar esa misma jerarquía explícita a la tabla de gates documentales.

- **Invariante declarada sin mecanismo de mantenimiento** *(lectura delegada)*. `CA-RF08-02` exige que ningún corredor quede en dos grupos efectivos, pero siendo los segmentos dinámicos, ningún documento explica qué impide el solapamiento cuando una etiqueta cambia después de crear el grupo. `phase-2-detailed-design-planning.md` introduce la reconfiguración multigrupo atómica, que parece resolverlo; conviene enlazar explícitamente el criterio con su mecanismo.

- **[V] Metadatos de cabecera no uniformes.** Unos ADRs declaran «Fecha de aceptación» además de «Fecha» y otros no, sin regla en el `README.md` ni campo en `adr-template.md`, pese a que buena parte del corpus lo usa.

- **[V] Brecha acotada entre el diseño de alto nivel y el detalle.** `phase-2-high-level-design.md` sí cubre Spring Modulith (línea 30), ArchUnit (línea 155) y los recursos derivados no persistidos (líneas 40, 122, 136). Lo que no aparece en ningún punto es el **cifrado AEAD con Azure Key Vault y rotación de claves**, que en `identity-access` y `notification-delivery` es un mecanismo central con implicaciones operativas (custodia y rotación). Merece una línea en el mapa de alto nivel.

- **Autorrevisión sin revisión independiente** *(lectura delegada)*. `phase-2-closure.md` reconoce que el estado «Validado» de toda la fase descansa en un único autor-revisor, incluidas 20 correcciones incorporadas en el propio cierre. Está bien declarado; el riesgo es que un lector externo lea «Validado» como validación independiente.

---

## Nota metodológica

Una primera versión de este reporte, generada mediante lectura delegada, afirmaba que `ADR-0024` a `ADR-0028` no existían en el repositorio. **Era falso**: existen en `main`, en `origin/main` y en varias ramas de trabajo. También sostenía que la línea base de seguridad no fijaba la caducidad de sesión ni la protección CSRF, que `ADR-0006`/`0008`/`0009` contradecían a `ADR-0021` sobre el borrador de publicación, y que el diseño de alto nivel omitía ArchUnit y Spring Modulith; ninguna de esas afirmaciones resiste la lectura directa de los ficheros.

El patrón del error es informativo: **todos los fallos fueron afirmaciones de ausencia** ("el documento no menciona X"), que es justo lo que una lectura parcial no puede sostener. Esta versión solo mantiene lo verificado o lo marca explícitamente como no verificado; las valoraciones de proporcionalidad de la sección 2 son juicios técnicos apoyados en cita textual, no hechos comprobables por grep.

**Cierre (Bloque 0 del plan de corrección):** dos hallazgos de esta misma versión resultaron parcialmente incorrectos y se han corregido en el sitio, marcados como tales: la incoherencia 2 (retención de 24/12 meses) presuponía que `ADR-0010` no distinguía categorías, cuando sí lo hace; la alerta de sobreingeniería 8 atribuía a ZAP y Schemathesis un coste de gate por PR que nunca han tenido, porque no están configurados en el pipeline. Se añadió además la incoherencia 10, un hallazgo real que esta auditoría no detectó en ninguna versión: la retro-edición de `ADR-0014` en el commit `b36d9fc`. Las tres correcciones se verificaron por lectura directa de `ADR-0010`, `build.gradle.kts`, `.github/workflows/` y el historial de git, no por delegación.
