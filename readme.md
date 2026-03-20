# AIPlayers

AIPlayers to plugin dla serwerów **Paper 1.18.2**, który dodaje **boty udające prawdziwych graczy**. Każdy bot ma własny profil, UUID, widoczny model gracza, stan online, pamięć czatu oraz konfigurację zachowania. Projekt jest przygotowany tak, aby działał zarówno w prostym trybie lokalnym (`DUMMY`), jak i z zewnętrznym backendem planującym akcje botów przez HTTP.

## Co robi projekt
- tworzy i utrzymuje profile AI graczy z trwałym UUID oraz zapisem ostatniej lokalizacji,
- spawnuje widocznych botów jako fake-player/NPC na mapie,
- pozwala botom poruszać się, pisać na czacie i reagować na aktywność graczy,
- wspiera ekonomię przez **Vault**,
- integruje się z **PlaceholderAPI**,
- może wysyłać dane do zewnętrznego planera HTTP,
- może raportować liczbę graczy/botów do mostka **Velocity**,
- zapisuje profile i przywraca sesje po restarcie pluginu.

## Aktualny stan projektu
Na dziś repozytorium zawiera działający plugin Java/Maven dla Paper z następującymi elementami:

- **lokalny kontroler `DUMMY`** do prostego zachowania botów bez usług zewnętrznych,
- **kontroler `REMOTE`** do planowania akcji przez zewnętrzne API HTTP,
- **historię czatu i pamięć wiadomości**, które mogą trafiać do logiki AI,
- **engagement chat**, czyli opcjonalne inicjowanie rozmów po okresie ciszy,
- **opcjonalny relay Discord**, konfigurowany w `config.yml`,
- **reload konfiguracji** przez komendę administracyjną,
- **placeholdery PlaceholderAPI** do prezentacji liczby botów i graczy,
- **bridge do Velocity** raportujący liczbę graczy przez socket Unix,
- **logowanie do pliku** oraz automatyczne przywracanie botów z zapisanych profili.

To oznacza, że README nie powinno już opisywać projektu wyłącznie jako „trybu lokalnego” — obecna implementacja obsługuje też integracje zewnętrzne, choć są one domyślnie wyłączone w konfiguracji.

## Wymagania
- **Java (JDK) 17+**
- **Maven**
- serwer **Paper 1.18.2**

### Opcjonalne zależności na serwerze
Plugin deklaruje soft-dependencies dla:
- **ProtocolLib** – wizualna obsługa fake-playerów,
- **Vault** – ekonomia botów,
- **PlaceholderAPI** – placeholdery,
- **Essentials** – integracja serwerowa, jeśli jest obecna.

## Budowanie i testy lokalne
W katalogu repozytorium uruchom:

### Linux / macOS
```bash
mvn test
```

### Windows (PowerShell)
```powershell
mvn test
```

Jeśli chcesz zbudować artefakt JAR, standardowo możesz użyć także:

```bash
mvn package
```

## Najważniejsze komendy
Wszystkie komendy wymagają uprawnienia `aiplayers.admin`.

- `/aiplayers add <name> <radius> '<chat instruction>'`
  - tworzy profil bota,
  - ustawia promień poruszania,
  - zapisuje instrukcję stylu rozmowy/charakteru,
  - spawnuje bota w pozycji administratora.
- `/aiplayers remove <name>` – usuwa bota i jego aktywną sesję.
- `/aiplayers list` – pokazuje aktywne boty wraz z pozycją, kontrolerem i trybem zachowania.
- `/aiplayers inspect <name>` – otwiera inventory bota do podglądu.
- `/aiplayers reload` – przeładowuje konfigurację pluginu bez restartu serwera.

## Jak działają boty
Każdy AIPlayer ma profil zawierający m.in.:
- nazwę i UUID,
- typ kontrolera (`DUMMY` albo `REMOTE`),
- tryb zachowania,
- spawn i ostatnią znaną lokalizację,
- promień poruszania (`roamRadius`),
- instrukcję czatu (`chatInstruction`).

