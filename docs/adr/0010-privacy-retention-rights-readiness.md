# ADR-0010: Preparación para privacidad, retención y derechos

**Estado:** Aceptado
**Fecha:** 2026-08-14
**Responsable de revisión:** Responsable de privacidad o asesor especializado
**Validación documental:** Aceptado explícitamente por el responsable el 2026-08-14; evidencias jurídicas y operativas pendientes antes de producción

## Contexto

El PMV trata datos personales de cuentas y corredores: correo e identidad operativa, roles, etiquetas, segmentos, grupos, destinatarios de publicaciones, historial de planes, seguimiento declarado, comentarios libres, notificaciones y registros de seguridad o auditoría. Administrador y entrenador tienen lectura global de datos operativos según `ADR-0004`.

`ADR-0009` no solicita campos clínicos, pero su comentario libre puede contener información sobre lesiones, enfermedad u otros datos de salud. Negar ese riesgo sería falso: si el sistema recibe ese contenido, lo trata aunque no lo haya pedido.

El RGPD exige finalidad determinada, licitud, minimización, exactitud, limitación de conservación, seguridad y capacidad de demostrar cumplimiento. La AEPD exige integrar protección desde el diseño y por defecto antes del tratamiento, mantener un registro de actividades, evaluar riesgos durante el ciclo de vida y habilitar procedimientos accesibles para el ejercicio de derechos.

Este ADR define las garantías que la arquitectura debe poder materializar y las evidencias obligatorias antes de producción. No determina por sí solo que el tratamiento sea lícito, no sustituye asesoramiento jurídico y no atribuye automáticamente al proyecto la obligación de designar un DPO.

## Decisión

El responsable del tratamiento será la persona física que promueve y presta directamente el servicio de planificación y seguimiento deportivo a los corredores mediante una relación contractual. El ADR público no almacenará su nombre legal, identificador fiscal, domicilio ni otros datos personales; su identidad y contacto completos constarán en el expediente privado de cumplimiento y en la información de privacidad antes de tratar datos reales.

La salida a producción quedará bloqueada hasta que el responsable del tratamiento apruebe y documente un expediente mínimo de privacidad con:

1. identidad y contacto del responsable del tratamiento y, cuando exista, su DPO o contacto de privacidad;
2. inventario de tratamientos, interesados, categorías de datos, finalidades, destinatarios y sistemas;
3. base jurídica documentada por finalidad y, si se admite información de salud, condición aplicable para categorías especiales;
4. plazos o criterios de conservación y acción final por categoría;
5. información al corredor en el momento de recogida y canal accesible para derechos;
6. encargados, subencargados, ubicación del tratamiento y garantías de transferencias internacionales cuando existan;
7. análisis de riesgos y una EIPD completa;
8. medidas técnicas y organizativas, procedimiento de incidentes y evidencias de prueba.

El sistema mantendrá un catálogo versionado de categorías de datos y finalidad. Como mínimo distinguirá:

- **Identidad y acceso:** correo, rol, estado, credenciales derivadas, sesiones y secretos de activación o recuperación.
- **Clasificación operativa:** perfil de corredor, etiquetas, segmentos, grupos y excepciones.
- **Planificación y publicación:** borradores, versiones, destinatarios, lugares y auditoría de publicación.
- **Seguimiento:** realizado, esfuerzo, sensación, comentario, versión y fechas.
- **Notificaciones:** destino, versión, contenido mínimo, idempotencia y estado técnico.
- **Seguridad y auditoría:** eventos estrictamente necesarios para investigar acceso, cambios sensibles e incidentes.

Las bases jurídicas previstas por finalidad serán:

| Finalidad | Categorías principales | Base prevista y condición |
| --- | --- | --- |
| Prestar planificación y seguimiento deportivo | Identidad y acceso, clasificación operativa, planificación, publicación y seguimiento estructurado | Ejecución del contrato, limitada a los datos objetivamente necesarios para prestar el servicio. |
| Habilitar el comentario libre opcional | Comentario de seguimiento y los posibles datos de salud que introduzca el corredor | Consentimiento explícito, específico, informado y revocable, tanto como base del tratamiento opcional como condición para las categorías especiales. |
| Proteger cuentas, servicio y evidencias técnicas | Seguridad y auditoría mínima | Interés legítimo sujeto a una evaluación de ponderación documentada, minimización y acceso restringido. |
| Atender derechos, brechas y obligaciones aplicables | Solicitudes, decisiones, comunicaciones y evidencias imprescindibles | Cumplimiento de obligación legal cuando resulte aplicable; cualquier conservación adicional deberá identificar la norma y el plazo concreto. |

