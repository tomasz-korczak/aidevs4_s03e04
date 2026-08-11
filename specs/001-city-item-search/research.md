# Research: City–Item File Search API

## Decision: Spring Boot 3.5.15 + Spring AI 1.1.8 + Java 23

**Rationale**: Spring AI `1.1.8` is the latest stable line compatible with Spring Boot `3.5.x` (BOM upgrades Boot to `3.5.15`). Spring AI `2.x` requires Boot `4.x` and is out of scope. Java 23 matches the local JDK at `C:\tools\jdk-23.0.2` and the sibling `filesmcp-spring` project.

**Alternatives considered**:
- Spring Boot 3.5.14 / Spring AI 1.1.5 (filesmcp versions) — workable but not latest stable pair
- Spring AI 2.0 + Boot 4 — rejected (JDK/ecosystem jump beyond stated constraints)

## Decision: OpenRouter via Spring AI OpenAI starter

**Rationale**: OpenRouter exposes an OpenAI-compatible HTTP API. Configure:

```yaml
spring.ai.openai.api-key: ${OPENROUTER_API_KEY}
spring.ai.openai.base-url: https://openrouter.ai/api
spring.ai.openai.chat.options.model: ${app.llm.model}
```

Model default: `nvidia/nemotron-3-ultra-550b-a55b:free`, overridable via config.

**Alternatives considered**:
- Native OpenAI-only endpoint — rejected (user requires OpenRouter)
- Custom WebClient without Spring AI — rejected (loses ChatClient tool-calling / MCP integration)

## Decision: Stdio MCP client to local filesmcp JAR

**Rationale**: Spec requires stdio files MCP started with the app. Inspected `C:\priv\aidevs4-cwiczenia\filesmcp-spring`:

| Item | Value |
|------|--------|
| Artifact | `target/filesmcp-0.0.1-SNAPSHOT.jar` |
| Transport | stdio (`spring.ai.mcp.server.stdio=true`) |
| Roots | `FS_ROOTS` or `FS_ROOT` (comma-separated absolute paths) |
| Other env | `MCP_NAME`, `MCP_VERSION`, `MCP_INSTRUCTIONS`, `LOG_LEVEL`, `MAX_FILE_SIZE` |
| Tools | `fs_read`, `fs_search`, `fs_write`, `fs_manage` |

Client startup (Windows):

```text
command: java
args: -jar <filesmcp-jar>
env:
  FS_ROOTS: C:\priv\aidevs4-cwiczenia\workspace
  JAVA_HOME: C:\tools\jdk-23.0.2
```

Use Spring AI `spring-ai-starter-mcp-client` stdio connection properties, with jar path and data root parametrized in `application.yml`.

**Fail-fast**: Application readiness fails if workspace files are missing/unreadable or MCP client initialization fails (aligns with FR-018).

**Alternatives considered**:
- In-process CSV parsing without MCP — rejected (spec mandates files MCP)
- SSE MCP server — rejected (stdio required)

## Decision: Read-only MCP usage (tool filtering + prompts)

**Rationale**: `filesmcp-spring` has **no** dedicated `READ_ONLY` environment flag. Enforce read-only by:

1. Exposing only `fs_read` and `fs_search` as `ToolCallback`s to `ChatClient` (exclude `fs_write` / `fs_manage`)
2. System prompts forbidding writes/modifications

**Alternatives considered**:
- Rely on prompt alone — weaker; model could still call write tools if registered
- Fork filesmcp for a read-only mode — out of scope / unjustified complexity

## Decision: API contract `{params}` → `{output}` with 4–500 UTF-8 bytes

**Rationale**: Plan input defines endpoints and size constraints for hub/client compatibility (“minimize size”).

| Path | Method | Success `output` | Error `output` |
|------|--------|------------------|----------------|
| `/api/items` | POST | `City1,City2,...` exact city **name** values | Descriptive error (missing items, out of scope, no city, overflow, …) |
| `/api/city` | POST | `Item1,Item2,...` exact item **name** values | Descriptive error (multi-city, unknown city, out of scope, overflow, …) |

Rules:
- Validate `output` UTF-8 byte length ∈ [4, 500] after model/post-processing
- If a correct name list cannot fit in 500 bytes, return a descriptive overflow error (e.g. found N items not fitting 500 bytes limit) that itself is 4–500 bytes
- No compression/abbreviation of names; exact file values only
- Domain “not found” / out-of-scope cases use descriptive `output` strings (plan-time supersession of earlier empty-list clarification)

HTTP:
- `200` + `{output}` for success and domain/descriptive errors (unfulfillable command, missing entity, overflow, empty intersection, etc.)
- `400` when `params` is missing/blank before LLM invocation
- `500` + `{output}` descriptive error (still 4–500 UTF-8 bytes) when LLM or MCP tools fail after a successful application start

Happy-path name delimiter: comma with **no spaces** (`City1,City2`).

No wall-clock latency success criterion (SC-006 removed).

**Alternatives considered**:
- JSON array of names — rejected by plan-time contract
- Separate error DTO / non-200 for domain errors — rejected to keep single `output` channel and size discipline

## Decision: Dedicated system prompts per endpoint (string templates)

**Rationale**: Different search semantics (items→cities intersection vs single-city→items) need separate instructions, MCP tips, and output formatting rules. Store as Spring resource string templates under `classpath:prompts/` and bind configurable tips/rules from `application.yml`.

**Alternatives considered**:
- One shared prompt with endpoint name injection — higher confusion risk
- Hard-coded Java strings only — harder to parametrize/review

## Decision: Logging console + file; model and tool verbosity

**Rationale**: User requires:
- Console and file appenders (Logback)
- Log every tool execution (parameters + results)
- Log model exchange (system prompt, tool definitions, user prompt, response)

Implement via Logback (`CONSOLE` + `FILE`) and dedicated logger components wrapping ChatClient calls / ToolCallbacks (or Spring AI advisors/observation hooks where practical).

**Alternatives considered**:
- DEBUG-only framework logs — insufficiently structured for required fields
- External APM only — overkill for exercise scope

## Decision: Parametrized configuration surface

| Parameter | Source |
|-----------|--------|
| `HUB_API_KEY` | Environment variable allowed but **out of scope** for `/api/*` search behavior (not required to serve endpoints) |
| `OPENROUTER_API_KEY` | Environment variable → `spring.ai.openai.api-key` |
| Files MCP server location | `app.mcp.files.jar-path` (default `C:\priv\aidevs4-cwiczenia\filesmcp-spring\target\filesmcp-0.0.1-SNAPSHOT.jar`) |
| Data files location | `app.mcp.files.data-root` / MCP `FS_ROOTS` (default `C:\priv\aidevs4-cwiczenia\workspace`) |
| HTTP addresses | `server.port`, optional `server.address` / `app.http.base-url` |
| Main system prompts | `app.prompts.items` / `app.prompts.city` + template files |
| LLM model name | `app.llm.model` → chat options |

## Decision: Maven coordinates

**Rationale**: User-specified `groupId=pl.tomaszko`, `artifactId=s03e04`.

## Corpus schema findings (inspected workspace)

Actual headers on disk:

- `cities.csv`: `name,code`
- `items.csv`: `name,code` (names may be long descriptive strings)
- `connections.csv`: `itemCode,cityCode`

Join path: item/city **name** ↔ **code** ↔ connections. Prompts must instruct the model to read headers, join via codes, and return **name** column values exactly (never invent; never return codes in `output`).

## Resolved NEEDS CLARIFICATION

None remain for planning; plan-time inputs + MCP/repo inspection resolved stack, paths, contract, and read-only strategy.
