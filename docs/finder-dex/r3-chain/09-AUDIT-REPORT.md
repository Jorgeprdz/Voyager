# FinderDex R3 — etapa 09: auditoría de integración y entrega

**Rama:** `feat/finderdex-r3-implementation`  
**Base R2.2 protegida:** `feat/finder-dex-foundation-01` @ `a5bd2dcd6be376cd867a4b411277f6d3e8f68cf6`  
**Fecha:** 2026-10-31 (UTC)  
**Resultado provisional:** `PARTIAL / BLOCKED_EXTERNAL / PENDING_PHYSICAL_DEX / PENDING_USER_VISUAL_REVIEW`.

Este informe no declara la funcionalidad completa por el mero hecho de compilar. Evidencia de etapas anteriores en `STATE.md`; la validación de **este** commit y su SHA256 deben consultarse en el workflow posterior a su publicación.

## Matriz de funcionalidades verificadas vs pendientes

| Función | Evidencia presente en la rama | Estado funcional | Lo que NO está probado/completo |
|---|---|---|---|
| Desktop R2.2 | Shell de FinderDex, controles de ventana y pruebas de regresión preexistentes | PASS_AUTOMATED previo | Pixel-stability física en monitores DeX |
| Mobile R3 | `FinderMobileTokens`, `HomeScreen`, `BrowserScreen`, compilación CI de etapa 08 | PARTIAL / PENDING_USER_VISUAL_REVIEW | Rediseño integral de todas las pantallas, pruebas táctiles e imágenes del dispositivo |
| Google Drive | `CloudFileProvider` y sidebar en estado desconectado | PARTIAL / BLOCKED_EXTERNAL | OAuth, acceso real a cuenta, listados y transferencias cloud |
| OneDrive | `OneDriveGraphProvider`, contrato HTTP y pruebas de rutas; sidebar desconectado | PARTIAL / BLOCKED_EXTERNAL | Microsoft app registration, OAuth/MSAL, permisos/consentimiento, uploads grandes, copy asíncrono, E2E |
| Arrastre externo | `FinderCrossAppTransfer`, URIs `content://`, grants temporales y pruebas de política | PASS_AUTOMATED de política / PENDING_PHYSICAL_DEX | Drop real entre ventanas/apps y compatibilidad de cada receptor MIME; carpetas como origen |
| Analizador | `StorageCategoryIndex.scanAll`, `StorageAnalyticsSummary`, `FinderAnalyticsDialog`, detector de duplicados reutilizado | PARTIAL / PASS_AUTOMATED | Reporte completo por volumen, limpieza asistida, áreas inaccesibles, datos físicos |
| Recientes | `FinderRecentStore`, `FinderRecentsDialog` y pruebas; apertura real separada de mtime | PARTIAL / PASS_AUTOMATED | Filtros por fecha/tipo/origen y navegación móvil completa |
| Red (SMB/SFTP/FTP/WebDAV) | Proveedores/ConnectionRepository preexistentes, conexiones guardadas en sidebar R3 | PARTIAL | Navegador de proveedor nativo DeX, pruebas contra servidores reales, reconexión/credenciales |
| Compartir / Quick Share | `FileUtils.createShareIntent`, `ShareIntentPlan` con `content://`, chooser Android | PASS_AUTOMATED de contrato / PENDING_PHYSICAL_DEX | Quick Share depende del sistema y app receptora |
| Pestañas | Sesiones locales, `FinderDesktopTabPolicy`, controlador de columnas por sesión | PARTIAL / PASS_AUTOMATED | Persistencia tras reinicio y validación mouse/teclado/scroll real |
| Doble panel | Segundo controlador de navegación de lectura, divisor por ancho de ventana | PARTIAL / PENDING_PHYSICAL_DEX | Copia/verificación entre paneles, foco dual, drag externo de ambos |
| Renombrado masivo | `FinderBulkRenamePlanner`, preview y batch con `FileProvider.rename` + pruebas | PARTIAL / PASS_AUTOMATED | Interfaz de todas las reglas, operaciones remotas, cancelación en dispositivo |
| Centro de transferencias | Motor de operaciones preexistente con progreso y cancelación | PARTIAL | Panel unificado, reintento/pausa reales y supervivencia de jobs largos |

## Seguridad y límites