El contrato no se usará como cláusula genérica para finalidades opcionales y el interés legítimo no se usará para levantar la prohibición de tratar categorías especiales. La evaluación definitiva de las bases y sus textos informativos será revisada por una persona especializada antes de producción.

No se incorporarán analítica, publicidad, perfilado comercial, venta de datos, integraciones deportivas ni reutilización para finalidades incompatibles bajo una cláusula genérica. Cada finalidad nueva exigirá revisar información, base jurídica, minimización, retención, destinatarios, riesgos y este ADR o uno que lo reemplace.

La configuración por defecto aplicará minimización de cantidad, accesibilidad y plazo. Las vistas, exportaciones, registros y soporte no incluirán comentarios de seguimiento ni otros datos personales salvo que sean necesarios para la operación autorizada. Los secretos y contraseñas no serán recuperables en claro. Los entornos no productivos usarán datos sintéticos o anonimizados; no se copiará la base de producción como mecanismo ordinario de prueba.

Cada categoría tendrá una política ejecutable con evento inicial, plazo, acción final y excepciones documentadas. Las acciones finales permitidas serán supresión, anonimización irreversible o bloqueo con acceso restringido cuando exista una obligación o defensa de reclamaciones documentada. No bastará ocultar datos en la interfaz, desactivar una cuenta ni marcarlos como borrados.

Los procesos automáticos de retención deberán ser idempotentes, auditables y probarse antes de producción. Las copias de seguridad no restaurarán datos suprimidos al entorno activo: si una restauración fuese necesaria, se reaplicarán las supresiones pendientes antes de reabrir el servicio. La caducidad de copias y su acceso formarán parte de la política de retención.

El ejercicio de acceso, rectificación, supresión, oposición, limitación y portabilidad dispondrá de un canal accesible informado al corredor. El responsable registrará recepción, verificación proporcionada de identidad, decisión, ejecución, comunicaciones y plazo. La respuesta se gestionará dentro del plazo legal aplicable, con objetivo operativo máximo de un mes y registro de cualquier prórroga justificada.

Una solicitud de derechos no concederá acceso al solicitante a datos de terceros ni permitirá alterar publicaciones históricas como si nunca hubieran existido. La rectificación, supresión, limitación o portabilidad se ejecutará por categoría conforme a finalidad, base jurídica, obligaciones aplicables y derechos de otras personas. Toda denegación o conservación parcial requerirá motivo documentado por el responsable; la aplicación no decidirá excepciones jurídicas de forma automática.

El producto no ofrecerá decisiones automatizadas con efectos jurídicos, perfilado comercial ni inferencias de salud. El comentario de seguimiento estará deshabilitado hasta obtener un consentimiento separado del contrato. Rechazarlo o retirarlo no limitará el resto del servicio. El sistema registrará la versión de la información aceptada, el instante, el actor y la retirada, sin reutilizar el consentimiento para otras finalidades. Tras la retirada impedirá nuevos comentarios y aplicará a los existentes la supresión, el bloqueo o la conservación estrictamente exigible según la política aprobada. Además mostrará una instrucción visible para no introducir diagnósticos, lesiones ni otra información de salud. Esa advertencia reduce recogida accidental, pero no sustituye el consentimiento ni elimina el riesgo.

El PMV se limitará a personas de `18` años o más. No permitirá invitar ni activar cuentas de menores. El alta registrará únicamente la declaración de mayoría de edad, su fecha y si procede del corredor o del administrador; no almacenará fecha de nacimiento, documento de identidad ni copia acreditativa. Introducir menores requerirá reemplazar este ADR y diseñar representación, información, ejercicio de derechos y bases aplicables antes de admitirlos.

El comentario libre de seguimiento se mantiene bajo consentimiento explícito conforme a los artículos `6.1.a` y `9.2.a` del RGPD. Antes de producción, una revisión especializada deberá confirmar que el consentimiento es libre, específico, informado, demostrable y revocable en la relación real con el corredor. Si esa revisión concluye que no existe una solución defendible, el comentario se eliminará del alcance antes de recoger seguimiento.

