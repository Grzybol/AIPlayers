# AIPlayers Roadmap Prompt

The following prompt outlines the long-term direction for AIPlayers, emphasizing AI-controlled server-side player agents with inventories, chat, and economy participation. Use it as a reference when planning future milestones or backend integrations.

```
You are an expert Java developer and Minecraft plugin developer (Paper 1.18.2).

Goal: Implement a plugin that adds **AI-controlled players** – not just dumb NPCs.

These "AI players" should behave as much as possible like real players from the server’s perspective:
- They have a name + UUID.
- They have their own inventory and ender chest.
- They have their own economy balance (via Vault).
- They can "speak" on chat (plugin prints messages as if they were players).
- Their movement and actions are driven by an AI controller (local or external via HTTP/OpenAI).

We understand that technically there is no actual TCP client connection.
We simulate the player server-side:
- Visually: fake player entity (Steve-like) on the map.
- Logically: an `AIPlayer` abstraction with inventory, ender chest, money, and chat I/O.

================================
PROJECT SETUP
================================
- Paper 1.18.2
- Java 17
- Maven
- Base package: `pl.nop.aiplayers`
- Main class: `pl.nop.aiplayers.AIPlayersPlugin`
- Plugin name: `AIPlayers`
- Use ProtocolLib as soft-dependency for packet-level stuff (fake player entity).
- Use Vault (soft-depend) for economy integration.

================================
FUNCTIONAL SPEC
================================

1. Commands
-----------
Base command: `/aiplayers` with subcommands:

- `/aiplayers add <name>`
  - Registers a new AI player profile with a generated UUID and the given name.
  - Spawns a visible fake player entity at the executor’s location.
  - Creates an economy account for this name via Vault (if Vault + economy present).
  - Initializes inventory and ender chest for this AI player.

- `/aiplayers remove <name>`
  - Despawns the fake player entity.
  - Removes the AI player from in-memory registry.
  - (For now, you can leave persisted data TODO.)

- `/aiplayers list`
  - Lists all AI player names and their current world/location and controller type.

- `/aiplayers inspect <name>`
  - Opens that AI player’s inventory for the executor (viewer) so admin can see/edit its inventory.

Permissions:
- `aiplayers.admin` for all these commands.

Tab completion:
- After `/aiplayers`: suggest `add`, `remove`, `list`, `inspect`.
- After `/aiplayers remove` and `/aiplayers inspect`: suggest existing AI player names.

2. AI Player Model (real “player-like” agents)
----------------------------------------------
Create class:

`pl.nop.aiplayers.model.AIPlayerProfile`
- Fields:
  - `UUID uuid`
  - `String name`
  - `AIControllerType controllerType` (DUMMY, HTTP, OPENAI)
  - `AIBehaviorMode behaviorMode` (PASSIVE, WANDER, TRADER, GUARD, etc. – for now just WANDER/PASSIVE)
  - Last known location/world.
  - Optional metadata map for future use (personality, long-term strategy state, etc.).

`pl.nop.aiplayers.model.AIPlayerSession`
- Represents **active online AI player**:
  - `AIPlayerProfile profile`
  - `NPCHandle npc` (visual representation of the fake player entity)
  - `Inventory inventory` (Bukkit inventory)
  - `Inventory enderChest` (separate inventory instance used as "ender chest")
  - Reference to economy (Vault) for balance operations.
  - Any runtime state needed by the AI logic.

`AIBehaviorMode` and `AIControllerType` as enums.

Create manager:

`pl.nop.aiplayers.manager.AIPlayerManager`
- Responsibilities:
  - Maintain maps:
    - `Map<String, AIPlayerProfile>` by name.
    - `Map<String, AIPlayerSession>` for currently active AI players (online).
  - Create / destroy AI players:
    - `AIPlayerProfile createProfile(String name)`
    - `AIPlayerSession spawnAIPlayer(String name, Location spawnLocation)`
    - `void despawnAIPlayer(String name)`
  - Accessors:
    - `AIPlayerProfile getProfile(String name)`
    - `AIPlayerSession getSession(String name)`
    - `Collection<AIPlayerSession> getAllSessions()`

The inventory and ender chest should be standard Bukkit `Inventory` instances so that:
- `/aiplayers inspect <name>` simply opens that inventory to the admin.
- Later we can hook these inventories into other systems.

3. Simulated Player Entity (visual)
-----------------------------------
We still use a fake player entity for visuals.

Create interface:

`pl.nop.aiplayers.npc.NPCHandle`
- Methods:
  - `void spawn(Location location)`
  - `void despawn()`
  - `void moveTo(Location target)`
  - `void lookAt(Location target)`
  - `Location getLocation()`
  - `void teleport(Location location)`

Implement:

`pl.nop.aiplayers.npc.ProtocolLibNPCHandle implements NPCHandle`
- Uses ProtocolLib to:
  - Spawn a fake player (Steve skin is enough) with given UUID & name.
  - Send movement/rotation packets so real players see it move.
  - Despawn on remove.

Do not over-engineer skins for now – Steve/default is enough.

4. Chat Integration
-------------------
Even though there is no real network connection, AI players should **act in chat**.

Create:

`pl.nop.aiplayers.chat.AIChatService`
- Methods:
  - `void sendChatMessage(AIPlayerSession session, String message)`
    - Broadcasts a chat message formatted like a normal player chat.
    - Use something like: `Bukkit.broadcastMessage(ChatColor.GRAY + "<" + session.getProfile().getName() + "> " + message);`
    - Later this could integrate with Paper’s adventure API or the server’s chat system.

Listen for global chat:

- Register a listener on `AsyncPlayerChatEvent`.
- Maintain a chat history (e.g. last 20 messages) inside some `ChatMemory` or `AIContextManager`.
- Periodically pass this short chat context to the AI controllers (esp. HTTP/OpenAI) so they can decide to respond.

For the first version:
- Just collect recent messages into a list.
- DummyAIController can ignore chat.
- HTTP/OpenAI controller can receive chat context as part of its JSON input (stub that and add TODO).

5. Economy / Balance
--------------------
If Vault + an economy plugin is present:

- On AI player creation, ensure there is an economy account for that name.
  - Use the AIPlayerProfile’s `name` and `uuid` for the account, if supported.
- Create service class:

`pl.nop.aiplayers.economy.AIEconomyService`
- Wraps Vault:
  - `double getBalance(AIPlayerProfile profile)`
  - `void deposit(AIPlayerProfile profile, double amount)`
  - `boolean withdraw(AIPlayerProfile profile, double amount)`

This allows AI players to:
- Pay for items.
- Receive money.
- Potentially be affected by other plugins that reference players by name/UUID (later).

6. AI Perception & Actions (movement, chat, economy)
----------------------------------------------------
We want AI controllers to see the world and decide actions.

Create:

`pl.nop.aiplayers.ai.Perception`
- Fields:
  - `String name`
  - `UUID uuid`
  - `String world`
  - Current location (x,y,z).
  - Nearby real players (name + distance).
  - Nearby AI players.
  - Current balance (from economy).
  - Snapshot of inventory summary (e.g. simple list of item types/counts).
  - Recent chat messages (last N lines as strings).
  - `long serverTimeTicks`.

`pl.nop.aiplayers.ai.ActionType`
- MOVE_TO, SAY, IDLE, FOLLOW_PLAYER, BUY_ITEM, SELL_ITEM, CUSTOM.

`pl.nop.aiplayers.ai.Action`
- Fields:
  - `ActionType type`
  - For MOVE_TO: target coordinates or relative vector.
  - For SAY: message text.
  - For FOLLOW_PLAYER: target name.
  - For BUY/SELL: simple string fields (`itemId`, `amount`, `price`) – just placeholders for now.

7. AI Controller Abstraction
----------------------------
`pl.nop.aiplayers.ai.controller.AIController`
- Method:
  - `Action decide(AIPlayerSession session, Perception perception);`

Implementations:

1) `DummyAIController`
   - Purely local, simple logic:
     - If there are nearby players:
       - Sometimes (random chance) move towards the closest player.
       - Sometimes say a random simple phrase like `"hi"`, `"o/"`, etc.
     - If no one nearby:
       - Wander randomly around spawn / last position.
   - Ignores economy and long-term strategy.

2) `HttpAIController`
   - Reads base URL from config (e.g. `ai.http.base-url`).
   - Builds a JSON payload from:
     - `AIPlayerProfile`, `Perception`, maybe recent chat history.
   - Sends POST request to `base-url` asynchronously.
   - Expects JSON with an action description, parses it to `Action`.
   - On errors / timeout -> fallback to DummyAIController.
   - Make sure HTTP is async (async task + schedule back to main thread to apply action).

3) `OpenAIAIController` (stub)
   - Reads `ai.openai.api-key` and `ai.openai.model` from config.
   - For now:
     - Implement structure and logging, but have `decide()` just delegate to DummyAIController.
   - Design with the future in mind:
     - Build a natural language prompt including:
       - short description of the world state.
       - AI player’s internal role/personality.
       - recent chat.
     - Parse a structured response (e.g., JSON in the model’s output) back into `Action`.

Also, create:

`pl.nop.aiplayers.ai.controller.AIControllerRegistry`
- Maps `AIControllerType` -> `AIController`.
- Used by the tick loop to pick the right controller for each AI player.

8. AI Tick Loop
----------------
Create task:

`pl.nop.aiplayers.task.AITickTask`
- Constructor receives:
  - `AIPlayerManager`
  - `AIControllerRegistry`
  - `AIEconomyService`
  - Chat context provider (or service).

Behavior:
- Run every N ticks (value from `config.yml`, e.g. 10).
- For each active `AIPlayerSession`:
  - Build `Perception`:
    - Gather nearby players and AI players (within radius).
    - Get balance, inventory summary, recent chat.
  - Find appropriate `AIController` from registry.
  - Call `decide()`.
  - Apply the `Action`:
    - MOVE_TO -> use `npc.moveTo`.
    - SAY -> use `AIChatService.sendChatMessage`.
    - FOLLOW_PLAYER -> compute target position and move gradually.
    - BUY/SELL -> for now just log or adjust balance directly as placeholder (TODO: integrate with shops).

9. Configuration
----------------
`config.yml` example:

```yaml
ai:
  tick-interval-ticks: 10

  default:
    behavior-mode: WANDER
    controller-type: DUMMY

  http:
    enabled: true
    base-url: "http://localhost:8081/ai/decision"
    timeout-millis: 500

  openai:
    enabled: false
    api-key: "CHANGE_ME"
    model: "gpt-4.1-mini"

