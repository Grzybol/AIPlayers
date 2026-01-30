# Dokumentacja API zewnętrznych integracji

Poniższy dokument opisuje **requesty wysyłane przez plugin** do zewnętrznych usług HTTP. Opis zawiera strukturę payloadów, przykładowe wywołania oraz informację, ile wiadomości z czatu jest przekazywanych w każdym requestcie.

## 1. Remote Planner API (AI planowanie)

### Gdzie jest wywoływane
- Konfiguracja: `ai.remote.*` w `config.yml`.
- Endpoint: `base-url + plan-path` (domyślnie `/v1/plan`).
- Tryb wysyłki: przy każdej decyzji AI, ale **nie częściej** niż co `request-interval-millis`, chyba że nastąpiła nowa aktywność czatu (wtedy request może zostać wysłany wcześniej).

### Ile wiadomości z czatu jest wysyłanych
Plugin pobiera snapshot historii czatu i wysyła **ostatnie N wpisów**, gdzie `N` = `ai.remote.chat-limit` (domyślnie 10). Jeśli historia jest krótsza, wysyłana jest cała dostępna lista. Każda linia czatu jest mapowana na obiekt z metadanymi (timestamp, nadawca, typ nadawcy, treść).

### Struktura requestu
`Content-Type: application/json`

```json
{
  "request_id": "uuid",
  "server": {
    "server_id": "string",
    "mode": "string",
    "online_players": 0
  },
  "tick": 0,
  "time_ms": 0,
  "bots": [
    {
      "bot_id": "uuid",
      "name": "string",
      "online": true,
      "cooldown_ms": 0,
      "persona": {
        "language": "pl",
        "tone": "casual",
        "style_tags": ["short", "memes_light"],
        "avoid_topics": ["payments", "admin_powers", "cheating"],
        "knowledge_level": "average_player"
      }
    }
  ],
  "chat": [
    {
      "ts_ms": 1730000000000,
      "sender": "Player123",
      "sender_type": "PLAYER",
      "message": "Cześć wszystkim!"
    },
    {
      "ts_ms": 1730000005000,
      "sender": "BotA",
      "sender_type": "BOT",
      "message": "Hej!"
    }
  ],
  "settings": {
    "max-actions": 3,
    "min-delay-ms": 800,
    "max-delay-ms": 4500,
    "global-silence-chance": 0.25,
    "reply-chance": 0.65
  }
}
```

### Opis pól
- `request_id`: losowy identyfikator requestu (UUID).
- `server`: metadane serwera.
  - `server_id`: identyfikator z `ai.remote.server-id`.
  - `mode`: tryb serwera z `ai.remote.server-mode`.
  - `online_players`: liczba aktualnie online.
- `tick`: tick serwera z percepcji AI.
- `time_ms`: timestamp w milisekundach.
- `bots`: lista botów objętych requestem (obecnie zawsze 1 bot).
  - `cooldown_ms`: wartość stała 0 (rezerwa pod przyszłe użycie).
  - `persona`: persona bota (domyślnie z `ai.remote.persona.*`, może być nadpisana metadanymi profilu bota).
- `chat`: lista ostatnich wpisów czatu (limit = `ai.remote.chat-limit`).
  - `sender_type`: `BOT` jeśli nadawca jest AIPlayerem, w przeciwnym razie `PLAYER`.
  - `message`: dla botów usuwany jest prefiks `:<botName>:` jeśli występuje.
- `settings`: bieżące ustawienia z `ai.remote.settings` (przekazywane 1:1).

### Przykładowe wywołanie (cURL)
```bash
curl -X POST "http://localhost:8080/v1/plan" \
  -H "Content-Type: application/json" \
  -d '{
    "request_id": "91cdeed1-67d6-4df3-9f7b-83dbb1bdb9ed",
    "server": {
      "server_id": "betterbox-1",
      "mode": "LOBBY",
      "online_players": 12
    },
    "tick": 123456,
    "time_ms": 1730000000000,
    "bots": [
      {
        "bot_id": "6cdb6376-3ba2-421a-8a42-98fc6f8a70f3",
        "name": "BotA",
        "online": true,
        "cooldown_ms": 0,
        "persona": {
          "language": "pl",
          "tone": "casual",
          "style_tags": ["short", "memes_light"],
          "avoid_topics": ["payments", "admin_powers", "cheating"],
          "knowledge_level": "average_player"
        }
      }
    ],
    "chat": [
      {
        "ts_ms": 1730000001000,
        "sender": "Player123",
        "sender_type": "PLAYER",
        "message": "Cześć!"
      }
    ],
    "settings": {
      "max-actions": 3,
      "min-delay-ms": 800,
      "max-delay-ms": 4500,
      "global-silence-chance": 0.25,
      "reply-chance": 0.65
    }
  }'
```

---

## 2. Chat Engagement API (inicjowanie rozmów)

### Gdzie jest wywoływane
- Konfiguracja: `chat.engagement.*` w `config.yml`.
- Endpoint: `base-url + engage-path` (domyślnie `/v1/engage`).
- Wywołanie tylko wtedy, gdy `chat.engagement.engage-players-on-chat` jest włączone i minął czas „ciszy” na czacie.

### Ile wiadomości z czatu jest wysyłanych
Do requestu trafia **ostatnie N wiadomości czatu**, gdzie `N` = `chat.engagement.chat-history-limit` (domyślnie 10). Jeśli czat ma mniej wpisów, wysyłana jest cała dostępna lista.

### Struktura requestu
`Content-Type: application/json`

```json
{
  "request_id": "uuid",
  "time_ms": 1730000000000,
  "bot_id": "uuid",
  "bot_name": "string",
  "target_player": "string",
  "chat_history": [
    "Player1: Siema!",
    "Player2: Co tam?"
  ],
  "example_prompt": "Siema PlayerX, robisz coś ciekawego? Nudzi mi się i chcę pogadać!"
}
```

### Opis pól
- `request_id`: losowy identyfikator requestu (UUID).
- `time_ms`: timestamp w milisekundach.
- `bot_id`, `bot_name`: identyfikator i nazwa AIPlayera.
- `target_player`: wybrany losowo gracz (nie-bot), do którego bot spróbuje się odezwać.
- `chat_history`: lista ostatnich wiadomości czatu (limit = `chat.engagement.chat-history-limit`).
- `example_prompt`: przykładowy prompt generowany przez plugin (pomocniczy, do inspiracji serwisu zewnętrznego).

### Przykładowe wywołanie (cURL)
```bash
curl -X POST "http://localhost:8080/v1/engage" \
  -H "Content-Type: application/json" \
  -d '{
    "request_id": "c2a0c349-1f34-4b71-8248-5b8a2f8bbf2c",
    "time_ms": 1730000000000,
    "bot_id": "6cdb6376-3ba2-421a-8a42-98fc6f8a70f3",
    "bot_name": "BotA",
    "target_player": "PlayerX",
    "chat_history": [
      "Player1: Siema!",
      "Player2: Co tam?"
    ],
    "example_prompt": "Siema PlayerX, robisz coś ciekawego? Nudzi mi się i chcę pogadać!"
  }'
```
