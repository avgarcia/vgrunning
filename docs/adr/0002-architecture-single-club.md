# ADR-0002: Arquitectura general del PMV y límites single-club

**Estado:** Aceptado
**Fecha:** 2026-08-11
**Fecha de aceptación:** 2026-08-11
**Responsable de revisión:** Revisor de arquitectura

## Contexto

El PMV es una aplicación web adaptable para la operación interna de un único club, con más de 500 corredores registrados y picos iniciales inferiores a 100 usuarios concurrentes. La decisión de Fase 1 `D-03` excluye organizaciones, tenants, administración por club, comercialización a terceros y aislamiento entre clubes.

El diseño de alto nivel separa identidad y acceso, administración y taxonomías, segmentación, planificación, publicación y notificación, consulta del corredor y seguimiento y revisión. Sin una decisión de arquitectura, estos límites pueden convertirse en una aplicación sin fronteras verificables o, en sentido contrario, en servicios distribuidos injustificados.

No se ha elegido framework, base de datos, proveedor de identidad, proveedor de correo ni plataforma de despliegue. La publicación atómica, la autorización y las notificaciones tienen ADRs propios pendientes, por lo que esta decisión no define sus mecanismos internos.

## Decisión

El PMV se implementará como una aplicación web de despliegue único, con un backend modular y una frontera única de datos transaccionales para el club.

Los módulos del backend corresponden a los componentes lógicos del diseño de alto nivel: identidad y acceso; administración y taxonomías; segmentación; planificación; publicación y notificación; consulta del corredor; y seguimiento y revisión. Cada módulo debe exponer interfaces explícitas al resto de la aplicación y conservar la responsabilidad sobre las reglas y datos que gobierna.

Un módulo es una unidad lógica del backend que agrupa una capacidad de negocio, sus casos de uso, reglas y contratos de acceso a sus datos. Cada módulo expone interfaces explícitas para colaborar con otros módulos; ningún módulo debe depender directamente de los modelos internos o detalles de persistencia de otro. Los módulos se ejecutan y despliegan como una única aplicación y pueden participar en la misma transacción. Un módulo no exige microservicios, DDD ni arquitectura hexagonal; esas técnicas podrán adoptarse después si una decisión posterior las justifica.

Un módulo no implica un servicio desplegable independiente. Los flujos centrales entre módulos se ejecutan dentro de la aplicación y no dependen de llamadas de red entre servicios. La frontera transaccional única no decide el tipo de almacenamiento ni sustituye las decisiones de consistencia de `ADR-0007`, solicitud de notificación de `ADR-0008` y entrega de correo de `ADR-0011`.

No se introducirán entidades, campos, APIs ni permisos de organización o tenant. Una futura evolución multiclub requerirá reemplazar este ADR y rediseñar explícitamente aislamiento, autorización, migración y operación.

## Alternativas consideradas

### Alternativa A: Microservicios por componente lógico

Se descarta para el PMV. El volumen y concurrencia previstos no justifican la complejidad de despliegues, observabilidad, comunicaciones distribuidas y consistencia entre servicios. Además, haría más difícil garantizar la publicación atómica definida en `ADR-0007`.

### Alternativa B: Arquitectura multiclub desde el inicio

Se descarta porque contradice `D-03` y añade aislamiento organizativo, administración por club y decisiones de comercialización fuera de alcance. Preparar campos de tenant sin comportamiento real solo oculta coste y no aporta valor al PMV.

### Alternativa C: Aplicación única sin módulos ni interfaces explícitas

Se descarta porque mezclaría permisos, segmentación, publicación y seguimiento. Esa estructura elevaría el riesgo de cambios que expongan datos de corredores o creen versiones de publicación inconsistentes.

## Consecuencias

- El despliegue, la operación y el diagnóstico iniciales se simplifican al existir una sola aplicación y una sola frontera transaccional.
- Los módulos reducen el acoplamiento lógico, pero requieren revisión de sus interfaces y pruebas de autorización y publicación en sus límites.
- No existe aislamiento entre organizaciones porque solo hay un club; esta limitación es intencionada y no debe presentarse como capacidad multiclub.
- La evolución a multiclub no será un cambio de configuración. Requerirá un ADR de reemplazo, migraciones de datos y rediseño de permisos.
- La elección de tecnología, persistencia, autenticación, autorización, publicación y correo sigue abierta. No debe inferirse de esta decisión.

## Requisitos relacionados

- `RF-01`
- `RF-02`
- `RF-03`
- `RF-04`
- `RF-05`
- `RF-06`
- `RF-07`
- `RF-08`
- `RF-09`
- `RF-10`
- `RF-11`
- `RF-12`
- `RF-13`
- `RF-14`
- `RF-15`
- `RF-16`
- `RF-17`
- `RF-18`
- `RF-19`
- `RF-20`

## Decisiones de Fase 1 relacionadas

- `D-01`: los destinatarios y publicaciones requieren una fuente de verdad común.
- `D-03`: el límite single-club excluye multiclub y aislamiento por organización.
- `D-06`: la publicación y republicación deben conservar atomicidad y versión.
- `D-08`: los permisos globales del entrenador y el aislamiento del corredor atraviesan los módulos.

## Validación prevista

- Revisar en el diseño detallado que cada caso de uso pertenezca a uno de los módulos declarados y que sus dependencias estén documentadas.
- Verificar en la implementación que no existan entidades, campos, rutas ni reglas de autorización de organización o tenant.
- Probar que autorización, publicación y seguimiento aplican sus reglas en el backend y no solo en la interfaz.
- Definir y ejecutar pruebas de carga antes de producción con la concurrencia objetivo; no se fija un SLA en este ADR.

## Decisiones pendientes

- **Bloqueante para implementar correo:** `ADR-0011` debe decidir proveedor, entrega, reintentos automáticos y observabilidad. Responsable: revisor de arquitectura. Tratamiento: aceptarlo antes de implementar cualquier correo.
- **Pendiente, sin bloquear este ADR:** seleccionar framework, persistencia y plataforma de despliegue. Responsable: revisor de arquitectura. Tratamiento: registrar decisiones específicas antes de implementar los componentes que dependan de ellas.
