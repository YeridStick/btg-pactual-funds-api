package co.btg.model.notification.gateways;

import co.btg.model.cliente.Cliente;
import co.btg.model.transaccion.Transaccion;
import reactor.core.publisher.Mono;

public interface NotificationStrategy {
    Mono<Void> enviar(Cliente cliente, Transaccion transaccion);
    String getTipoPreferencia(); // Para identificar si es SMS o EMAIL
}
