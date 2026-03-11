package co.btg.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ClienteEntity {
    private String id;
    private String nombre;
    private Double saldo; // Debe inicializarse en 500000
    private String preferenciaNotificacion; // "SMS" o "EMAIL"
    private List<String> fondosSuscritos; // IDs de los fondos vinculados

    @DynamoDbPartitionKey
    public String getId() { return id; }
}