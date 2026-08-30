# UX-01 — Especificación de experiencia del portal del corredor

**Estado:** Validado documentalmente — no valida usabilidad con corredores ni autoriza implementación
**Fecha:** 2026-08-29
**Fecha de validación:** 2026-08-30
**Responsable de decisión:** Revisor de producto
**Revisión requerida en implementación:** Revisores de producto, arquitectura, accesibilidad y privacidad según el gate aplicable

## Propósito y autoridad

Esta especificación consolida la dirección visual y el comportamiento de UX confirmados para el portal web de un corredor `active`. Traduce a una referencia de implementación las decisiones ya aprobadas en:

- [requisitos funcionales y no funcionales](phase-1-requirements.md);
- [criterios de aceptación](phase-1-acceptance-criteria.md);
- [diseño detallado de identidad y acceso](phase-2-detailed-design-identity-access.md);
- [diseño detallado de seguimiento y revisión](phase-2-detailed-design-tracking-review.md);
- [diseño detallado del portal del corredor](phase-2-detailed-design-runner-portal.md);
- [cierre documental de Fase 2](phase-2-closure.md).

Los documentos anteriores conservan autoridad sobre producto, datos, permisos, API y arquitectura. Esta especificación gobierna presentación, interacción y contenido visible dentro de UX-01. Si aparece una contradicción, la implementación debe detenerse y resolverla en la fuente normativa; no puede elegir silenciosamente una interpretación.

La aprobación de este documento no autoriza por sí sola una slice, datos personales reales, proveedores reales, staging productivo ni producción. Cada slice seguirá la Definition of Ready y los gates del [gobierno operativo de la IA](ai-governance.md).

## Validación documental

El Revisor de producto validó esta especificación, el plan de prueba v0.2 y los seis recorridos cognitivos sintéticos el `2026-08-30`. La validación confirma alcance, decisiones, criterios, trazabilidad y límites documentales. No convierte la revisión experta en evidencia con participantes, no demuestra conformidad WCAG de una implementación y no autoriza una slice, merge, datos reales ni producción.

La validación corrigió una incoherencia de contraste sin cambiar la dirección `Atlántico sereno`: `#AABDBD` queda limitado a separación decorativa y `#6F8585` identifica los límites de controles cuando sean necesarios. La implementación deberá volver a medir cada combinación y estado sobre el código real.

## Resultado confirmado

| Dimensión | Decisión |
| --- | --- |
| Dirección | `Rendimiento sereno`: sobria, clara y confiable, con energía contenida. |
| Paleta | `Atlántico sereno`. |
| Plataforma | Aplicación web adaptable, diseñada primero para móvil. |
| Actor | Exclusivamente corredor `active` autenticado. |
| Destino inicial | `Mi plan`, en la semana local actual. |
| Navegación principal | `Mi plan` y `Historial`. |
| Menú personal | `Privacidad de comentarios` y `Cerrar sesión`. |
| Idioma visible | Castellano. |
| Datos | Exclusivamente ficticios o sintéticos hasta superar los gates de datos reales. |
| Accesibilidad | WCAG `2.2` nivel `AA` en todo el PMV web. |

La elección de `Rendimiento sereno` y `Atlántico sereno` fue confirmada por decisión humana después de comparar alternativas. No se infiere un motivo adicional distinto del objetivo declarado de obtener una interfaz sobria, clara y confiable.

## Alcance

UX-01 incluye:

1. inicio de sesión con correo electrónico y contraseña;
2. entrada en `Mi plan` para la semana local actual;
3. navegación entre semanas propias publicadas;
4. resumen semanal y detalle completo de un entrenamiento;
5. registro y edición del seguimiento con guardado explícito;
6. consentimiento justo a tiempo para el comentario opcional;
7. historial propio con detalle y carga progresiva;
8. consulta y retirada del consentimiento en `Privacidad de comentarios`;
9. cierre de la sesión actual.

UX-01 excluye:

- activación, reactivación y recuperación de contraseña;
- panel de indicadores o `dashboard`;
- calendario mensual;
- `feed`, chat o mensajería;
- perfil editable;
- vistas administrativas o del entrenador;
- aplicación nativa y funcionamiento sin conexión;
- modo oscuro;
- logotipo definitivo;
- datos administrativos, métricas inventadas o contenido jurídico presentado como aprobado;
- el product shell completo o la implementación conjunta de todo el portal.

