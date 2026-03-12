package co.btg.model.transaccion.gateways;

import co.btg.model.transaccion.Transaccion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransaccionRepository {
    Mono<Transaccion> save(Transaccion transaccion);
    Flux<Transaccion> consultarHistorialPorCliente(String clienteId);
}
