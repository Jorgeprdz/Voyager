# FinderDex — Fase 1

Base: Voyager 594d9a839e19a84145e1e7d5bd4a058718b4247f, GPLv3.
Ejecución autónoma solicitada; alcance: navegación local de escritorio y APK experimental.

- [ ] Controlador de columnas independiente de browseState: consulta capturada por sesión,
  cancelación, identificadores de carga, selección y scroll por columna, historial.
- [ ] Shell Compose propio: toolbar, sidebar ajustable, columnas, preview y estado.
  Referencia visual: Finder moderno; recursos vectoriales originales, tipografía del sistema.
- [ ] Integrar por tamaño real de ventana (compacta <600dp, media <840dp, amplia >=840dp).
  Mantener motor, permisos y UI móvil; sincronizar ubicación y selección al cambiar tamaño.
- [ ] Identidad aislada com.finderdex; rama experimental, CI con pruebas/lint/APK debug.
- [ ] Verificar carreras de cargas, historia, errores, límites de sesión y navegación local.
  Obtener captura real si hay dispositivo autorizado; documentar límites sin afirmar pixel-perfect.

Archivos: desktop/FinderColumnController.kt (estado y navegación),
desktop/DesktopModeController.kt (modo adaptable), desktop/FinderDesktopScreen.kt (shell),
FileBrowserViewModel.kt (lectura por sesión y sincronización), Navigation.kt (host adaptable).
No operaciones destructivas nuevas; las operaciones existentes mantienen FileOperationCoordinator.

Riesgos a probar: respuesta tardía tras cambiar de carpeta, columna desconectada,
permiso revocado, ruta ajena a sesión, cambio móvil/escritorio sin perder selección.
Pendientes Fase 2: iconos/lista/galería, Quick Look, operaciones/contextuales, DnD, etiquetas,
papelera y pestañas de escritorio. Proveedores remotos conservan su UI móvil en Fase 1.
