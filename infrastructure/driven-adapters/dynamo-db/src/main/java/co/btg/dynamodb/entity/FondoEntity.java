package co.btg.dynamodb.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FondoEntity {

    @NotBlank(message = "El ID del fondo es obligatorio")
    private String id;

    @NotBlank(message = "El nombre del fondo no puede estar vacío")
    private String nombre;

    @NotNull(message = "El monto mínimo de vinculación es obligatorio")
    @Min(value = 1, message = "El monto mínimo debe ser mayor a cero")
    private Double montoMinimo;

    private String categoria;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    @DynamoDbAttribute("monto_minimo")
    public Double getMontoMinimo() {
        return montoMinimo;
    }

    public void setMontoMinimo(Double montoMinimo) {
        this.montoMinimo = montoMinimo;
    }
}