# GitHub Actions de QUIPU

## Workflows

### `android-ci.yml`

Ejecuta tres jobs independientes en `ubuntu-24.04` con JDK Temurin 21, el Gradle
Wrapper del repositorio y Android SDK 37:

1. **Unit tests** — `:app:testDebugUnitTest`.
2. **Static analysis** — `:app:lintDebug`.
3. **Debug build** — `:app:compileDebugKotlin :app:assembleDebug`.

Publica como artefactos temporales:

- resultados XML de pruebas unitarias;
- reporte HTML/XML/SARIF de lint;
- APK Debug.

Los artefactos se conservan 7 días. El APK no se publica en el repositorio ni se
distribuye automáticamente.

### `android-instrumented.yml`

Ejecuta `:app:connectedDebugAndroidTest` en un emulador Android de API 35 con
imagen `default`, arquitectura `x86_64` y perfil `pixel_6`.

Incluye:

- habilitación de KVM;
- emulador sin ventana y con aceleración SwiftShader;
- desactivación de animaciones y corrector ortográfico;
- timeout de 60 minutos;
- publicación de resultados y reportes de pruebas instrumentadas.

El emulador de CI reproduce las pruebas. La validación con Gboard y el teclado
real se realiza en el Motorola Moto G34 5G.

## Triggers

Ambos workflows se ejecutan en:

- `push` a cualquier rama;
- `pull_request` a cualquier rama;
- `workflow_dispatch`.

No hay un pipeline exclusivo de `main`.

## Permisos

- `permissions: {}` a nivel de workflow.
- `contents: read` únicamente en cada job.
- `persist-credentials: false` en `actions/checkout`.
- Sin secretos, tokens personales, keystores ni material de firma.
- Sin `pull_request_target`.
- Sin publicación de releases ni APK.
- Sin permisos de escritura.

## Pinning de acciones

Las acciones externas están fijadas a un SHA completo de 40 caracteres. Los
comentarios indican la versión documentada:

| Acción | Versión |
| --- | --- |
| `actions/checkout` | v7.0.1 |
| `actions/setup-java` | v6.3.0 |
| `gradle/actions/setup-gradle` | v6.4.1 |
| `android-actions/setup-android` | v3.2.2 |
| `ReactiveCircus/android-emulator-runner` | v2.38.0 |
| `actions/upload-artifact` | v4.6.2 |

Antes de actualizar una acción se debe revisar el repositorio oficial, verificar
el SHA y ejecutar los workflows en una rama de trabajo.

## Caché

`gradle/actions/setup-gradle` gestiona la caché de Gradle User Home. No se
almacenan credenciales ni keystores. La caché es de solo lectura en ramas
distintas de la rama por defecto.

## Reproducir localmente

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Para las pruebas instrumentadas se necesita un emulador o un dispositivo
conectado. En el dispositivo físico se usa el número de serie explícito:

```powershell
adb -s ZY32JGVXVJ install -r ".\app\build\outputs\apk\debug\app-debug.apk"
adb -s ZY32JGVXVJ shell am instrument -w -r `
  io.github.dijksys.quipu.test/androidx.test.runner.AndroidJUnitRunner
```

## Consultar resultados

- GitHub → **Actions** → workflow y ejecución.
- Cada job muestra su log y sus artefactos.
- Los reportes de lint están en `app/build/reports/lint-results-debug.*` después
  de `lintDebug`.
- Los resultados de pruebas unitarias están en
  `app/build/test-results/testDebugUnitTest/`.
- Los resultados instrumentados están en
  `app/build/outputs/androidTest-results/connected/`.

## Dependabot

`.github/dependabot.yml` configura actualizaciones semanales para Gradle y
GitHub Actions, con agrupación de dependencias relacionadas.

Las actualizaciones mayores de AGP, Kotlin/KSP y Room quedan ignoradas para
requerir revisión manual. Toda actualización que llegue como pull request pasa
por los workflows de CI antes de fusionarse.

## Seguridad

- No se interpolan contenidos de pull requests en comandos de shell.
- No se usan secretos para compilar Debug.
- No se publican APK ni releases.
- No se ejecutan tareas Release que requieran `keystore.properties`.
- Las acciones se fijan por SHA.
- Los permisos se limitan a lectura del repositorio.
