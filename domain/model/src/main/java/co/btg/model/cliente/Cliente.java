package co.btg.model.cliente;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    private String id;
    private String nombre;
    private Double saldo;
    private String preferenciaNotificacion;
    private List<String> fondosSuscritos;
    private Long version;
}