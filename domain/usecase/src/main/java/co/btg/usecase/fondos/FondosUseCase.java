package co.btg.usecase.fondos;

import co.btg.model.cliente.Cliente;
import co.btg.model.cliente.gateways.ClienteRepository;
import co.btg.model.common.BusinessException;
import co.btg.model.fondo.Fondo;
import co.btg.model.fondo.gateways.FondoRepository;
import co.btg.model.notification.NotificationFactory;
import co.btg.model.transaccion.Transaccion;
import co.btg.model.transaccion.gateways.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class FondosUseCase {

    private final ClienteRepository clienteGateway;
    private final FondoRepository fondoGateway;
    private final TransaccionRepository transaccionGateway;
    private final NotificationFactory notificationFactory;

    public Mono<Transaccion> suscribirClienteAFondo(String clienteId, String fondoId) {
        return Mono.zip(clienteGateway.obtenerCliente(clienteId), fondoGateway.findById(fondoId))
                .flatMap(t -> procesarSuscripcion(t.getT1(), t.getT2()));
    }

    public Mono<Transaccion> cancelarSuscripcion(String clienteId, String fondoId) {
        return Mono.zip(clienteGateway.obtenerCliente(clienteId), fondoGateway.findById(fondoId))
                .flatMap(t -> procesarCancelacion(t.getT1(), t.getT2()));
    }

    public Flux<Fondo> obtenerFondosDisponibles(int page, int size) {
        return fondoGateway.findAllFondo(page, size);
    }

    public Mono<Fondo> crearFondo(Fondo fondo) {
        return validarFondo(fondo).then(fondoGateway.crearFondo(fondo));
    }

    public Mono<Fondo> editarFondo(String id, Fondo datos) {
        return fondoGateway.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("El fondo con ID " + id + " no existe")))
                .map(existente -> {
                    Optional.ofNullable(datos.getNombre()).filter(s -> !s.isBlank()).ifPresent(existente::setNombre);
                    Optional.ofNullable(datos.getCategoria()).filter(s -> !s.isBlank()).ifPresent(existente::setCategoria);
                    Optional.ofNullable(datos.getMontoMinimo()).filter(m -> m > 0).ifPresent(existente::setMontoMinimo);
                    return existente;
                })
                .flatMap(fondoGateway::editarFondo);
    }

    private Mono<Void> validarFondo(Fondo fondo) {
        if (fondo.getNombre() == null || fondo.getNombre().isBlank())
            return Mono.error(new BusinessException("El nombre del fondo es obligatorio"));

        if (fondo.getMontoMinimo() == null || fondo.getMontoMinimo() <= 0)
            return Mono.error(new BusinessException("El monto mínimo debe ser mayor a cero"));

        return Mono.empty();
    }

    // --- Lógica de Suscripción ---

    private Mono<Transaccion> procesarSuscripcion(Cliente cliente, Fondo fondo) {
        if (estaSuscrito(cliente, fondo.getId())) {
            return Mono.error(new BusinessException("Ya posee una suscripción activa al fondo " + fondo.getNombre()));
        }

        if (cliente.getSaldo() < fondo.getMontoMinimo()) {
            return Mono.error(new BusinessException(String.format(
                    "Saldo insuficiente para el fondo %s. Saldo: $%.0f, Requerido: $%.0f",
                    fondo.getNombre(), cliente.getSaldo(), fondo.getMontoMinimo())));
        }

        return clienteGateway.actualizarSaldoYFondo(cliente.getId(), cliente.getSaldo() - fondo.getMontoMinimo(), fondo.getId(), true)
                .flatMap(cli -> registrarYNotificar(cli, fondo, "APERTURA"));
    }

    // --- Lógica de Cancelación ---

    private Mono<Transaccion> procesarCancelacion(Cliente cliente, Fondo fondo) {
        if (!estaSuscrito(cliente, fondo.getId())) {
            return Mono.error(new BusinessException("El cliente no tiene una suscripción activa al fondo " + fondo.getNombre()));
        }

        return clienteGateway.actualizarSaldoYFondo(cliente.getId(), cliente.getSaldo() + fondo.getMontoMinimo(), fondo.getId(), false)
                .flatMap(cli -> registrarYNotificar(cli, fondo, "CANCELACION"));
    }

    // --- Métodos Auxiliares ---

    private boolean estaSuscrito(Cliente cliente, String fondoId) {
        return cliente.getFondosSuscritos() != null && cliente.getFondosSuscritos().contains(fondoId);
    }

    private Mono<Transaccion> registrarYNotificar(Cliente cliente, Fondo fondo, String tipo) {
        Transaccion tx = Transaccion.builder()
                .id(UUID.randomUUID().toString())
                .clienteId(cliente.getId())
                .fondoId(fondo.getId())
                .monto(fondo.getMontoMinimo())
                .tipo(tipo)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        return transaccionGateway.save(tx)
                .flatMap(savedTx -> notificationFactory.getStrategy(cliente.getPreferenciaNotificacion())
                        .enviar(cliente, savedTx)
                        .thenReturn(savedTx));
    }
}