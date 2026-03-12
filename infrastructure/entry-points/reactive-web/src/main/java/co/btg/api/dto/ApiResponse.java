package co.btg.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Estructura genérica para todas las respuestas de la API")
public class ApiResponse<T> {

    @Schema(description = "Mensaje informativo del resultado de la operación", example = "Suscripción exitosa")
    private String mensaje;

    @Schema(description = "Datos de la respuesta (puede ser un objeto, una lista o un paginado)")
    private T data;

    @Schema(description = "Código de estado HTTP", example = "200")
    private int codigo;
}