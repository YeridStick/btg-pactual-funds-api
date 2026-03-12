package co.btg.usecase.cliente;

import co.btg.model.cliente.Cliente;
import co.btg.model.cliente.gateways.ClienteRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RequiredArgsConstructor
public class ClienteUseCase {

    private final ClienteRepository clienteGateway;

    public Flux<Cliente> listarClientes(int page, int size) {
        return clienteGateway.listarClientes(page, size);
    }

    public Mono<Cliente> crearCliente(Cliente cliente) {
        return clienteGateway.crearCliente(cliente);
    }

    public Mono<Cliente> editarCliente(String id, Cliente datosActualizados) {
        return clienteGateway.obtenerCliente(id)
                .map(existente -> {
                    Optional.ofNullable(datosActualizados.getNombre())
                            .filter(s -> !s.isBlank())
                            .ifPresent(existente::setNombre);

                    Optional.ofNullable(datosActualizados.getPreferenciaNotificacion())
                            .filter(s -> !s.isBlank())
                            .map(String::toUpperCase)
                            .ifPresent(existente::setPreferenciaNotificacion);

                    return existente;
                })
                .flatMap(clienteGateway::editarCliente);
    }
}