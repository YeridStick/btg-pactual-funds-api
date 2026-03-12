package co.btg.api;

import co.btg.api.config.GlobalErrorHandler;
import co.btg.model.common.BusinessException;
import co.btg.model.fondo.Fondo;
import co.btg.model.transaccion.Transaccion;
import co.btg.usecase.fondos.FondosUseCase;
import co.btg.usecase.transaccion.TransaccionUseCase;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FondosController – Pruebas unitarias")
class FondosControllerTest {

    @Mock private FondosUseCase fondosUseCase;
    @Mock private TransaccionUseCase transaccionUseCase;

    @InjectMocks
    private FondosController fondosController;

    private WebTestClient webTestClient;

    private Fondo fondoBase;
    private Transaccion txBase;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToController(fondosController)
                .controllerAdvice(new GlobalErrorHandler())
                .build();

        fondoBase = Fondo.builder()
                .id("FPV-001")
                .nombre("FPV_BTG_PACTUAL_RECAUDADORA")
                .montoMinimo(75000.0)
                .categoria("FPV")
                .build();

        txBase = Transaccion.builder()
                .id("TX-001")
                .clienteId("CLI-001")
                .fondoId("FPV-001")
                .monto(75000.0)
                .tipo("APERTURA")
                .timestamp(1000L)
                .build();
    }

    // ====================================================================
    //  POST /api/fondos/suscribir
    // ====================================================================
    @Nested
    @DisplayName("POST /api/fondos/suscribir")
    class Suscribir {

        @Test
        @DisplayName("Debe suscribir cliente a fondo y retornar 200")
        void debeSuscribirYRetornar200() {
            when(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .thenReturn(Mono.just(txBase));

            webTestClient.post()
                    .uri("/api/fondos/suscribir?clienteId=CLI-001&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Suscripción exitosa al fondo")
                    .jsonPath("$.data.tipo").isEqualTo("APERTURA")
                    .jsonPath("$.data.clienteId").isEqualTo("CLI-001")
                    .jsonPath("$.data.fondoId").isEqualTo("FPV-001");
        }

        @Test
        @DisplayName("Debe retornar 400 por saldo insuficiente")
        void debeRetornar400PorSaldoInsuficiente() {
            when(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .thenReturn(Mono.error(new BusinessException("Saldo insuficiente para el fondo FPV_BTG_PACTUAL_RECAUDADORA")));

            webTestClient.post()
                    .uri("/api/fondos/suscribir?clienteId=CLI-001&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(400)
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("Saldo insuficiente"));
        }

        @Test
        @DisplayName("Debe retornar 400 por suscripción duplicada")
        void debeRetornar400PorSuscripcionDuplicada() {
            when(fondosUseCase.suscribirClienteAFondo("CLI-001", "FPV-001"))
                    .thenReturn(Mono.error(new BusinessException("Ya posee una suscripción activa al fondo FPV_BTG_PACTUAL_RECAUDADORA")));

            webTestClient.post()
                    .uri("/api/fondos/suscribir?clienteId=CLI-001&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("suscripción activa"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el cliente no existe")
        void debeRetornar400ClienteNoExiste() {
            when(fondosUseCase.suscribirClienteAFondo("NO-EXISTE", "FPV-001"))
                    .thenReturn(Mono.error(new BusinessException("El cliente con ID NO-EXISTE no existe en el sistema")));

            webTestClient.post()
                    .uri("/api/fondos/suscribir?clienteId=NO-EXISTE&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("NO-EXISTE"));
        }
    }

    // ====================================================================
    //  POST /api/fondos/cancelar
    // ====================================================================
    @Nested
    @DisplayName("POST /api/fondos/cancelar")
    class Cancelar {

        @Test
        @DisplayName("Debe cancelar suscripción y retornar 200")
        void debeCancelarYRetornar200() {
            Transaccion txCancelacion = Transaccion.builder()
                    .id("TX-002").clienteId("CLI-001").fondoId("FPV-001")
                    .monto(75000.0).tipo("CANCELACION").timestamp(2000L)
                    .build();

            when(fondosUseCase.cancelarSuscripcion("CLI-001", "FPV-001"))
                    .thenReturn(Mono.just(txCancelacion));

            webTestClient.post()
                    .uri("/api/fondos/cancelar?clienteId=CLI-001&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Cancelación exitosa")
                    .jsonPath("$.data.tipo").isEqualTo("CANCELACION");
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el cliente no tiene suscripción activa")
        void debeRetornar400SinSuscripcionActiva() {
            when(fondosUseCase.cancelarSuscripcion("CLI-001", "FPV-001"))
                    .thenReturn(Mono.error(new BusinessException("El cliente no tiene una suscripción activa al fondo FPV_BTG_PACTUAL_RECAUDADORA")));

            webTestClient.post()
                    .uri("/api/fondos/cancelar?clienteId=CLI-001&fondoId=FPV-001")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("suscripción activa"));
        }
    }

    // ====================================================================
    //  GET /api/fondos/historial/{clienteId}
    // ====================================================================
    @Nested
    @DisplayName("GET /api/fondos/historial/{clienteId}")
    class ConsultarHistorial {

        @Test
        @DisplayName("Debe retornar historial de transacciones del cliente")
        void debeRetornarHistorial() {
            Transaccion tx2 = Transaccion.builder()
                    .id("TX-002").clienteId("CLI-001").fondoId("FPV-002")
                    .monto(50000.0).tipo("CANCELACION").timestamp(2000L)
                    .build();

            when(transaccionUseCase.obtenerHistorialPorCliente("CLI-001"))
                    .thenReturn(Flux.just(txBase, tx2));

            webTestClient.get()
                    .uri("/api/fondos/historial/CLI-001")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Historial obtenido correctamente")
                    .jsonPath("$.data.length()").isEqualTo(2)
                    .jsonPath("$.data[0].tipo").isEqualTo("APERTURA")
                    .jsonPath("$.data[1].tipo").isEqualTo("CANCELACION");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay transacciones")
        void debeRetornarListaVacia() {
            when(transaccionUseCase.obtenerHistorialPorCliente("CLI-001"))
                    .thenReturn(Flux.empty());

            webTestClient.get()
                    .uri("/api/fondos/historial/CLI-001")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }

    // ====================================================================
    //  GET /api/fondos/all
    // ====================================================================
    @Nested
    @DisplayName("GET /api/fondos/all")
    class ConsultarAll {

        @Test
        @DisplayName("Debe retornar catálogo paginado de fondos")
        void debeRetornarCatalogoPaginado() {
            Fondo fondo2 = Fondo.builder().id("FIC-001").nombre("FIC_BTG").montoMinimo(50000.0).categoria("FIC").build();
            when(fondosUseCase.obtenerFondosDisponibles(0, 10)).thenReturn(Flux.just(fondoBase, fondo2));

            webTestClient.get()
                    .uri("/api/fondos/all?page=0&size=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Catálogo de fondos disponibles")
                    .jsonPath("$.data.length()").isEqualTo(2)
                    .jsonPath("$.data[0].id").isEqualTo("FPV-001");
        }

        @Test
        @DisplayName("Debe usar valores por defecto page=0 y size=10")
        void debeUsarValoresPorDefecto() {
            when(fondosUseCase.obtenerFondosDisponibles(0, 10)).thenReturn(Flux.just(fondoBase));

            webTestClient.get()
                    .uri("/api/fondos/all")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay fondos")
        void debeRetornarListaVacia() {
            when(fondosUseCase.obtenerFondosDisponibles(0, 10)).thenReturn(Flux.empty());

            webTestClient.get()
                    .uri("/api/fondos/all")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }

    // ====================================================================
    //  POST /api/fondos
    // ====================================================================
    @Nested
    @DisplayName("POST /api/fondos")
    class CrearFondo {

        @Test
        @DisplayName("Debe crear fondo y retornar 201")
        void debeCrearFondoYRetornar201() {
            when(fondosUseCase.crearFondo(any())).thenReturn(Mono.just(fondoBase));

            webTestClient.post()
                    .uri("/api/fondos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "FPV_BTG_PACTUAL_RECAUDADORA",
                                "montoMinimo": 75000.0,
                                "categoria": "FPV"
                            }
                            """)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(201)
                    .jsonPath("$.mensaje").isEqualTo("Fondo creado exitosamente")
                    .jsonPath("$.data.id").isEqualTo("FPV-001")
                    .jsonPath("$.data.nombre").isEqualTo("FPV_BTG_PACTUAL_RECAUDADORA");
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el nombre es nulo")
        void debeRetornar400PorNombreNulo() {
            when(fondosUseCase.crearFondo(any()))
                    .thenReturn(Mono.error(new BusinessException("El nombre del fondo es obligatorio")));

            webTestClient.post()
                    .uri("/api/fondos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "montoMinimo": 75000.0,
                                "categoria": "FPV"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("nombre del fondo"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el monto mínimo es inválido")
        void debeRetornar400PorMontoInvalido() {
            when(fondosUseCase.crearFondo(any()))
                    .thenReturn(Mono.error(new BusinessException("El monto mínimo debe ser mayor a cero")));

            webTestClient.post()
                    .uri("/api/fondos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Fondo X",
                                "montoMinimo": 0,
                                "categoria": "FPV"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("monto mínimo"));
        }
    }

    // ====================================================================
    //  PUT /api/fondos/{id}
    // ====================================================================
    @Nested
    @DisplayName("PUT /api/fondos/{id}")
    class EditarFondo {

        @Test
        @DisplayName("Debe editar fondo y retornar 200")
        void debeEditarFondoYRetornar200() {
            Fondo fondoActualizado = Fondo.builder()
                    .id("FPV-001").nombre("Fondo Actualizado").montoMinimo(100000.0).categoria("FIC").build();

            when(fondosUseCase.editarFondo(eq("FPV-001"), any())).thenReturn(Mono.just(fondoActualizado));

            webTestClient.put()
                    .uri("/api/fondos/FPV-001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Fondo Actualizado",
                                "montoMinimo": 100000.0,
                                "categoria": "FIC"
                            }
                            """)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.codigo").isEqualTo(200)
                    .jsonPath("$.mensaje").isEqualTo("Fondo actualizado exitosamente")
                    .jsonPath("$.data.nombre").isEqualTo("Fondo Actualizado")
                    .jsonPath("$.data.montoMinimo").isEqualTo(100000.0);
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el fondo no existe")
        void debeRetornar400FondoNoExiste() {
            when(fondosUseCase.editarFondo(eq("NO-EXISTE"), any()))
                    .thenReturn(Mono.error(new BusinessException("El fondo con ID NO-EXISTE no existe")));

            webTestClient.put()
                    .uri("/api/fondos/NO-EXISTE")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {
                                "nombre": "Fondo X"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.mensaje").value(msg -> msg.toString().contains("NO-EXISTE"));
        }
    }
}