La matriz mínima de conservación activa será:

| Categoría | Evento inicial | Plazo activo | Acción ordinaria al vencer |
| --- | --- | --- | --- |
| Cuenta, perfil y clasificación | Fin de la relación contractual | `30` días | Cesar el uso ordinario y ejecutar supresión; bloquear solo los elementos y durante el plazo que exija una responsabilidad identificada. |
| Planes publicados y seguimiento | Fecha del entrenamiento | `24` meses | Anonimizar irreversiblemente cuando exista finalidad estadística aprobada o ejecutar supresión y eventual bloqueo restringido. |
| Notificaciones | Estado técnico final | `90` días | Suprimir destino, contenido y detalle de entrega no necesario; bloquear únicamente la evidencia imprescindible cuando proceda. |
| Invitaciones y recuperaciones | Uso o caducidad | `30` días | Invalidar de inmediato al usar o caducar y, al vencer el plazo, suprimir verificadores y metadatos salvo bloqueo exigible. |
| Auditoría y seguridad | Fecha del evento | `12` meses | Finalizar acceso operativo y suprimir o bloquear por incidente o responsabilidad documentada. |
| Copias de respaldo | Creación de la copia | Máximo `35` días | Destruir la copia completa mediante rotación verificable y reaplicar supresiones pendientes tras cualquier restauración. |

La arquitectura distinguirá conservación activa, bloqueo restringido, anonimización irreversible y destrucción definitiva. El bloqueo impedirá uso ordinario y visualización, limitará el acceso a la puesta a disposición legalmente exigible, registrará motivo e inicio y terminará en destrucción al prescribir la responsabilidad aplicable. No se asignará automáticamente un plazo genérico de cinco años a todas las categorías: cada bloqueo deberá enlazar la obligación o responsabilidad y su plazo revisado.

Al terminar la relación se desactivará inmediatamente el acceso. Durante los `30` días siguientes el interesado podrá solicitar su exportación; al vencer se ejecutarán las acciones de cuenta, perfil y clasificación, y el resto seguirá su plazo propio. Cualquier conservación identificable posterior deberá estar respaldada por la finalidad y base aprobadas y tendrá acceso restringido.

El canal de derechos será `privacidad@<dominio-final>`, un buzón dedicado gestionado por el propio responsable del tratamiento. La dirección real deberá existir y publicarse antes de producción; el marcador no podrá llegar a configuración productiva. El procedimiento interno registrará responsable operativo, recepción, verificación proporcionada, decisión, ejecución y comunicación. Cuando proceda una entrega electrónica, la exportación se generará como archivo `ZIP` con datos estructurados en `JSON` o `CSV` y documentos legibles necesarios, sin secretos ni datos de terceros.

La verificación aplicará un mecanismo proporcional:

- una cuenta activa usará sesión autenticada y confirmación enviada a su correo registrado;
- una persona sin acceso usará un desafío de un solo uso enviado al correo registrado;
- solo se solicitará evidencia adicional cuando existan dudas razonables sobre la identidad o representación;
- no se conservarán copias de DNI u otros documentos identificativos por defecto; si excepcionalmente fueran necesarios, se limitarán a la comprobación y se eliminarán al terminarla salvo obligación documentada.

Todo proveedor con acceso o capacidad de tratamiento será inventariado antes de contratarse o habilitarse. Como mínimo se evaluarán Microsoft Azure, GHCR, Scaleway, Grafana Cloud, Brevo y el futuro proveedor del buzón de privacidad. El responsable aprobará cuando corresponda el DPA, subencargados, ubicación, transferencias, retención, eliminación o devolución al terminar y asistencia para derechos, incidentes y auditoría. Ningún proveedor tratará datos reales antes de completar su evaluación. Las transferencias internacionales no se inferirán de la marca comercial del proveedor; se documentarán según la región y garantías efectivas del servicio contratado.

Existirá un procedimiento de brechas que permita detectar, contener, preservar evidencias, valorar riesgo, escalar al responsable y documentar la decisión de notificación. Deberá permitir cumplir el plazo legal cuando proceda, incluida la referencia de `72` horas desde que el responsable tenga constancia para notificar a la autoridad de control, sin afirmar que toda incidencia deba notificarse.

