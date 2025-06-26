(ns glimmer.app.docs
  (:require [io.pedestal.http.route :as route]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [glimmer.app.api.reitit :as reitit-api]))

;; Simple handler to serve OpenAPI spec
(defn swagger-spec-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (pr-str
           {:openapi "3.0.0"
            :info {:title "Glimmer API"
                   :description "Anonymous location-based ping/pong API for sharing presence and acknowledgments"
                   :version "1.0.0"}
            :paths {"/api/ping" {:post {:summary "Create a ping"
                                        :description "Submit your location when feeling invisible, lonely, or need help"
                                        :requestBody {:required true
                                                     :content {"application/json" {:schema {:type "object"
                                                                                           :properties {:coords {:type "array"
                                                                                                                :items {:type "number"}
                                                                                                                :minItems 2
                                                                                                                :maxItems 2}
                                                                                                       :tag {:type "string"}}
                                                                                           :required ["coords" "tag"]}}}}
                                        :responses {200 {:description "Ping created successfully"
                                                        :content {"application/json" {:schema {:type "object"
                                                                                              :properties {:status {:type "string"}
                                                                                                          :ping {:type "object"}}}}}}}}}
                    "/api/ping/query" {:post {:summary "Query pings"
                                              :description "Get recent pings, optionally filtered by tag"}}
                    "/api/pong" {:post {:summary "Create pongs (acknowledge pings)"
                                        :description "Acknowledge multiple pings at once to show support"}}
                    "/api/pong/query" {:post {:summary "Query pongs for a ping"
                                              :description "See who acknowledged a specific ping"}}}})})

;; Handler to serve Swagger UI HTML
(defn swagger-ui-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<!DOCTYPE html>
<html>
<head>
  <title>Glimmer API Documentation</title>
  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css\" />
</head>
<body>
  <div id=\"swagger-ui\"></div>
  <script src=\"https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js\"></script>
  <script>
    SwaggerUIBundle({
      url: '/docs/swagger.json',
      dom_id: '#swagger-ui',
      presets: [
        SwaggerUIBundle.presets.apis,
        SwaggerUIBundle.presets.standalone
      ],
      layout: \"StandaloneLayout\"
    });
  </script>
</body>
</html>"})

;; Pedestal routes for documentation
(def routes
  #{["/docs/swagger.json" :get [swagger-spec-handler] :route-name :swagger-spec]
    ["/docs/" :get [swagger-ui-handler] :route-name :swagger-ui]
    ["/docs" :get [swagger-ui-handler] :route-name :swagger-ui-redirect]})