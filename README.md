# FinderDex

Gestor de archivos para Android con interfaces móvil y de escritorio, temas configurables e interacción con ratón y teclado. Basado en [Voyager](https://github.com/AlanHuang99/Voyager), de Alan Huang, bajo GPLv3.

## Instalar

Descarga la APK firmada desde [GitHub Releases](https://github.com/Jorgeprdz/Voyager/releases). Requiere Android 8.0 o posterior.

La aplicación de producción usa **`com.finderdex`**. La variante de desarrollo usa `com.finderdex.preview` y puede coexistir con ella; sus datos y permisos son independientes.

## Uso

FinderDex abre en **Inicio**. Desde allí puedes entrar al almacenamiento, favoritos, categorías y conexiones. En Ajustes puedes elegir **Automático, Móvil o Escritorio**. El modo automático detecta ratón/trackpad o un entorno de escritorio; la distribución se adapta al tamaño de la ventana.

En el explorador de escritorio:

- Clic para seleccionar y doble clic para abrir; Ctrl/Shift para selección múltiple y por rango.
- Selección rectangular, arrastre de archivos, menú contextual, pestañas y doble panel local.
- Vistas de iconos, detalles, columnas y galería; ordenar y agrupar por nombre, tipo, tamaño o fecha.
- Espacio abre/cierra la vista rápida; Escape cierra y las flechas recorren los archivos visibles. F2 permite renombrar.
- Ctrl+C/X/V, Ctrl+A, Supr y navegación por historial mediante Alt+flechas.

## Funciones

- Almacenamiento interno, SD/USB y carpetas autorizadas mediante el selector de Android.
- Copia, movimiento, renombrado, papelera local, duplicados y búsqueda de archivos.
- Conexiones SFTP, FTP, SMB y WebDAV; integración Google Drive y OneDrive que requiere configurar sus registros OAuth.
- Creación ZIP y extracción ZIP, TAR, TGZ, TBZ2, GZIP y BZIP2. **RAR se reconoce, pero no se extrae.**
- Temas Glass, Material, One UI y Fluent; luminosidad, contraste, transparencia y movimiento completo/reducido/desactivado. La desactivación de animaciones del sistema tiene prioridad.
- «Guardar en FinderDex» recibe uno o varios archivos compartidos, pide destino y utiliza el motor de transferencias con progreso, cancelación y resolución de conflictos.
- Las ubicaciones locales autorizadas de FinderDex aparecen en el selector de documentos de Android para abrir y guardar. Android administra el selector y las aplicaciones predeterminadas.

## Estado y límites

El explorador de escritorio utiliza ubicaciones locales. Las conexiones de red y nube conservan sus navegadores existentes. La cola no reanuda automáticamente una transferencia tras la muerte del proceso; el historial identifica operaciones interrumpidas. El rediseño completo Material 3 Expressive y la paridad de todos los proveedores siguen siendo trabajo separado.

La validación automatizada y las comprobaciones físicas pendientes están registradas en [el informe de integración](docs/finder-dex/desktop-integration-2026-11-06.md). Compilar las pruebas Android no equivale a ejecutarlas en un teléfono o Samsung DeX.

## Compilar

JDK 17 y Android SDK 35:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Para firmar una compilación `assembleRelease`, configura `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`. Las claves privadas se mantienen fuera del repositorio. El publicador heredado de Voyager no corresponde a FinderDex; las versiones FinderDex utilizan etiquetas `finderdex-v<versión>`.

## Licencia y origen

[GNU GPL v3.0](LICENSE). FinderDex conserva el código y los créditos de Voyager y añade las interfaces, temas e integración de escritorio de este fork.
