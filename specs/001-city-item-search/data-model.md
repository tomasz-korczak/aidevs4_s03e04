# Data Model: City–Item File Search API

## Entities (local corpus)

### City

| Field | Source column | Notes |
|-------|---------------|-------|
| name | `cities.csv` → `name` | Exact display value returned by `/api/items` |
| code | `cities.csv` → `code` | Join key only; never returned in `output` |

**Identity**: `code` unique in corpus.  
**Validation**: Must exist in `cities.csv` when referenced by a `/api/city` command.

### Item

| Field | Source column | Notes |
|-------|---------------|-------|
| name | `items.csv` → `name` | Exact display value returned by `/api/city` (may be long descriptions) |
| code | `items.csv` → `code` | Join key only; never returned in `output` |

**Identity**: `code` unique in corpus.  
**Validation**: Every item named in an `/api/items` command MUST exist; otherwise descriptive error (no partial city list).

### Connection

| Field | Source column | Notes |
|-------|---------------|-------|
| itemCode | `connections.csv` → `itemCode` | FK → Item.code |
| cityCode | `connections.csv` → `cityCode` | FK → City.code |

**Meaning**: Item is present in city.  
**Validation**: Orphan codes at request time → descriptive error for that search (startup only requires files readable, not referential perfection).

## API message model

### SearchRequest

| Field | Type | Rules |
|-------|------|-------|
| params | string | Required, non-blank natural-language command |

### SearchResponse

| Field | Type | Rules |
|-------|------|-------|
| output | string | UTF-8 byte length 4–500 inclusive; either comma-separated exact names with **no spaces** (`Name1,Name2`) or a descriptive error string |

### Output semantics by endpoint

#### `/api/items` (items → cities)

- Interpret `params` to extract one or more item names/descriptions present in `items.csv`.
- Resolve cities linked to **all** named items (intersection over connections).
- Success: `output` = city **names** joined by `,` (exact `cities.csv` name values, unmodified).
- Errors (still in `output`, 4–500 bytes):
  - no item identified / out of scope
  - at least one named item missing from corpus
  - no city contains all items
  - result list exceeds 500-byte budget (state how many cities found)

#### `/api/city` (city → items)

- Interpret `params` to extract **exactly one** city name present in `cities.csv`.
- Success: `output` = item **names** linked to that city, joined by `,` (exact `items.csv` name values).
- Errors:
  - zero/multiple cities named or out of scope
  - city not found
  - city has no items (descriptive not-found/empty-domain message within byte limits)
  - result list exceeds 500-byte budget (state how many items found)

## Runtime configuration entities

### AppSettings (parametrized)

| Key | Purpose |
|-----|---------|
| hubApiKey | `HUB_API_KEY` env (optional; unused by search endpoints) |
| openRouterApiKey | `OPENROUTER_API_KEY` env |
| mcpJarPath | filesmcp JAR location |
| dataRoot | workspace directory passed as `FS_ROOTS` |
| httpPort / baseUrl | server bind / documented base |
| llmModel | OpenRouter model id |
| prompts.items / prompts.city | system prompt templates + tips |

### McpProcess

Child stdio process: Java + filesmcp JAR + `FS_ROOTS=<dataRoot>`. Tools available to the LLM: `fs_read`, `fs_search` only.

## Relationships

```text
Item (code) 1──* Connection *──1 City (code)
         └── name (output for /api/city)
                              └── name (output for /api/items)
```

## State / lifecycle

- **Application**: Starting → (corpus OK + MCP init OK) Ready → Running until external stop; otherwise fail-fast abort.
- **Corpus files**: Immutable for this feature (read-only access).
- **Request**: Validate params → LLM+tools search → build `output` → enforce byte window → respond.
