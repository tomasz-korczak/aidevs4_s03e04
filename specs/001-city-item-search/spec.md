# Feature Specification: City–Item File Search API

**Feature Branch**: `001-city-item-search`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "I'm building application that's exposing rest endpoints which can be used to search through local files. There will be two endpoints: 1. cities - endpoint return list of items found in given city 2. items - endpoint returns list of cities that has given items. Both endpoints take one parameter, which is command that has to be processed by LLM model. For understanding the command LLM model integration is to be deployed. LLM model is given command that will most likely looking though city items or searching a city having specified items, although command comes through public endpoint so there's no guarantee what that command will exactly be. To fullfill given command the LLM model will be given stdio files mcp server, that has to be started upon application run. LLM searches through available files using files mcp server and prepares answer for given command, that in turn is the endpoint response. There are 3 files in total: 1. cities.csv - city list, each city has it's own ID 2. items.csv - item list, each item has it's own ID 3. connections.csv - intersection between city and item, IDs only. Application upon start starts stdio files mcp server and exposed it's own 2 endpoints, then waits for executions. No other special preparations is required. Application runs until externally terminated."

## Clarifications

### Session 2026-08-11

- Q: When a command on the items endpoint names more than one item, should the returned cities be those that have all of the named items, or those that have any of them? → A: Intersection (all named items); if at least one named item is not found in the local item list, return a descriptive error
- Q: When a command on the cities endpoint names a city that is not in the local city list, should the response be a descriptive error or an empty item list? → A: Descriptive error when any named city is missing
- Q: When a command on the cities endpoint names more than one known city, should the returned items be the combined items from all of those cities, or should the endpoint require exactly one city? → A: Single city only; if more than one city is named, return a clear client error
- Q: When a command is off-topic or cannot be answered from the local city/item data (but the search machinery itself is working), should the endpoint always return a descriptive error, or may it return an empty list? → A: Always descriptive error when the command cannot be fulfilled from local data
- Q: If the local data files or the files MCP server cannot be started successfully, should the application refuse to start, or start and fail later when a search request arrives? → A: Fail fast: application exits/refuses to become ready if files or MCP cannot start

### Session 2026-08-12

- Q: On a successful search, should the endpoint response body be only a structured list of cities or items, or may it also include free-form text from the language model? → A: Name-only values in response (no codes); superseded for wire format by plan-time `{ "output": "Name1,Name2" }` string contract (see Session 2026-08-12b)
- Q: Spec↔plan conflicts (paths, `params`, `output` string, no empty-list success, `code` vocabulary) — update spec to match plan as source of truth? → A: Yes; plan/contracts are authoritative; spec updated to match
- Q: Happy-path name delimiter/spacing? → A: Comma only, no spaces (`City1,City2`)
- Q: Keep 30-second completion success criterion? → A: No; do not use a time-completion rule
- Q: LLM/MCP failure after successful startup — response shape? → A: HTTP 500 with `{ "output": "<descriptive error>" }` still obeying 4–500 UTF-8 bytes
- Q: `HUB_API_KEY` for this feature? → A: Out of scope for search endpoints; may exist as env only, unused by `/api/*`

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find Items in a City (Priority: P1)

A client calls `POST /api/city` with JSON `{ "params": "<natural language>" }` asking which items are associated with exactly one city. The service interprets the command, looks up the local city–item data, and returns `{ "output": "..." }` with comma-separated item names or a descriptive error string.

**Why this priority**: This is one of the two core search outcomes and delivers immediate value for city-centric queries.

**Independent Test**: Can be fully tested by sending a clear city-focused command to `/api/city` and verifying `output` matches exact `items.csv` names for that city (or a descriptive error).

**Acceptance Scenarios**:

1. **Given** the service is running with the three local data files available, **When** a client sends `params` that identify exactly one known city, **Then** `POST /api/city` returns HTTP 200 with `output` equal to that city's item **name** values joined by `,` with no spaces (exact `items.csv` names).
2. **Given** a known city has no linked items, **When** a client asks for that city's items, **Then** `POST /api/city` returns HTTP 200 with a descriptive error in `output` (4–500 UTF-8 bytes), not an empty success string.
3. **Given** the service is running, **When** a client omits or blanks `params`, **Then** `POST /api/city` returns HTTP 400 with a clear client error before LLM invocation.
4. **Given** `params` name a city not in `cities.csv`, **When** processed, **Then** HTTP 200 with descriptive error in `output` and no item name list.
5. **Given** `params` name more than one city, **When** processed, **Then** HTTP 200 with descriptive client error in `output` and no item name list.

---

### User Story 2 - Find Cities for Items (Priority: P1)

A client calls `POST /api/items` with JSON `{ "params": "<natural language>" }` asking which cities have given item(s). The service returns cities linked to **all** named items (intersection) as `{ "output": "City1,City2" }` or a descriptive error.

