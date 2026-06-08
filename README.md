# Football Manager - Spring Boot REST API Backend

Este proyecto consiste en el desarrollo completo del backend para un simulador de gestión deportiva (Football Manager) estructurado como una **API REST** funcional. Desarrollado de manera individual para la asignatura de Programación Avanzada (Tecnocampus), el objetivo principal fue diseñar un sistema robusto en memoria capaz de gestionar recursos interconectados (Jugadores, Equipos y Simulaciones de Partidos).

El proyecto se planteó bajo un enfoque **TDD (Test-Driven Development)**, donde el código final fue validado y completado logrando superar con éxito una suite estricta de pruebas automatizadas con **JUnit 5 y MockMvc**.

### Tecnologías y Competencias Demostradas:
* **Framework Principal:** Spring Boot 3 / Java 17.
* **Arquitectura del Software:** Separación estricta en capas: Controladores HTTP (`@RestController`), Objetos de Transferencia de Datos (`DTO / Records`) y Entidades del Dominio.
* **Diseño RESTful:** Modelado de URLs orientadas a recursos, uso correcto de verbos HTTP (`GET`, `POST`, `PUT`, `DELETE`) y gestión semántica de códigos de estado de respuesta (200, 201, 204, 400, 404, 409).
* **Control de Errores Global:** Implementación de mapeos de excepciones centralizados mediante `@ExceptionHandler` para retornar respuestas en formato JSON estandarizado ante fallos de validación o negocio.
* **Uso Avanzado de Java:** Aplicación intensiva de programación funcional con Java Streams, manejo seguro de nulos con `Optional`, comparadores personalizados para clasificaciones y encapsulamiento con Java Records.

### Funcionalidades e Inyección de Dependencias Implementadas por mí:

A partir de un esqueleto inicial de la universidad, diseñé e implementé la lógica completa de los siguientes controladores utilizando **inyección de dependencias por constructor**:

1. **`PlayerRestController` (/players):** Gestión del ciclo de vida de futbolistas. Implementé filtros dinámicos por parámetros de consulta (posición/estado), control de lesiones, validación estricta de datos (clase `InvalidDataException`) y un algoritmo para calcular el Top 10 de goleadores de la liga aplicando criterios de desempate por partidos jugados y nivel de habilidad.
2. **`TeamRestController` (/teams):** Gestión de clubes, finanzas y mercado de fichajes. Diseñé la lógica de negocio para la compra (`sign`), venta (`release`) y transferencias directas entre clubes independientes, controlando dinámicamente las restricciones de presupuesto, saldos financieros y límites de plantilla (máximo 11 jugadores). También estructuré la ordenación matemática para la tabla de clasificación de la liga.
3. **`MatchRestController` (/matches):** El núcleo lógico del simulador. Desarrollé el algoritmo de simulación de partidos que calcula el resultado en base al potencial acumulado de los jugadores disponibles (excluyendo lesionados) combinando factores de aleatoriedad, ventaja de campo, reparto ponderado de goleadores (dando el doble de probabilidad a centrocampistas y delanteros) y cálculo probabilístico de nuevas lesiones tras el encuentro.

### Estructura de Capas del Proyecto:
* **api/**: Capa de exposición HTTP y endpoints (Código desarrollado íntegramente por mí).
* **domain/**: Entidades de lógica de negocio y estados del modelo.
* **dto/**: Contratos e intercambio de datos limpios en formato JSON.
* **exception/**: Excepciones personalizadas de la aplicación de control de flujo.

---
*Proyecto académico desarrollado con fines de aprendizaje en el ecosistema Spring, enfocado en el manejo de contratos HTTP, lógica empresarial interconectada y arquitecturas backend modernas.*
