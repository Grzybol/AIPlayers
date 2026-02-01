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
- Endpoint: `base-url + plan-path` (domyślnie `/v1/plan`).
- Wywołanie tylko wtedy, gdy `chat.engagement.engage-players-on-chat` jest włączone i minął czas „ciszy” na czacie.

### Ile wiadomości z czatu jest wysyłanych
Do requestu trafia **ostatnie N wiadomości czatu**, gdzie `N` = `chat.engagement.chat-history-limit` (domyślnie 10). Jeśli czat ma mniej wpisów, wysyłana jest cała dostępna lista.

### Struktura requestu
`Content-Type: application/json`

```json
{
  "request_id": "uuid",
  "time_ms": 1730000000000,
  "server": {
    "server_id": "betterbox-1",
    "mode": "LOBBY",
    "online_players": 12
  },
  "bots": [
    {
      "bot_id": "uuid",
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
      "ts_ms": 1730000000100,
      "sender": "Player1",
      "sender_type": "PLAYER",
      "message": "Siema!"
    }
  ],
  "settings": {
    "max_actions": 3,
    "min_delay_ms": 800,
    "max_delay_ms": 4500,
    "global_silence_chance": 0.25,
    "reply_chance": 0.65
  },
  "target_player": "string",
  "example_prompt": "Napisz krótką wiadomość angażującą gracza/bota o nicku PlayerX."
}
```

### Opis pól
- `request_id`: losowy identyfikator requestu (UUID).
- `time_ms`: timestamp w milisekundach.
- `server`: informacje o serwerze (`ai.remote.server-id`, `ai.remote.server-mode`, liczba online).
- `bots`: lista botów (dla engage gracz-bot zawiera jednego bota).
- `chat`: ostatnie linie czatu od graczy (limit = `chat.engagement.chat-history-limit`).
- `settings`: ustawienia planera (kopiowane z `ai.remote.settings.*`).
- `target_player`: wybrany losowo gracz (nie-bot), do którego bot spróbuje się odezwać.
- `example_prompt`: krótkie pole z promptem pomocniczym do rozpoczęcia rozmowy.

### Przykładowe wywołanie (cURL)
```bash
curl -X POST "http://localhost:8080/v1/plan" \
  -H "Content-Type: application/json" \
  -d '{
    "request_id": "c2a0c349-1f34-4b71-8248-5b8a2f8bbf2c",
    "time_ms": 1730000000000,
    "server": {
      "server_id": "betterbox-1",
      "mode": "LOBBY",
      "online_players": 12
    },
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
        "ts_ms": 1730000000100,
        "sender": "Player1",
        "sender_type": "PLAYER",
        "message": "Siema!"
      }
    ],
    "settings": {
      "max_actions": 3,
      "min_delay_ms": 800,
      "max_delay_ms": 4500,
      "global_silence_chance": 0.25,
      "reply_chance": 0.65
    },
    "target_player": "PlayerX",
    "example_prompt": "Napisz krótką wiadomość angażującą gracza/bota o nicku PlayerX."
  }'
```

### Bot2bot engagement
- Konfiguracja: `chat.engagement.bot2bot.*` w `config.yml`.
- Endpoint: `base-url + plan-path` (domyślnie `/v1/plan`).
- Wywołanie tylko wtedy, gdy `chat.engagement.bot2bot.enabled` jest włączone, minął czas ciszy
  w zakresie `min/max-empty-chat-time-to-engage-in-seconds` i jest dostępnych przynajmniej 2 boty online.
- `settings.global_silence_chance` jest zwiększane o `bot2bot.silence-multiplier` przy każdej kolejnej próbie,
  aby rozmowa botów nie trwała w nieskończoność.

---

## 3. Velocity Player Count Bridge API (proxy ↔ backend)

Ten rozdział opisuje **prosty protokół komunikacji** pomiędzy pluginem Bukkit/Paper (AIPlayers)
a osobnym pluginem na Velocity. Celem jest raportowanie do proxy **sumarycznej liczby graczy**
(humans + AI) ze wszystkich serwerów w sieci i podmiana wartości w odpowiedzi ping listy serwerów.