**Why this priority**: Equal core value to city→items search; both endpoints are required for the product.

**Independent Test**: Can be fully tested by sending a clear item-focused command to `/api/items` and verifying `output` city names match the intersection over local data (or a descriptive error).

**Acceptance Scenarios**:

1. **Given** the service is running with the three local data files available, **When** a client sends `params` that identify one or more known items, **Then** `POST /api/items` returns HTTP 200 with `output` equal to city **name** values linked to every named item, joined by `,` with no spaces.
2. **Given** every named item exists but no city contains all of them, **When** processed, **Then** HTTP 200 with a descriptive error in `output` (not an empty success string).
3. **Given** the service is running, **When** a client omits or blanks `params`, **Then** `POST /api/items` returns HTTP 400 before LLM invocation.
4. **Given** `params` name multiple items and at least one is missing from `items.csv`, **When** processed, **Then** HTTP 200 with descriptive error in `output` and MUST NOT return a partial city list.

---

### User Story 3 - Start Ready-to-Serve Search Service (Priority: P2)

An operator starts the application. The service becomes ready to accept `POST /api/items` and `POST /api/city` without further manual setup, and keeps running until stopped externally.

**Why this priority**: Startup readiness is required for the endpoints to be usable, but search behavior itself is the primary product value.

**Independent Test**: Can be tested by starting the application and confirming both endpoints accept requests without additional preparation steps.

**Acceptance Scenarios**:

1. **Given** the local data files are present and the files MCP server can start, **When** the application starts, **Then** both search endpoints become available and the service waits for requests until externally terminated.
2. **Given** the application has started successfully, **When** a client sends valid `params` to either endpoint, **Then** the service can fulfill the search using local files without an extra startup action.
3. **Given** local data files are missing/unreadable or the files MCP server cannot start, **When** the application attempts to start, **Then** it fails fast and MUST NOT advertise the search endpoints as available.

---

### User Story 4 - Handle Unexpected Public Commands (Priority: P3)

Because `params` is public and unconstrained, clients may send ambiguous, unrelated, or impossible commands. The service responds with a descriptive error in `output` without inventing corpus facts.

**Why this priority**: Important for robustness of a public interface, but secondary to the happy-path search flows.

**Independent Test**: Can be tested by sending nonsensical or off-topic commands and verifying HTTP 200 with descriptive `output` (4–500 bytes) and no fabricated names.

**Acceptance Scenarios**:

1. **Given** the service is running, **When** a client sends `params` that cannot be answered from local data, **Then** the endpoint returns HTTP 200 with a descriptive error in `output` and MUST NOT invent cities or items.
2. **Given** the service is running, **When** `params` intent does not match the endpoint, **Then** the service returns HTTP 200 with a descriptive inability-to-fulfill error in `output` unless it can still produce a valid in-contract success `output` grounded in local data.

---

### Edge Cases

