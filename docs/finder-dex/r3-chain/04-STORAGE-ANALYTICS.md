# Etapa 04 — Analizador de almacenamiento

**Antes:** orquestador + estado. **Después:** actualizar/commit/push y ejecutar `05-RECENTS-NETWORK-SHARING.md`.

## Objetivo
Llevar la gestión de espacio de Mis Archivos a una experiencia FinderDex de escritorio, reutilizando el detector de duplicados y el indexador de categorías ya existentes en Voyager.

## Auditoría
Revisar `ui/screens/DuplicatesScreen.kt`, `data/index/`, `StorageSearchScreen.kt`, `HomeScreen.kt` y helpers de almacenamiento. No construir un segundo escáner si ya existe uno.

## Alcance
1. Vista de espacio por volumen real (memoria interna, SD/USB cuando haya permisos), categorías y tendencias de tamaño disponibles sin inventar métricas.
2. Listados: mayores archivos, mayores carpetas, duplicados por contenido (hash sólo cuando sea necesario), descargas antiguas y limpieza sugerida.
3. Scans incrementales y cancelables en IO, paginación y límites de memoria; progreso visible y nada de bloquear Compose/scroll.
4. Explicar exclusiones debidas a restricciones de permisos Android; nunca contar cache restringido como explorado.
5. Previsualizar qué se liberaría. **Nunca borrar automáticamente**, ni marcar seguro eliminar únicamente por edad/tamaño; usar papelera y confirmaciones existentes.
6. Resultados navegables: abrir ubicación, seleccionar en FinderDex y operar con herramientas actuales.
7. Offline por defecto; no subir hashes/listados de archivos personales a ningún servicio.
8. Mantener estilo R2.2, panel de análisis nativo dentro del shell actual.

## Tests
- Fixtures con árbol profundo, permisos revocados, symlinks, carpetas inaccesibles, archivos gigantes y duplicados, cancelación.
- Verificar que no duplica conteos y que el resumen coincide con fuentes del sistema dentro de limitaciones conocidas.
- Build, lint, tests; estado DeX pendiente hasta prueba real.

## Cierre
`STATE.md` + commit/push + leer y ejecutar **05-RECENTS-NETWORK-SHARING.md**.
