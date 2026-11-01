# Etapa 03 — OneDrive en FinderDex

**Antes:** orquestador + estado. **Después:** estado/commit/push y abrir `04-STORAGE-ANALYTICS.md`.

## Objetivo
Añadir OneDrive como ubicación de sidebar con las mismas acciones básicas que Google Drive: autenticación legítima, navegación, búsqueda, descarga/subida, crear, renombrar, mover, papelera (según capacidades), metadatos y progreso.

## Implementación
1. Reutilizar contrato `CloudFileProvider` y controladores probados en etapa 01; no duplicar UI ni lógica de transferencias.
2. Microsoft Graph con OAuth de aplicación Android/MSAL o mecanismo oficial vigente. Aplicación registrada con scopes mínimos (p. ej. archivos autorizados por el usuario); no almacenar client secrets en APK ni usar cuentas ajenas.
3. Soporte correcto de `driveId`/`itemId`, paginación, delta si es necesario, límite de API, enlaces de descarga temporales y sesiones de upload para archivos grandes.
4. Tratar cuentas personales/empresa y carpetas compartidas según el flujo permitido. No afirmar compatibilidad empresarial total sin pruebas.
5. Estados de cuenta, expiración, 401/403/429, cuota, desconexión, conflictos, cancelación y caché acotada.
6. Integrar sidebar sin modificar identidad R2.2; permitir transferencias entre almacenamiento local y OneDrive y, si el motor lo permite, entre nubes sin pérdida de datos. Para mover entre nubes usar copiar-verificar-eliminar **sólo con consentimiento explícito**.
7. Si faltan App Registration/redirect URI/client ID de Microsoft: implementar tests con mocks, reportar `BLOCKED_EXTERNAL` y continuar etapas independientes.

## Pruebas / Gate
- Tests de adaptación Graph, tokens, paginación, colisiones, upload resumible, errores y permisos.
- CI sin secretos ni cuenta real.
- Prueba real sólo si hay credenciales legítimas y usuario ha consentido.
- Build, lint y comprobación de no regresión de Drive.

## Cierre
Actualizar `STATE.md`, commit/push, leer y ejecutar **04-STORAGE-ANALYTICS.md**.
