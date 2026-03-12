package co.btg.api;

import co.btg.api.config.GlobalErrorHandler;
import co.btg.api.dto.ClienteRequest;
import co.btg.model.cliente.Cliente;
import co.btg.model.common.BusinessException;
import co.btg.usecase.cliente.ClienteUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteController – Pruebas unitarias")
class ClienteControllerTest {

    @Mock
    private ClienteUseCase clienteUseCase;

    @InjectMocks
    private ClienteController clienteController;

    private WebTestClient webTestClient;

    private Cliente clienteBase;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToController(clienteController)
                .controllerAdvice(new GlobalErrorHandler())
                .build();

        clienteBase = Cliente.builder()
                .id("CLI-001")
                .nombre("Juan Pérez")
                .saldo(500000.0)
                .preferenciaNotificacion("EMAIL")
                .build();
    }

    // ====================================================================
    //  GET /api/clientes
    // ====================================================================
    @Nested
    @DisplayName("GET /api/clientes")
    class ListarClientes {

        @Test
        @DisplayName("Debe retornar lista de clientes con status 200")
        void debeRetornarListaClientes() {
            Cliente cliente2 = Cliente.builder().id("CLI-002").nombre("María López").saldo(300000.0).build();
            when(clienteUseCase.listarClientes(0, 10)).thenReturn(Flux.just(clienteBase, cliente2));

            webTestClient.get()
                    .uri("/api/clientes?page=0&size=10")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Listado de clientes")
                    .jsonPath("$.data.length()").isEqualTo(2)
                    .jsonPath("$.data[0].id").isEqualTo("CLI-001")
                    .jsonPath("$.data[1].id").isEqualTo("CLI-002");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay clientes")
        void debeRetornarListaVacia() {
            when(clienteUseCase.listarClientes(0, 10)).thenReturn(Flux.empty());

            webTestClient.get()
                    .uri("/api/clientes?page=0&size=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("Debe usar valores por defecto page=0 y size=10")
        void debeUsarValoresPorDefecto() {
            when(clienteUseCase.listarClientes(0, 10)).thenReturn(Flux.just(clienteBase));

            webTestClient.get()
                    .uri("/api/clientes")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }
    }

    // ====================================================================
    //  POST /api/clientes
    // ====================================================================
    @Nested
    @DisplayName("POST /api/clientes")
    class CrearCliente {

        @Test
        @DisplayName("Debe crear cliente y retornar status 201")
        void debeCrearClienteYRetornar201() {
            when(clienteUseCase.crearCliente(any())).thenReturn(Mono.just(clienteBase));

            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan Pérez",
                                "preferenciaNotificacion": "EMAIL",
                                "saldo": 500000.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(201)
                    .jsonPath("$.mensaje").isEqualTo("Cliente creado exitosamente")
                    .jsonPath("$.data.nombre").isEqualTo("Juan Pérez");
        }

        @Test
        @DisplayName("Debe convertir preferencia a mayúsculas antes de crear")
        void debeConvertirPreferenciaAMayusculas() {
            Cliente clienteSms = Cliente.builder()
                    .id("CLI-002").nombre("Ana").saldo(500000.0).preferenciaNotificacion("SMS").build();
            when(clienteUseCase.crearCliente(any())).thenReturn(Mono.just(clienteSms));

            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Ana",
                                "preferenciaNotificacion": "sms",
                                "saldo": 500000.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.data.preferenciaNotificacion").isEqualTo("SMS");
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el nombre está vacío")
        void debeRetornar400PorNombreVacio() {
            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "",
                                "preferenciaNotificacion": "EMAIL",
                                "saldo": 500000.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(400)
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("nombre"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando la preferencia es inválida")
        void debeRetornar400PorPreferenciaInvalida() {
            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan",
                                "preferenciaNotificacion": "WHATSAPP",
                                "saldo": 500000.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(400);
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el saldo es negativo")
        void debeRetornar400PorSaldoNegativo() {
            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan",
                                "preferenciaNotificacion": "EMAIL",
                                "saldo": -100.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(400);
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el use case lanza BusinessException")
        void debeRetornar400PorBusinessException() {
            when(clienteUseCase.crearCliente(any()))
                    .thenReturn(Mono.error(new BusinessException("Error de validación del negocio")));

            webTestClient.post()
                    .uri("/api/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan",
                                "preferenciaNotificacion": "EMAIL",
                                "saldo": 500000.0
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").isEqualTo("Error de validación del negocio");
        }
    }

    // ====================================================================
    //  PUT /api/clientes/{id}
    // ====================================================================
    @Nested
    @DisplayName("PUT /api/clientes/{id}")
    class EditarCliente {

        @Test
        @DisplayName("Debe editar cliente y retornar status 200")
        void debeEditarClienteYRetornar200() {
            Cliente clienteActualizado = Cliente.builder()
                    .id("CLI-001").nombre("Juan Actualizado").preferenciaNotificacion("SMS").saldo(500000.0).build();

            when(clienteUseCase.editarCliente(eq("CLI-001"), any())).thenReturn(Mono.just(clienteActualizado));

            webTestClient.put()
                    .uri("/api/clientes/CLI-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan Actualizado",
                                "preferenciaNotificacion": "SMS"
                            }
                            """)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Cliente actualizado exitosamente")
                    .jsonPath("$.data.nombre").isEqualTo("Juan Actualizado");
        }

        @Test
        @DisplayName("Debe convertir preferencia a mayúsculas al editar")
        void debeConvertirPreferenciaAlEditar() {
            Cliente clienteActualizado = Cliente.builder()
                    .id("CLI-001").nombre("Juan").preferenciaNotificacion("EMAIL").saldo(500000.0).build();

            when(clienteUseCase.editarCliente(eq("CLI-001"), any())).thenReturn(Mono.just(clienteActualizado));

            webTestClient.put()
                    .uri("/api/clientes/CLI-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan",
                                "preferenciaNotificacion": "email"
                            }
                            """)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.preferenciaNotificacion").isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el cliente no existe")
        void debeRetornar400CuandoClienteNoExiste() {
            when(clienteUseCase.editarCliente(eq("NO-EXISTE"), any()))
                    .thenReturn(Mono.error(new BusinessException("El cliente con ID NO-EXISTE no existe en el sistema")));

            webTestClient.put()
                    .uri("/api/clientes/NO-EXISTE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Juan",
                                "preferenciaNotificacion": "EMAIL"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("NO-EXISTE"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el body es inválido")
        void debeRetornar400PorBodyInvalido() {
            webTestClient.put()
                    .uri("/api/clientes/CLI-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "",
                                "preferenciaNotificacion": "EMAIL"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(400);
        }
    }
}