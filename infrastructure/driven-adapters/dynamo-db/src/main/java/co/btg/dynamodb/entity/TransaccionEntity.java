package co.btg.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TransaccionEntity {
    private String id; // UUID único
    private String clienteId;
    private String fondoId;
    private String tipo; // "APERTURA" o "CANCELACION" / Futuro ENUM
    private Double monto;
    private Long timestamp;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    // Índice secundario para consultar el historial por cliente fácilmente
    @DynamoDbSecondaryPartitionKey(indexNames = "ClienteIndex")
    public String getClienteId() { return clienteId; }
}