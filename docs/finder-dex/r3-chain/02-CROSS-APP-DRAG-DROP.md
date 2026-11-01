# Etapa 02 — Drag & drop ENTRE aplicaciones en Android/DeX

**Antes:** leer orquestador/estado. **Después:** evidencia, commit/push, abrir `03-ONEDRIVE.md`.

## Contexto comprobado
Existe `desktop/FinderDragDrop.kt` con gestos internos, estado de `FinderPendingMove` y destinos dentro de FinderDex. **Eso no implica drag & drop entre apps**. Inspeccionar también `FinderDragPremium.kt`, selección múltiple y menú contextual.

## Objetivo
1. De FinderDex hacia apps compatibles: arrastrar uno o varios documentos, imágenes y carpetas compatibles cuando corresponda; usar `ClipData`, `content://`, proveedor de contenido con permisos temporales y la API de drag & drop global aplicable. Jamás emitir `file://` sin protección.
2. Desde otras apps hacia FinderDex: recibir `DragEvent`/Compose drag-and-drop y URI en el directorio de destino; aceptar sólo si permisos y tipos MIME lo permiten; UI de destino válida/inválida.
3. Multi-selección intacta. El gesto no debe sustituir doble clic, scroll, marquee, clic derecho ni hover 1.06x.
4. Semántica explícita: copiar por defecto entre apps; mover sólo cuando haya control seguro y confirmación donde aplique. Un DROP aceptado no demuestra que la app externa guardó el archivo; no borrar origen por aceptación del gesto.
5. Transferencias largas y cloud: `ContentProvider.openFile` / streaming o staging controlado, cancelación y limpieza. No bloquear el hilo de UI.
6. Drop en carpetas, sidebar, doble panel futuro y ventana vacía; integrarse con transferencias, gestión de conflictos y carpeta de destino.
7. Android 17/DeX, pantallas grandes, window resize y accesibilidad. No garantizar que WhatsApp, Gmail, Chrome u otras apps acepten todos los MIME, algunos pueden requerir compartir/adjuntar.

## Pruebas
- Tests unitarios de permisos, MIME, múltiples URI, validación de destino, copy/move y cancelación.
- Instrumentadas cuando se pueda: simulated drop, permiso efímero, revocación, dos ventanas y selección.
- Checklist físico DeX con dos apps compatibles, ambos sentidos, 1/múltiples archivos, carpeta, permisos denegados y comportamiento al abortar.
- Build + tests + lint; registrar PASS automatizado vs físico separado.

## Cierre
Registrar estado y SHA. No fingir interoperabilidad de todas las apps. Leer y ejecutar **03-ONEDRIVE.md**.