Estas exclusiones no son huecos que la implementación pueda completar. Cualquier incorporación requiere una decisión posterior de alcance.

## Arquitectura de información

```text
Inicio de sesión
└── Mi plan — destino inicial
    ├── Semana publicada
    │   └── Detalle de entrenamiento
    │       └── Registrar o editar seguimiento
    ├── Historial
    │   └── Detalle histórico
    │       └── Editar seguimiento, cuando la capacidad siga abierta
    └── Menú personal
        ├── Privacidad de comentarios
        └── Cerrar sesión
```

`Privacidad de comentarios` también se enlaza desde el formulario de seguimiento cuando el comentario está disponible. No existe una sección de perfil editable.

## Flujos observables

### Acceso

1. La pantalla muestra el wordmark de trabajo `Running Coach`, correo electrónico, contraseña e `Iniciar sesión`.
2. Un inicio correcto para una cuenta de corredor `active` crea la sesión y abre `Mi plan`.
3. Credenciales o estado inválidos producen una respuesta genérica que no revela si la cuenta existe, su rol o su estado.
4. Una sesión caducada devuelve al acceso. Tras autenticarse solo se recupera una ruta de retorno todavía autorizada.
5. Activación y recuperación no aparecen en UX-01.

### `Mi plan`

1. La primera consulta solicita la semana local actual, de lunes a domingo.
2. La cabecera muestra intervalo, nombre del plan y grupo publicado.
3. Hoy queda identificado con texto y tratamiento visual cuando pertenece a la semana visible.
4. La semana conserva los días sin entrenamiento publicado; no los convierte en entrenamientos de descanso.
5. `Anterior` y `Siguiente` recorren exclusivamente semanas propias publicadas.
6. El portal no salta automáticamente a una semana futura.
7. Sin publicación actual muestra `No tienes un plan publicado para esta semana` y, cuando exista, un acceso explícito a la siguiente publicación futura autorizada.
8. Una semana futura no presenta estados ni acciones de seguimiento antes de comenzar la fecha de cada entrenamiento.

Cada tarjeta de entrenamiento muestra:

- día de la semana y fecha;
- tipo de la parte principal;
- modalidad `presencial` o `en-linea`;
- lugar de encuentro solo cuando fue publicado para una sesión presencial;
- estado de seguimiento únicamente desde la fecha del entrenamiento;
- una acción accesible para abrir el detalle.

La tarjeta no calcula ni inventa totales de distancia, duración, ritmo o esfuerzo.

### Detalle del entrenamiento

El detalle reproduce la versión autorizada y mantiene el orden:

1. datos de plan, grupo, día, fecha, modalidad, ubicación y aclaraciones publicadas;
2. calentamiento;
3. parte principal;
4. enfriamiento;
5. bloques, repeticiones, carga, recuperaciones y objetivos publicados;
6. estado y seguimiento propio cuando esté autorizado;
7. `Registrar seguimiento`, `Editar seguimiento` o solo lectura, según la capacidad recibida.

No muestra destinatarios, autoría, versiones internas, entrega de correo, tablas ni otros metadatos administrativos. Una ubicación ausente permanece ausente.

### Seguimiento

El formulario nunca guarda automáticamente y ofrece `Guardar` y `Cancelar`.

| Declaración | Campos permitidos |
| --- | --- |
| `Realizado` | Esfuerzo `1..5`, sensación `bien`, `normal` o `mal` y comentario opcional con consentimiento vigente. |
| `No realizado` | Sin esfuerzo ni sensación; comentario opcional con consentimiento vigente. |

Para `Realizado` se pregunta exactamente «¿Cuánto esfuerzo te supuso este entrenamiento?» sin valor preseleccionado y se presentan:

- `1 Muy suave`;
- `2 Suave`;
- `3 Moderado`;
- `4 Intenso`;
- `5 Muy intenso`.

Comportamiento obligatorio:

- `Guardar` envía una sola representación completa con su precondición;
- `Cancelar` vuelve sin petición y conserva el último registro persistido;
- un error de campo mantiene los valores locales y no sustituye el registro anterior;
- `No realizado` con esfuerzo o sensación se rechaza por completo;
- después del cierre de la ventana el formulario queda en lectura;
- un conflicto con una republicación exige recargar y no mezcla versiones;
- no existe borrador persistente, reintento silencioso ni sincronización entre pestañas.

### Comentario y consentimiento

1. El comentario permanece deshabilitado hasta que el corredor intenta usarlo.
2. Sin consentimiento vigente se muestra por separado la información efectiva, su versión y una advertencia para no introducir diagnósticos, lesiones u otros datos de salud.
3. Rechazar esa información cierra el comentario, pero permite guardar el seguimiento estructurado.
4. Aceptar habilita un comentario opcional de texto plano con máximo de `1000` caracteres.
5. La maqueta y los fixtures usan información sintética marcada como pendiente de revisión de privacidad; no constituyen texto jurídico aprobado.

### `Historial`

1. Incluye solo entrenamientos cuya fecha local ya comenzó.
2. Ordena del más reciente al más antiguo y agrupa por semana.
3. Cada elemento muestra fecha, tipo, modalidad y estado textual.
4. `Cargar más` añade la página siguiente y conserva la posición.
5. El detalle histórico muestra el entrenamiento y el seguimiento autorizados para ese contexto.
6. Un seguimiento editable puede abrirse desde el historial mientras la capacidad siga vigente.
7. No existen buscador, filtros, calendario mensual, exportación ni orden alternativo.

Los estados visibles canónicos son:

- `Pendiente de seguimiento` mientras no exista registro y la ventana siga abierta;
- `Sin seguimiento` después de cerrar la ventana sin registro;
- `Realizado`;
- `No realizado`;
- `Retirado`.

Ningún estado se comunica únicamente mediante color o icono.

### `Privacidad de comentarios`

La vista muestra el estado `not-granted`, `granted` o `withdrawn` y la versión informada. La retirada está disponible siempre que el consentimiento esté otorgado, aunque no exista un plan o una ventana de seguimiento abierta.

Antes de confirmar la retirada se explican las tres consecuencias:

1. se deshabilitan nuevos comentarios;
2. los textos anteriores no se recuperan;
3. el seguimiento estructurado permanece.

Retirar vacía los comentarios operativos, conserva los campos estructurados y evita que una lectura o restauración ordinaria vuelva a exponer los textos. Volver a consentir solo habilita comentarios nuevos en ventanas todavía abiertas; nunca recupera comentarios anteriores.

### Cierre de sesión

`Cerrar sesión` está en el menú personal, revoca la sesión actual mediante `identity-access` y devuelve a la pantalla de acceso. El frontend no simula la salida ocultando contenido local.

## Sistema visual confirmado

### Color por función semántica

| Función | Token de referencia | Uso |
| --- | --- | --- |
| Canvas | `#EEF3F3` | Fondo general del portal. |
| Superficie | `#FCFEFD` | Formularios, detalles y tarjetas. |
| Texto principal | `#17313D` | Contenido principal. |
| Texto secundario | `#4A616A` | Contexto y metadatos no esenciales. |
| Acción primaria | `#164E63` | Acción principal y cabecera. |
| Acción activa | `#0F3E50` | Estado interactivo de la acción primaria. |
| Acento | `#C45C36` | Énfasis energético contenido en elementos no textuales; no se usa para texto normal ni como único indicador. |
| Foco | `#005FCC` | Indicador de foco visible. |
| Éxito | `#2E6659` | Confirmación acompañada por texto. |
| Aviso | `#80531A` | Advertencia acompañada por texto. |
| Error | `#A33131` | Error acompañado por texto y asociación al campo. |
| Separador | `#AABDBD` | Separación decorativa; no identifica por sí sola un control o estado. |
| Borde de control | `#6F8585` | Límite de un control cuando sea necesario para identificarlo. |

Los valores son referencias de diseño, no una dispensa de contraste. En la validación documental, `#6F8585` ofrece aproximadamente `3,49:1` sobre el canvas y `3,86:1` sobre la superficie; `#AABDBD` no alcanza `3:1` y por eso no puede aportar información visual necesaria. La implementación debe verificar cada combinación, incluido texto, foco, estados interactivos, deshabilitados y mensajes, contra WCAG `2.2 AA`.