- Unknown city or item: descriptive error in `output` (HTTP 200).
- Multiple items on `/api/items`: intersection; any missing item → descriptive error.
- Multiple cities on `/api/city`: descriptive error.
- Blank/missing `params`: HTTP 400 before LLM.
- Off-topic / unfulfillable command: HTTP 200 descriptive error in `output`.
- Result names exceed 500 UTF-8 bytes: HTTP 200 descriptive overflow error stating how many names were found (error itself 4–500 bytes); never truncate/abbreviate names.
- Startup: missing corpus or MCP → fail fast.
- Orphan connection codes at request time: descriptive error in `output` (HTTP 200).
- LLM unavailable or MCP tools fail after successful start: HTTP 500 with `{ "output": "<descriptive error>" }` still 4–500 UTF-8 bytes.
- Concurrent requests: best-effort; per-response correctness still required.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `POST /api/city` accepting JSON `{ "params": string }` and returning JSON `{ "output": string }` listing item names for exactly one city implied by `params`, or a descriptive error string in `output`.
- **FR-002**: System MUST expose `POST /api/items` accepting JSON `{ "params": string }` and returning JSON `{ "output": string }` listing city names for the item(s) implied by `params`, or a descriptive error string in `output`.
- **FR-013**: When `/api/items` `params` name multiple items, `output` MUST list only cities linked to every named item (intersection over connections).
- **FR-014**: When `/api/items` names one or more items and at least one is not in the local item list, the system MUST return a descriptive error in `output` and MUST NOT return a partial city list.
- **FR-015**: When `/api/city` names a city not in the local city list, the system MUST return a descriptive error in `output` and MUST NOT return an item list.
- **FR-016**: When `/api/city` names more than one city, the system MUST return a descriptive error in `output` and MUST NOT return an item list.
- **FR-003**: System MUST interpret `params` using a language model; `params` is free-form text and MUST NOT be assumed to follow a fixed syntax.
- **FR-004**: System MUST ground answers in the local corpus of exactly three files: `cities.csv` (`name,code`), `items.csv` (`name,code`), and `connections.csv` (`itemCode,cityCode`). Join keys are **codes**; returned values are **names** only.
- **FR-005**: System MUST give the language model access to those files through a stdio files MCP server started with the application, in read-only fashion (only `fs_read` / `fs_search` exposed to the model).
- **FR-006**: The language model’s final text MUST become the `output` string (after server-side byte-window validation); no separate free-form narrative field.
- **FR-007**: System MUST NOT invent cities, items, or connections absent from the local files, and MUST NOT modify/compress returned name values.
- **FR-008**: System MUST reject missing or blank `params` with HTTP 400 before LLM invocation.
- **FR-009**: When `params` cannot be fulfilled from local data (including off-topic commands), the system MUST return HTTP 200 with a descriptive error in `output` rather than an empty success `output`.
- **FR-017**: Domain “no matches” cases (known entities but empty intersection / no linked items) MUST use a descriptive error in `output` (HTTP 200), not an empty success string.
- **FR-019**: Successful name lists in `output` MUST use comma separators with **no spaces** (e.g. `Name1,Name2,Name3`).
- **FR-020**: Every `output` value (success or descriptive error) MUST have UTF-8 byte length between 4 and 500 inclusive. If a correct name list cannot fit, return a descriptive overflow error that states how many names were found and remains within 4–500 bytes.
- **FR-021**: When the language model or MCP file tools fail during a request after successful startup, the system MUST return HTTP 500 with `{ "output": "<descriptive error>" }` still satisfying FR-020.
- **FR-010**: On application start, the system MUST verify the local corpus is readable, start the stdio files MCP server, expose the two REST endpoints, and wait for requests until externally terminated.
- **FR-018**: If local data files are missing/unreadable or the stdio files MCP server cannot start, the system MUST fail fast and MUST NOT advertise the search endpoints as available.
- **FR-011**: System MUST expose only these search capabilities as its user-facing interface (no GUI).
- **FR-012**: Successful `output` MUST contain only exact CSV **name** values (no codes, no free-form model commentary). For `/api/city` those are item names; for `/api/items` those are city names.
- **FR-022**: `HUB_API_KEY` is out of scope for `/api/items` and `/api/city` behavior in this feature (env may exist for later hub use but MUST NOT be required for search).

### Key Entities

- **City**: Row in `cities.csv` with `name` (returned in `/api/items` success) and `code` (join key only).
- **Item**: Row in `items.csv` with `name` (returned in `/api/city` success) and `code` (join key only).
- **Connection**: Row in `connections.csv` linking `itemCode` to `cityCode`.
- **Command (`params`)**: Free-form client text for either endpoint.
- **Search Result (`output`)**: Comma-separated exact names (no spaces) or a descriptive error string; UTF-8 length 4–500 bytes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For commands that clearly name a city present in local data, `POST /api/city` returns the correct item-name `output` in at least 90% of representative test cases.
- **SC-002**: For commands that clearly name item(s) present in local data, `POST /api/items` returns the correct city-name `output` (intersection when multiple) in at least 90% of representative test cases.
- **SC-012**: Successful responses use only exact name values in `output` (no codes, no free-form narrative) in 100% of successful test cases, formatted as comma-separated names with no spaces.
- **SC-007**: When an `/api/items` command names at least one absent item, the client receives a descriptive error in `output` in 100% of cases (no partial city list).
- **SC-008**: When an `/api/city` command names an absent city, the client receives a descriptive error in `output` in 100% of cases.
- **SC-009**: When an `/api/city` command names more than one city, the client receives a descriptive error in `output` in 100% of cases.
- **SC-003**: After a successful start, both endpoints are reachable and can complete a valid search without further manual setup.
- **SC-011**: When required local files are missing/unreadable or MCP cannot start, the application fails to become ready in 100% of such startup attempts.
- **SC-004**: Missing/blank `params` yield HTTP 400 in 100% of cases.
- **SC-005**: When LLM or MCP tools fail during a request after startup, the client receives HTTP 500 with `output` descriptive error (4–500 UTF-8 bytes) in 100% of observed cases (no fabricated matches).
- **SC-010**: Off-topic or otherwise unfulfillable commands (machinery working) return HTTP 200 with descriptive `output` in 100% of representative cases, never an empty success `output`.
- **SC-013**: Every returned `output` (including errors) is between 4 and 500 UTF-8 bytes inclusive in 100% of responses that include `output`.

## Assumptions

- Corpus path and filesmcp JAR path are configured; default locations are those documented in plan/research.
- Plan/contracts are the wire-level source of truth aligned with this updated spec.
- No authentication/authorization for public `params` in this version.
- `HUB_API_KEY` is not required for search endpoints.
- No wall-clock latency success criterion is in scope for this feature.
- Concurrent request handling is best-effort; correctness of individual responses matters more than throughput.
- Credentials for OpenRouter come from environment/configuration, not source code.
- The application is a long-running process stopped only by external termination.
