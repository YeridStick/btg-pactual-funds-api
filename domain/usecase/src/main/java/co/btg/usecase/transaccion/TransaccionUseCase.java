package co.btg.usecase.transaccion;

import co.btg.model.transaccion.Transaccion;
import co.btg.model.transaccion.gateways.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class TransaccionUseCase {

    private final TransaccionRepository transaccionGateway;

    public Flux<Transaccion> obtenerHistorialPorCliente(String clienteId) {
        return transaccionGateway.consultarHistorialPorCliente(clienteId);
    }
}