- **No se deben inventar tokens OAuth ni guardar credenciales**. Las nubes continúan desconectadas hasta autorización externa del usuario.
- `FinderCrossAppPolicy` acepta únicamente `content://` bien formados, máximo 64 URIs, operación copy-only y **nunca autoriza eliminar origen**.
- El renombrado muestra una vista previa, valida nombres y conflictos, y ejecuta sólo sobre el proveedor local autorizado; resultados parciales no son transacciones atómicas.
- El análisis de almacenamiento informa sobre archivos escaneados accesibles: no extrapola cachés o carpetas privadas, no sube hashes a servicios remotos y **no elimina archivos automáticamente**.
- Los movimientos entre diferentes proveedores están **deshabilitados/no implementados** en el panel secundario R3 mientras no exista copia+verificación+confirmación. No se atribuye éxito a un drop aceptado.
- Compartir utiliza `ACTION_SEND`/`ACTION_SEND_MULTIPLE`, `content://` y permisos temporales. No se garantiza que Quick Share exista.
- El workflow construye la APK principal `app-debug.apk` separada de `app-debug-androidTest.apk`, comprueba package `com.finderdex.preview`, firma y recursos de launcher, y emite SHA256 real.
- `compileDebugAndroidTestKotlin` / `assembleDebugAndroidTest` **no significan ejecución de tests instrumentados**: no se ejecutaron en hardware durante este pase. Pruebas unitarias + lint + compilación sí se ejecutan en GitHub Actions.
- No afirmar rendimiento/jank, TLS/host key reales, ruta de persistencia o compatibilidad con Android 17/DeX sin pruebas físicas y perfiles correspondientes.

## Entrega de APK sin instalar automáticamente

- Script: `scripts/finderdex-r3-download.sh` (clon de downloader R2.2, sin modificarlo).
- Requiere **Termux local del usuario**, `gh` autenticado, acceso concedido a Descargas y el SHA de 40 caracteres del commit cuya ejecución haya concluido en `success`.
- Selecciona solo el artifact `FinderDex-R3-APK`, compara exactamente el SHA del workflow y del APK descargado; no sobrescribe otro APK y nunca instala por sí mismo.
- Ruta destino, **sólo si el script se ejecuta en Termux autorizado**: `/storage/emulated/0/Download/FinderDex-R3-debug.apk`.
- Un agente GitHub remoto **no tiene acceso automático al Termux del teléfono**. Un artifact publicado no demuestra que esté en Descargas.

## Checklist de aceptación física (sin marcas PASS inventadas)

- [ ] Samsung DeX en monitor externo pequeño y grande: sidebar, toolbar, status bar, zoom, carpetas volumétricas y hover iguales a R2.2.
- [ ] Variar tamaño/DPI/rotación de ventana y confirmar layout responsivo, sin clipping ni fugas de foco.
- [ ] Pestañas Ctrl+T / Ctrl+W / Ctrl+Tab: historial, selección y scroll independientes; suspender/reanudar.
- [ ] Doble panel Ctrl+Shift+D: navegación separada, divisor con mouse; comprobar que copiar/mover no se presenta como exitoso sin confirmación real.
- [ ] Arrastrar archivo a una segunda app DeX con MIME compatible y viceversa; probar rechazo de MIME y permiso URI revocado; origen intacto.
- [ ] Analizador: SD/USB si disponibles, permisos revocados, scan cancelado, árbol profundo, symlinks, duplicados grandes, estimaciones sin borrado.
- [ ] Recientes: apertura exitosa, modificación sin apertura, historial vaciado sin borrar fichero.
- [ ] Compartir varios archivos por el menú Android; Quick Share sólo si instalado y compatible.
- [ ] Renombrado masivo con Unicode, extensiones compuestas, colisiones, 1000 entradas, ejecución parcial y permisos revocados; sin sobrescrituras.
- [ ] Browser Mobile en portrait/landscape, touch targets, selección por pulsación prolongada, grid/list, búsqueda, breadcrumb, lector de pantalla y fontScale.
- [ ] Regresión: volver de Mobile a Desktop, actividad en ventanas múltiples, desconexión de red, sin perder datos existentes.
- [ ] OAuth Google Drive y OneDrive por credenciales/registros propios; flujos cloud→local/local→cloud sólo después de dar permisos explícitos.

## Decisión

**No declarar R3 completo/producción**. El APK integra el código parcial de las etapas 01–08 y puede distribuirse como **build de evaluación** después de CI satisfactorio. Permanecen `BLOCKED_EXTERNAL`, `PENDING_PHYSICAL_DEX`, `PENDING_USER_VISUAL_REVIEW` y funciones `PARTIAL` descritas arriba. La rama estable R2.2 no se modifica.
