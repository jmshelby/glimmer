# Glimmer Project - Claude Development Guide

## Project Overview
Glimmer is an anonymous location-based ping/pong API for sharing presence and acknowledgments. Built with Clojure, Pedestal, and Datomic Cloud.

## Architecture Patterns

### Code Conventions
- **Database references**: Use `$` symbol to represent Datomic database values (not `db`)
  ```clojure
  (let [$ (d/db conn)]
    (d/q {:query '[:find ?e :where [?e :ping/id]]
          :args [$]}))
  ```

- **Connection references**: Use `!` to represent Datomic connections
  ```clojure
  (defn my-service [! input]
    (let [$ (d/db !)]
      ;; ... use $ for queries
      ))
  ```

### Project Structure
Following huckleberry template patterns:
- `glimmer.app.*` - Application layer (service, API, config)
- `glimmer.datomic.*` - Database utilities and connections
- Schema definitions in `resources/app/db/schema/`
- Development helpers in `dev/local/`

### API Design
RPC-style API with single endpoint `/glimmer/op`:
- Operations: `ping`, `ping/query`, `pong`, `ping.pong/query`
- Request format: `{"op": "operation-name", "in": {...}}`
- Coordinates format: `"coords": [lat, lng]` (array, not separate fields)

### Development Commands
```bash
# Start REPL with dev profile
clj -M:dev

# Run linting
clj-kondo --lint src dev

# Start local server (in REPL)
(require '[local.server :as server])
(def dev-server (server/run-dev 8080))
```

### Testing Approach
Use sandbox namespace for manual testing:
```clojure
(require '[local.sandbox])
;; Test operations in REPL
```

## Schema Conventions
- Use `:entity.date/created` for timestamps
- Unique identifiers: `:entity/id` with `:db.type/uuid`
- References: `:entity/ref-name` with `:db.type/ref`