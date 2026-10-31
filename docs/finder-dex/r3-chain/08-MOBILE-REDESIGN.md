# Etapa 08 — FinderDex Mobile: mismo diseño, interfaz nativa de teléfono

Antes: leer 00-ORCHESTRATOR.md y STATE.md; asegurar etapas 01–07 registradas. Después: actualizar estado/commit/push y ejecutar 09-HARDENING-DELIVERY.md.

## Objetivo
Rediseñar la experiencia de teléfono y ventanas compactas de FinderDex con la misma identidad visual aprobada en Samsung DeX R2.2: blanco limpio, azul hielo, carpetas azules con volumen, glass moderado, iconografía coherente, tipografía refinada y microinteracciones. NO copiar la ventana de escritorio a una pantalla pequeña y NO forzar a touch el layout desktop.

## Límites del proyecto
- Reutilizar motor Voyager, permisos Android, categorías, proveedores, acciones, sesiones, papelera, duplicados y transferencias. Sin refactor masivo ni reescribir con otro framework.
- Mantener experiencia DeX R2.2 pixel-estable excepto correcciones funcionales necesarias. Comprobar layout por dimensiones reales, modo de ventana/input, no por marca/modelo de dispositivo.
- Diseño touch-first, pero compatible con mouse/teclado en ventanas pequeñas.

## Arquitectura UX
1. **Inicio móvil**: lista clara de ubicaciones (Memoria del dispositivo, Descargas, Documentos, Imágenes, etc.), Google Drive, OneDrive y conexiones sólo cuando estén disponibles/autorizadas; accesos a Recientes, Favoritos, Papelera, Análisis y Transferencias. No presentar proveedores sin implementar como activos.
2. **Browser**: título y breadcrumb legibles, barra superior compacta con búsqueda, menú y orden; barra inferior accesible para navegación/acciones según necesidades y sin duplicar controles del sistema. No esconder acciones esenciales detrás de gestos ocultos.
3. **Vista grid/lista**: carpetas con mismos recursos R2.2, tamaños táctiles adecuados, nombres legibles y densidad responsive; listas/columnas sólo cuando sean prácticas en móvil.
4. **Selección múltiple**: pulsación prolongada, casillas/selección visual nítida, barra contextual de acciones (copiar/mover/compartir/borrar/renombrar) con confirmaciones. No degradar selección por el hover escritorio.
5. **Previsualización**: Quick Look/tipo de archivo en full-screen o sheet propio, navegación entre elementos y abrir con app externa mediante content://.
6. **Transferencias**: tarjeta/panel visible con progreso real, cancelar/reintentar, sin modal que bloquee exploración.
7. **Adaptativo**: teléfono portrait y landscape, foldables/tablets, multiventana y retorno a DeX sin perder ruta ni selección; controles de 48dp o equivalentes accesibles y respeto de fontScale.
8. **Estética**: glass sólo en superficies relevantes y fallback sin blur, contraste AA cuando aplique, sombras discretas. No saturar la pantalla con blur/shadows ni material dinámico invasivo. Mantener estados de selección distinguibles.
9. **Gestos**: swipe para navegación únicamente si no interfiere con scroll/selección; drag interno según soporte touch, evitar gestos difíciles de descubrir; vibración/haptics opcionales de sistema sin abuso.
10. **Accesibilidad**: lector de pantalla, foco, orden de navegación, touch targets, textos escalados, modo oscuro si ya está disponible o planificarlo sin romper R2.2 claro.

## Entregables por etapa
- Auditoría de UI móvil actual, mapa pantalla actual -> nueva pantalla; migración incremental.
- Componentes reutilizados de identidad visual sin duplicar tokens ni iconos.
- Capturas o tests de pantalla compacta por CI sólo cuando realmente se ejecuten; siempre etiquetar emulador vs dispositivo físico.
- Tests de navegación, permisos, long press/multi-select, grid/list, resize, restauración de estado y compatibilidad de proveedores.
- Un APK R3 integrado: desktop + mobile en el mismo paquete, no forks divergentes.
- NO afirmar aprobación visual del usuario: marcar PENDING_USER_VISUAL_REVIEW si faltan capturas reales.

## Gate
No romper DeX R2.2; pruebas unitarias, lint, compilación y regresión. Registrar exactamente qué vistas móviles fueron modificadas y cuáles quedan pendientes.

Cierre obligatorio: actualizar STATE.md, commit/push y leer 09-HARDENING-DELIVERY.md inmediatamente.
