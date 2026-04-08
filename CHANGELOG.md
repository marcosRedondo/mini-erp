# Changelog

Todas las versiones notables de este proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
y este proyecto se adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.4] - 2026-03-29

### Añadido
- Módulo de **Facturación** completo: creación, edición y eliminación de facturas.
- Generación de **PDF para presupuestos y facturas** con diseño profesional (OpenPDF).
- Soporte para **sublíneas y detalles** en cada concepto de los documentos.
- Funcionalidad de **conversión automática**: crear factura desde un presupuesto con un solo clic.
- Enlaces cruzados entre documentos relacionados (navega de factura a presupuesto y viceversa).
- Botón de **acceso rápido al Dashboard** (Home) en todas las pantallas de edición.

### Cambiado
- Rediseño de la cabecera del PDF: ahora incluye **logotipo de empresa**, datos fiscales y fechas de vencimiento destacadas.
- Reordenación de botones de acción en la interfaz para un flujo de trabajo más intuitivo.
- Gestión persistente de la numeración de facturas y presupuestos por cliente y año.

## [1.0.3] - 2026-03-28

### Añadido
- Visualización, edicion creacion presupuestos

## [1.0.2] - 2026-03-19

### Corregido
- Solucionado el problema de que la aplicación no se iniciara en Windows.


## [1.0.1] - 2026-03-18

### Añadido
- Interfaz y base de datos para la **gestión de clientes**, permitiendo crear, consultar, modificar y eliminar usuarios desde las nuevas pantallas `ClientListScreen` y `ClientDetailScreen`.
- Nueva pantalla base de **configuración** (`SettingsScreen`) accesible desde el menú principal.

### Cambiado
- **Centralización de las versiones** (Android y Windows) en el archivo de catálogo `gradle/libs.versions.toml` para poder gestionar los lanzamientos desde un único lugar.
- Ajustes de estilo estandarizados en los botones de acción del formulario.

### Corregido
- Solucionados los flujos de navegación (Navigation graphs) que llevaban a vistas incorrectas tras eliminar un registro.

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
