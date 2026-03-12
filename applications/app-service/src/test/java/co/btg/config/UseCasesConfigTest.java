package co.btg.config;

import co.btg.model.cliente.gateways.ClienteRepository;
import co.btg.model.fondo.gateways.FondoRepository;
import co.btg.model.transaccion.gateways.TransaccionRepository;
import co.btg.model.notification.gateways.NotificationStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCasesConfigTest {

    @Test
    void testUseCaseBeansAreScanned() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {

            String[] beanNames = context.getBeanDefinitionNames();

            boolean foundRealUseCase = false;
            for (String name : beanNames) {
                // Verificamos que se haya encontrado al menos un caso de uso real (ej: fondosUseCase)
                if (name.toLowerCase().contains("usecase")) {
                    foundRealUseCase = true;
                    break;
                }
            }

            assertTrue(foundRealUseCase, "El ComponentScan no encontró los UseCases reales en co.btg.usecase");
            assertTrue(context.containsBean("notificationFactory"), "El bean notificationFactory no fue creado");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        // Creamos Mocks de las interfaces que los UseCases necesitan en su constructor
        @Bean
        public ClienteRepository clienteRepository() {
            return Mockito.mock(ClienteRepository.class);
        }

        @Bean
        public FondoRepository fondoRepository() {
            return Mockito.mock(FondoRepository.class);
        }

        @Bean
        public TransaccionRepository transaccionRepository() {
            return Mockito.mock(TransaccionRepository.class);
        }

        @Bean
        public List<NotificationStrategy> notificationStrategies() {
            return new ArrayList<>();
        }
    }
}