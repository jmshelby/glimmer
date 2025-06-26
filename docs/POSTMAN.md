# Glimmer API - Postman Collection

Easy way to test the Glimmer API endpoints for developing the frontend.

## Setup

1. **Import Collection**: Import `postman-collection.json` into Postman
2. **Default Server**: Pre-configured to use `http://glimmer.dev.auticmatic.com:4000`
3. **Local Development**: Change `baseUrl` variable to `http://localhost:4000` for local testing
4. **Test**: Use the pre-configured requests

## Collection Overview

### 🏥 **Health Check**
- Simple GET to verify server is running
- Returns service status and timestamp

### 📍 **Ping Operations**
- **Create Ping**: Submit your location when feeling invisible/lonely/etc
- **Query All Pings**: Get recent pings (for map display)
- **Query by Tag**: Filter pings by specific tags like "invisible", "lonely", "help"

### 👋 **Pong Operations** 
- **Create Pongs**: Acknowledge multiple pings at once (bulk operation)
- **Query Pongs**: See who acknowledged a specific ping

### ⚠️ **Error Testing**
- Test error handling with invalid operations

## Usage Workflow

### 1. **Create Some Test Data**
Run the "Create Ping" requests to populate the database with test pings around Denver.

### 2. **Query Pings** 
Use "Query All Pings" to see what's available - copy the ping IDs from the response.

### 3. **Create Pongs**
- Copy ping IDs from step 2
- Paste them into the "Create Pongs" request (replace the placeholder UUIDs)
- Send to acknowledge those pings

### 4. **Check Acknowledgments**
Use "Query Pongs for Ping" with a specific ping ID to see who acknowledged it.

## Sample Coordinates (Denver Area)

- **Downtown**: `[39.7392, -104.9903]`
- **Capitol Hill**: `[39.7518, -105.0178]` 
- **Cherry Creek**: `[39.7047, -104.9598]`
- **Highland**: `[39.7817, -105.0178]`
- **Aurora**: `[39.6403, -104.8608]`

## API Design Notes

- **Single Endpoint**: All operations go to `/glimmer/op`
- **RPC Style**: Use `op` field to specify operation, `in` for parameters
- **String UUIDs**: Copy/paste UUIDs as strings - no need for special formatting
- **Bulk Operations**: Pongs can acknowledge multiple pings in one request
- **Anonymous**: No authentication required - designed for anonymous usage

## For Frontend Development

The Postman collection shows exactly how your React Native app should structure the API calls:

```javascript
// Example ping creation (use your actual server URL)
const API_BASE = 'http://glimmer.dev.auticmatic.com:4000';

fetch(`${API_BASE}/glimmer/op`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    op: 'ping',
    in: { coords: [lat, lng], tag: 'invisible' }
  })
})

// Example pong creation (bulk acknowledge)
fetch(`${API_BASE}/glimmer/op`, {
  method: 'POST', 
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    op: 'pong',
    in: { pings: [pingId1, pingId2], source: 'user123' }
  })
})
```