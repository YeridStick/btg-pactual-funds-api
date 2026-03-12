#!/bin/bash
# =============================================================================
# Script de inicialización de DynamoDB para LocalStack
# Se ejecuta automáticamente al iniciar el contenedor o se puede correr manualmente
# =============================================================================

ENDPOINT="http://localhost:4566"

echo "⏳ Esperando a que DynamoDB esté disponible..."
until aws dynamodb list-tables --endpoint-url $ENDPOINT --region us-east-1 > /dev/null 2>&1; do
  sleep 1
done
echo "✅ DynamoDB disponible"

# ----- Tabla: Clientes -----
echo "📦 Creando tabla Clientes..."
aws dynamodb create-table \
  --endpoint-url $ENDPOINT \
  --region us-east-1 \
  --table-name Clientes \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --no-cli-pager 2>/dev/null || echo "   ⚠️  Tabla Clientes ya existe"

# ----- Tabla: Fondos -----
echo "📦 Creando tabla Fondos..."
aws dynamodb create-table \
  --endpoint-url $ENDPOINT \
  --region us-east-1 \
  --table-name Fondos \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --no-cli-pager 2>/dev/null || echo "   ⚠️  Tabla Fondos ya existe"

# ----- Tabla: Transacciones (con GSI ClienteIndex) -----
echo "📦 Creando tabla Transacciones con índice ClienteIndex..."
aws dynamodb create-table \
  --endpoint-url $ENDPOINT \
  --region us-east-1 \
  --table-name Transacciones \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=cliente_id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "ClienteIndex",
      "KeySchema": [{"AttributeName": "cliente_id", "KeyType": "HASH"}],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --billing-mode PAY_PER_REQUEST \
  --no-cli-pager 2>/dev/null || echo "   ⚠️  Tabla Transacciones ya existe"

echo ""
echo "🔄 Cargando datos de prueba..."

# ----- Datos: Clientes -----
echo "👤 Insertando clientes de prueba..."
aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Clientes --no-cli-pager --item '{
  "id": {"S": "user123"},
  "nombre": {"S": "Yerid"},
  "saldo": {"N": "500000"},
  "preferencia_notificacion": {"S": "SMS"},
  "fondos_suscritos": {"L": []}
}'

aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Clientes --no-cli-pager --item '{
  "id": {"S": "user456"},
  "nombre": {"S": "María García"},
  "saldo": {"N": "500000"},
  "preferencia_notificacion": {"S": "EMAIL"},
  "fondos_suscritos": {"L": []}
}'

# ----- Datos: Fondos (5 fondos de inversión BTG) -----
echo "💰 Insertando fondos de prueba..."
aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Fondos --no-cli-pager --item '{
  "id": {"S": "1"},
  "nombre": {"S": "FPV_BTG_PACTUAL_RECAUDADORA"},
  "monto_minimo": {"N": "75000"},
  "categoria": {"S": "FPV"}
}'

aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Fondos --no-cli-pager --item '{
  "id": {"S": "2"},
  "nombre": {"S": "FPV_BTG_PACTUAL_ECOPETROL"},
  "monto_minimo": {"N": "75000"},
  "categoria": {"S": "FPV"}
}'

aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Fondos --no-cli-pager --item '{
  "id": {"S": "3"},
  "nombre": {"S": "DEUDAPRIVADA"},
  "monto_minimo": {"N": "50000"},
  "categoria": {"S": "FIC"}
}'

aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Fondos --no-cli-pager --item '{
  "id": {"S": "4"},
  "nombre": {"S": "FDO-ACCIONES"},
  "monto_minimo": {"N": "250000"},
  "categoria": {"S": "FIC"}
}'

aws dynamodb put-item --endpoint-url $ENDPOINT --region us-east-1 --table-name Fondos --no-cli-pager --item '{
  "id": {"S": "5"},
  "nombre": {"S": "FPV_BTG_PACTUAL_DINAMICA"},
  "monto_minimo": {"N": "100000"},
  "categoria": {"S": "FPV"}
}'

echo ""
echo "✅ Inicialización completada. Tablas y datos listos."
echo ""
echo "📋 Tablas creadas:"
aws dynamodb list-tables --endpoint-url $ENDPOINT --region us-east-1 --no-cli-pager