### Założenia
- **Każdy backend** (Paper/Spigot) z AIPlayers wysyła okresowo (lub przy zmianach) liczbę graczy.
- **Plugin Velocity** zbiera raporty ze wszystkich serwerów i **nadpisuje licznik online** w pingach.
- Komunikacja oparta o **Plugin Messaging Channel** (Velocity <-> Bukkit), ponieważ jest szybka
  i nie wymaga zewnętrznych zależności. Alternatywnie można to przenieść na Redis/HTTP, ale
  poniżej opisano wariant oparty o messaging.

### Kanały i format
Kanał główny: `aiplayers:count`

Wiadomości są kodowane jako JSON (UTF-8). Zalecany format:

```json
{
  "protocol": "aiplayers-count-v1",
  "server_id": "survival-1",
  "timestamp_ms": 1730000000000,
  "online_humans": 12,
  "online_ai": 5,
  "online_total": 17,
  "max_players_override": 0
}
```

#### Opis pól
- `protocol`: wersja protokołu, stała wartość `aiplayers-count-v1`.
- `server_id`: identyfikator backendu; powinien odpowiadać `ai.remote.server-id` albo nazwie z konfiguracji Velocity.
- `timestamp_ms`: czas wysłania (system time w ms).
- `online_humans`: liczba realnych graczy (Bukkit `getOnlinePlayers().size()`).
- `online_ai`: liczba aktywnych AI (sesje AI).
- `online_total`: suma humans + AI (wartość docelowa do agregacji na proxy).
- `max_players_override`: opcjonalnie, jeśli > 0, Velocity może ustawić max players na tę wartość lub
  przynajmniej `max(current, override)`.

### Cykl wysyłki (backend → Velocity)
- **Heartbeat co 5–10 sekund** (konfigurowalne).
- **Dodatkowo** wysyłka przy zdarzeniach:
  - join/quit gracza,
  - spawn/despawn AI,
  - restart/reload pluginu.

### Agregacja i logika po stronie Velocity
1. Velocity przechowuje `last_seen` + `online_total` dla każdego backendu.
2. Jeżeli `last_seen` jest starsze niż np. 30s, backend uznawany jest za offline i **nie jest sumowany**.
3. Na `ProxyPingEvent` proxy:
   - sumuje wszystkie aktywne `online_total`,
   - opcjonalnie sumuje `online_humans` i `online_ai` (na potrzeby debugowania),
   - ustawia `ping.setOnlinePlayers(sum)`.
4. Wartość `max_players`:
   - jeśli `max_players_override > 0` z któregokolwiek backendu, można przyjąć `max` z tych wartości,
   - w przeciwnym razie zachować konfigurację proxy.

### Obsługa błędów / bezpieczeństwo
- Jeśli `protocol` się nie zgadza → zignorować wiadomość.
- Jeśli `online_total` < `online_humans + online_ai` → poprawić na sumę.
- Jeśli `server_id` jest nieznany dla Velocity → przyjąć, ale oznaczyć jako `unverified` w logach (opcjonalnie).

### Przykładowy payload (backend → Velocity)
```json
{
  "protocol": "aiplayers-count-v1",
  "server_id": "lobby-1",
  "timestamp_ms": 1730000012345,
  "online_humans": 32,
  "online_ai": 8,
  "online_total": 40,
  "max_players_override": 0
}
```

### Wymagane API po stronie AIPlayers (Bukkit/Paper)
1. **Publiczna metoda** do pobierania liczb:
   - `getOnlineHumansCount()` (lub użycie `Bukkit.getOnlinePlayers().size()`),
   - `getOnlineAICount()` (sesje AI),
   - `getOnlineTotalCount()` (suma).
2. **Scheduler / task** wysyłający heartbeat na kanał `aiplayers:count`.
3. **Listener** na zdarzenia join/quit i spawn/despawn AI, aby triggerować szybką aktualizację.

### Wymagane API po stronie Velocity
1. **Rejestracja kanału** `aiplayers:count`.
2. **Listener** na `PluginMessageEvent` do parsowania JSON.
3. **Cache** z `server_id → {online_total, last_seen, max_players_override}`.
4. **Listener** na `ProxyPingEvent` do nadpisania liczby online.

### Notatki dla Codexa
- Zakładamy Velocity 3.4.0.
- Protokół jest **jednokierunkowy** (backend → proxy), brak potrzeby ACK.
- JSON można parsować przez Gson (Velocity i Bukkit).