### Tipografía, escala y forma

- Tipografía: `Inter` cuando esté disponible; `Segoe UI` y fuente del sistema como respaldo.
- Pesos: `400` y `500`.
- Escala de referencia: `12`, `14`, `16`, `20` y `28` px.
- Espaciado: `4`, `8`, `12`, `16`, `24` y `32` px.
- Radios: `8` px para controles, `12` px para tarjetas y `18` px para superficies principales.
- Elevación: interfaz plana por defecto; sombra suave solo para menú y acceso.
- Iconografía: trazo lineal; todo icono que comunique acción o estado se acompaña de texto o nombre accesible.
- Marca: `Running Coach` se usa solo como wordmark de trabajo; no define un logotipo.

### Componentes

- Una sola acción primaria por grupo de acciones.
- Botones secundarios para cancelar, volver y acciones de menor énfasis.
- Campos con etiqueta persistente; el placeholder no sustituye la etiqueta.
- Errores asociados al campo y resumen cuando existan varios.
- Tarjetas breves para la semana y representación completa en el detalle.
- Chips o tratamientos compactos solo como apoyo; el texto conserva el significado.
- Mensajes de éxito, aviso y error con encabezado o frase explícita.
- Foco visible en todos los controles interactivos.

## Adaptabilidad y accesibilidad

La implementación comienza con una columna móvil y amplía la composición sin alterar el orden semántico. No existe un breakpoint de producto obligatorio: los componentes cambian cuando dejan de ser legibles, no por simular un dispositivo concreto.

Comportamientos verificables:

- reflow a `320 CSS px` sin desplazamiento bidimensional no esencial;
- zoom del navegador al `400 %`;
- texto al `200 %`;
- funcionamiento completo mediante teclado;
- foco visible y retorno de foco al cerrar un detalle o formulario;
- encabezados, listas, formularios y botones semánticos;
- etiquetas y errores programáticamente asociados;
- objetivos de puntero de al menos `24 × 24 CSS px` o excepción oficial documentada;
- fechas comprensibles con día, mes y contexto semanal;
- contenido esencial disponible sin `hover`;
- ninguna información incrustada como texto en una imagen;
- orden lunes a domingo preservado al reorganizar tarjetas;
- acceso equivalente a fases, bloques, objetivos, ubicación y acciones en móvil y escritorio.

En escritorio la semana puede usar siete columnas solo cuando todo el contenido siga siendo legible. En anchos menores las tarjetas se reorganizan sin convertir la semana en un calendario horizontal desplazable.

## Contenido y datos de referencia

- Toda muestra, fixture, captura o prueba usa datos inequívocamente sintéticos.
- No se inventan marcas personales, ritmos, zonas, métricas acumuladas, porcentajes, rachas ni indicadores de rendimiento.
- Los objetivos publicados se muestran literalmente; el portal no los calcula ni solicita referencias personales.
- Los comentarios no aparecen en telemetría, cachés compartidas, logs, trazas, métricas ni informes de error.
- Los textos de privacidad sintéticos se identifican como no aprobados y nunca se reutilizan en producción por defecto.

## Criterios de aceptación de UX-01