Tick task pluginu okresowo buduje kontekst dla każdego aktywnego bota i podejmuje decyzję o następnej akcji. W praktyce bot może:
- poruszać się po wyznaczonym obszarze,
- odpowiadać na czacie,
- reagować na najbliższych graczy,
- korzystać z danych o historii rozmowy i ustawieniach persony.

## Tryby działania
### 1. Tryb lokalny (`DUMMY`)
To najprostszy tryb działania. Plugin nie potrzebuje backendu AI i używa lokalnej logiki do podejmowania decyzji. Ten tryb jest domyślny.

### 2. Tryb zewnętrzny (`REMOTE`)
Jeżeli w `config.yml` włączysz `ai.remote.enabled: true` i ustawisz poprawny `base-url`, plugin może wysyłać requesty HTTP do zewnętrznego planera. Taki backend dostaje dane o:
- serwerze,
- bocie lub botach,
- historii czatu,
- ustawieniach persony,
- parametrach planowania akcji.

Domyślnie projekt zawiera gotowy opis requestów w `docs/api.md`.

## Konfiguracja
Najważniejsze sekcje w `src/main/resources/config.yml`:

- `ai.tick-interval-ticks` – jak często działa główna pętla AI,
- `ai.default.*` – domyślny kontroler i tryb zachowania,
- `ai.roam.*` – parametry ruchu lokalnego,
- `ai.remote.*` – konfiguracja zewnętrznego planera HTTP,
- `chat.*` – historia czatu, limity oraz integracje,
- `chat.discord.*` – relay wiadomości do Discorda,
- `chat.engagement.*` – inicjowanie rozmów po ciszy na czacie,
- `economy.enabled` – integracja Vault,
- `velocity.bridge.*` – raportowanie liczby graczy do Velocity.

### Przykładowe aktualne wartości domyślne
```yaml
ai:
  tick-interval-ticks: 10
  default:
    behavior-mode: WANDER
    controller-type: DUMMY
  remote:
    enabled: false
    base-url: "http://localhost:8080"
    plan-path: "/v1/plan"
chat:
  history-size: 20
  discord:
    enabled: false
economy:
  enabled: true
velocity:
  bridge:
    enabled: true
```

## PlaceholderAPI
Jeżeli na serwerze działa PlaceholderAPI, plugin rejestruje ekspansję `aiplayers` z następującymi placeholderami:

- `%aiplayers_bots%` / `%aiplayers_online_bots%` – liczba aktywnych botów online,
- `%aiplayers_online%` / `%aiplayers_online_total%` / `%aiplayers_total_online%` / `%aiplayers_total%` – raportowana łączna liczba graczy i botów,
- `%aiplayers_humans%` / `%aiplayers_players%` – liczba realnych graczy online.

## Integracje zewnętrzne
### Remote Planner API
Plugin może komunikować się z backendem HTTP do planowania akcji. Dokumentacja payloadów i kontraktu API znajduje się w:

- `docs/api.md`

### Chat engagement API
Plugin może wywoływać osobny endpoint do inicjowania rozmów, gdy czat jest pusty przez dłuższy czas.

### Velocity bridge
Plugin potrafi wysyłać heartbeat z liczbą graczy/botów do procesu pośredniczącego po socketcie Unix. To przydaje się np. przy prezentowaniu sumarycznej liczby graczy w środowisku proxy.

## Przechowywanie danych
Plugin zapisuje profile botów na dysku i przy starcie próbuje je odtworzyć. Dzięki temu po restarcie serwera nie trzeba ręcznie tworzyć wszystkich botów od nowa.

## Dokumentacja w repozytorium
Dodatkowe materiały:
- `docs/README.md` – techniczny opis pluginu,
- `docs/api.md` – kontrakty HTTP dla integracji zewnętrznych,
- `docs/roadmap.md` – kierunek rozwoju projektu.

## Krótkie podsumowanie
AIPlayers jest obecnie projektem bardziej rozbudowanym niż prosty lokalny NPC plugin. To baza pod boty „graczopodobne” dla Paper, z miejscem na własny backend AI, integracje czatowe, ekonomię i raportowanie do infrastruktury serwerowej.
