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

## Konfiguracja OpenAI
Projekt nie korzysta z pliku `.env`. Klucz OpenAI ustawiany jest w `config.yml` jako wartość `ai.openai.api-key` i wymaga włączenia `ai.openai.enabled`. Przykład:

```yaml
ai:
  openai:
    enabled: true
    api-key: "WSTAW_KLUCZ_OPENAI"
    model: "gpt-4.1-mini"
```