| ID | Trazabilidad | Escenario observable |
| --- | --- | --- |
| `CA-UX01-01` | `RF-01` | Dada una cuenta de corredor `active`, al introducir credenciales válidas se crea la sesión y se abre `Mi plan`; credenciales o estado inválidos muestran una respuesta genérica sin revelar información de la cuenta. |
| `CA-UX01-02` | `RF-16` | Al entrar en `Mi plan`, se solicita la semana local actual, hoy queda identificado y la navegación solo alcanza semanas propias publicadas; sin publicación se muestra el estado vacío y no se salta automáticamente. |
| `CA-UX01-03` | `RF-13`, `RF-16` | Cada tarjeta muestra fecha, tipo, modalidad, ubicación publicada cuando corresponda y estado aplicable; un hueco semanal no inventa un entrenamiento ni una ubicación. |
| `CA-UX01-04` | `RF-16` | Al abrir una tarjeta se muestra el detalle autorizado completo y ordenado; no aparecen datos administrativos, destinatarios ni métricas calculadas. |
| `CA-UX01-05` | `RF-17` | Dentro de la ventana, `Realizado` exige esfuerzo y sensación válidos y `No realizado` excluye ambos; `Guardar` persiste una representación completa y `Cancelar` no envía cambios. |
| `CA-UX01-06` | `RF-17` | Una combinación inválida o un conflicto rechaza toda la escritura, conserva el registro anterior y mantiene los valores locales cuando el error sea de campo. |
| `CA-UX01-07` | `RF-17` | Al intentar comentar sin consentimiento vigente se muestra información versionada y advertencia; rechazarla permite guardar los campos estructurados y aceptar habilita texto plano opcional de hasta `1000` caracteres. |
| `CA-UX01-08` | `RF-17` | Al retirar el consentimiento se explican y aplican sus tres consecuencias; volver a aceptar no recupera ningún comentario eliminado. |
| `CA-UX01-09` | `RF-18` | `Historial` agrupa por semana solo fechas iniciadas, muestra estado textual y añade páginas mediante `Cargar más`; una página vacía no incorpora futuros ni datos ajenos. |
| `CA-UX01-10` | `RF-16`, `RF-18` | Un recurso propio se presenta en móvil y escritorio sin perder contenido ni acciones; un recurso ajeno o no disponible se rechaza sin exponer datos de otro corredor. |
| `CA-UX01-11` | `RF-16` | A `320 CSS px`, zoom `400 %`, texto `200 %` y mediante teclado se conservan contenido, orden, foco, etiquetas, errores y acciones sin desplazamiento bidimensional no esencial. |
| `CA-UX01-12` | `RF-01` | Al seleccionar `Cerrar sesión`, la sesión actual se revoca y vuelve el acceso; el contenido protegido no permanece operativo mediante navegación local. |

Estos criterios validan documentación. No se consideran probados hasta que la implementación aporte pruebas automáticas y revisión manual sobre el código real.

## Trazabilidad de pantallas

| Pantalla o estado | Criterios UX-01 | Fuente principal |
| --- | --- | --- |
| Inicio de sesión | `CA-UX01-01`, `CA-UX01-12` | `identity-access`, `CA-RF01-01`, `CA-RF01-02`. |
| `Mi plan` y semana vacía | `CA-UX01-02`, `CA-UX01-03`, `CA-UX01-10`, `CA-UX01-11` | `runner-portal`, `CA-RF16-01`, `CA-RF16-02`. |
| Detalle | `CA-UX01-04`, `CA-UX01-10`, `CA-UX01-11` | `runner-portal`, `publication`. |
| Seguimiento y error | `CA-UX01-05`, `CA-UX01-06` | `tracking-review`, `CA-RF17-01`, `CA-RF17-02`. |
| Consentimiento y retirada | `CA-UX01-07`, `CA-UX01-08` | `tracking-review`, `ADR-0010`. |
| `Historial` | `CA-UX01-09`, `CA-UX01-10`, `CA-UX01-11` | `runner-portal`, `tracking-review`, `CA-RF18-01`, `CA-RF18-02`. |

## Dependencias y Definition of Ready

Antes de implementar una slice que materialice parte de UX-01 deberán existir y revisarse, en el alcance concreto de esa slice:

1. issue o historia vertical con requisitos y criterios aplicables;
2. contrato OpenAPI `3.1` aprobado y comprobación de compatibilidad;
3. catálogo de Problem Details;
4. autorización, alcance y recursos propios definidos;
5. modelo, migraciones, índices y transacciones cuando la slice posea datos;
6. fixtures exclusivamente sintéticos;
7. plan de pruebas funcionales, contrato, aislamiento, accesibilidad y privacidad;
8. dependencias previas realmente implementadas, incluida `identity-access` antes de exponer el portal autenticado.

La implementación debe dividirse en slices verticales. Esta especificación no autoriza construir de una vez todo el product shell ni crear endpoints provisionales fuera de OpenAPI.

## Decisiones y alternativas

