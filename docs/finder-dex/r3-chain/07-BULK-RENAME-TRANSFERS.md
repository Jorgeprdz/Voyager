# Etapa 07 — Renombrado masivo y centro de transferencias

Antes: orquestador + estado. Después: evidencia, estado, commit/push y 08-MOBILE-REDESIGN.md.

## Renombrado masivo
- Acción contextual para varios elementos: prefijo/sufijo, secuencia con ceros, fecha, búsqueda/reemplazo, mayúsculas/minúsculas y limpieza de nombres.
- VISTA PREVIA obligatoria antes de confirmar; mostrar antiguo -> nuevo, errores y conflictos de nombres, caracteres inválidos, nombres reservados y límites del proveedor.
- Tratar extensiones con cuidado; no cambiar extensión inadvertidamente salvo opción explícita.
- Simular toda la operación antes de ejecutarla; resolver colisiones también entre archivos seleccionados; orden determinista.
- Cancelar cuando sea seguro y reportar resultados parciales claramente. No prometer atomicidad entre proveedores.
- Atajos de escritorio sin interferir con F2 renombrado individual ni selección múltiple.

## Gestor de transferencias
- Panel compacto en escritorio y accesible desde móvil después: operaciones en curso, porcentaje cuando sea calculable, velocidad/ETA sólo si hay datos, errores, reintentar, pausar cuando el protocolo lo permita y cancelar.
- Reutilizar operaciones existentes y unificarlas; no iniciar un segundo motor de copia independiente.
- Sobrevivir cambios de ventanas y configuración; jobs prolongados sólo con restricciones Android correctas (foreground notification/work manager según uso real).
- Progreso en subidas/bajadas nube, redes y local; no mostrar 100% antes de confirmación del destino.
- Seguridad: copias temporales limpias; conflictos sin pérdida de datos; no exponer rutas privadas ni credenciales en notificaciones.

## Pruebas
Renombrado con fixtures multiarchivo, unicode, extensión, colisiones, rutas inaccesibles, 1000 elementos; monitor de transferencias con desconexión, cancelación, retry y proceso interrumpido.
Build + lint + unit, evidencias y pendientes de DeX.

Cierre: actualizar STATE.md, commit/push; leer y ejecutar 08-MOBILE-REDESIGN.md.