Antes de producción se realizará un análisis de riesgos con los tratamientos reales y proveedores elegidos y se completará una EIPD aunque el análisis jurídico posterior concluyera que no es estrictamente obligatoria. Incluirá descripción sistemática, necesidad y proporcionalidad, riesgos para derechos y libertades, proveedores, accesos globales, conservación y medidas. El riesgo residual deberá ser aceptado expresamente por el responsable y la EIPD se revisará ante cambios significativos.

## Alternativas consideradas

### Alternativa A: Resolver privacidad después del lanzamiento

Se descarta porque bases jurídicas, información, retención, derechos, proveedores y medidas deben decidirse antes del tratamiento. Corregirlas después puede exigir borrar o migrar datos ya recogidos sin garantías adecuadas.

### Alternativa B: Usar consentimiento genérico para todo

Se descarta porque cada finalidad necesita una base jurídica adecuada y el consentimiento no convierte en necesario ni lícito un tratamiento excesivo. Las categorías especiales requieren además una condición específica documentada.

### Alternativa C: Conservar todos los datos indefinidamente

Se descarta porque contradice limitación de conservación y minimización. La utilidad histórica no justifica por sí sola identificar permanentemente al corredor.

### Alternativa D: Eliminar el comentario libre para evitar cualquier dato de salud

No se adopta en esta propuesta porque `RF-17` lo incluye y ADR-0009 lo limita. Sigue siendo una alternativa válida si el responsable no identifica una condición y garantías suficientes para el contenido que pueda recibirse.

### Alternativa E: Suprimir inmediatamente todo ante cualquier solicitud

Se descarta como automatismo porque los derechos no son absolutos y pueden coexistir con obligaciones o defensa de reclamaciones. La decisión debe ser trazable, por categoría y conforme a la base aplicable.

### Alternativa F: Delegar cumplimiento en proveedores

Se descarta porque el responsable conserva sus obligaciones y debe decidir fines, medios esenciales, garantías y encargos. Un contrato o certificación del proveedor no sustituye el análisis propio.

### Alternativa G: Amparar el comentario libre en el contrato o en interés legítimo

Se descarta porque el comentario no es necesario para prestar el resto del servicio y puede contener categorías especiales. Ni la ejecución contractual ni el interés legítimo levantan por sí solos la prohibición del artículo `9` del RGPD. Habilitarlo exige consentimiento explícito y una alternativa equivalente sin comentario.

### Alternativa H: Destruir inmediatamente todos los datos al vencer el plazo activo

Se descarta como regla universal porque una supresión puede exigir bloqueo para atender responsabilidades durante su plazo de prescripción. El bloqueo no permite seguir usando el dato: exige reserva, acceso excepcional y destrucción final. Tampoco se conservará por defecto todo durante un plazo genérico de cinco años.

### Alternativa I: Decidir si se hace una EIPD solo al final de la implementación

Se descarta. El comentario potencialmente sanitario, el seguimiento longitudinal y el acceso global crean suficientes factores de riesgo para integrar la EIPD en el diseño y usarla como gate de producción, aunque una revisión posterior concluyera que no era estrictamente preceptiva.

## Consecuencias

- Producción queda condicionada a evidencias revisadas por una persona responsable de privacidad, no solo a pruebas técnicas.
- El modelo de datos y los procesos operativos deben soportar exportación, rectificación, supresión, anonimización, limitación y retención por categorías.
- El acceso global de entrenador y administrador aumenta el riesgo y exige medidas, registro y revisión periódica de cuentas privilegiadas.
- El comentario libre puede obligar a tratar categorías especiales; una advertencia no elimina esa realidad.
- Copias de seguridad, logs, proveedores y entornos de prueba entran en el alcance de privacidad y no pueden tratarse como excepciones invisibles.
- Los plazos cortos reducen exposición, pero pueden limitar historial operativo; los largos exigen justificación específica.
- La evaluación deberá repetirse cuando cambien datos, finalidades, proveedores, transferencias, escala, menores o capacidades de análisis.
- Excluir menores reduce alcance y evita recoger fecha de nacimiento o documentos, pero exige impedir altas que no declaren cumplir el umbral.
- La matriz aprobada convierte retención en comportamiento verificable; cualquier excepción deberá documentarse y no podrá convertirse en conservación indefinida.
- El responsable será una persona física y deberá mantener su identidad y contacto completos fuera del repositorio público; producción seguirá bloqueada hasta publicarlos en la información de privacidad y habilitar el buzón dedicado.
- La verificación evita recopilar documentos de identidad de forma rutinaria, pero requiere proteger los desafíos y registrar el resultado sin conservar evidencia excesiva.
- Tratar el comentario por consentimiento exige una alternativa real: el corredor debe poder rechazarlo o retirarlo sin perder planificación ni seguimiento estructurado.
- Incorporar bloqueo restringido evita confundir supresión con borrado inmediato, pero exige aislar esos datos de las consultas y operaciones ordinarias.
- La EIPD pasa a ser una evidencia obligatoria de producción aunque un análisis posterior concluyera que no era legalmente preceptiva.

