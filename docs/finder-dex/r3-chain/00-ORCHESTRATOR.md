# FinderDex R3 — Orquestador secuencial de desarrollo

## Misión
Ejecutar la cadena completa de funciones aprobadas para FinderDex R3 en **Jorgeprdz/Voyager**, sin rediseñar R2.2. La cadena es una **instrucción para un agente Codex activo**, no un servicio en segundo plano ni un disparador automático de GitHub Actions.

**Rama que contiene estas instrucciones:** `plan/finderdex-r3-chain`, nacida de `a5bd2dcd6be376cd867a4b411277f6d3e8f68cf6` en `feat/finder-dex-foundation-01`.
**Plataforma:** Android 17, Samsung DeX, ARM64, mouse/teclado primero.
**Paquete de APK validado anteriormente:** `com.finderdex.preview`.

## Instrucción principal para Codex
1. Leer COMPLETOS este archivo, `STATE.md` y el siguiente archivo numerado según `CURRENT_STAGE`.
2. Hacer una auditoría inicial real: `git status --short`, `git rev-parse HEAD`, rama, módulos, proveedores y pruebas. Comprobar si el código cambió desde la base. **No asumir que las funciones declaradas en documentos antiguos siguen pendientes**, ni que archivos existentes implican pruebas físicas exitosas.
3. Proteger el trabajo existente: no `reset --hard`, no `clean -fd`, no sobrescribir trabajo concurrente ni actualizar a la fuerza la rama de R2.2. Para implementación usar una rama de trabajo `feat/finderdex-r3-implementation` creada desde la base de producto más reciente, incorporando únicamente estos documentos de planificación cuando haga falta. No crear una implementación desde una revisión desactualizada si `feat/finder-dex-foundation-01` avanzó.
4. Ejecutar cada etapa **en orden estricto**. Al terminar, verificar criterios, guardar evidencia en `STATE.md`, hacer commit y push. Leer el **siguiente** archivo numerado inmediatamente; no esperar otro mensaje mientras la sesión de Codex esté activa, salvo que exista una dependencia humana imprescindible.
5. **No confundir éxito de compilación con funcionalidad confirmada.** Señalar `PASS_AUTOMATED`, `PASS_STATIC`, `PENDING_PHYSICAL_DEX`, `PARTIAL`, `BLOCKED_EXTERNAL` o `FAILED`.
6. Si falta registro OAuth, consentimiento, client ID o permiso del usuario, **no inventar credenciales ni usar credenciales de terceros**. Implementar todo lo que se pueda con un proveedor simulado y pruebas; registrar `BLOCKED_EXTERNAL`. Continuar con etapas independientes, conservando la deuda explícita. Nunca marcar ese proveedor como funcional.
7. Un fallo real de seguridad, posible pérdida de datos, corrupción, borrado indebido o regresión P0 **detiene la cadena** hasta corregirlo. Para bloqueo externo verificable puede avanzarse con etapas independientes y se debe retomar el bloqueo antes de declarar R3 listo.
8. Mantener la identidad visual R2.2 **congelada**: carpetas azules, sidebar hielo, vidrio moderado, toolbar, status bar, slider, microanimación de iconos. Integrar nuevas acciones con componentes existentes sin cambiar tokens de diseño.
9. No instalar APK automáticamente ni ejecutar operaciones destructivas con archivos reales. Para validar movimientos y borrados usar fixtures/archivos de prueba.

## Orden de ejecución obligatorio
- **01** → `01-GOOGLE-DRIVE.md` — Drive integrado en sidebar con operaciones remotas seguras.
- **02** → `02-CROSS-APP-DRAG-DROP.md` — arrastrar entre FinderDex y otras ventanas Android/DeX.
- **03** → `03-ONEDRIVE.md` — segundo proveedor de nube.
- **04** → `04-STORAGE-ANALYTICS.md` — grandes, duplicados, categorías, análisis de uso.
- **05** → `05-RECENTS-NETWORK-SHARING.md` — recientes, redes y Quick Share / share sheet.
- **06** → `06-TABS-DUAL-PANE.md` — pestañas y doble panel de escritorio.
- **07** → `07-BULK-RENAME-TRANSFERS.md` — renombrado masivo y panel de transferencias.
- **08** → `08-MOBILE-REDESIGN.md` — rediseño de la interfaz móvil con identidad visual R2.2, sin encoger la versión DeX.
- **09** → `09-HARDENING-DELIVERY.md` — integración escritorio+móvil, rendimiento, CI, APK y entrega.

## Protocolo entre etapas: obligatorio
Al finalizar la etapa N:
- Capturar cambios, tests ejecutados/reales y su resultado, commit/SHA, número y URL del workflow y riesgos.
- Actualizar `STATE.md`: `LAST_COMPLETED_STAGE`, `CURRENT_STAGE`, `BLOCKERS`, `NEXT_ACTION` y la tabla de evidencias.
- Commit y push de código + actualización de estado, sin alterar el HEAD de otra tarea.
- Leer y ejecutar **inmediatamente** `NN+1-*.md`.
- Si cambió la rama o la sesión terminó, al reanudar leer `STATE.md` y retomar exactamente la etapa pendiente. No repetir operaciones destructivas ni reimportar datos.
- Tras etapa 09, mostrar informe consolidado y separar integraciones realmente verificadas de integraciones pendientes de OAuth/DeX.

## Reglas técnicas globales
- Extender arquitectura existente de Voyager y providers; reutilizar operaciones, sesiones, cifrado/Keystore donde corresponda, papelera y pruebas.
- Usar SAF/`content://`, permisos URI de duración limitada, operaciones con cancelación, progreso, conflictos y recuperación de errores.
- Nunca incluir secretos, tokens, correos, rutas personales reales, archivos del usuario o IDs de cuenta en logs, commits, artifacts o reportes.
- No prometer drag & drop hacia aplicaciones que no admiten ese MIME o interacción.
- Soporte offline explícito: nunca declarar sincronización bidireccional si sólo existe cache/descarga.
- Emplear scopes OAuth mínimos, consentimiento explícito y registro de app legítimo.
- Revisar las API oficiales vigentes en el momento de implementar; probar versiones reales de dependencias.
- Cada etapa: tests unitarios de lógica, lint, build, smoke reproducible, y criterio funcional trazable; preferir pequeñas entregas sobre commits gigantes.
- Si el runner no tiene equipo DeX, dejar una **matriz pendiente**; capturas de emulador no equivalen a PASS físico.
- No consumir horas de builds fallidos sin causa raíz: inspeccionar errores y arreglar el bloqueador puntual.

## APK al final
Actualizar y reutilizar si es viable `scripts/finderdex-r2.2-download.sh` para R3. Descargar `app-debug.apk` separado de `app-debug-androidTest.apk`, verificar paquete, certificado, SHA256 y commit. Si el agente está físicamente en Termux autorizado, copiar a `/storage/emulated/0/Download/FinderDex-R3-debug.apk`. Si está remoto, publicar artifact y entregar **un comando** local de Termux: no afirmar que se descargó en el teléfono.

## Estado inicial
Leer `STATE.md` antes de empezar. **Plan documentado != funcionalidades implementadas.** No avanzar a R3.1 sin haber registrado evidencia por etapa.
