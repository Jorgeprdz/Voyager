# Etapa 06 — Pestañas y doble panel de escritorio

Antes: orquestador + estado. Después: evidencia, estado, commit/push y 07-BULK-RENAME-TRANSFERS.md.

## Alcance
- Pestañas nativas para varias ubicaciones: nueva, cerrar, reabrir, cambiar (teclado y mouse), preservar historial, selección y scroll por pestaña.
- Doble panel opcional con divisor redimensionable, navegación/selección/scroll/breadcrumb independientes, foco visible del panel activo.
- Copiar y arrastrar entre paneles locales, nube y SMB/otras conexiones sólo cuando esas fuentes estén verdaderamente disponibles. Manejo de conflictos, permisos, errores, cancelación.
- En caso de proveedores distintos, copiar y verificar antes de ofrecer eliminación de origen; NO tratar un drop como éxito ni borrar el original automáticamente.
- Persistencia del estado de pestañas/paneles que no guarde credenciales ni rutas inaccesibles; recuperar con fallback cuando permisos se pierden.
- Adaptar a ancho real de ventana DeX: en ventanas compactas esconder/colapsar el segundo panel sin perder estado; sin clipping.
- Mantener R2.2 intacta visualmente; reutilizar toolbar, sidebar y patrones de selección, no crear una estética nueva.
- No romper el drag & drop entre aplicaciones implementado en etapa 02 ni el interno.

## Auditoría técnica
Investigar FinderColumnController, FinderDesktopScreen, ViewModel y sesión de proveedores. Evitar compartir un único browseState mutable para dos paneles, potencial causa de saltos y operaciones en carpeta equivocada.

## Validación
Unit tests de independencia, cambio rápido de pestañas, cancelación de cargas tardías, historial y selección; pruebas de doble panel, resize y copia segura.
CI build, lint, tests; checks físicos DeX PENDING hasta ejecución real.

Cierre: actualizar STATE.md con commit/workflow y leer 07-BULK-RENAME-TRANSFERS.md.
