# s03e04 — City–Item File Search API

Spring Boot REST service that searches local CSV city/item files via OpenRouter LLM and a stdio files MCP server.

## Prerequisites

- JDK 23 (`JAVA_HOME=C:\tools\jdk-23.0.2`)
- Maven 3.9+
- Built filesmcp JAR: `C:\priv\aidevs4-cwiczenia\filesmcp-spring\target\filesmcp-0.0.1-SNAPSHOT.jar`
- Corpus directory `C:\priv\aidevs4-cwiczenia\workspace` with `cities.csv`, `items.csv`, `connections.csv`
- Environment variable `OPENROUTER_API_KEY` (required)
- `HUB_API_KEY` optional and unused by search endpoints

## Run

```powershell
$env:JAVA_HOME="C:\tools\jdk-23.0.2"
$env:Path="C:\tools\jdk-23.0.2\bin;$env:Path"
$env:OPENROUTER_API_KEY="your-key"

cd C:\priv\aidevs4-cwiczenia\s03e04
mvn spring-boot:run
```

The process stays up until stopped with Ctrl+C. On startup it launches the filesmcp stdio child (`FS_ROOTS` = data root) and exposes:

- `POST /api/city` — items in exactly one city
- `POST /api/items` — cities containing all named items

Request body: `{"params":"<natural language command>"}`  
Response body: `{"output":"<comma-separated names or descriptive error>"}` (`output` UTF-8 length 4–500 bytes).

## Fail-fast

Startup aborts if the corpus files are missing/unreadable, the filesmcp JAR is missing, or MCP tools cannot initialize. The app will not become ready in those cases.

## Stop

Terminate the Spring Boot process (Ctrl+C). The MCP child process exits with the parent.
