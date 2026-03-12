package co.btg.dynamodb.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TransaccionEntity {

    private String id;
    private String clienteId;
    private String fondoId;
    private String tipo;
    private Double monto;
    private Long timestamp;

    @NotBlank(message = "El ID de transacción es obligatorio")
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NotBlank(message = "El ID del cliente es obligatorio")
    @DynamoDbSecondaryPartitionKey(indexNames = "ClienteIndex")
    @DynamoDbAttribute("clienteId")
    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    @NotBlank(message = "El ID del fondo es obligatorio")
    @DynamoDbAttribute("fondo_id")
    public String getFondoId() {
        return fondoId;
    }

    public void setFondoId(String fondoId) {
        this.fondoId = fondoId;
    }

    @NotBlank(message = "El tipo de transacción es requerido")
    @DynamoDbAttribute("tipo")
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 0, message = "El monto no puede ser negativo")
    @DynamoDbAttribute("monto")
    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    @DynamoDbAttribute("timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}