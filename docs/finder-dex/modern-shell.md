# Shell moderna — FinderDex

Dirección solicitada: Finder Big Sur/Sonoma, superficie continua, sidebar ligera,
contenido amplio. La segunda imagen mencionada no está disponible en esta conversación;
no se declara comparación exacta ni fidelidad pixel-perfect.

- Cuadrícula inicial real con selección y apertura separadas; columnas opcionales.
- Se elimina el marco interior y la vista previa vacía al iniciar.
- Caption transparente en API 35+, toolbar dibujada en la misma superficie.
- Botones de ventana reales reservados mediante bounding rects del sistema;
  áreas interactivas excluidas del gesto de arrastre; el resto mantiene arrastre nativo.
- Si el fabricante no expone los límites de los controles, se conserva su espacio
  superior sobre el mismo fondo para evitar superposiciones.
- Navegación y proveedores de Voyager conservados; interfaz móvil por tamaño de ventana.

Verificación local: geometría caption 4/4, contrato localización 4/4.
Pendiente: Actions, pruebas UI Android y comparación en DeX con la segunda imagen.
