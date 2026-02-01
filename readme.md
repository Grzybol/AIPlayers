# AIPlayers

## Wymagania lokalne
- Java (JDK) 17+
- Maven

## Lokalne testy

### Linux
1. Zainstaluj Java (JDK) 17 lub nowszą i Maven.
2. W katalogu repozytorium uruchom:

```bash
mvn test
```

### Windows (PowerShell)
1. Zainstaluj Java (JDK) 17 lub nowszą i Maven.
2. W katalogu repozytorium uruchom:

```powershell
mvn test
```

## Tryb lokalny
Plugin działa wyłącznie lokalnie i nie komunikuje się z żadnymi zewnętrznymi usługami AI. Wszystkie decyzje są podejmowane przez wbudowany kontroler `DUMMY`, którego zachowanie można dostosować przez ustawienia w `config.yml`.

## PlaceholderAPI
Jeżeli na serwerze działa PlaceholderAPI, plugin rejestruje ekspansję `aiplayers` z następującymi placeholderami:

- `%aiplayers_bots%` / `%aiplayers_online_bots%` – liczba aktywnych botów.
- `%aiplayers_online%` / `%aiplayers_online_total%` / `%aiplayers_total_online%` / `%aiplayers_total%` – suma graczy online + botów.
- `%aiplayers_humans%` / `%aiplayers_players%` – liczba graczy online bez botów.
