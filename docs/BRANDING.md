# Branding de QUIPU

## Archivo maestro

El recurso oficial de identidad visual es:

```text
assets/branding/quipu.png
```

Características:

- PNG de 1080 × 1080 px.
- Fondo blanco.
- Símbolo cuadrado con marco verde, documento y marcapáginas.
- Colores verde y gris.
- Es la fuente de verdad para generar los recursos Android.

En QUIPU 1.1.7 se incorporó el nuevo logo cuadrado aprobado y se retiró el
logo inclinado anterior. El archivo histórico sigue disponible en el historial
de Git, pero no debe usarse para nuevos recursos.

El archivo maestro **no debe modificarse** desde los recursos de la aplicación.
Cualquier actualización futura debe partir de un nuevo maestro aprobado y
regenerar los derivados.

## Recursos Android generados

### Icono adaptativo

```text
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
```

Ambos declaran las tres capas:

- `background`: color `@color/quipu_icon_background` (`#FFFFFF`).
- `foreground`: `@drawable/ic_launcher_foreground`.
- `monochrome`: `@drawable/ic_launcher_monochrome`.

### Capas rasterizadas

```text
app/src/main/res/drawable-nodpi/ic_launcher_foreground.png
app/src/main/res/drawable-nodpi/ic_launcher_monochrome.png
app/src/main/res/drawable-nodpi/quipu_logo.png
```

- `ic_launcher_foreground.png`: 432 × 432 px ARGB. Lienzo transparente con una
  teja blanca redondeada centrada que contiene el símbolo oficial. Se conserva
  el interior blanco del documento y el fondo blanco del maestro; no se
  reinterpretan ni deforman los trazos.
- `ic_launcher_monochrome.png`: 432 × 432 px ARGB. Silueta monocromática blanca
  generada a partir de las zonas no blancas del maestro, con transparencia en
  el fondo y en los huecos interiores.
- `quipu_logo.png`: 512 × 512 px ARGB. Versión para Compose, con la misma teja
  blanca redondeada. Se utiliza en la bienvenida y en la tarjeta Acerca de.

El contenido se centra ópticamente y mantiene la zona segura de Android para
las máscaras circular, cuadrada redondeada y squircle.

### Iconos heredados

Para coherencia entre densidades se generan variantes redondeadas y circulares:

| Densidad | Tamaño | Recursos |
|---|---:|---|
| `mdpi` | 48 px | `mipmap-mdpi/ic_launcher.png`, `ic_launcher_round.png` |
| `hdpi` | 72 px | `mipmap-hdpi/ic_launcher.png`, `ic_launcher_round.png` |
| `xhdpi` | 96 px | `mipmap-xhdpi/ic_launcher.png`, `ic_launcher_round.png` |
| `xxhdpi` | 144 px | `mipmap-xxhdpi/ic_launcher.png`, `ic_launcher_round.png` |
| `xxxhdpi` | 192 px | `mipmap-xxxhdpi/ic_launcher.png`, `ic_launcher_round.png` |

Los iconos heredados usan fondo blanco, el mismo símbolo y la misma zona
segura. `ic_launcher` usa esquinas redondeadas; `ic_launcher_round` usa recorte
circular.

## Notificaciones

El icono de notificaciones es independiente del launcher:

```text
app/src/main/res/drawable/ic_notification.xml
```

Se mantiene como un vector monocromático de campana, usado por
`ReminderNotificationManager` mediante `R.drawable.ic_notification`. No debe
sustituirse por el logo completo.

## Paleta

El fondo del icono utiliza blanco para coincidir con el maestro:

```text
#FFFFFF
```

La paleta de Compose no cambia; el color del icono se define en
`res/values/colors.xml` como `quipu_icon_background`.

## Actualización futura

1. Partir del nuevo PNG maestro aprobado.
2. Mantener las proporciones y los colores originales del símbolo.
3. Generar un foreground con teja blanca redondeada y exterior transparente,
   preservando el interior blanco del documento.
4. Generar una silueta monocromática independiente.
5. Centrar el contenido y respetar la zona segura de Android.
6. Regenerar los PNG heredados en las cinco densidades.
7. Ejecutar:

   ```powershell
   .\gradlew.bat clean
   .\gradlew.bat test
   .\gradlew.bat assembleDebug
   ```

8. Instalar en el emulador y verificar el icono con máscaras circular,
   cuadrada redondeada y squircle.
9. Comprobar que `ic_notification.xml` no haya sido reemplazado.
