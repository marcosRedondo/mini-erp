# Changelog

Todas las versiones notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-05-22

### Añadido
- Versión inicial de la aplicación **MiniErp**.
- Soporte para **Windows** (generación de instaladores `.exe` y `.msi`).
- Integración con **GitHub Actions** para compilación automática y creación de Releases.
- Sistema de **actualización automática** que consulta la API de GitHub.
- Visualización de la versión actual en la interfaz de usuario.
- Configuración de dependencias base: Compose Multiplatform, Ktor y Kotlinx Serialization.

### Cambiado
- Ajustada la estructura del proyecto para enfocarse únicamente en Windows y Android.

### Corregido
- Solucionados problemas de caché en los workflows de GitHub Actions en entornos Windows.