chat:
  history-size: 20

economy:
  enabled: true  # if Vault + economy detected, otherwise ignore
Persistence (optional, simple for now)

For the initial version, AI players can exist only in memory.
However, please:

Create a simple YAML storage API like AIPlayerStorage with TODO methods:

void saveAll(Collection<AIPlayerProfile> profiles)

List<AIPlayerProfile> loadAll()

On plugin enable:

Try to load saved profiles (if any).

On plugin disable:

Save profiles.

Actually implementing this storage can be left basic with TODO comments; focus on live behavior first.

================================
plugin.yml
Generate plugin.yml:

name: AIPlayers
main: pl.nop.aiplayers.AIPlayersPlugin
version: 1.0.0
api-version: 1.18
author: "nop"
description: "AI-controlled player-like agents with inventory, chat and economy."

softdepend:
  - ProtocolLib
  - Vault

commands:
  aiplayers:
    description: "Manage AI players."
    usage: "/aiplayers <add|remove|list|inspect>"
    permission: "aiplayers.admin"

permissions:
  aiplayers.admin:
    description: "Allows managing AI players."
    default: op
================================
DELIVERABLE
Produce:

pom.xml (Maven)

All Java classes and packages listed above

plugin.yml

config.yml

The result should:

Build as a single JAR.

After placing on a Paper 1.18.2 server with ProtocolLib (and optionally Vault + economy):

/aiplayers add Bot1 spawns a fake player named Bot1 with its own inventory and balance.

The AI tick loop moves Bot1 around and occasionally chats (DummyAIController).

/aiplayers inspect Bot1 shows its inventory.

/aiplayers remove Bot1 despawns it and removes its session.
```
