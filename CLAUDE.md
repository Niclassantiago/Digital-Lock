# Digital Lock — Briefing para Claude Code

Este archivo es el contexto operativo del proyecto. Leelo entero antes de tocar
código. Fue redactado en una sesión previa de planificación con el autor
(Niclas) y refleja **decisiones ya tomadas** — no las re-cuestiones, seguilas.

---

## Objetivo del mod

Un mod de Minecraft **1.21.1** para **NeoForge** que agrega un **candado
digital con PIN de 4 dígitos** aplicable a cofres, cofres dobles, cofres
trampa y barriles.

## Stack técnico (fijo)

- Minecraft **1.21.1**
- NeoForge **21.1.250** (ver `gradle.properties`)
- Java **21** (JDK 21 instalado en `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)
- Gradle plugin: **ModDevGradle**
- Parchment mappings: `2024.11.17-1.21.1`
- Package base: `com.digitallock`
- Mod ID: `digitallock`

## Estado actual

- Proyecto generado con el Mod Generator oficial de NeoForge.
- El generator dejó un mod de ejemplo dummy en `src/main/java/com/digitallock/`.
  **Borrar el contenido de ejemplo antes de empezar** (respetando el package).
- Falta implementar todo lo que viene abajo.

---

## Flujo de usuario objetivo

1. Jugador craftea el ítem **Candado Digital**.
2. Con el candado en la mano, hace **Shift + click derecho** sobre un cofre /
   barril / cofre trampa.
3. El candado se aplica: el ítem se consume y aparece un **indicador visual
   verde** sobre el bloque (candado sin PIN aún, "desbloqueado").
4. Al abrir el cofre, la GUI vanilla muestra un **botón "Establecer PIN"**.
5. El jugador ingresa un PIN de 4 dígitos → se guarda hasheado → el indicador
   pasa a **rojo** (bloqueado).
6. El **owner** (quien seteó el PIN) puede abrir el cofre libremente sin
   re-ingresar el PIN. Al abrirlo, ve un botón adicional **"Quitar candado"**
   que le devuelve el ítem.
7. **Otros jugadores** que intenten abrirlo ven una **GUI de ingreso de PIN**:
   - PIN correcto → acceden al cofre + se les habilita en esa sesión el botón
     "Quitar candado".
   - PIN incorrecto → reciben **2 corazones (4.0f) de daño**, se cierra la GUI.
     Sin cooldown.
8. Un cofre bloqueado (con PIN) es **irrompible** para quien no lo desbloqueó,
   e inmune a hoppers, explosiones y pistones.
9. Los **admins con permisos OP** pueden romper cofres bloqueados normalmente
   (bypass de moderación). No hay comando de "reset de PIN" — si el owner se
   fue, el admin rompe el bloque.

---

## Decisiones arquitectónicas (no cambiar sin avisar)

### 1. Almacenamiento del lock

- Usar **Data Attachments de NeoForge** (`AttachmentType`) sobre el BlockEntity
  vanilla del cofre / barril. **No** reemplazar el BlockEntity vanilla.
- Estructura del attachment:

  ```java
  public record LockData(
      UUID ownerId,              // quien seteó el PIN (no quien aplicó el candado)
      Optional<byte[]> pinHash,  // empty = candado aplicado sin PIN todavía
      Optional<byte[]> salt,
      long createdAt
  ) { }
  ```

- El attachment debe persistir en NBT y sincronizarse al cliente para el
  renderer.

### 2. Hash del PIN

- **Nunca** guardar el PIN en plano.
- **PBKDF2-HmacSHA256**, salt aleatorio de 16 bytes, ~10.000 iteraciones,
  output 32 bytes.
- 4 dígitos = 10.000 combinaciones; el hash defiende contra lectura offline
  del `level.dat`. La defensa runtime es el daño por intento.

### 3. Protección contra hoppers y automatización

- Registrar un **`BlockCapability` provider propio** para `Blocks.CHEST`,
  `Blocks.TRAPPED_CHEST`, `Blocks.BARREL` sobre `Capabilities.ItemHandler.BLOCK`.
- Si el BE tiene `LockData` **con PIN** → devolver `EmptyItemHandler.INSTANCE`.
- Si no tiene lock, o tiene lock sin PIN → delegar al handler vanilla.
- Esto corta hoppers, hopper minecarts, AE2, pipes, y cualquier otra
  automatización que use la capability estándar.

### 4. Protección contra rotura / explosión / pistón

- `BlockEvent.BreakEvent` → cancelar si tiene PIN y el jugador **no** es OP
  (`player.hasPermissions(2)`) en creativo.
- `ExplosionEvent.Detonate` → filtrar `getAffectedBlocks()` removiendo los
  bloqueados con PIN.
- `PistonEvent.Pre` → cancelar si el push/pull afecta un bloque bloqueado
  (previene dupe rompiendo el bloque de soporte).

### 5. Indicador visual

- Overlay **client-side** vía `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS`.
- Billboard con el ícono del candado sobre el bloque, verde si `pinHash.isEmpty()`,
  rojo si tiene PIN.
- Alimentado por sync S2C del `LockData`.

### 6. Cofres dobles

- Los cofres dobles son 2 BE adyacentes. Estrategia:
  - Al aplicar candado: detectar via `ChestBlock.getBlockType(state)`,
    escribir la misma `LockData` en ambos halves.
  - Helper `LockAccess.getEffectiveLock(level, pos)` resuelve siempre a la
    mitad "primary" (single source of truth).
  - Al quitar / cambiar PIN: propagar a ambas mitades.
- Trapped chests y barrels son single-BE, no requieren nada especial.

### 7. Networking

Sistema nuevo de NeoForge 1.21.1: `RegisterPayloadHandlersEvent`. Cada packet
es un `record` que implementa `CustomPacketPayload` con `StreamCodec` y `TYPE`.

Packets requeridos:
- **C2S** `SetPinPacket(BlockPos pos, String pin)`
- **C2S** `SubmitPinPacket(BlockPos pos, String pin)`
- **C2S** `RemoveLockPacket(BlockPos pos)`
- **S2C** `LockSyncPacket(BlockPos pos, Optional<LockData> data)`

**Todo se valida server-side.** El cliente nunca decide si el PIN es correcto,
solo lo envía. No hace falta encriptar el PIN en el wire (la conexión de MC ya
va cifrada).

### 8. Configuración

- Archivo de config vía `ModConfigSpec` (config común de NeoForge).
- Valores configurables mínimos:
  - Daño por PIN incorrecto (default `4.0` = 2 corazones)
  - Lista de bloques soportados (default: chest, trapped_chest, barrel)
  - Si los admins bypassean protecciones (default `true`)
  - Si el candado dropea al quitarlo (default `true`)

### 9. Recuperación del ítem

- Al quitar el candado (botón en la GUI), **el ítem se dropea** al jugador.
- Si un admin rompe el bloque en creativo, el candado **no** se dropea (es
  moderación, no recuperación).

---

## Estructura del proyecto

```
src/main/java/com/digitallock/
├── DigitalLockMod.java              # @Mod entrypoint
├── registry/
│   ├── ModItems.java                # DeferredRegister.Items
│   ├── ModDataAttachments.java      # LockData attachment
│   ├── ModMenus.java                # MenuTypes de PIN
│   ├── ModPayloads.java             # registro de packets
│   └── ModDamageTypes.java          # daño por PIN incorrecto
├── item/
│   └── DigitalPadlockItem.java      # useOn para aplicar
├── data/
│   ├── LockData.java                # record + codec + streamCodec
│   ├── LockAccess.java              # helpers: canOpen, isLocked, resolveDoubleChest
│   └── PinHasher.java               # PBKDF2 wrapper
├── event/
│   ├── LockEvents.java              # rotura, explosión, pistón
│   └── CapabilityHandler.java       # provider de item handler
├── menu/
│   ├── PinSetupMenu.java
│   └── PinEntryMenu.java
├── network/
│   ├── SetPinPacket.java
│   ├── SubmitPinPacket.java
│   ├── RemoveLockPacket.java
│   └── LockSyncPacket.java
├── config/
│   └── ModConfig.java
└── client/
    ├── ClientEvents.java            # inyectar botones en GUI vanilla del cofre
    ├── screen/
    │   ├── PinSetupScreen.java
    │   └── PinEntryScreen.java
    └── render/
        └── LockIndicatorRenderer.java
```

---

## Etapas de implementación (avanzar en orden, no saltar)

Cada etapa tiene que dejar el mod **compilando y corriendo**. Después de cada
etapa hacer commit y avisar al usuario para que teste.

- **Etapa 1** ✅ — Setup + ítem candado registrado (sin lógica)
- **Etapa 2** ✅ — Data Attachment `LockData` + aplicar candado con shift+click derecho (sin PIN)
- **Etapa 3** — Renderer del indicador verde/rojo + sync S2C
- **Etapa 4** — GUI para setear PIN (botón inyectado en `AbstractContainerScreen`)
- **Etapa 5** — Control de acceso: owner directo, otros van a la GUI de PIN
- **Etapa 6** — GUI para ingresar PIN + daño por PIN incorrecto
- **Etapa 7** — Botón para quitar candado (owner o sesión validada)
- **Etapa 8** — Protecciones pasivas (hoppers via capability, explosiones, pistones, rotura)
- **Etapa 9** — Cofres dobles (sincronizar halves)
- **Etapa 10** — Trapped chests + barriles en la whitelist
- **Etapa 11** — Config file + receta del candado + polish

---

## Reglas de trabajo

1. **Etapa por etapa.** No implementar la siguiente hasta tener testeada la anterior.
2. **Cada etapa deja el proyecto compilando.** Correr `./gradlew build` antes de dar por cerrada una etapa.
3. **Idioma de comentarios y logs:** español. Idioma de código (nombres): inglés.
4. **Traducciones:** mantener `en_us.json`, `es_uy.json` y `es_es.json` sincronizados.
5. **No cambiar el package `com.digitallock`, el mod ID `digitallock`, ni el stack técnico.**
6. **Server-side authoritative:** cualquier validación crítica (PIN correcto, permisos, drop del ítem) va en el servidor.
7. **No crear un BlockEntity propio.** Usar Data Attachments sobre los BE vanilla.
8. Preguntar antes de agregar dependencias externas al `build.gradle`.

---

## Comandos útiles

```powershell
# Compilar
./gradlew build

# Correr cliente de test
./gradlew runClient

# Correr servidor de test
./gradlew runServer

# Regenerar el workspace tras cambios en build.gradle
./gradlew --refresh-dependencies
```
