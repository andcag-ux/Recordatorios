# Recordatorios (Programación Mobile)

Aplicación Android de recordatorios hecha con Kotlin, Jetpack Compose y Material 3.
La **versión 3** (Actividad 3) agrega persistencia local, preferencias, repositorio
local-first, consumo de un servicio remoto con Retrofit/OkHttp y funcionamiento offline.

## Requisitos

- Android Studio reciente (con AGP 9.4.x y Gradle 9.6, como indica el proyecto).
- JDK 17 o superior (el que trae Android Studio).
- Un emulador o dispositivo con Android 7.0 (API 24) o superior.

## Cómo configurar, compilar y ejecutar

1. Abrí la carpeta del proyecto en Android Studio y esperá a que termine **Sync**.
2. (Opcional) Copiá `local.properties.example` como `local.properties` y ajustá:
   - `api.baseUrl`: URL base del servicio (debe terminar en `/`).
   - `api.usarSimulado`: `true` usa el servidor simulado incluido en la app (no necesita internet);
     `false` envía las solicitudes reales a `api.baseUrl`.
   Si no existe el archivo, la app usa el servidor simulado por defecto.
   `local.properties` no se versiona: no hay credenciales ni secretos en el código.
3. Elegí el emulador y presioná **Run**.
4. Pruebas unitarias: **Run > Run Tests** sobre `app/src/test`, o `./gradlew testDebugUnitTest`.

## Estructura de la capa de datos

```
data/
  local/    Room: RecordatorioEntity, RecordatorioDao, AppDatabase
  prefs/    DataStore: Preferencias, PreferenciasRepository
  remote/   Retrofit/OkHttp: RecordatorioApi, RecordatorioDto, ResultadoRed,
            ServidorSimulado (interceptor), ApiFactory
  Mapeos.kt                       DTO <-> Entity <-> Dominio
  RecordatorioRepository(+Impl)   repositorio local-first
  MonitorConectividad.kt          dispara la sincronización al recuperar la red
```

## Probar el funcionamiento offline

En la pantalla principal, menú **Opciones > Red simulada** permite elegir: red normal,
sin conexión, timeout, error 500 y error 400. Con "Sin conexión" los recordatorios
creados o editados quedan como *Pendiente de sincronizar*; al volver a "Red normal"
se envían automáticamente, sin duplicarse.
