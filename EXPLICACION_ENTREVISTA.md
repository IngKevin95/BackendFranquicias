# Guía de Preparación para la Entrevista: API de Franquicias

Este documento contiene un análisis profundo del proyecto, la arquitectura utilizada, las decisiones de diseño y, lo más importante, **los "gaps" (puntos de mejora) que un ingeniero Senior o Arquitecto notaría inmediatamente**. 

Usa esta guía para defender tu prueba técnica, mostrar un nivel de madurez alto y dominar la entrevista técnica.

---

## 1. Arquitectura y Tecnologías Utilizadas

El proyecto utiliza un enfoque moderno, robusto y escalable:
*   **Spring Boot WebFlux (Reactivo):** Se utilizó para soportar alta concurrencia con un consumo menor de recursos (hilos no bloqueantes). Es un "plus" muy valorado por empresas modernas.
*   **Arquitectura Hexagonal (Puertos y Adaptadores):** El código está organizado en `application`, `domain`, e `infrastructure`. Esto desacopla el negocio de las tecnologías subyacentes. Si en el futuro cambias PostgreSQL por MongoDB, solo tocas `infrastructure`.
*   **R2DBC (Reactive Relational Database Connectivity):** Es el driver reactivo para PostgreSQL. A diferencia de JDBC/JPA, R2DBC no bloquea el hilo principal esperando respuestas de la base de datos.
*   **Flyway:** Utilizado para migraciones automáticas de base de datos. Configurado usando JDBC clásico solo al arranque.
*   **Docker & Testcontainers:** Se utiliza Testcontainers para levantar una base de datos real en los tests de integración. Esto garantiza que las pruebas sean fiables.

---

## 2. Decisiones de Diseño y Funcionalidades Avanzadas (¡Para presumir!)

Aquí están los huecos que un entrevistador experimentado normalmente busca, los cuales **ya hemos cubierto y solucionado** en el código. Esta es tu mejor estrategia defensiva y demostrará un nivel Senior.

### 🟢 Solución a Condiciones de Carrera (Race Conditions) al actualizar el Stock
*   **Lo que hicimos:** En `ProductoService.modificarStock`, evitamos el típico y propenso a errores `findById` -> modificar memoria -> `save`. En lugar de eso, usamos un Query Nativo (`UPDATE producto SET stock = stock + :cantidad WHERE id = :id AND stock + :cantidad >= 0`) y registramos la transacción en una tabla de Kardex (`transaccion_stock`).
*   **Por qué presumirlo:** Al usar R2DBC (sin JPA), si dos personas compran el mismo producto al mismo milisegundo, un update en memoria fallaría (ambos leerían stock=10 y guardarían 9). Nuestro Query nativo es **Atómico**, previniendo pérdida de consistencia. Además, el Kardex ayuda a mantener un log de auditoría inmutable de ENTRADAS y SALIDAS. Todo esto envuelto en `@Transactional`.

### 🟢 Arquitectura de Paginación
*   **Lo que hicimos:** El endpoint `max-stock` por franquicia no retorna toda la base de datos de golpe. Le implementamos parámetros `limit` y `offset` inyectados en la consulta nativa de Postgres.
*   **Por qué presumirlo:** En la vida real, los endpoints que devuelven listas grandes sin paginación tumban la memoria del servicio y saturan la red. Implementar esto muestra madurez en diseño de APIs.

### 🟢 Semántica de Errores REST estricta
*   **Lo que hicimos:** Si intentas modificar una sucursal que sí existe, pero *no pertenece* a la franquicia indicada en la URL, no arrojamos `404 Not Found`, arrojamos `409 Conflict` (vía `ConflictoRelacionException`).
*   **Por qué presumirlo:** Un purista de REST te diría que la sucursal *sí* existe, pero la relación es conflictiva. Manejar esto de forma granular en vez de esconderlo en un 404 demuestra un profundo conocimiento del protocolo HTTP.

### 🟢 Alta Disponibilidad y Caché con Redis
*   **Lo que hicimos:** El requerimiento mencionaba poder usar Redis. La consulta de "producto con más stock por sucursal" (`DISTINCT ON`) es pesada. Implementamos Caché usando `ReactiveRedisTemplate`. Si la data no está, va a PostgreSQL, la guarda en Redis con un TTL de 5 minutos, y la retorna.
*   **Por qué presumirlo:** Al modificar el stock de un producto (añadir o eliminar), **invalidamos** la caché automáticamente. Esto garantiza que la próxima lectura sea fresca y reduce enormemente la carga en PostgreSQL.

### 🟡 GAP Operativo: Requisito de Docker para el Build (`mvn test`)
*   **El Problema:** Al compilar el proyecto o correr las pruebas (ej. `./mvnd.sh test`), fallará si el evaluador no tiene Docker Desktop encendido (por Testcontainers).
*   **La Solución a proponer:** Es normal, pero conviene indicarlo claramente en el `README.md` ("Pre-requisito: Tener Docker corriendo para la fase de pruebas de integración").

---

## 3. Resumen de Puntos Fuertes (Para presumir)

*   **Sin `@Autowired` en campos:** Usaste inyección de dependencias por constructor. Es la mejor práctica actual de Spring Boot porque facilita crear tests unitarios puros (inyectando Mocks manualmente).
*   **DTOs como Registros (`record` de Java 14+):** Reduce el boilerplate inmenso de crear clases con Getters, Setters, Equals y HashCode. Es Java moderno.
*   **Validaciones Integradas (`jakarta.validation`):** Todos los DTOs impiden campos nulos o stocks negativos desde el punto de entrada (Controller). 
*   **Manejo Global de Excepciones (`@ControllerAdvice`):** Capturas `WebExchangeBindException` y tus excepciones de dominio y devuelves Problem Details o respuestas claras, en lugar de dejar escapar el stacktrace de Tomcat/Netty al cliente.

## 4. Posibles Preguntas Técnicas y Respuestas

**P: ¿Por qué usaste Spring WebFlux en lugar del Spring Web MVC clásico?**
> **R:** "WebFlux utiliza un modelo no bloqueante (basado en Event Loop de Netty). Si una request se queda esperando la base de datos, el hilo se libera para atender a otros usuarios. Esto permite manejar miles de conexiones concurrentes consumiendo mucha menos memoria RAM que el modelo tradicional de un-hilo-por-request."

**P: ¿Qué patrón de arquitectura seguiste?**
> **R:** "Arquitectura Hexagonal. Mi lógica core (dominio y servicios de aplicación) no conoce de bases de datos, solo expone Puertos (interfaces). La infraestructura (controladores REST y adaptadores R2DBC) implementa estos puertos. Esto aísla el negocio y lo hace testeable."

**P: Veo que usas Flyway y R2DBC al mismo tiempo, ¿cómo lograste que funcionaran juntos?**
> **R:** "Excelente pregunta. R2DBC es el driver reactivo y no soporta migraciones fácilmente, así que usé el driver JDBC clásico exclusivamente para que Flyway corra de manera síncrona al arrancar el contexto de Spring, y R2DBC para las operaciones normales de la aplicación."

---
*¡Mucho éxito en la entrevista! Demuestra confianza y plantea los Gaps como puntos que tenías previstos para iteraciones futuras.*