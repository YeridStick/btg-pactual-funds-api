package co.btg.dynamodb.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ClienteEntity {

    @NotBlank(message = "El ID no puede estar vacío")
    private String id;

    @NotBlank(message = "El Nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El saldo es obligatorio")
    @Min(value = 0, message = "El saldo no puede ser negativo")
    private Double saldo;

    @NotBlank(message = "La preferencia de notificación es obligatoria")
    private String preferenciaNotificacion;

    private List<String> fondosSuscritos;

    private Long version;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("preferencia")
    public String getPreferenciaNotificacion() {
        return preferenciaNotificacion;
    }

    public void setPreferenciaNotificacion(String preferenciaNotificacion) {
        this.preferenciaNotificacion = preferenciaNotificacion;
    }

    @DynamoDbAttribute("fondos_suscritos")
    public List<String> getFondosSuscritos() {
        return fondosSuscritos;
    }

    public void setFondosSuscritos(List<String> fondosSuscritos) {
        this.fondosSuscritos = fondosSuscritos;
    }

    @DynamoDbVersionAttribute
    @DynamoDbAttribute("version")
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}