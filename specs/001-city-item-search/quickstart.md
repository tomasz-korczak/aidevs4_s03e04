# Quickstart: City–Item File Search API

Validate the feature end-to-end after implementation. This guide is a runbook, not an implementation dump.

## Prerequisites

- JDK 23 at `C:\tools\jdk-23.0.2`
- Maven on `PATH`
- Built filesmcp JAR at  
  `C:\priv\aidevs4-cwiczenia\filesmcp-spring\target\filesmcp-0.0.1-SNAPSHOT.jar`  
  (run `mvn clean package` in `filesmcp-spring` if missing)
- Data directory `C:\priv\aidevs4-cwiczenia\workspace` containing:
  - `cities.csv`, `items.csv`, `connections.csv`
- Environment:
  - `OPENROUTER_API_KEY` — required
  - `HUB_API_KEY` — set if hub integration is exercised
  - `JAVA_HOME=C:\tools\jdk-23.0.2`

## Configuration checklist

Confirm `application.yml` (or env overrides) for:

| Setting | Default / example |
|---------|-------------------|
| `server.port` | `8080` |
| `app.llm.model` | `nvidia/nemotron-3-ultra-550b-a55b:free` |
| `app.mcp.files.jar-path` | filesmcp JAR path above |
| `app.mcp.files.data-root` | `C:\priv\aidevs4-cwiczenia\workspace` |
| `spring.ai.openai.base-url` | `https://openrouter.ai/api` |
| `spring.ai.openai.api-key` | `${OPENROUTER_API_KEY}` |
| Prompt templates | `classpath:prompts/items-system.st`, `city-system.st` |
| Logging | console + rolling file; tool + model exchange loggers enabled |

Contract reference: [contracts/openapi.yaml](./contracts/openapi.yaml), [contracts/system-prompts.md](./contracts/system-prompts.md).  
Data shapes: [data-model.md](./data-model.md).

## Build & run

```powershell
$env:JAVA_HOME="C:\tools\jdk-23.0.2"
$env:Path="C:\tools\jdk-23.0.2\bin;$env:Path"
$env:OPENROUTER_API_KEY="..."
$env:HUB_API_KEY="..."   # if used

cd C:\priv\aidevs4-cwiczenia\s03e04
mvn clean package
mvn spring-boot:run
```

**Expected startup**: process stays up; MCP stdio child starts with `FS_ROOTS` = data root; both endpoints reachable.  
**Fail-fast**: missing CSVs or MCP jar/init failure → application does not become ready.

## Validation scenarios

Base URL: `http://localhost:8080`

### 1. Cities for items (`POST /api/items`)

```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/items `
  -ContentType "application/json" `
  -Body '{"params":{"items":["<known item name from items.csv>"]}}'
```

**Expect**: `{"output":"..."}` with UTF-8 length 4–500; on success, comma-separated **exact** city names from `cities.csv`.

### 2. Items in a city (`POST /api/city`)

```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/city `
  -ContentType "application/json" `
  -Body '{"params":{"city":"Warszawa"}}'
```

**Expect**: comma-separated exact item names, or a descriptive error if the result cannot fit in 500 bytes.

### 3. Out-of-scope / multi-city / missing entity

- `/api/city` with two city names → descriptive error in `output`
- `/api/items` with unknown item → descriptive error (no partial cities)
- Missing/empty `params` object → `400` (or documented client error) before LLM call

### 4. Byte-window enforcement

Pick a city known to have a very large item set (or mock in tests): response must be a descriptive overflow error mentioning the count, still within 4–500 bytes — never truncated names.

### 5. Logging checks

While calling endpoints, confirm:

- Console and log file both receive entries
- Each tool call logs parameters and results
- Each model turn logs system prompt, tool definitions, user prompt, and model response

### 6. Read-only MCP

Confirm logs show only `fs_read` / `fs_search` (no `fs_write` / `fs_manage`). Workspace files unchanged after searches.

## Stop

Terminate the Spring Boot process externally (Ctrl+C / process manager). MCP child should exit with the parent.
