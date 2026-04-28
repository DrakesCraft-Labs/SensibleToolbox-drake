# SensibleToolbox-drake

[![Rama](https://img.shields.io/badge/branch-1.21--latin-2ea44f)](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake/tree/1.21-latin)
[![Licencia](https://img.shields.io/github/license/DrakesCraft-Labs/SensibleToolbox-drake)](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake/blob/1.21-latin/LICENSE)
[![Ultimo commit](https://img.shields.io/github/last-commit/DrakesCraft-Labs/SensibleToolbox-drake/1.21-latin)](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake/commits/1.21-latin)

## Descripción técnica
Addon técnico de Slimefun orientado a máquinas, automatización de recursos y utilidades de energía.

## Qué añade a Slimefun
- Nuevas rutas de automatización con bloques técnicos reutilizables.
- Opciones de generación/consumo energético para redes Slimefun avanzadas.
- Herramientas y bloques orientados a infraestructura de base.

## Características principales
- Máquinas de procesamiento y generación con inventarios guiados.
- Sistema de bloques funcionales y upgrades para progresión tecnológica.
- Compatibilidad adaptada al stack Drake/Paper 1.21.

## Matriz de compatibilidad
| Componente | Estado |
|---|---|
| Minecraft | 1.21.x |
| Paper/Purpur | 1.21.x |
| Slimefun Core Drake | 11.x (línea `1.21-latin`) |
| Java | 21 |

## Instalación
1. Descarga el `.jar` de Releases del repositorio.
2. Copia el archivo en la carpeta `plugins/` del servidor.
3. Asegura dependencias (`Slimefun`, `ProtocolLib` u otras según addon).
4. Reinicia el servidor y revisa `logs/latest.log` para validar carga.

## Build local
```bash
mvn -Dmaven.test.skip=true clean package
```

Artefacto esperado:
- `target/SensibleToolbox-drake-*.jar`

## Flujo de release
1. Crear branch de cambios (`feature/*` o `fix/*`).
2. Abrir PR hacia `1.21-latin` con plan de pruebas.
3. Al mergear, crear tag/release y publicar jar compilado.

Tag semántico por addon (`vX.Y.Z-drake`) + changelog con cambios de compatibilidad API/MC.

## Relación con el monorepo
Este repositorio se mantiene en paralelo con `drakes-slimefun-labs` para desarrollo aislado por addon y despliegues independientes.