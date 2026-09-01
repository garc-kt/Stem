# 📘 Sprout — Bitácora Completa de Proceso, Arquitectura y Control de Versiones

Documento técnico exhaustivo que recopila todo el proceso de ingeniería, diseño, arquitectura de software, gestión de versiones (SemVer) y despliegue del asistente de escritura **Sprout** (`com.veggiebit.sprout`).

---

## 📑 Tabla de Contenidos
1. [Visión General del Proyecto](#1-visión-general-del-proyecto)
2. [Proceso de Desarrollo Paso a Paso](#2-proceso-de-desarrollo-paso-a-paso)
3. [Estructura del Proyecto (Por Función + Por Tipo)](#3-estructura-del-proyecto-por-función--por-tipo)
4. [Estrategia de Control de Versiones (SemVer)](#4-estrategia-de-control-de-versiones-semver)
5. [Matriz de Compatibilidad Android (10 a 17)](#5-matriz-de-compatibilidad-android-10-a-17)
6. [Licenciamiento y Despliegue](#6-licenciamiento-y-despliegue)

---

## 1. Visión General del Proyecto

**Sprout** es un asistente de escritura flotante y ambiental para Android, desarrollado 100% en **Kotlin** con **Jetpack Compose**, diseñado bajo los principios de **Material 3** (Light + Dark, con color dinámico Monet) y privacidad radical (*Zero-Cloud Privacy*).

> **Nota v1.2.0:** el objetivo de diseño original era Material 3 *Expressive* (`MaterialExpressiveTheme`, `ButtonGroup`, `ToggleButton`, `HorizontalFloatingToolbar`, `LoadingIndicator`, `MaterialShapes`, los roles de forma `*Increased`). En la versión de `androidx.compose.material3:material3` que realmente resuelve este proyecto (`1.4.0`, fijada por el BOM `androidx.compose:compose-bom:2026.03.01`), toda esa superficie Expressive —incluida la propia anotación de opt-in `ExperimentalMaterial3ExpressiveApi`— compila como `internal`, inaccesible desde código de la app. Esto se confirmó con una recompilación limpia y sin caché, no fue un problema transitorio de tooling. La interfaz de v1.2.0 usa Material 3 estable en su lugar (`FilterChip` para selección, `TopAppBar`/`MaterialTheme` planos, una barra de acciones basada en `Row`, `CircularProgressIndicator`), con los tokens de forma y color propios de Sprout aportando la intención expresiva. Revisar `MaterialExpressiveTheme` cuando el BOM avance a una versión de `material3` donde sea pública.

### Capacidades Principales
- **Pill Flotante de 36dp**: Se adhiere suavemente cerca del campo de texto enfocado en cualquier app con animación de pulso orgánico.
- **Doble Motor de Inteligencia**:
  - *Motor Local de Reglas (Instantáneo - 0ms)*: Corre en la memoria RAM del teléfono sin conexión a internet.
  - *Motor de IA Local con Ollama (PC / LAN)*: Se conecta por red local al servidor Ollama de la PC del usuario (`llama3.2`, `mistral`, etc.) con fallback automático al motor local si la PC se desconecta.
- **9 Presets de Transformación**: Fix & Polish, Concise, Professional, Punchy, Friendly, Summarize, Bulletize, Expand y Custom (instrucción libre, requiere un motor de IA).
- **Visor de Diferencias LCS**: Comparador palabra por palabra con cálculo dinámico de palabras ahorradas; con fallback a nivel de párrafo por encima de 1.200 tokens por lado para evitar asignaciones de memoria sin límite.
- **Triggers en Línea y Expansor**: Comandos `?fix`, `?friendly`, `?summary`, `?bullets`, `?expand`, `?calc: (25+4)^2`, `?now`, `?undo` y snippets de texto `..email`, `..shrug`.
- **Reglas por App, Historial de Sesión y Pill Arrastrable**: Modo Auto/Always/Never por aplicación, historial de transformaciones de la sesión (nunca persistido en disco) y una pill flotante que se puede arrastrar y ajusta su posición al borde más cercano.

---

## 2. Proceso de Desarrollo Paso a Paso

### Fase 1: Análisis e Inicialización
- **Estudio de Referencias**: Análisis de los repositorios de referencia (*SwiftSlate* y *TypeAssist*) para adoptar las mejores prácticas de servicios de accesibilidad, captura de texto sin latencia y prevención de bloqueos de interfaz.
- **Configuración de Gradle & Tooling**: Configuración de `libs.versions.toml` con Compose BOM, OkHttp, Kotlinx Serialization, DataStore Preferences y herramientas de pruebas unitarias.

### Fase 2: Motor de Reglas On-Device y Algoritmo LCS
- **LocalRuleEngine**: Implementación de diccionarios de corrección ortográfica en inglés y español, detección de mayúsculas/minúsculas inteligentes y reglas de tono.
- **DiffCalculator (LCS)**: Algoritmo de Longest Common Subsequence para calcular con precisión de tokens las adiciones (resaltadas en verde esmeralda) y eliminaciones (tachadas en rosa suave).
- **TransformHistory** (renombrado de `UndoManager` en v1.2.0): Pila de estados transaccionales que permite revertir cualquier inyección previa con un solo toque o comando `?undo`, ahora expuesta como `StateFlow` para que tanto la hoja de historial del overlay como la pantalla de Historial en Ajustes puedan listar y reaplicar cualquier entrada de la sesión, no solo la última.

### Fase 3: Integración de IA Local con Ollama en PC (LAN)
- **OllamaClient**: Cliente HTTP basado en OkHttp con timeouts configurados y soporte de texto plano sobre redes locales (`usesCleartextTraffic="true"`).
- **Descubrimiento Dinámico**: Endpoint `GET /api/tags` para consultar en un toque qué modelos tiene instalados el usuario en su PC.
- **OllamaRuleEngine**: Motor con plantillas de prompts optimizadas para evitar respuestas con charla o prefijos no deseados, devolviendo únicamente el texto transformado. Si la red falla, conmuta silenciosamente al motor de reglas local.

### Fase 4: Capa de Accesibilidad e Interfaz Flotante
- **SproutAccessibilityService**: Monitorea eventos de texto (`typeViewTextChanged`, `typeViewFocused`) de manera pasiva y segura.
- **ComposeLifecycleServiceView**: Wrapper de `WindowManager` que inyecta un ciclo de vida completo (`LifecycleOwner`, `SavedStateRegistryOwner`, `ViewModelStoreOwner`) dentro de un servicio en segundo plano para renderizar Jetpack Compose sobre otras apps.
- **Inyección de Texto Híbrida**: Prioridad a `AccessibilityNodeInfoCompat.ACTION_SET_TEXT` (sin activar menús del sistema) con fallback a portapapeles y `ACTION_PASTE`.

### Fase 5: Refinamiento de Diseño de Alto Nivel (/impeccable)
- **Paleta Citrus/Amber**: Color semilla `#E65100` con tonos secundarios Sprout Green `#2E7D32`.
- **Micro-interacciones y Spring Animations**: Botones y chips con rebote háptico, escala de selección `1.03x` y transiciones suaves de 220ms.
- **Contraste y Accesibilidad**: Cumplimiento estricto de WCAG AAA en legibilidad y tamaños de toque mínimos de 48dp.

### Fase 6: Compatibilidad Android 10 a 17 y Firma Digital
- **Resolución de Bloqueo de Instalación en Android 14**: Configuración de firma digital (V1, V2 y V3) en Gradle para permitir sideloading en Android 14 y 15.
- **Depuración de Permisos**: Eliminación de permisos innecesarios de Foreground Service que causaban validaciones fallidas en Android 14.
- **Guía de Ajustes Restringidos**: Inclusión de instrucciones en la interfaz para desbloquear servicios de accesibilidad en apps instaladas fuera de Google Play en Android 13+.

### Fase 7: Identidad de Marca y Release en GitHub
- **Procesamiento de Icono (`sprout.jpg`)**: Generación de iconos adaptativos en todas las densidades de pantalla (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) y banners para GitHub.
- **Licenciamiento**: Adopción de **Apache License 2.0**.
- **Publicación en GitHub**: Creación del repositorio `https://github.com/Garc2004/Sprout` y publicación del Release **v1.0.0** con APKs adjuntos.

### Fase 8: Corrección de Errores, Rediseño Material 3 y Expansión de Funciones (v1.2.0)
- **Corrección de la ruta crítica de rendimiento**: el overlay y el sandbox de Ajustes invocaban el motor de transformación en cada pulsación de tecla — con un motor de IA activo, esto suponía una solicitud HTTP facturable por carácter escrito. Se añadió debounce diferenciado (400ms motor local, 1.200ms motores en la nube), un `TransformCache` en memoria y la eliminación del `LaunchedEffect` de auto-ejecución del sandbox.
- **Corrección de fugas y límites de memoria**: `SproutOverlayManager` reutiliza ahora una única instancia de `ComposeView` entre ciclos de ocultar/mostrar en lugar de reconstruirla cada vez; `DiffCalculator` limita la matriz LCS a 1.200 tokens por lado con reserva a un diff por párrafo por encima de ese umbral; el recorrido del árbol de accesibilidad de respaldo pasó de recursión sin límite a BFS iterativo con techo de profundidad y de nodos visitados.
- **Corrección de calidad de texto**: las reglas de puntuación protegen ahora correos, URLs, menciones, decimales y abreviaturas antes de aplicarse (evitando `user@example.com` → `user@example. Com`); el analizador de `?calc:` pasó de un escáner de dos pasadas que descartaba paréntesis y `^` a un parser recursivo-descendente correcto; la inyección por portapapeles ahora selecciona el buffer completo antes de pegar (reemplaza en vez de insertar) y restaura el portapapeles anterior.
- **Actualización del cliente Claude**: modelo por defecto `claude-haiku-4-5` (el anterior `claude-3-7-sonnet-20250219` está retirado), `output_config.effort: "low"`, `max_tokens` escalado con el tamaño de entrada, manejo de `stop_reason: "refusal"` y mensajes de error estructurados en fallos HTTP.
- **Modo oscuro**: `ThemeMode` (System/Light/Dark) sobre color dinámico Monet, sustituyendo la restricción original de "solo modo claro" de `plan.md` §3.1 por decisión explícita de producto.
- **Reconstrucción de Ajustes**: `SettingsViewModel` + grafo de Navigation 3 (`Home`/`Onboarding`/`Engine`/`Appearance`/`AppRules`/`Snippets`/`History`/`Sandbox`) en sustitución de una única composable de 1.150 líneas; los campos de credenciales ahora persisten al perder el foco en vez de por cada pulsación.
- **Expansión de funciones**: 5 presets nuevos (Friendly, Summarize, Bulletize, Expand, Custom) en el motor local y los cuatro motores de IA; reglas de overlay por app; pill flotante arrastrable con ajuste al borde; historial de sesión con `TransformHistory`; reglas de idioma español en `LocalRuleEngine` (`engine/rules/`: `EnglishRules`, `SpanishRules`, `LanguageDetector`); cifrado de claves de API en reposo con `CryptoBox` (Android Keystore AES-256-GCM).
- **Hallazgo de dependencias**: el objetivo de diseño Material 3 Expressive no es accesible en `material3:1.4.0` (la versión que realmente resuelve el BOM del proyecto) — ver la nota en §1. El rediseño usa Material 3 estable en su lugar.

---

## 3. Estructura del Proyecto (Por Función + Por Tipo)

El proyecto sigue una arquitectura modular y escalable dividida por **Tipo + Función**:

```
Sprout/
├── .github/                                    # Flujos de trabajo e integración
├── app/                                        # Módulo principal de la aplicación Android
│   ├── build.gradle.kts                        # Configuración de compilación, SDKs y firma
│   ├── proguard-rules.pro                      # Reglas de ofuscación y optimización R8
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml             # Manifiesto con permisos y servicios
│       │   ├── java/com/veggiebit/sprout/
│       │   │   ├── app/                        # Ciclo de vida y Sistema de Diseño M3
│       │   │   │   ├── SproutApplication.kt    # Punto de entrada de la aplicación
│       │   │   │   └── theme/                  # Tokens de Color, Tipografía, Formas y Tema
│       │   │   │       ├── Color.kt            # Esquemas claro/oscuro + colores extendidos del diff
│       │   │   │       ├── Shape.kt
│       │   │   │       ├── Theme.kt            # MaterialTheme estable (ver nota v1.2.0 en §1)
│       │   │   │       ├── ThemeMode.kt        # System/Light/Dark
│       │   │   │       └── Type.kt
│       │   │   │
│       │   │   ├── core/                       # Utilidades transversales y metadatos
│       │   │   │   ├── crypto/
│       │   │   │   │   └── CryptoBox.kt        # Cifrado AES-256-GCM (Android Keystore) para claves de API
│       │   │   │   ├── utils/
│       │   │   │   │   ├── AccessibilityUtils.kt # Inyección de texto y recorrido de nodos (BFS acotado)
│       │   │   │   │   ├── HapticHelper.kt     # Motor adaptativo de vibración háptica
│       │   │   │   │   └── PermissionHelper.kt # Verificación y solicitud de permisos
│       │   │   │   └── version/
│       │   │   │       └── AppVersion.kt       # Acceso tipado a metadatos SemVer
│       │   │   │
│       │   │   └── features/                   # Módulos organizados por función
│       │   │       ├── enhancement/            # Motores de transformación y diff
│       │   │       │   ├── data/
│       │   │       │   │   ├── api/            # Clientes HTTP de motores en la nube
│       │   │       │   │   │   ├── ClaudeClient.kt
│       │   │       │   │   │   ├── GeminiClient.kt
│       │   │       │   │   │   └── OpenAIClient.kt
│       │   │       │   │   ├── engine/         # Implementaciones de motores de texto
│       │   │       │   │   │   ├── ClaudeRuleEngine.kt
│       │   │       │   │   │   ├── DiffCalculator.kt
│       │   │       │   │   │   ├── GeminiRuleEngine.kt
│       │   │       │   │   │   ├── InlineCommandEngine.kt
│       │   │       │   │   │   ├── LocalRuleEngine.kt
│       │   │       │   │   │   ├── OllamaRuleEngine.kt
│       │   │       │   │   │   ├── OpenAIRuleEngine.kt
│       │   │       │   │   │   ├── TextEngine.kt
│       │   │       │   │   │   ├── TextEngineProvider.kt
│       │   │       │   │   │   ├── TransformCache.kt    # LRU en memoria — evita reprocesar texto/preset repetido
│       │   │       │   │   │   ├── TransformHistory.kt  # Antes UndoManager.kt; ahora expuesto como StateFlow
│       │   │       │   │   │   └── rules/               # Diccionarios por idioma para LocalRuleEngine
│       │   │       │   │   │       ├── EnglishRules.kt
│       │   │       │   │   │       ├── LanguageDetector.kt
│       │   │       │   │   │       ├── LanguageRules.kt
│       │   │       │   │   │       └── SpanishRules.kt
│       │   │       │   │   ├── models/         # Modelos de datos y estados
│       │   │       │   │   │   ├── DiffToken.kt
│       │   │       │   │   │   ├── EngineMode.kt
│       │   │       │   │   │   ├── LanguagePreference.kt
│       │   │       │   │   │   ├── TextPayload.kt
│       │   │       │   │   │   ├── TransformPreset.kt   # 9 presets: +Friendly/Summarize/Bulletize/Expand/Custom
│       │   │       │   │   │   └── TransformResult.kt
│       │   │       │   │   └── ollama/         # Cliente y serialización de Ollama LAN
│       │   │       │   │       ├── OllamaClient.kt
│       │   │       │   │       └── OllamaModels.kt
│       │   │       │   └── ui/components/      # Componentes visuales de diff y presets
│       │   │       │       ├── DiffViewer.kt
│       │   │       │       └── PresetChips.kt
│       │   │       │
│       │   │       ├── overlay/                # Servicio y vistas flotantes WindowManager
│       │   │       │   ├── service/
│       │   │       │   │   ├── ComposeLifecycleServiceView.kt
│       │   │       │   │   ├── SproutAccessibilityService.kt
│       │   │       │   │   ├── SproutOverlayManager.kt  # Reutiliza el ComposeView; snap-to-edge de la pill
│       │   │       │   │   └── SproutTileService.kt
│       │   │       │   └── ui/
│       │   │       │       ├── SproutFloatingOverlay.kt
│       │   │       │       └── components/
│       │   │       │           └── SproutPill.kt        # Arrastrable; badge con pulso condicional
│       │   │       │
│       │   │       ├── selection/              # Menú nativo flotante ACTION_PROCESS_TEXT
│       │   │       │   └── ui/
│       │   │       │       └── ProcessTextActivity.kt
│       │   │       │
│       │   │       └── settings/               # Ajustes: ViewModel + grafo Navigation 3
│       │   │           ├── data/
│       │   │           │   ├── AppRuleMode.kt          # Auto/Always/Never por app
│       │   │           │   └── PreferencesRepository.kt
│       │   │           └── ui/
│       │   │               ├── HomeScreen.kt           # Panel principal + tarjetas de navegación
│       │   │               ├── MainActivity.kt
│       │   │               ├── SettingsViewModel.kt     # Sustituye el estado local de la antigua SettingsScreen.kt
│       │   │               ├── SproutNavDisplay.kt      # NavDisplay + entryProvider (Navigation 3)
│       │   │               ├── SproutRoutes.kt
│       │   │               ├── components/
│       │   │               │   ├── PermissionStepCard.kt
│       │   │               │   ├── SproutSegmentedGroup.kt  # FilterChip-based (no ButtonGroup)
│       │   │               │   └── SproutSubScreen.kt
│       │   │               ├── onboarding/
│       │   │               │   └── OnboardingScreen.kt  # Flujo de bienvenida de 3 pasos
│       │   │               └── sections/
│       │   │                   ├── AppRulesScreen.kt
│       │   │                   ├── AppearanceScreen.kt  # ThemeMode + LanguagePreference
│       │   │                   ├── EngineScreen.kt
│       │   │                   ├── HistoryScreen.kt
│       │   │                   ├── SandboxScreen.kt      # Solo ejecución explícita (ya no auto-run)
│       │   │                   └── SnippetsScreen.kt
│       │   │
│       │   └── res/                            # Recursos Android (XML, Mipmaps, Strings)
│       │       ├── drawable/                   # Capas de icono adaptativo
│       │       ├── mipmap-*/                   # Iconos WebP en todas las densidades
│       │       ├── values/                     # Strings, Colores, Temas
│       │       └── xml/                        # Configuración de accesibilidad y seguridad de red
│       │
│       └── test/java/com/veggiebit/sprout/     # Suite de pruebas unitarias
│           ├── core/version/SemVerTest.kt
│           └── features/enhancement/
│               ├── AIEnginesTest.kt
│               ├── DiffCalculatorTest.kt
│               ├── InlineCommandEngineTest.kt
│               ├── LocalRuleEngineTest.kt
│               ├── OllamaRuleEngineTest.kt
│               ├── SpanishRulesTest.kt
│               ├── TextPayloadTest.kt
│               └── TransformCacheTest.kt
│
├── art/                                        # Banners y logotipos para GitHub y documentación
├── gradle/                                     # Wrapper y catálogo de dependencias
├── build.gradle.kts                            # Script de compilación raíz
├── settings.gradle.kts                         # Configuración de módulos del proyecto
├── version.properties                          # Archivo de control central de versión SemVer
├── LICENSE                                     # Licencia Apache 2.0
└── README.md                                   # Documentación principal del repositorio
```

---

## 4. Estrategia de Control de Versiones (SemVer)

El proyecto utiliza **Semantic Versioning 2.0.0 (SemVer)** bajo el estándar `MAJOR.MINOR.PATCH[+BUILD]`:

### Definición de Incrementos
- **`MAJOR` (1.0.0)**: Cambios incompatibles con versiones anteriores (breaking changes), rediseño de arquitectura o reescritura de APIs fundamentales.
- **`MINOR` (x.1.0)**: Nuevas funcionalidades compatibles hacia atrás (ej. soporte para nuevos modelos de IA, nuevos comandos inline o nuevos presets).
- **`PATCH` (x.x.1)**: Corrección de errores (bug fixes) y mejoras de rendimiento que no alteran la interfaz pública.
- **`BUILD METADATA` (`+YYYYMMDD`)**: Metadatos de compilación para trazabilidad exacta de la fecha de build.

### Inyección Automatizada en Gradle
El archivo `version.properties` centraliza la versión:
```properties
SEMVER_MAJOR=1
SEMVER_MINOR=0
SEMVER_PATCH=0
SEMVER_BUILD=20260831
```

En `app/build.gradle.kts`, se calcula matemáticamente el `versionCode` para Android:
$$\text{versionCode} = (\text{MAJOR} \times 10000) + (\text{MINOR} \times 100) + \text{PATCH}$$

Ejemplo: `1.0.0` $\rightarrow$ `versionCode = 10000`.

### Convención de Commits (Conventional Commits)
Todos los cambios en Git siguen el estándar:
- `feat:` Nuevas características añadidas.
- `fix:` Correcciones de errores.
- `refactor:` Mejoras internas de código sin cambio de comportamiento externo.
- `chore:` Tareas de mantenimiento, dependencias o configuración.
- `docs:` Actualizaciones de documentación.
- `test:` Nuevas pruebas unitarias o de integración.

### Flujo de Git y Tags
- Rama principal: `main` (código estable listo para producción).
- Tags de Release: `vMAJOR.MINOR.PATCH` (ej. `v1.0.0`).
- GitHub Releases: Generación de binarios firmados adjuntos (`Sprout-v1.0.0.apk`) vinculados al tag.

---

## 5. Matriz de Compatibilidad Android (10 a 17)

| Versión de Android | API Level | Compatibilidad | Mecanismos de Adaptación |
|---|---|---|---|
| **Android 10** (Q) | API 29 | 100% Soportado | Inyección `ACTION_SET_TEXT`, vibración `VibrationEffect` y métricas de pantalla. |
| **Android 11** (R) | API 30 | 100% Soportado | Detección de insets de teclado y observador de foco. |
| **Android 12 / 12L** (S) | API 31–32 | 100% Soportado | Soporte moderno de `VibratorManager` para hápticos precisos. |
| **Android 13** (Tiramisu) | API 33 | 100% Soportado | Manejo seguro de portapapeles y guía para ajustes restringidos. |
| **Android 14** (Upside Down Cake) | API 34 | 100% Soportado | Firma digital completa (V1, V2, V3) para sideloading sin bloqueos. |
| **Android 15** (Vanilla Ice Cream) | API 35 | 100% Soportado | Alineación de páginas de 16 KB y Edge-to-Edge nativo. |
| **Android 16 / 17** (Baklava / Futuro) | API 36–37+ | 100% Soportado | `compileSdk = 36` y `targetSdk = 36` (Target SDK oficial desde v1.2.0, antes 35), APIs de accesibilidad retrocompatibles. |

---

## 6. Licenciamiento y Despliegue

- **Licencia**: **Apache License 2.0**
  - Permite uso libre, comercial, distribución y modificación.
  - Incluye protección expresa de patentes para el autor y los usuarios.
  - Exige mención de autoría (*Copyright 2026 garc-kt*).
- **Repositorio Oficial**: [https://github.com/garc-kt/Stem](https://github.com/garc-kt/Stem)
- **Primer Release Publicado**: [https://github.com/garc-kt/Stem/releases/tag/v1.0.0](https://github.com/garc-kt/Stem/releases/tag/v1.0.0)
