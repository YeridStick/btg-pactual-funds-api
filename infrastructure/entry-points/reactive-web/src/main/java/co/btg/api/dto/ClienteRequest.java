package co.btg.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClienteRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;

    @NotBlank(message = "La preferencia de notificación es obligatoria")
    @Pattern(regexp = "^(?i)(SMS|EMAIL)$", message = "La preferencia debe ser SMS o EMAIL")
    private String preferenciaNotificacion;

    @Min(value = 0, message = "El saldo inicial no puede ser negativo")
    private Double saldo = 500000.0; // Valor por defaul
}