## Requisitos relacionados

- `RF-01` a `RF-20`
- Requisito no funcional de datos y privacidad de Fase 1

## Decisiones de Fase 1 relacionadas

- `D-07`: seguimiento estructurado y comentario opcional con revisión global.
- `D-08`: acceso global de entrenador y aislamiento del corredor.

## Validación prevista

- Revisar y aprobar el inventario y registro de actividades de tratamiento con responsable, finalidades, datos, destinatarios, bases, transferencias, retención y medidas.
- Probar la política de cada categoría con datos que alcanzan su vencimiento, incluidas copias restauradas.
- Probar exportación y acceso sin exponer datos de terceros ni secretos.
- Probar rechazo de invitación o activación sin declaración de mayoría de edad y comprobar que no se almacenan fecha de nacimiento ni documentos identificativos.
- Probar rectificación, supresión, limitación y anonimización, incluidas referencias históricas y registros vinculados.
- Probar que el comentario permanece deshabilitado sin consentimiento, que el consentimiento registra texto versionado, instante y actor y que su rechazo no impide el seguimiento estructurado.
- Probar que la retirada impide nuevos comentarios, no degrada planificación ni seguimiento estructurado y aplica a los comentarios existentes la política aprobada.
- Probar desactivación inmediata al terminar la relación, exportación durante `30` días y aplicación independiente del resto de plazos.
- Probar cada fila de la matriz en sus límites temporales, incluidos `24` meses, `90` días, `30` días, `12` meses y copias de `35` días.
- Probar que los datos bloqueados quedan fuera de consultas, exportaciones y operación ordinaria, que solo accede el rol excepcional autorizado y que se destruyen al vencer el plazo registrado.
- Probar que la exportación `ZIP` contiene formatos `JSON` o `CSV` legibles y excluye secretos y datos ajenos.
- Probar verificación mediante sesión y correo para cuentas activas y mediante desafío de un solo uso para personas sin acceso.
- Probar que una duda razonable puede escalar a evidencia adicional y que no se conserva una copia identificativa por defecto.
- Probar que desactivar una cuenta no se confunde con ejecutar una política de supresión.
- Probar que logs, soporte y errores no incorporan comentarios, credenciales, tokens ni contenido operativo innecesario.
- Verificar datos sintéticos o anonimizados en desarrollo, pruebas y demostraciones.
- Revisar contratos, subencargados, regiones y eliminación de cada proveedor antes de habilitarlo.
- Verificar que el marcador `privacidad@<dominio-final>` no puede alcanzar producción y probar recepción y respuesta desde el buzón real.
- Ejecutar un simulacro de solicitud de derechos y comprobar registro y respuesta dentro del plazo.
- Ejecutar un ejercicio de brecha y comprobar detección, escalado, evaluación y capacidad de cumplir plazos.
- Documentar el análisis de riesgos, completar y aprobar la EIPD y registrar la aceptación del riesgo residual.
- Obtener revisión humana de privacidad antes de autorizar producción.

## Resultado de validación documental

La revisión documental iniciada el 2026-08-13 y las decisiones cerradas por el responsable el 2026-08-14 concluyen:

