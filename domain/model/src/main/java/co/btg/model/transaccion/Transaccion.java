package co.btg.model.transaccion;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Transaccion {
    private String id; // UUID único
    private String clienteId;
    private String fondoId;
    private String tipo; // "APERTURA" o "CANCELACION" / Futuro ENUM
    private Double monto;
    private Long timestamp;
}
