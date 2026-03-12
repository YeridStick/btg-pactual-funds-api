package co.btg.config;

import co.btg.model.notification.NotificationFactory;
import co.btg.model.notification.gateways.NotificationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "co.btg.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

        /*
         * Al declarar el Factory como un Bean aquí, Spring buscará todos los
         * componentes que implementen NotificationStrategy (tus Adapters de SMS y EMAIL)
         * y se los pasará automáticamente.
         */
        @Bean
        public NotificationFactory notificationFactory(List<NotificationStrategy> strategies) {
                return new NotificationFactory(strategies);
        }
}
