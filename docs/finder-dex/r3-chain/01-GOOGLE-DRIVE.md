# Etapa 01 — Google Drive nativo en FinderDex

**Antes:** leer `00-ORCHESTRATOR.md` y `STATE.md`. **Después:** actualizar estado, commit/push, leer y ejecutar `02-CROSS-APP-DRAG-DROP.md`.

## Objetivo visible
Google Drive en la sidebar como ubicación conectable; navegación por carpetas, búsqueda dentro del proveedor, descarga, subida, copia/movimiento/renombrado/borrado cuando la API y los permisos lo autoricen. Estado de red y errores claros; ningún cambio de estética R2.2.

## Auditoría obligatoria
Inspeccionar `app/src/main/java/com/voyagerfiles/data/repository/FileProvider.kt`, `FileProviderFactory.kt`, `ConnectionRepository.kt`, `app/src/main/java/com/voyagerfiles/desktop/FinderDesktopScreen.kt` y proveedores remotos existentes. Verificar qué acciones soporta el motor y qué queda sólo en interfaz móvil. Reutilizar DTO, sesiones, paginación, transferencias y cancelación.

## Diseño funcional
1. Definir interfaz de `CloudFileProvider` o adaptador reutilizable, sin duplicar lógica con OneDrive.
2. Integración oficial Google Drive API y flujo OAuth para Android, siguiendo documentación vigente. Scopes mínimos; nunca credenciales incrustadas, ni secretos de cliente OAuth para Android. Si hace falta registrar app/client ID o configurar huellas, documentar el requisito y bloquear autenticación real con estado `BLOCKED_EXTERNAL` sin inventar éxito.
3. Cuenta conectada/desconectada, reconexión, revocación y gestión segura de tokens con API/almacenamiento adecuado. No escribir tokens en logs.
4. Sidebar: Google Drive, nombre de cuenta sólo tras autorización, estado offline; cuentas múltiples sólo si la arquitectura lo permite de manera robusta.
5. Listado con paginación, orden y metadatos; distinguir Google Docs/Sheets/Slides (archivos nativos) de binarios, manejar exportación sólo a formatos soportados.
6. Operaciones: abrir/descargar a demanda, subir con reintentos para archivos grandes, renombrar, crear carpeta, mover, copiar y mandar a papelera según capacidades reales; detección de conflictos y permisos.
7. Transferencia con progreso, cancelación, manejo de 401/403/429, cuota, desconexión, token expirado y archivos eliminados remotamente.
8. No presentar Drive como sistema de archivos local POSIX. Mantener IDs remotos, MIME correcto y `content://` al compartir con otras apps.
9. Estado offline con cache limitada/opt-in; nada de sincronización global automática ni sobrescrituras silenciosas.

## Pruebas / Gate
- Unit: paginación, errores de red, cuotas, conflictos, expiración y reintento; adaptador contra backend simulado.
- Integración: lista/sube/descarga/renombra/mueve con cuenta de prueba **sólo si el usuario ha autorizado credenciales**.
- Build, lint, tests del proyecto; ningún test de red obligatorio para CI público.
- Validar sidebar y navegación en ventana DeX con fake provider; identificar pruebas físicas pendientes.
- Reportar claramente `PARTIAL` cuando solo exista UI/mock.

## Cierre obligatorio
Actualizar `STATE.md`, commit y push. Si la parte OAuth queda bloqueada, marcarlo y seguir a la etapa 02 independientemente, sin decir que Drive quedó funcional. Leer **02-CROSS-APP-DRAG-DROP.md**.