| Decisión | Motivo y autoridad | Alternativas | Impacto | Aplicación |
| --- | --- | --- | --- | --- |
| `Rendimiento sereno` | Elección humana confirmada para el tono sobrio, claro y confiable solicitado. | `Energía de pista` y `Club editorial`, no seleccionadas para UX-01. | La jerarquía, densidad y componentes evitan una estética de panel de métricas o red social. | Slices frontend que materialicen UX-01. |
| `Atlántico sereno` | Paleta elegida por la persona responsable después de revisar variantes del modelo. | Variantes anteriores, no seleccionadas. | Fija los tokens de referencia; cada uso real conserva el gate de contraste. | Componentes y rutas frontend de UX-01. |
| Tema claro | Modo oscuro estaba expresamente excluido. | Modo oscuro. | La primera implementación no mantiene dos juegos de tokens; añadirlo exige nuevo alcance y validación. | Primera implementación del portal del corredor. |
| Wordmark de trabajo | La identidad visual definitiva no forma parte de UX-01. | Diseñar un logotipo durante el handoff. | Evita convertir la especificación funcional en un proyecto de marca. | Se conserva hasta que exista un trabajo de identidad aprobado. |
| Plan e historial separados | Evita presentar entrenamientos futuros como ausencias. | `dashboard`, `feed` o calendario único. | Conserva dos destinos principales y estados temporales comprensibles. | Rutas y navegación del portal del corredor. |
| Guardado explícito | Coincide con la sustitución atómica del seguimiento y evita estados parciales. | Autoguardado o borrador persistente. | Exige `Guardar`, `Cancelar`, gestión de error y precondición. | Slice de seguimiento de `tracking-review` y su integración en el portal. |

No se crea un ADR: estas decisiones materializan la experiencia de usuario sin cambiar límites modulares, propiedad de datos, contratos ni decisiones arquitectónicas aceptadas.

## Supuestos, riesgos y cuestiones aplazadas

| Elemento | Clasificación | Responsable y gate | Tratamiento |
| --- | --- | --- | --- |
| Usabilidad con corredores reales | Riesgo, no validado en UX-01 | Revisor de producto, antes de declarar la experiencia lista para lanzamiento | Ejecutar pruebas de usabilidad con protocolo y datos sintéticos; corregir sin ampliar alcance silenciosamente. |
| Texto informativo del comentario | Bloqueante para datos reales y producción | Responsable del tratamiento con Revisor de privacidad o DPO, antes de usar datos reales | Aprobar base, versión, información, retirada y tratamiento de posibles datos de salud. |
| Logotipo definitivo | Aplazado deliberadamente, sin fecha de calendario | Responsable de producto o marca, antes de lanzamiento público | Sustituir el wordmark solo mediante trabajo de identidad separado. |
| Modo oscuro | Fuera de alcance, no aplazamiento implícito | Revisor de producto, solo si se propone una ampliación | Reabrir alcance, tokens, contraste y pruebas. |
| Límites de página y rendimiento | Trabajo de implementación | Revisor de arquitectura, antes de implementar historial | Medir cardinalidades y aprobar límites contract-first. |
| Breakpoints exactos | Decisión técnica subordinada al comportamiento | Revisor de frontend y accesibilidad, durante la slice | Elegirlos por legibilidad y verificar `320 CSS px`; no convertir anchos de dispositivo en requisito de producto. |

No quedan preguntas de producto o arquitectura pendientes dentro de UX-01. Los elementos anteriores están clasificados y no autorizan datos reales ni producción.

## Cambios de alcance de esta especificación

Este documento:

- no añade requisitos funcionales, actores, permisos, datos, módulos, API ni integraciones;
- registra en el repositorio la dirección visual y la paleta ya confirmadas;
- hace verificable la presentación de `RF-01`, `RF-16`, `RF-17` y `RF-18` dentro del portal del corredor;
- conserva las exclusiones del PMV y los bloqueantes de privacidad y producción;
- no convierte la maqueta exploratoria en fuente de verdad técnica;
- no autoriza todavía el product shell definitivo ni una implementación completa.

Un cambio posterior que altere flujos, estados, consentimiento, permisos, datos visibles, alcance adaptable o dirección visual deberá revisar esta especificación y sus fuentes normativas mediante una PR independiente.
