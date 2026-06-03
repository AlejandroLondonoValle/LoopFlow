# LoopFlow 2.0

> **LoopFlow** es un ecosistema monolítico ligero de alto rendimiento diseñado para la gestión automatizada de hábitos operativos y la planificación de productividad personal a través de un núcleo híbrido que combina un Tablero Scrum (Kanban) y un motor de métricas analíticas diarias.

---

## Contenido
- [Descripción del Proyecto](#descripción-del-proyecto)
- [Arquitectura y Diagramas del Sistema (Draw.io)](#arquitectura-y-diagramas-del-sistema-drawio)
- [Características Principales](#características-principales)
- [Stack Tecnológico](#stack-tecnológico)
- [Requisitos del Sistema](#requisitos-del-sistema)
- [Instalación, Configuración y Ejecución](#instalación-configuración-y-ejecución)
  - [Variables de Entorno](#variables-de-enlace-persistencia)
  - [Desarrollo Local](#desarrollo-local)
  - [Empaquetado Monolítico (Fat JAR)](#empaquetado-monolítico-fat-jar)
- [Referencia Detallada de la API REST](#referencia-detallada-de-la-api-rest)
- [Interfaz Frontend (SPA)](#interfaz-frontend-spa)
- [Estrategia de Pruebas (Testing)](#estrategia-de-pruebas-testing)
- [Despliegue en Producción](#despliegue-en-producción)
- [Estructura del Proyecto de Interés](#estructura-del-proyecto-de-interés)
- [Contribución y Buenas Prácticas](#contribución-y-buenas-prácticas)
- [Autoría](#autoría)

---

## Descripción del Proyecto

LoopFlow unifica la capa de servicios backend y la interfaz de usuario en un único proceso síncrono. Desarrollado bajo la filosofía de despliegue de frontend sin compilación externa y acoplado a un motor Java optimizado, el sistema proporciona una solución sin tiempos de arranque en frío.

El backend expone una API REST robusta encargada de procesar las transacciones de negocio de forma atómica, mientras que el servidor web embebido se encarga de despachar los recursos estáticos de una Single Page Application (SPA) construida en Vanilla JavaScript. Esta cohesión técnica minimiza la latencia de red de las peticiones concurrentes y reduce la huella de memoria del servidor a niveles mínimos, ideal para entornos de computación en la nube optimizados.

---

## Arquitectura y Diagramas del Sistema (Draw.io)

El diseño de LoopFlow sigue un patrón arquitectónico por capas claramente desacopladas: Capa de Presentación (SPA), Capa de Controladores (JAX-RS / Jersey), Capa de Servicios y Lógica de Negocio, y Capa de Persistencia (JDBC).

Nota: Para editar o visualizar de manera interactiva las especificaciones técnicas estructurales, utiliza los siguientes contenedores web enlazados a Draw.io:

### 1. Diagrama de Flujo de Datos y Componentes del Núcleo
Espacio reservado para mapear la interacción entre el cliente JavaScript, el enrutador de Jersey y la base de datos distribuida.
* Enlace: [Editar Diagrama de Componentes en Draw.io](https://drive.google.com/file/d/1Sn_YJJ9dZey5aP9eNq86r-9UYtIc-Zk-/view?usp=sharing)


## Características Principales

* **Motor Analítico de Hábitos:** Seguimiento binario en tiempo real. Algoritmo integrado para el cálculo automático de la Tasa de Flujo diaria basado en la fórmula: (Hábitos Completados / Hábitos Totales) * 100.
* **Tablero Kanban / Scrum Técnico:** Clasificación granular de tareas en tres estados operativos (TODO, IN_PROGRESS, DONE) con un sistema interno de auditoría que registra el historial de movimientos cronológicos por tarea.
* **Aislamiento de Persistencia Dinámica:** Capacidad híbrida para alternar de manera transparente entre entornos relacionales robustos (MySQL) para despliegues de producción y bases de datos en memoria inyectadas (H2) para la ejecución aislada de pruebas unitarias.
* **Seguridad Perimetral en Frontend:** Capa interna de sanitización de texto para evitar inyecciones de scripts maliciosos de tipo Cross-Site Scripting (XSS).

---

## Stack Tecnológico

* **Backend Core:** Java 17 (LTS)
* **Gestor de Dependencias:** Maven 3.x
* **Servidor Web Embebido:** Jetty (Contenedor de Servlets de alto rendimiento)
* **Framework API REST:** Jersey (JAX-RS)
* **Procesamiento de Datos:** Jackson (Serialización/Deserialización nativa de JSON)
* **Motores de Base de Datos:** MySQL 8.0 (Producción) / H2 Database (Entorno de Tests)

---

## Requisitos del Sistema

* **Entorno de Ejecución:** Java Development Kit (JDK) 17 o superior instalado y configurado en las variables de entorno del sistema (JAVA_HOME).
* **Gestor de Proyectos:** Apache Maven 3.6.0+ o el uso de wrappers en su defecto.
* **Almacenamiento:** Instancia local o remota de MySQL Server activa.

---

## Instalación, Configuración y Ejecución

### Variables de Enlace (Persistencia)
El núcleo de la aplicación utiliza una jerarquía de prioridades bien definida para capturar las credenciales de acceso a la base de datos:
1. Archivo de configuración estática del Classpath: `src/main/resources/db.properties`.
2. Inyección directa mediante variables de entorno globales del sistema (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
3. Variables locales declaradas en archivo `.env` (exclusivo para flujos de desarrollo local).

---

### Desarrollo Local

#### Configuración en PowerShell (Windows)
```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/loopflow"
$env:DB_USER = "tu_usuario"
$env:DB_PASSWORD = "tu_password"
$env:PORT = "8080"

# Compilar dependencias y arrancar el servidor embebido
mvn package
mvn exec:java -Dexec.mainClass=com.loopflow.Main -Dexec.cleanupDaemonThreads=false

```

#### Configuración en Bash (Linux/macOS)

```bash
export DB_URL='jdbc:mysql://localhost:3306/loopflow'
export DB_USER='tu_usuario'
export DB_PASSWORD='tu_password'
export PORT=8080

# Compilar dependencias y arrancar el servidor embebido
mvn package
mvn exec:java -Dexec.mainClass=com.loopflow.Main -Dexec.cleanupDaemonThreads=false

```

---

### Empaquetado Monolítico (Fat JAR)

Para generar un binario unificado e independiente que contenga todas las dependencias compiladas y los recursos web estáticos listos para su distribución:

```bash
mvn clean package -DskipTests

# Ejecución del binario distribuible optimizado
java -jar target/loopflow-1.0.0-shaded.jar

```

*Nota: Al ejecutarse, el método de inicialización mapeará el subdirectorio `webapp` dentro del JAR empaquetado. En caso de no existir, conmutará dinámicamente a `src/main/resources/webapp` para facilitar la depuración ágil en tiempo real.*

---

## Referencia Detallada de la API REST

Todos los servicios consumibles se encuentran expuestos bajo el prefijo contractual `/api`. A continuación se detallan los endpoints del sistema:

### Dashboard Operacional

| Método | Endpoint | Descripción | Formato Body / Params |
| --- | --- | --- | --- |
| **GET** | `/api/dashboard` | Retorna el resumen consolidado de métricas, tareas activas y tasas de hábitos. | Ninguno |

### Gestión de Categorías Organizacionales

| Método | Endpoint | Descripción | Formato Body / Params |
| --- | --- | --- | --- |
| **GET** | `/api/categories` | Lista la totalidad de categorías con sus metadatos cromáticos. | Ninguno |
| **GET** | `/api/categories/{id}` | Recupera la información de una categoría específica. | `id` (numérico entero) |
| **POST** | `/api/categories` | Registra una nueva categoría en la base de datos distribuida. | `{ "name": "...", "color": "..." }` |
| **PUT** | `/api/categories/{id}` | Actualiza estructuralmente los parámetros de la categoría. | `{ "name": "...", "color": "..." }` |
| **DELETE** | `/api/categories/{id}` | Elimina de forma atómica la categoría indicada. | `id` (numérico entero) |

### Motor de Tareas (Tablero Scrum)

| Método | Endpoint | Descripción | Formato Body / Params |
| --- | --- | --- | --- |
| **GET** | `/api/tasks` | Obtiene el backlog de tareas permitiendo filtrado reactivo. | `?status=IN_PROGRESS&priority=HIGH` |
| **POST** | `/api/tasks` | Registra una nueva tarea dentro del ecosistema Kanban. | `{ "title": "...", "priority": "..." }` |
| **PATCH** | `/api/tasks/{id}/move` | Modifica el estado operacional de una tarea (Mover columna). | `{ "status": "DONE" }` |
| **GET** | `/api/tasks/{id}/history` | Recupera el log de movimientos cronológicos de la tarea. | `id` (numérico entero) |

### Módulo de Control de Hábitos

| Método | Endpoint | Descripción | Formato Body / Params |
| --- | --- | --- | --- |
| **GET** | `/api/habits` | Lista los hábitos creados con filtros de actividad. | `?active=true` |
| **POST** | `/api/habits/{id}/complete` | Almacena el log de cumplimiento diario de un hábito. | `{ "completed": true, "notes": "..." }` |
| **GET** | `/api/habits/{id}/stats` | Procesa y retorna las estadísticas de cumplimiento. | `?from=2026-05-01&to=2026-05-31` |

### Configuración del Sistema

| Método | Endpoint | Descripción | Formato Body / Params |
| --- | --- | --- | --- |
| **GET** | `/api/config` | Lista los parámetros de configuración dinámica del sistema. | Ninguno |
| **GET** | `/api/config/{key}` | Obtiene el valor asociado a una clave de configuración específica. | `key` (cadena de texto) |
| **PUT** | `/api/config/{key}` | Modifica o establece un parámetro de configuración dinámica. | `{ "value": "...", "description": "..." }` |
| **DELETE** | `/api/config/{key}` | Elimina el parámetro de configuración del sistema. | `key` (cadena de texto) |

---

## Interfaz Frontend (SPA)

El frontend de LoopFlow reside en `src/main/resources/webapp`. Adopta la arquitectura de Single Page Application (SPA) nativa e independiente, construida de extremo a extremo sin frameworks de empaquetado adicionales.

La capa de vista interactúa con el servidor mediante peticiones asíncronas aisladas en el módulo de red corporativo `js/api.js`. El diseño visual consume las clases de utilidad atómicas de Tailwind CSS, soportando un motor de cambio dinámico de Modo Claro / Oscuro sincronizado directamente con las preferencias almacenadas en el almacenamiento del navegador (`localStorage`).

---

## Estrategia de Pruebas (Testing)

El ciclo de integración de LoopFlow exige la verificación automatizada de los endpoints principales antes de cada despliegue. Para ejecutar la suite completa de pruebas:

```bash
mvn test

```

El archivo de configuración `pom.xml` inyecta automáticamente la dependencia de la base de datos relacional H2 en el ciclo de vida del scope de pruebas, aislando por completo la base de datos MySQL de producción y garantizando entornos independientes y repetibles en servidores de Integración Continua (CI).

---

## Despliegue en Producción

La naturaleza monolítica autoejecutable de la aplicación simplifica los despliegues en plataformas modernas como Render, Railway o Clever Cloud:

1. Asignación de Puertos: El motor detecta y enlaza automáticamente la variable de entorno `$PORT` expuesta por el proveedor de nube.
2. Configuración Obligatoria: Asegúrate de mapear las variables de producción `DB_URL`, `DB_USER` y `DB_PASSWORD` en el panel de administración del proveedor de infraestructura antes de inicializar la compilación.

---

## Estructura del Proyecto de Interés

Para auditorías o modificaciones estructurales sobre el núcleo del software, preste especial atención a los siguientes componentes base:

* `src/main/java/com/loopflow/Main.java`: Punto de entrada de la aplicación. Configura e inicializa el servidor Jetty embebido y monta los servlets de Jersey.
* `src/main/java/com/loopflow/config/DatabaseConnection.java`: Singleton administrador del pool de conexiones JDBC del sistema.
* `src/main/resources/webapp/index.html`: Layout base y nodo de inyección de componentes de la UI.
* `src/main/resources/webapp/js/api.js`: Conector modular y encapsulador de peticiones `fetch()` hacia la API REST.

---

## Contribución y Buenas Prácticas

1. Realiza un Fork del repositorio y genera una rama descriptiva de desarrollo (ej. `feature/modulo-notificaciones` o `bugfix/ajuste-cors`).
2. Garantice que todo el código nuevo preserve las convenciones de nomenclatura limpia de Java 17.
3. Es mandatorio validar la integridad estructural ejecutando la suite de testing local (`mvn test`) antes de abrir un Pull Request.

---

## Autoría

* **Desarrollador Principal:** Luis Alejandro Londoño Valle — [AlejandroLondonoValle](https://github.com/AlejandroLondonoValle) (Mck_Dev)
* **Ecosistema:** LoopFlow Core Project · Versión 2.0 (2026)

