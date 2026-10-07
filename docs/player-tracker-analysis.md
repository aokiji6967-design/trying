# FriendGuard - Source Analysis Report

## Project Structure
```
src/main/java/xyz/lyki/friendguard/
├── FriendGuard.java              # Mod entry point (ModInitializer)
├── FriendGuardClient.java        # Client mod logic (ClientModInitializer)
├── Config/
│   ├── FriendGuardConfigScreen.java   # In-game config GUI
│   └── FriendGuardModMenu.java        # ModMenu API integration
└── KeyUtils/
    ├── AddRemovePlayer.java       # Keybind: add/remove protected players
    └── ClearList.java             # Keybind: clear protected list

src/main/resources/
├── fabric.mod.json               # Mod manifest (id: friendguard, v2.2.2)
├── friendguard.json              # Mixin config (net.lyki.extendedservers.mixin)
├── FriendGuardConfig.json        # Default config (empty)
└── logo.png                      # Mod icon
```

## Player Tracker (FriendGuardClient.java)

The tracker runs in `HudRenderCallback.EVENT` and fires every render frame.

### Player discovery

Players are enumerated from the **server player list**, not from the loaded world:

```java
ClientPlayNetworkHandler handler = client.getNetworkHandler();
for (PlayerListEntry entry : handler.getPlayerList()) { ... }
```

`getPlayerList()` returns every player the server has announced, in all dimensions.
This replaced the old `client.world.getPlayers()` call, which only ever saw players
in the dimension the client was standing in.

### Locating a player

Each player-list UUID is resolved against the current world:

```java
PlayerEntity entity = client.world.getPlayerAnyDimension(entry.getProfile().id());
```

- **Entity found** → the player is in the same dimension and inside the range the
  server streams entities for. We get a direction arrow, a distance in metres and
  the GLOWING effect.
- **Entity not found** → the player is somewhere else (another dimension, or outside
  the entity-tracking range). They are still listed by name and labelled
  `[<player> is in another dimension]`, but no direction can be drawn.

### Distance

The old hard-coded `chunkRenderDistance = 16` filter has been **removed**. There is no
longer any distance cap in the tracker: any player the client has an entity for is
tracked, however far away. The practical ceiling is now the server's entity-tracking
range (roughly the view/simulation distance), not an arbitrary 16-chunk diamond.

### HUD output

- Closest non-protected player: `Name is nearby! ↑ (42m)`
- More than 2 other non-protected players: `Name closest! ↑ | 5 more players found!`
- No locatable player, only remote ones: `Name is in another dimension!`
- Players that cannot be located are appended as `Name [other dimension]`
- Protected players: green `(N) ` prefix, as before

### Dimension labels

`getDimensionLabel(RegistryKey<World>)` maps `World.OVERWORLD` / `World.NETHER` /
`World.END` to localised names, falling back to `dimensionOther`. The Nether/End
strings are present for all 8 languages and are used for any player whose dimension
can be resolved.

## Hard limitation: the client cannot know another player's dimension

This is a platform limit, not a missing implementation:

1. `PlayerListS2CPacket` (which fills `ClientPlayNetworkHandler.getPlayerList()`)
   carries name, UUID, game mode, latency and chat session — **no dimension**.
2. `MinecraftClient` holds a single `ClientWorld` field — the dimension the client is
   currently in. There is no map of other dimensions' worlds on the client.
3. Player entities in other dimensions are never spawned on this client, so
   `getPlayerAnyDimension` cannot find them.

Consequently a client-side mod **cannot** print "in the Nether" / "in the End" for
another player. The only vanilla mechanism that conveys cross-dimension direction is
the 1.21.6+ locator-bar waypoint system, which is **server-controlled**
(`WaypointS2CPacket` / `ClientWaypointHandler` / `TrackedWaypoint`) and depends on the
server enabling and ranging waypoints.

Ways to get true dimension names would be: a server-side companion that sends the
dimension in a custom payload, or reading the waypoint handler's `TrackedWaypoint`s.

## Attack blocking

`AttackEntityCallback`: if the attacked entity is a `PlayerEntity` whose name is in
`ProtectedPlayers` and the mod is enabled → didgeridoo sound, `ActionResult.FAIL`.
Otherwise `PASS`. Unchanged, and not range-limited.

## Build status

Verified on this checkout:

```
./gradlew build   →   BUILD SUCCESSFUL
build/libs/friendguard-2.2.2.jar
```

One warning, pre-existing and unrelated to the tracker: `HudRenderCallback` is
deprecated in the Fabric API used here.

## Language Support
8 languages: English (default), Turkish, French, Spanish, German, Portuguese (BR),
Russian, Chinese (Simplified). All include the tracker + dimension strings.

## Dependencies
- Fabric Loader ≥ 0.15.11
- Minecraft ≥ 1.18.2 (built against 1.21.11)
- Java ≥ 17
- ModMenu (`libs/modmenu-17.0.1-beta.1.jar`)

## Keybindings
- **Add/Remove Player** (keycode 296): look at a player and press to toggle protection
- **Clear List**: clears all protected players
