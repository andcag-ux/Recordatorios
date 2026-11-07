# Recordatorios (Programación Mobile)

Aplicación Android de recordatorios hecha con Kotlin, Jetpack Compose y Material 3.
La **versión 4** (Actividad 4, versión candidata `1.0.0-rc1`, versionCode 4) agrega
Firebase Authentication, sincronización con Cloud Firestore por usuario, reglas de
seguridad y una compilación release firmada (APK y AAB). Room sigue siendo la fuente
local de la interfaz (enfoque local-first).

## Requisitos

- Android Studio reciente (AGP 9.4.x, Gradle 9.6, Kotlin 2.2.x).
- Un emulador o dispositivo con Android 7.0 (API 24) o superior, con internet para iniciar sesión.
- Un proyecto de Firebase con **Authentication (correo/contraseña)** y **Cloud Firestore** habilitados.

## Configuración

1. `app/google-services.json`: archivo del proyecto de Firebase (paquete `com.example.recordatorios`).
   No es un secreto: la protección de los datos la dan las reglas de Firestore.
2. Reglas de Firestore: publicar el contenido de `firestore.rules` (cada cuenta solo accede a `users/{su uid}`).
3. Firma de release (solo para generar la versión firmada):
   copiar `keystore.properties.example` como `keystore.properties` y completar la ruta del `.jks`
   y sus contraseñas. `keystore.properties` y los `.jks` están en `.gitignore`: no se suben al repositorio.
   Sin ese archivo el proyecto compila igual, pero el release queda sin firmar.

## Variantes

| Variante | Nombre visible | versionName | Uso |
|---|---|---|---|
| debug   | Recordatorios (debug) | 1.0.0-rc1-debug | desarrollo; incluye la "Prueba de seguridad" |
| release | Recordatorios | 1.0.0-rc1 | testers; firmada, optimizada, sin herramientas de debug |

## Compilar

```
./gradlew testDebugUnitTest                 # pruebas unitarias
./gradlew assembleDebug                     # APK de desarrollo
./gradlew clean assembleRelease bundleRelease
```

Artefactos: `app/build/outputs/apk/release/app-release.apk` y
`app/build/outputs/bundle/release/app-release.aab`.

## Estructura de la capa de datos

```
data/
  auth/     AuthRepository (contrato) y FirebaseAuthRepository
  local/    Room v2: RecordatorioEntity (con uid), RecordatorioDao, AppDatabase
  prefs/    DataStore: Preferencias, PreferenciasRepository
  remote/   FuenteRemotaRecordatorios (contrato), FirestoreFuenteRemota,
            RecordatorioRemoto (documento), ResultadoRed (interpreta fallas)
  Mapeos.kt                       Documento <-> Entity <-> Dominio
  RecordatorioRepository(+Impl)   repositorio local-first por usuario
  MonitorConectividad.kt          sincroniza al recuperar la red
```

Ruta en Firestore: `users/{uid}/reminders/{clientId}`. El id del documento es el mismo
`clientId` (UUID) que guarda Room, por eso un reintento sobrescribe el mismo documento.
