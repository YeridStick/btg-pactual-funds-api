package co.btg.model.fondo;
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
public class Fondo {
    private String id;
    private String nombre;
    private Double montoMinimo;
    private String categoria;
}