- estructura y trazabilidad correctas;
- decisiones funcionales de mayoría de edad, minimización, retención propuesta, cierre de cuenta, canal, exportación y verificación documentadas;
- validaciones técnicas y operativas previstas de forma observable;
- referencias oficiales y riesgos identificados;
- responsable persona física y relación contractual directa definidos;
- canal dedicado decidido, con creación y publicación diferidas hasta antes de producción;
- bases previstas por finalidad y consentimiento explícito del comentario decididos;
- matriz activa, bloqueo, anonimización y destrucción decididos;
- EIPD, evaluación de proveedores y evidencias operativas declaradas obligatorias antes de producción;
- decisiones pendientes de aceptación cerradas y evidencias de producción clasificadas con responsable y tratamiento.

La validación explícita del responsable acepta este ADR como decisión de arquitectura. No constituye revisión jurídica o de privacidad ni autoriza producción. La salida con datos reales seguirá bloqueada hasta aportar y revisar las evidencias específicas indicadas en este ADR.

## Decisiones pendientes

No quedan decisiones pendientes para aceptar este ADR. Permanecen estas evidencias obligatorias:

- **Bloqueante para producción:** documentar en el expediente privado y publicar en la información de privacidad la identidad y contacto completos de la persona física responsable; adquirir el dominio, crear `privacidad@<dominio-final>` e identificar a su proveedor. Responsable: responsable del tratamiento. Tratamiento: completar y probar el canal antes de datos reales.
- **Bloqueante para producción:** obtener revisión especializada de las bases jurídicas, del consentimiento explícito, de la evaluación de interés legítimo y de los plazos activos y de bloqueo; eliminar el comentario si el consentimiento no resulta defendible. Responsable: responsable del tratamiento con asesoramiento de privacidad. Tratamiento: incorporar conclusiones y cambios antes de datos reales.
- **Bloqueante para producción:** inventariar y aprobar, cuando corresponda, DPA, subencargados, regiones, transferencias, retención y terminación de Microsoft Azure, GHCR, Scaleway, Grafana Cloud, Brevo y el proveedor del buzón. Responsable: responsable del tratamiento y revisor de arquitectura. Tratamiento: no habilitar cada proveedor con datos reales hasta completar su evidencia.
- **Bloqueante para producción:** realizar el análisis de riesgos y completar y aprobar una EIPD con el stack, escala y proveedores reales. Responsable: responsable del tratamiento con asesoramiento de privacidad o DPO cuando exista. Tratamiento: aceptar el riesgo residual y revisar la EIPD ante cambios significativos.
- **Bloqueante para producción:** aprobar la información de privacidad, registro de actividades, procedimiento de derechos, automatización de retención, bloqueo y destrucción, gestión de brechas, medidas técnicas y organizativas y sus simulacros. Responsable: responsable del tratamiento. Tratamiento: aportar evidencia y revisión especializada antes de datos reales.

## Referencias oficiales

- [Reglamento (UE) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj), especialmente artículos `5`, `6`, `9`, `13` a `22`, `25`, `28`, `30`, `32` a `35`.
- [AEPD: protección de datos por defecto](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/medidas-de-cumplimiento/proteccion-de-datos-por-defecto).
- [AEPD: ejercicio de derechos](https://www.aepd.es/derechos-y-deberes/ejerce-tus-derechos) y [derecho de información](https://www.aepd.es/derechos-y-deberes/conoce-tus-derechos/derecho-de-informacion).
- [AEPD: registro de actividades de tratamiento](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/medidas-de-cumplimiento/actividades-tratamiento).
- [AEPD: evaluación del riesgo](https://www.aepd.es/derechos-y-deberes/cumple-tus-deberes/medidas-de-cumplimiento/evaluacion-del-riesgo-que-un) y [contenido mínimo de una EIPD](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/10-evaluacion-de-impacto/FAQ-0229-que-debe-incluir-una-evaluacion-de-impacto-de-proteccion-de-datos).
- [AEPD: bases para categorías especiales](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/5-bases-legitimadoras-del-tratamiento/FAQ-0215-cuales-son-las-bases-de-legitimacion-para-el-tratamiento-de-las-categorias-especiales-de-datos).
- [AEPD: supuestos y criterios para realizar una EIPD](https://www.aepd.es/preguntas-frecuentes/2-tus-obligaciones-como-responsable-del-tratamiento/10-evaluacion-de-impacto/FAQ-0226-en-que-supuestos-es-necesario-realizar-una-evaluacion-de-impacto).
- [LOPDGDD, artículo 32: bloqueo de los datos](https://www.boe.es/buscar/act.php?id=BOE-A-2018-16673#a32).
