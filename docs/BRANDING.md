# Branding de QUIPU

## Archivo maestro

El recurso oficial de identidad visual es:

```text
assets/branding/quipu.png
```

Características:

- PNG de 1080 × 1080 px.
- Fondo blanco.
- Símbolo en verde y gris.
- Es la fuente de verdad para generar los recursos Android.

El archivo maestro **no debe modificarse, moverse ni sustituirse** desde los
recursos de la aplicación. Cualquier actualización futura del logo debe partir
de un nuevo maestro aprobado y generar nuevos derivados.

## Recursos Android generados

### Icono adaptativo

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

Ambos declaran las tres capas:

- `background`: color `@color/quipu_icon_background` (`#F9F8F4`).
- `foreground`: `@drawable/ic_launcher_foreground`.
- `monochrome`: `@drawable/ic_launcher_monochrome`.

### Capas rasterizadas

```text
app/src/main/res/drawable-nodpi/ic_launcher_foreground.png
app/src/main/res/drawable-nodpi/ic_launcher_monochrome.png
```

- `ic_launcher_foreground.png`: 432 × 432 px ARGB. Contiene únicamente el
  símbolo oficial sobre fondo transparente. El blanco exterior fue eliminado
  sin modificar el maestro.
- `ic_launcher_monochrome.png`: 432 × 432 px ARGB. Silueta monocromática
  generada por separado, con transparencia en el fondo y en los huecos
  interiores. No reutiliza directamente la capa a color.

El contenido se centra ópticamente y se limita a un radio máximo de 32 dp
dentro del lienzo adaptativo de 108 dp. Esto deja margen dentro de la zona
segura de Android y evita recortes en máscaras circulares, cuadradas
redondeadas y squircle.

### Iconos heredados

Para coherencia entre densidades se generan variantes redondeadas y circulares:

| Densidad | Tamaño | Recursos |
|---|---:|---|
| `mdpi` | 48 px | `mipmap-mdpi/ic_launcher.webp`, `ic_launcher_round.webp` |
| `hdpi` | 72 px | `mipmap-hdpi/ic_launcher.webp`, `ic_launcher_round.webp` |
| `xhdpi` | 96 px | `mipmap-xhdpi/ic_launcher.webp`, `ic_launcher_round.webp` |
| `xxhdpi` | 144 px | `mipmap-xxhdpi/ic_launcher.webp`, `ic_launcher_round.webp` |
| `xxxhdpi` | 192 px | `mipmap-xxxhdpi/ic_launcher.webp`, `ic_launcher_round.webp` |

Los iconos heredados usan el mismo fondo de papel, el mismo símbolo y la misma
zona segura. `ic_launcher` usa esquinas redondeadas; `ic_launcher_round` usa
recorte circular.

## Notificaciones

El icono de notificaciones es independiente del launcher:

```text
app/src/main/res/drawable/ic_notification.xml
```

Se mantiene como un vector monocromático de campana, usado por
`ReminderNotificationManager` mediante `R.drawable.ic_notification`. No debe
sustituirse por el logo completo.

## Paleta

El icono utiliza el tono de papel del sistema visual:

```text
#F9F8F4
```

La paleta de Compose no cambia; el color del icono se define en
`res/values/colors.xml` como `quipu_icon_background`.

## Actualización futura

1. Partir del nuevo PNG maestro aprobado.
2. Mantener las proporciones y los colores originales del símbolo.
3. Generar un foreground transparente eliminando únicamente el fondo.
4. Generar una silueta monocromática independiente.
5. Centrar el contenido y mantenerlo dentro de un radio máximo de 32 dp
   (equivalente a 128 px en el lienzo de 432 px).
6. Regenerar los WebP heredados en las cinco densidades.
7. Ejecutar:

   ```powershell
   .\gradlew.bat clean
   .\gradlew.bat test
   .\gradlew.bat assembleDebug
   ```

8. Instalar en el emulador y verificar el icono con máscaras circular,
   cuadrada redondeada y squircle.
9. Comprobar que `ic_notification.xml` no haya sido reemplazado.
