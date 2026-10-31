# Etapa 09 — End-to-end, seguridad, rendimiento y entrega

Antes: orquestador + estado + informes de etapas 01–08.

## Auditoría de integración final
1. Verificar existencia real de funcionalidades con código, tests y UI: Google Drive, drag/drop inter-app, OneDrive, analizador, Recientes, red, Quick Share, pestañas, doble panel, renombrado masivo, centro de transferencias, móvil R3.
2. Etiquetar cada una como PASS_AUTOMATED, PASS_STATIC, PENDING_PHYSICAL_DEX, PENDING_USER_VISUAL_REVIEW, PARTIAL o BLOCKED_EXTERNAL. Nada de marcar integración remota completa si falta OAuth o consentimiento de cuentas.
3. Probar flujos local→nube, nube→local, red→local y entre ventanas cuando los proveedores estén realmente autorizados. Para move entre proveedores exigir copy+verify y confirmación antes de eliminar origen.
4. Probar límites: revocación de URI, low storage, permisos, offline, 401/403/429, fallos de red, conflictos, archivo enorme, 1000 elementos, rotación, DPI, resize, DeX/móvil, procesos interrumpidos, lector de pantalla.
5. Auditar secretos/logs, cuentas, cancelación, manejo de archivos temporales, permisos persistentes, y no romper datos del usuario.
6. Rendimiento: scroll sin jank relevante, carga paginada, decodificación de miniaturas acotada, transiciones y hover sin reflow, evitar leaks y tareas pesadas en Main.
7. Verificar que diseño DeX R2.2 sigue congelado y mobile conserva su identidad derivada.

## CI y artifact
- Ejecutar tests reales (unit + instrumentadas disponibles), lint, build. Documentar qué se saltó.
- APK principal com.finderdex.preview firmado y verificado, launcher correcto; separar app-debug.apk de app-debug-androidTest.apk, artifacts nombrados inequívocamente.
- Calcular SHA256 del binario real, capturar commit y URL del workflow; nunca inventar enlaces o hashes.
- Preferir reutilizar/adaptar scripts/finderdex-r2.2-download.sh para descarga R3; no romper el anterior.
- Si corre directamente en Termux con permisos y gh autenticado: esperar workflow del commit, descargar artefacto correcto, verificar y colocar /storage/emulated/0/Download/FinderDex-R3-debug.apk sin instalar. Si está en nube/remoto: sólo publicar artifact + comando Termux, no declarar descarga local.
- Entregar checklist físico DeX y Android móvil, matriz PASS vs pendientes OAuth/cuenta/usuario.

## Resultado
Actualizar STATE.md con STATUS FINAL o PARTIAL/EXTERNAL_BLOCKED; informe consolidado y enlace directo a workflow real. NO declarar R3 completo si hay fallos P0 o pruebas físicas/OAuth necesarias no realizadas.
