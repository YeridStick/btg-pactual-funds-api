package co.btg.usecase.cliente;

import co.btg.model.cliente.Cliente;
import co.btg.model.cliente.gateways.ClienteRepository;
import co.btg.model.common.BusinessException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteUseCase – Pruebas unitarias")
class ClienteUseCaseTest {

    @Mock
    private ClienteRepository clienteRepository;

    private ClienteUseCase clienteUseCase;

    private Cliente clienteBase;

    @BeforeEach
    void setUp() {
        clienteUseCase = new ClienteUseCase(clienteRepository);

        clienteBase = Cliente.builder()
                .id("CLI-001")
                .nombre("Juan Pérez")
                .saldo(500000.0)
                .preferenciaNotificacion("EMAIL")
                .fondosSuscritos(List.of("FPV-001"))
                .build();
    }

    // ====================================================================
    //  listarClientes
    // ====================================================================
    @Nested
    @DisplayName("listarClientes()")
    class ListarClientes {

        @Test
        @DisplayName("Debe retornar clientes de la página solicitada")
        void debeRetornarClientesPaginados() {
            Cliente cliente2 = Cliente.builder().id("CLI-002").nombre("María López").saldo(300000.0).build();
            when(clienteRepository.listarClientes(0, 2)).thenReturn(Flux.just(clienteBase, cliente2));

            StepVerifier.create(clienteUseCase.listarClientes(0, 2))
                    .expectNext(clienteBase)
                    .expectNext(cliente2)
                    .verifyComplete();

            verify(clienteRepository).listarClientes(0, 2);
        }

        @Test
        @DisplayName("Debe retornar Flux vacío cuando no hay clientes")
        void debeRetornarVacioCuandoNoHayClientes() {
            when(clienteRepository.listarClientes(0, 5)).thenReturn(Flux.empty());

            StepVerifier.create(clienteUseCase.listarClientes(0, 5))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe propagar error del repositorio")
        void debePropagararErrorDelRepositorio() {
            when(clienteRepository.listarClientes(0, 5))
                    .thenReturn(Flux.error(new RuntimeException("Error de conexión")));

            StepVerifier.create(clienteUseCase.listarClientes(0, 5))
                    .expectErrorMatches(e -> e instanceof RuntimeException
                            && e.getMessage().equals("Error de conexión"))
                    .verify();
        }
    }

    // ====================================================================
    //  crearCliente
    // ====================================================================
    @Nested
    @DisplayName("crearCliente()")
    class CrearCliente {

        @Test
        @DisplayName("Debe crear cliente correctamente")
        void debeCrearClienteCorrectamente() {
            when(clienteRepository.crearCliente(clienteBase)).thenReturn(Mono.just(clienteBase));

            StepVerifier.create(clienteUseCase.crearCliente(clienteBase))
                    .expectNextMatches(c -> "CLI-001".equals(c.getId())
                            && "Juan Pérez".equals(c.getNombre()))
                    .verifyComplete();

            verify(clienteRepository).crearCliente(clienteBase);
        }

        @Test
        @DisplayName("Debe propagar error cuando el repositorio falla")
        void debePropagararErrorAlCrear() {
            when(clienteRepository.crearCliente(any()))
                    .thenReturn(Mono.error(new BusinessException("Error de validación")));

            StepVerifier.create(clienteUseCase.crearCliente(clienteBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().equals("Error de validación"))
                    .verify();
        }
    }

    // ====================================================================
    //  editarCliente
    // ====================================================================
    @Nested
    @DisplayName("editarCliente()")
    class EditarCliente {

        @Test
        @DisplayName("Debe actualizar nombre y preferencia cuando ambos vienen con datos")
        void debeActualizarNombreYPreferencia() {
            Cliente datosActualizados = Cliente.builder()
                    .nombre("Carlos Ruiz")
                    .preferenciaNotificacion("sms")  // debe convertirse a uppercase
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(clienteRepository.editarCliente(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(clienteUseCase.editarCliente("CLI-001", datosActualizados))
                    .expectNextMatches(c -> "Carlos Ruiz".equals(c.getNombre())
                            && "SMS".equals(c.getPreferenciaNotificacion()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe conservar campos existentes cuando los nuevos vienen nulos")
        void debeConservarCamposNulos() {
            Cliente datosActualizados = Cliente.builder()
                    .nombre(null)
                    .preferenciaNotificacion(null)
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(clienteRepository.editarCliente(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(clienteUseCase.editarCliente("CLI-001", datosActualizados))
                    .expectNextMatches(c -> "Juan Pérez".equals(c.getNombre())
                            && "EMAIL".equals(c.getPreferenciaNotificacion()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe conservar campos existentes cuando los nuevos vienen en blanco")
        void debeConservarCamposEnBlanco() {
            Cliente datosActualizados = Cliente.builder()
                    .nombre("   ")
                    .preferenciaNotificacion("   ")
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(clienteRepository.editarCliente(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(clienteUseCase.editarCliente("CLI-001", datosActualizados))
                    .expectNextMatches(c -> "Juan Pérez".equals(c.getNombre())
                            && "EMAIL".equals(c.getPreferenciaNotificacion()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe actualizar solo el nombre cuando la preferencia viene nula")
        void debeActualizarSoloNombre() {
            Cliente datosActualizados = Cliente.builder()
                    .nombre("Nuevo Nombre")
                    .preferenciaNotificacion(null)
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(clienteRepository.editarCliente(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(clienteUseCase.editarCliente("CLI-001", datosActualizados))
                    .expectNextMatches(c -> "Nuevo Nombre".equals(c.getNombre())
                            && "EMAIL".equals(c.getPreferenciaNotificacion()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe propagar error cuando el cliente no existe")
        void debePropagararErrorCuandoClienteNoExiste() {
            when(clienteRepository.obtenerCliente("NO-EXISTE"))
                    .thenReturn(Mono.error(new BusinessException("El cliente con ID NO-EXISTE no existe en el sistema")));

            StepVerifier.create(clienteUseCase.editarCliente("NO-EXISTE", clienteBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("NO-EXISTE"))
                    .verify();

            verify(clienteRepository, never()).editarCliente(any());
        }

        @Test
        @DisplayName("Debe convertir preferencia a mayúsculas")
        void debeConvertirPreferenciaAMayusculas() {
            Cliente datosActualizados = Cliente.builder()
                    .nombre(null)
                    .preferenciaNotificacion("email")
                    .build();

            when(clienteRepository.obtenerCliente("CLI-001")).thenReturn(Mono.just(clienteBase));
            when(clienteRepository.editarCliente(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(clienteUseCase.editarCliente("CLI-001", datosActualizados))
                    .expectNextMatches(c -> "EMAIL".equals(c.getPreferenciaNotificacion()))
                    .verifyComplete();
        }
    }
}