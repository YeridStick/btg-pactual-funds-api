package co.btg.notificationadapter;

import co.btg.model.cliente.Cliente;
import co.btg.model.notification.gateways.NotificationStrategy;
import co.btg.model.transaccion.Transaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SmsNotificationAdapter implements NotificationStrategy {

    @Override
    public Mono<Void> enviar(Cliente cliente, Transaccion transaccion) {
        return Mono.fromRunnable(() ->
                log.info("Simulando envío de [SMS] a {}: Suscripción exitosa al fondo {}. Monto: {}",
                        cliente.getNombre(), transaccion.getFondoId(), transaccion.getMonto())
        );
    }

    @Override
    public String getTipoPreferencia() { return "SMS"; }
}
