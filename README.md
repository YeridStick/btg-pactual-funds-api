# BTG Pactual — Backend Challenge

API REST reactiva para la gestión de fondos de inversión, suscripciones de clientes y consulta de historial de transacciones. Desarrollada con **Spring Boot 3 (WebFlux)**, **DynamoDB** y **Clean Architecture**.

---

## Arquitectura

```
domain/
├── model/       → Entidades de dominio (Cliente, Fondo, Transaccion)
└── usecase/     → Casos de uso (FondosUseCase, TransaccionUseCase, ClienteUseCase)

infrastructure/
├── driven-adapters/dynamo-db/   → Adaptadores DynamoDB (ClienteAdapter, FondoAdapter, TransaccionAdapter)
├── entry-points/reactive-web/   → Controllers REST (FondosController, ClienteController)
└── helpers/metrics/             → Métricas con Micrometer

applications/app-service/        → Configuración y arranque Spring Boot
```

---

## Requisitos previos

- **Java 21+** (JDK)
- **Podman** (o Docker)
- **AWS CLI v2** (para cargar datos de prueba)

---

## 🚀 Puesta en marcha

### 1. Iniciar LocalStack con Podman

```bash
podman run --rm -it \
  -p 4567:4566 \
  -e SERVICES=dynamodb,sts \
  -e DEBUG=1 \
  --name btg_pactual_localstack \
  localstack/localstack
```

### 2. Crear tablas y cargar datos de prueba

En otra terminal, ejecutar los siguientes comandos con AWS CLI apuntando al endpoint local:

```bash
# Variables de entorno
$ENDPOINT = "http://localhost:4567"
$REGION = "us-east-1"
```

#### Crear tabla Clientes

```bash
aws dynamodb create-table `
  --endpoint-url $ENDPOINT --region $REGION `
  --table-name Clientes `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --no-cli-pager
```

#### Crear tabla Fondos

```bash
aws dynamodb create-table `
  --endpoint-url $ENDPOINT --region $REGION `
  --table-name Fondos `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --no-cli-pager
```

#### Crear tabla Transacciones (con índice `ClienteIndex`)

> ⚠️ **Importante:** Esta tabla requiere un GSI (Global Secondary Index) llamado `ClienteIndex` sobre el atributo `cliente_id`. Sin este índice, el endpoint de historial `/api/fondos/historial/{clienteId}` fallará con error 500.

```bash
aws dynamodb create-table `
  --endpoint-url $ENDPOINT --region $REGION `
  --table-name Transacciones `
  --attribute-definitions AttributeName=id,AttributeType=S AttributeName=cliente_id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --no-cli-pager `
  --% --global-secondary-indexes "[{\"IndexName\":\"ClienteIndex\",\"KeySchema\":[{\"AttributeName\":\"cliente_id\",\"KeyType\":\"HASH\"}],\"Projection\":{\"ProjectionType\":\"ALL\"}}]"
```

#### Insertar clientes de prueba

```bash
aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Clientes --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"user123\"},\"nombre\":{\"S\":\"Yerid\"},\"saldo\":{\"N\":\"500000\"},\"preferencia_notificacion\":{\"S\":\"SMS\"},\"fondos_suscritos\":{\"L\":[]}}"

aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Clientes --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"user456\"},\"nombre\":{\"S\":\"María García\"},\"saldo\":{\"N\":\"500000\"},\"preferencia_notificacion\":{\"S\":\"EMAIL\"},\"fondos_suscritos\":{\"L\":[]}}"
```

#### Insertar fondos de prueba

```bash
aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Fondos --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"1\"},\"nombre\":{\"S\":\"FPV_BTG_PACTUAL_RECAUDADORA\"},\"monto_minimo\":{\"N\":\"75000\"},\"categoria\":{\"S\":\"FPV\"}}"

aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Fondos --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"2\"},\"nombre\":{\"S\":\"FPV_BTG_PACTUAL_ECOPETROL\"},\"monto_minimo\":{\"N\":\"75000\"},\"categoria\":{\"S\":\"FPV\"}}"

aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Fondos --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"3\"},\"nombre\":{\"S\":\"DEUDAPRIVADA\"},\"monto_minimo\":{\"N\":\"50000\"},\"categoria\":{\"S\":\"FIC\"}}"

aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Fondos --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"4\"},\"nombre\":{\"S\":\"FDO-ACCIONES\"},\"monto_minimo\":{\"N\":\"250000\"},\"categoria\":{\"S\":\"FIC\"}}"

aws dynamodb put-item --endpoint-url $ENDPOINT --region $REGION --table-name Fondos --no-cli-pager `
  --% --item "{\"id\":{\"S\":\"5\"},\"nombre\":{\"S\":\"FPV_BTG_PACTUAL_DINAMICA\"},\"monto_minimo\":{\"N\":\"100000\"},\"categoria\":{\"S\":\"FPV\"}}"
```

### 3. Compilar y ejecutar la aplicación

```bash
# Compilar
.\gradlew.bat clean build

# Ejecutar
.\gradlew.bat bootRun
```

 La aplicación estará disponible en **http://localhost:8082**

---

## 🐳 Despliegue con Docker / Podman

Para construir y ejecutar la aplicación en un contenedor:

### 1. Construir la imagen

Asegúrate de haber compilado el proyecto primero con `./gradlew clean build`. Luego, desde la raíz del proyecto:

```bash
podman build -t btg-pactual-funds-api -f deployment/Dockerfile .
```

### 2. Ejecutar el contenedor

```bash
podman run -d \
  -p 8082:8082 \
  --name btg-api-container \
  -e "SPRING_PROFILES_ACTIVE=dev" \
  -e "AWS_DYNAMODB_ENDPOINT=http://localhost:4567" \
  btg-pactual-funds-api
```

> **Nota:** Si LocalStack se está ejecutando en el mismo host, asegúrate de que el contenedor pueda alcanzar el puerto `4567`.


---

## 📡 Endpoints disponibles

### Fondos

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/api/fondos/suscribir?clienteId=X&fondoId=Y` | Suscribir cliente a fondo |
| `POST` | `/api/fondos/cancelar?clienteId=X&fondoId=Y` | Cancelar suscripción |
| `GET` | `/api/fondos/historial/{clienteId}` | Historial de transacciones |
| `GET` | `/api/fondos/all?page=0&size=10` | Catálogo de fondos (paginado) |

### Clientes

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/api/clientes?page=0&size=10` | Listar clientes (paginado) |
| `POST` | `/api/clientes` | Crear nuevo cliente |

#### Ejemplo — Crear cliente

```bash
curl -X POST http://localhost:8082/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"id":"user789","nombre":"Carlos López","preferenciaNotificacion":"EMAIL"}'
```

> El saldo se asigna automáticamente en **$500.000**.

#### Ejemplo — Suscribir a fondo

```bash
curl -X POST "http://localhost:8082/api/fondos/suscribir?clienteId=user123&fondoId=1"
```

---

## 📖 Documentación API (Swagger)

Disponible en: **http://localhost:8082/swagger-ui.html**

---

## Verificar tablas en LocalStack

```bash
aws dynamodb list-tables --endpoint-url http://localhost:4567 --region us-east-1

aws dynamodb scan --endpoint-url http://localhost:4567 --region us-east-1 --table-name Clientes --no-cli-pager

aws dynamodb scan --endpoint-url http://localhost:4567 --region us-east-1 --table-name Fondos --no-cli-pager

aws dynamodb scan --endpoint-url http://localhost:4567 --region us-east-1 --table-name Transacciones --no-cli-pager
```
