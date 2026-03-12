package co.btg.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ScalarConfig {

    @Bean
    public RouterFunction<ServerResponse> scalarRouter() {
        // 1. Definimos el contrato de la API manualmente en formato JSON
        String openApiJson = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "BTG Pactual API",
                    "version": "1.0.0",
                    "description": "Documentación manual para Spring 4"
                  },
                  "paths": {
                    "/api/fondos/all": {
                      "get": {
                        "summary": "Listar fondos disponibles",
                        "responses": { "200": { "description": "Lista de fondos" } }
                      }
                    },
                    "/api/fondos/suscribir": {
                      "post": {
                        "summary": "Suscribir a un fondo",
                        "parameters": [
                          { "name": "clienteId", "in": "query", "required": true, "schema": { "type": "string" } },
                          { "name": "fondoId", "in": "query", "required": true, "schema": { "type": "string" } }
                        ],
                        "responses": { "200": { "description": "Suscrito" } }
                      }
                    }
                  }
                }
                """;

        // 2. Insertamos ese JSON en el atributo 'data-spec' de Scalar
        String html = """
                <!doctype html>
                <html>
                  <head>
                    <title>API Reference - BTG Pactual</title>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                  </head>
                  <body>
                    <script id="api-reference" data-spec='%s'></script>
                    <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                  </body>
                </html>
                """.formatted(openApiJson);

        return route(GET("/docs"),
                req -> ServerResponse.ok()
                        .header("Content-Type", "text/html")
                        .bodyValue(html));
    }
}