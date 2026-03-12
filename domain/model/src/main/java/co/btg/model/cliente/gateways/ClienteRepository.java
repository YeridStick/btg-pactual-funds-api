package co.btg.model.cliente.gateways;

import co.btg.model.cliente.Cliente;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClienteRepository {
    Mono<Cliente> guardarSaldo(Cliente cliente);
    Mono<Cliente> obtenerCliente(String id);
    Flux<Cliente> listarClientes(int page, int size);
    Mono<Cliente> crearCliente(Cliente cliente);
    Mono<Cliente> editarCliente(Cliente cliente);
    Mono<Cliente> actualizarSaldoYFondo(String clienteId, Double nuevoSaldo, String fondoId, boolean esSuscripcion);
}
