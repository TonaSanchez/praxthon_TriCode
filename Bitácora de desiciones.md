# Bitácora de decisiones

## Decisiones

- Se prioriza evitar respuestas 500 y cumplir el contrato de la API: errores de entrada, transiciones inválidas, paginación e idempotencia deben producir respuestas controladas. Después se comprobarán con pruebas de aceptación y casos límite. El criterio es que una petición incorrecta nunca provoque un fallo interno ni deje datos inconsistentes.
- Se conserva H2 como base predeterminada y Gradle Wrapper para que el sandbox arranque sin instalar ni configurar MySQL o Gradle. Las cuatro operaciones semilla permiten verificar consultas y escenarios desde el primer inicio; la persistencia se reinicia al detener la aplicación.
- El trabajo se ordena como indica la checklist: primero robustez (A), pruebas obligatorias (D) y verificación final (F); después, si queda tiempo, refactor de arquitectura (B). Así se protege primero el comportamiento exigido antes de reorganizar componentes.

## Fuera de alcance por ahora

MySQL como requisito de ejecución y las mejoras opcionales de la sección C (guardar identificación fiscal, expiración de claves, Swagger y encabezado `Location`) se difieren: no son necesarias para levantar y probar el sandbox básico. La refactorización B también se pospone hasta que A, D y F estén verificadas; no se descarta, pues aporta valor de arquitectura. El criterio para recortar es priorizar contrato, estabilidad y pruebas sobre persistencia externa y mejoras no obligatorias.

La aplicación no se realiza pues se prioriza el desarollo de la API, para no comprometernos a tener más errores de los que podemos manejar.

La checklist es un plan de trabajo, no evidencia de implementación: los puntos aún sin verificar deben mantenerse como pendientes.
