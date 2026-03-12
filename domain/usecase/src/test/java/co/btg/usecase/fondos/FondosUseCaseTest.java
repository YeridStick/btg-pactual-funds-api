package co.btg.usecase.fondos;

import co.btg.model.cliente.Cliente;
import co.btg.model.cliente.gateways.ClienteRepository;
import co.btg.model.common.BusinessException;
import co.btg.model.fondo.Fondo;
import co.btg.model.fondo.gateways.FondoRepository;
import co.btg.model.notification.NotificationFactory;
import co.btg.model.notification.gateways.NotificationStrategy;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FondosUseCase – Pruebas unitarias")
class FondosUseCaseTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private FondoRepository fondoRepository;
    @Mock private TransaccionRepository transaccionRepository;
    @Mock private NotificationFactory notificationFactory;
    @Mock private NotificationStrategy notificationStrategy;

    private FondosUseCase fondosUseCase;

    private Cliente clienteBase;
    private Fondo fondoBase;

    @BeforeEach
    void setUp() {
        fondosUseCase = new FondosUseCase(clienteRepository, fondoRepository, transaccionRepository, notificationFactory);

        clienteBase = Cliente.builder()
                .id("CLI-001")
                .nombre("Juan Pérez")
                .saldo(500000.0)
                .preferenciaNotificacion("EMAIL")
                .fondosSuscritos(new ArrayList<>())
                .build();

        fondoBase = Fondo.builder()
                .id("FPV-001")
                .nombre("FPV_BTG_PACTUAL_RECAUDADORA")
                .montoMinimo(75000.0)
                .categoria("FPV")
                .build();
    }

    // ====================================================================
    //  suscribirClienteAFondo
    // ====================================================================
    @Nested
    @DisplayName("suscribirClienteAFondo()")
    class SuscribirClienteAFondo {

        @Test
        @DisplayName("Debe suscribir cliente con saldo suficiente")
        void debeSuscribirConSaldoSuficiente() {
            Cliente clienteActualizado = Cliente.builder()
                    .id("CLI-001")
                    .saldo(425000.0)
                    .preferenciaNotificacion("EMAIL")
                    .fondosSuscritos(List.of("FPV-001"))
                    .build();

            Transaccion txEsperada = Transaccion.builder()
                    .clienteId("CLI-001")
                    .fondoId("FPV-001")
                    .monto(75000.0)
                    .tipo("APERTURA")
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));
            when(clienteRepository.actualizarSaldoYFondo("CLI-001", 425000.0, "FPV-001", true))
                    .thenReturn(Mono.just(clienteActualizado));
            when(transaccionRepository.save(any())).thenReturn(Mono.just(txEsperada));
            when(notificationFactory.getStrategy("EMAIL")).thenReturn(notificationStrategy);
            when(notificationStrategy.enviar(any(), any())).thenReturn(Mono.empty());

            StepVerifier.create(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .expectNextMatches(tx -> "APERTURA".equals(tx.getTipo())
                            && "CLI-001".equals(tx.getClienteId())
                            && "FPV-001".equals(tx.getFondoId()))
                    .verifyComplete();

            verify(clienteRepository).actualizarSaldoYFondo("CLI-001", 425000.0, "FPV-001", true);
            verify(transaccionRepository).save(any());
            verify(notificationStrategy).enviar(any(), any());
        }

        @Test
        @DisplayName("Debe rechazar suscripción por saldo insuficiente")
        void debeRechazarPorSaldoInsuficiente() {
            Cliente clientePobre = Cliente.builder()
                    .id("CLI-001")
                    .saldo(10000.0)
                    .preferenciaNotificacion("EMAIL")
                    .fondosSuscritos(new ArrayList<>())
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clientePobre));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));

            StepVerifier.create(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("Saldo insuficiente"))
                    .verify();

            verify(clienteRepository, never()).actualizarSaldoYFondo(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Debe rechazar suscripción duplicada al mismo fondo")
        void debeRechazarSuscripcionDuplicada() {
            Cliente clienteYaSuscrito = Cliente.builder()
                    .id("CLI-001")
                    .saldo(500000.0)
                    .preferenciaNotificacion("EMAIL")
                    .fondosSuscritos(new ArrayList<>(List.of("FPV-001")))
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteYaSuscrito));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));

            StepVerifier.create(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("Ya posee una suscripción activa"))
                    .verify();
        }

        @Test
        @DisplayName("Debe propagar error cuando el cliente no existe")
        void debePropagararErrorClienteNoExiste() {
            when(clienteRepository.obtenerCliente("NO-EXISTE"))
                    .thenReturn(Mono.error(new BusinessException("El cliente con ID NO-EXISTE no existe")));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));

            StepVerifier.create(fondosUseCase.suscribirClienteAFondo("NO-EXISTE", "FPV-001"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("NO-EXISTE"))
                    .verify();
        }

        @Test
        @DisplayName("Debe propagar error cuando el fondo no existe")
        void debePropagararErrorFondoNoExiste() {
            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(fondoRepository.findById("NO-EXISTE"))
                    .thenReturn(Mono.error(new BusinessException("Fondo no encontrado")));

            StepVerifier.create(fondosUseCase.suscribirClienteAFondo("CLI-001", "NO-EXISTE"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("Fondo no encontrado"))
                    .verify();
        }
    }

    // ====================================================================
    //  cancelarSuscripcion
    // ====================================================================
    @Nested
    @DisplayName("cancelarSuscripcion()")
    class CancelarSuscripcion {

        @Test
        @DisplayName("Debe cancelar suscripción activa y devolver saldo")
        void debeCancelarSuscripcionActiva() {
            Cliente clienteSuscrito = Cliente.builder()
                    .id("CLI-001")
                    .saldo(425000.0)
                    .preferenciaNotificacion("EMAIL")
                    .fondosSuscritos(new ArrayList<>(List.of("FPV-001")))
                    .build();

            Cliente clienteActualizado = Cliente.builder()
                    .id("CLI-001")
                    .saldo(500000.0)
                    .preferenciaNotificacion("EMAIL")
                    .fondosSuscritos(new ArrayList<>())
                    .build();

            Transaccion txEsperada = Transaccion.builder()
                    .clienteId("CLI-001")
                    .fondoId("FPV-001")
                    .monto(75000.0)
                    .tipo("CANCELACION")
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteSuscrito));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));
            when(clienteRepository.actualizarSaldoYFondo("CLI-001", 500000.0, "FPV-001", false))
                    .thenReturn(Mono.just(clienteActualizado));
            when(transaccionRepository.save(any())).thenReturn(Mono.just(txEsperada));
            when(notificationFactory.getStrategy("EMAIL")).thenReturn(notificationStrategy);
            when(notificationStrategy.enviar(any(), any())).thenReturn(Mono.empty());

            StepVerifier.create(fondosUseCase.cancelarSuscripcion("CLI-001", "FPV-001"))
                    .expectNextMatches(tx -> "CANCELACION".equals(tx.getTipo())
                            && tx.getMonto() == 75000.0)
                    .verifyComplete();

            verify(clienteRepository).actualizarSaldoYFondo("CLI-001", 500000.0, "FPV-001", false);
        }

        @Test
        @DisplayName("Debe rechazar cancelación si el cliente no está suscrito al fondo")
        void debeRechazarCancelacionSinSuscripcion() {
            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));

            StepVerifier.create(fondosUseCase.cancelarSuscripcion("CLI-001", "FPV-001"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("no tiene una suscripción activa"))
                    .verify();

            verify(clienteRepository, never()).actualizarSaldoYFondo(any(), any(), any(), anyBoolean());
        }
    }

    // ====================================================================
    //  obtenerFondosDisponibles
    // ====================================================================
    @Nested
    @DisplayName("obtenerFondosDisponibles()")
    class ObtenerFondosDisponibles {

        @Test
        @DisplayName("Debe retornar fondos paginados")
        void debeRetornarFondosPaginados() {
            Fondo fondo2 = Fondo.builder().id("FPV-002").nombre("Fondo B").montoMinimo(100000.0).categoria("FIC").build();
            when(fondoRepository.findAllFondo(0, 2)).thenReturn(Flux.just(fondoBase, fondo2));

            StepVerifier.create(fondosUseCase.obtenerFondosDisponibles(0, 2))
                    .expectNext(fondoBase)
                    .expectNext(fondo2)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar Flux vacío cuando no hay fondos")
        void debeRetornarVacioCuandoNoHayFondos() {
            when(fondoRepository.findAllFondo(0, 5)).thenReturn(Flux.empty());

            StepVerifier.create(fondosUseCase.obtenerFondosDisponibles(0, 5))
                    .verifyComplete();
        }
    }

    // ====================================================================
    //  crearFondo
    // ====================================================================
    @Nested
    @DisplayName("crearFondo()")
    class CrearFondo {

        @Test
        @DisplayName("Debe crear fondo con datos válidos")
        void debeCrearFondoValido() {
            when(fondoRepository.crearFondo(fondoBase)).thenReturn(Mono.just(fondoBase));

            StepVerifier.create(fondosUseCase.crearFondo(fondoBase))
                    .expectNextMatches(f -> "FPV-001".equals(f.getId()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe rechazar fondo con nombre nulo")
        void debeRechazarFondoSinNombre() {
            Fondo sinNombre = Fondo.builder().montoMinimo(75000.0).categoria("FPV").build();

            StepVerifier.create(fondosUseCase.crearFondo(sinNombre))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("nombre del fondo es obligatorio"))
                    .verify();

            verify(fondoRepository, never()).crearFondo(any());
        }

        @Test
        @DisplayName("Debe rechazar fondo con nombre en blanco")
        void debeRechazarFondoConNombreBlanco() {
            Fondo nombreBlanco = Fondo.builder().nombre("   ").montoMinimo(75000.0).categoria("FPV").build();

            StepVerifier.create(fondosUseCase.crearFondo(nombreBlanco))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("nombre del fondo es obligatorio"))
                    .verify();
        }

        @Test
        @DisplayName("Debe rechazar fondo con monto mínimo nulo")
        void debeRechazarFondoSinMonto() {
            Fondo sinMonto = Fondo.builder().nombre("Fondo X").categoria("FPV").build();

            StepVerifier.create(fondosUseCase.crearFondo(sinMonto))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("monto mínimo debe ser mayor a cero"))
                    .verify();
        }

        @Test
        @DisplayName("Debe rechazar fondo con monto mínimo cero o negativo")
        void debeRechazarFondoConMontoInvalido() {
            Fondo montoInvalido = Fondo.builder().nombre("Fondo X").montoMinimo(0.0).categoria("FPV").build();

            StepVerifier.create(fondosUseCase.crearFondo(montoInvalido))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("monto mínimo debe ser mayor a cero"))
                    .verify();
        }
    }

    // ====================================================================
    //  editarFondo
    // ====================================================================
    @Nested
    @DisplayName("editarFondo()")
    class EditarFondo {

        @Test
        @DisplayName("Debe actualizar todos los campos con datos válidos")
        void debeActualizarTodosLosCampos() {
            Fondo datos = Fondo.builder().nombre("Nuevo Nombre").categoria("FIC").montoMinimo(200000.0).build();

            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));
            when(fondoRepository.editarFondo(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(fondosUseCase.editarFondo("FPV-001", datos))
                    .expectNextMatches(f -> "Nuevo Nombre".equals(f.getNombre())
                            && "FIC".equals(f.getCategoria())
                            && f.getMontoMinimo() == 200000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe conservar campos existentes cuando los nuevos vienen nulos")
        void debeConservarCamposNulos() {
            Fondo datos = Fondo.builder().nombre(null).categoria(null).montoMinimo(null).build();

            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));
            when(fondoRepository.editarFondo(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(fondosUseCase.editarFondo("FPV-001", datos))
                    .expectNextMatches(f -> "FPV_BTG_PACTUAL_RECAUDADORA".equals(f.getNombre())
                            && "FPV".equals(f.getCategoria())
                            && f.getMontoMinimo() == 75000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe ignorar monto mínimo con valor cero o negativo")
        void debeIgnorarMontoInvalido() {
            Fondo datos = Fondo.builder().montoMinimo(0.0).build();

            when(fondoRepository.findById("FPV-001")).thenReturn(Mono.just(fondoBase));
            when(fondoRepository.editarFondo(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(fondosUseCase.editarFondo("FPV-001", datos))
                    .expectNextMatches(f -> f.getMontoMinimo() == 75000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe propagar error cuando el fondo no existe")
        void debePropagararErrorFondoNoExiste() {
            when(fondoRepository.findById("NO-EXISTE")).thenReturn(Mono.empty());

            StepVerifier.create(fondosUseCase.editarFondo("NO-EXISTE", fondoBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("NO-EXISTE"))
                    .verify();

            verify(fondoRepository, never()).editarFondo(any());
        }
    }
}