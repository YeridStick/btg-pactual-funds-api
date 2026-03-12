package co.btg.usecase.transaccion;

import co.btg.model.transaccion.Transaccion;
import co.btg.model.transaccion.gateways.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionUseCase – Pruebas unitarias")
class TransaccionUseCaseTest {

    @Mock
    private TransaccionRepository transaccionRepository;

    private TransaccionUseCase transaccionUseCase;

    @BeforeEach
    void setUp() {
        transaccionUseCase = new TransaccionUseCase(transaccionRepository);
    }

    @Nested
    @DisplayName("obtenerHistorialPorCliente()")
    class ObtenerHistorialPorCliente {

        @Test
        @DisplayName("Debe retornar historial de transacciones del cliente")
        void debeRetornarHistorialDelCliente() {
            Transaccion tx1 = Transaccion.builder()
                    .id("TX-001").clienteId("CLI-001").fondoId("FPV-001")
                    .monto(75000.0).tipo("APERTURA").timestamp(1000L)
                    .build();
            Transaccion tx2 = Transaccion.builder()
                    .id("TX-002").clienteId("CLI-001").fondoId("FPV-002")
                    .monto(50000.0).tipo("CANCELACION").timestamp(2000L)
                    .build();

            when(transaccionRepository.consultarHistorialPorCliente("CLI-001"))
                    .thenReturn(Flux.just(tx1, tx2));

            StepVerifier.create(transaccionUseCase.obtenerHistorialPorCliente("CLI-001"))
                    .expectNext(tx1)
                    .expectNext(tx2)
                    .verifyComplete();

            verify(transaccionRepository).consultarHistorialPorCliente("CLI-001");
        }

        @Test
        @DisplayName("Debe retornar Flux vacío cuando el cliente no tiene transacciones")
        void debeRetornarVacioCuandoNoHayTransacciones() {
            when(transaccionRepository.consultarHistorialPorCliente("CLI-001"))
                    .thenReturn(Flux.empty());

            StepVerifier.create(transaccionUseCase.obtenerHistorialPorCliente("CLI-001"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe propagar error del repositorio")
        void debePropagararErrorDelRepositorio() {
            when(transaccionRepository.consultarHistorialPorCliente("CLI-001"))
                    .thenReturn(Flux.error(new RuntimeException("Error de conexión")));

            StepVerifier.create(transaccionUseCase.obtenerHistorialPorCliente("CLI-001"))
                    .expectErrorMatches(e -> e instanceof RuntimeException
                            && e.getMessage().equals("Error de conexión"))
                    .verify();
        }

        @Test
        @DisplayName("Debe delegar correctamente el clienteId al repositorio")
        void debeDelegarClienteIdAlRepositorio() {
            when(transaccionRepository.consultarHistorialPorCliente("CLI-999"))
                    .thenReturn(Flux.empty());

            transaccionUseCase.obtenerHistorialPorCliente("CLI-999").subscribe();

            verify(transaccionRepository).consultarHistorialPorCliente("CLI-999");
            verify(transaccionRepository, never()).consultarHistorialPorCliente("CLI-001");
        }
    }
}