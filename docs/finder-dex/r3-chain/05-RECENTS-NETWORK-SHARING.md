# Etapa 05 — Recientes, almacenamiento de red y Quick Share

Antes: leer 00-ORCHESTRATOR.md y STATE.md. Después: cerrar con estado/commit/push y leer y ejecutar 06-TABS-DUAL-PANE.md.

## Recientes
- Agregar ubicación Recientes a la barra lateral de DeX y a la navegación móvil posterior. Separar documentos abiertos RECIENTEMENTE por FinderDex (historial verificado local) de documentos MODIFICADOS recientemente (fecha filesystem); nunca presentar mtime como último acceso.
- Filtrar por tipo, origen y fecha, abrir ubicación de origen, y permitir limpiar historial de uso sin borrar archivos.
- No registrar nombres de archivos en telemetría ni sincronizar historial automáticamente. Respetar permisos revocados.

## Almacenamiento de red
- Auditar proveedores existentes: SMB, SFTP, FTP, WebDAV, ConnectionRepository y ConnectionsScreen.
- Integrar conexiones en sidebar y navegación de escritorio. Conectar, desconectar, navegar, subir, descargar, crear carpetas, renombrar, mover/copiar sólo si el proveedor lo permite.
- Verificar certificados TLS, huellas SSH/host keys y credenciales; NO desactivar comprobación de certificados/hosts. FTP sin cifrar: advertencia clara, no habilitar silenciosamente.
- Estados de timeout, offline, reconexión, sesiones y copia con cancelación; no exigir que la interfaz móvil anterior cambie aún.

## Quick Share / compartir
- Añadir Share de sistema vía ACTION_SEND/ACTION_SEND_MULTIPLE con content:// y permisos temporales. Ofrecer Quick Share cuando Android lo exponga, sin APIs privadas, sin garantizar que esté instalado.
- Multiarchivos donde sea compatible; tipos MIME, fallos y tamaño de archivos. Nada de exponer file://.

## Validaciones
- Tests de historia local, filtros, permisos, reconexión y fallos de red con simulaciones.
- Instrumentadas para intents si es posible; prueba real de Quick Share queda PENDING_PHYSICAL_DEX.
- Estética R2.2 congelada. Integrar sin mover controles visuales ya aprobados.
- Build, unit tests, lint, evidencia y estado.

Al cierre actualizar STATE.md, commit/push, abrir 06-TABS-DUAL-PANE.md de inmediato.
