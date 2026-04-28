# Contributing Guide

## Flujo de trabajo
1. Crea una rama desde `1.21-latin` (`feature/*`, `fix/*`, `chore/*`).
2. Mantén commits atómicos y con mensaje claro.
3. Abre Pull Request hacia `1.21-latin` usando la plantilla.
4. Espera CI verde + review antes de merge.

## Estándares técnicos
- Java 21
- Compatibilidad Paper/Purpur 1.21.x
- Evitar romper APIs públicas sin nota de migración
- Incluir validación en `logs/latest.log` cuando aplique

## Build local
- Maven: `mvn -DskipTests clean package`
- Gradle: `./gradlew build -x test` (o `./gradlew shadowJar`)

## Reportes
- Bugs y propuestas por Issue Templates
- Seguridad: usar `SECURITY.md`