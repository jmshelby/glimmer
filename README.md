# Glimmer API

Anonymous location-based ping/pong API for sharing presence and acknowledgments.

## API Endpoint

```
POST /glimmer/op
```

## Operations

### Create Ping
```json
{
  "op": "ping",
  "in": {
    "coords": [40.7128, -74.0060],
    "tag": "invisible"
  }
}
```

### Query Pings
```json
{
  "op": "ping/query", 
  "in": {
    "tag": "invisible",
    "limit": 1000
  }
}
```

### Create Pongs (Bulk Acknowledge)
```json
{
  "op": "pong",
  "in": {
    "pings": ["uuid1", "uuid2", "uuid3"],
    "source": "moonchild47"
  }
}
```

### Query Pongs for Ping
```json
{
  "op": "ping.pong/query",
  "in": {
    "ping": "ping-uuid"
  }
}
```

## Development

```bash
# Start REPL
clj -M:dev

# Initialize database (in REPL)
(require '[local.env])
(require '[datomic.client.api :as d])
(require '[glimmer.app.datomic :as datomic])
(require '[glimmer.core :as core])

(def client @datomic/client)
(d/create-database client {:db-name "glimmer"})
(core/init-schema!)

# Start development server
(require '[local.server :as server])
(def dev-server (server/run-dev 8080))
```

## Health Check

```
GET /health
```