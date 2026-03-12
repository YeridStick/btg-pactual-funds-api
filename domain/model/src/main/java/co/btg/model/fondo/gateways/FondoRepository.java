package co.btg.model.fondo.gateways;

import co.btg.model.fondo.Fondo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FondoRepository {
    Mono<Fondo> findById(String fondoId);
    Flux<Fondo> findAllFondo(int page, int size);
    Mono<Fondo> crearFondo(Fondo fondo);
    Mono<Fondo> editarFondo(Fondo fondo);
}
