package co.btg.model.notification;

import co.btg.model.notification.gateways.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class NotificationFactory {
    // Lista de estrategias (SMS, EMAIL, etc.) inyectada mediante el constructor
    // por la configuración de Beans en la capa de Application.
    private final List<NotificationStrategy> strategies;

    public NotificationStrategy getStrategy(String preferencia) {
        return strategies.stream()
                .filter(s -> s.getTipoPreferencia().equalsIgnoreCase(preferencia))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Estrategia de notificación no soportada: " + preferencia));
    }
}