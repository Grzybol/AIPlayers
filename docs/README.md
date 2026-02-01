# AIPlayers Plugin Documentation

## Overview
AIPlayers provides AI-controlled player-like agents for Paper 1.18.2 servers. Each AI player has its own profile, inventory, ender chest, balance (via Vault when available), and a visible fake-player body driven by pluggable controllers. The plugin focuses on modularity so behaviors can evolve from the built-in dummy logic to HTTP or OpenAI-powered decision makers.

## Core capabilities
- **AI player lifecycle:** Profiles are created with UUID-backed identities and spawned as visible entities at a chosen location; despawning clears the session while keeping the profile in memory. Inventories and ender chests are standard Bukkit containers so admins can view or edit them directly. Economy accounts are created when Vault is available. 
- **Chat participation:** AI players can broadcast formatted chat messages and the plugin tracks a rolling history for controller context.
- **Economy awareness:** Balances are read through Vault, letting future behaviors consider or modify funds alongside movement and chat.
- **Profile persistence:** AI player profiles (controller, behavior, metadata, and last known location) are saved to `profiles.yml` on shutdown and reloaded on startup.

## Commands
All commands require the `aiplayers.admin` permission.

- `/aiplayers add <name>` – Creates a profile, spawns the AI player at the executor’s location, and initializes its inventories and economy entry.
- `/aiplayers remove <name>` – Despawns and removes the AI player.
- `/aiplayers list` – Shows active AI players with their world, coordinates, and controller type.
- `/aiplayers inspect <name>` – Opens the AI player’s inventory for inspection and editing.

Tab completion suggests `add`, `remove`, `list`, and `inspect`, with AI player names for the latter two subcommands.

## AI control loop and behavior
The `AITickTask` runs at the configured interval, building a perception snapshot (nearby players/AI, chat history, balance, and inventory summary) for each session. The appropriate controller (Dummy, HTTP, or OpenAI stub) decides an action, which is then applied on the main thread for movement, chat, or other behaviors.

## Chat integration
`AIChatService` formats broadcast messages to mimic player chat and retains a bounded history that controllers can consume. The chat listener records human chat to populate this context.

## Economy integration
`AIEconomyService` hooks into Vault when present, creating accounts for AI players using their UUID-backed offline player identity and exposing balance, deposit, and withdrawal helpers so controllers can react to server economies.

## Configuration
`config.yml` controls tick intervals, default controller/behavior, and integration toggles. The HTTP controller supports cooldowns and simple retries, and it sends richer perception data (balance, chat, inventory, nearby players) to the remote service:

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
    cooldown-millis: 1000
    retry-count: 1
  openai:
    enabled: false
    api-key: "CHANGE_ME"
    model: "gpt-4.1-mini"
chat:
  history-size: 20
economy:
  enabled: true
```

Adjust the tick interval to change how frequently AI decisions execute, and toggle HTTP/OpenAI integrations as your backend services become available.

## API integrations
Detailed request documentation for the HTTP-based integrations lives in [`docs/api.md`](api.md), including example payloads and how many chat messages are forwarded to external services.
