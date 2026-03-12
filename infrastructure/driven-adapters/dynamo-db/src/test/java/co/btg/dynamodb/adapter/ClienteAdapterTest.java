package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.ClienteEntity;
import co.btg.model.cliente.Cliente;
import co.btg.model.common.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteAdapter – Pruebas unitarias")
class ClienteAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient enhancedClient;

    @Mock
    @SuppressWarnings("unchecked")
    private DynamoDbAsyncTable<ClienteEntity> asyncTable;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    private ClienteAdapter clienteAdapter;

    private Cliente clienteBase;
    private ClienteEntity entityBase;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("Clientes"), any(TableSchema.class))).thenReturn(asyncTable);

        clienteAdapter = new ClienteAdapter(enhancedClient, objectMapper, validator);

        clienteBase = Cliente.builder()
                .id("CLI-001")
                .nombre("Yerid Ramirez")
                .saldo(500000.0)
                .preferenciaNotificacion("EMAIL")
                .fondosSuscritos(new ArrayList<>())
                .build();

        entityBase = ClienteEntity.builder()
                .id("CLI-001")
                .nombre("Yerid Ramirez")
                .saldo(500000.0)
                .preferenciaNotificacion("EMAIL")
                .fondosSuscritos(new ArrayList<>())
                .build();
    }

    // ====================================================================
    //  guardarSaldo
    // ====================================================================
    @Nested
    @DisplayName("guardarSaldo()")
    class GuardarSaldo {

        @Test
        @DisplayName("Debe guardar cuando la validación es exitosa")
        void debeGuardarCuandoValidacionExitosa() {
            when(objectMapper.map(clienteBase, ClienteEntity.class)).thenReturn(entityBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());
            when(asyncTable.putItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.guardarSaldo(clienteBase))
                    .expectNextMatches(c -> c.getId().equals("CLI-001") && c.getSaldo() == 500000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe fallar cuando hay violaciones de validación")
        void debeFallarConViolaciones() {
            when(objectMapper.map(clienteBase, ClienteEntity.class)).thenReturn(entityBase);

            @SuppressWarnings("unchecked")
            ConstraintViolation<ClienteEntity> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("El saldo no puede ser negativo");
            when(validator.validate(entityBase)).thenReturn(Set.of(violation));

            StepVerifier.create(clienteAdapter.guardarSaldo(clienteBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("El saldo no puede ser negativo"))
                    .verify();
        }
    }

    // ====================================================================
    //  obtenerCliente
    // ====================================================================
    @Nested
    @DisplayName("obtenerCliente()")
    class ObtenerCliente {

        @Test
        @DisplayName("Debe retornar el cliente cuando existe")
        void debeRetornarClienteCuandoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityBase));
            when(objectMapper.map(entityBase, Cliente.class)).thenReturn(clienteBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());

            StepVerifier.create(clienteAdapter.obtenerCliente("CLI-001"))
                    .expectNextMatches(c -> "CLI-001".equals(c.getId()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe lanzar BusinessException cuando no existe")
        void debeFallarCuandoNoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.obtenerCliente("NO-EXISTE"))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("no existe en el sistema"))
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
        @DisplayName("Debe crear cliente con validación exitosa")
        void debeCrearCliente() {
            when(objectMapper.map(clienteBase, ClienteEntity.class)).thenReturn(entityBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());
            when(asyncTable.putItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.crearCliente(clienteBase))
                    .expectNextMatches(c -> c.getNombre().equals("Yerid Ramirez"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe rechazar cliente con datos inválidos")
        void debeRechazarInvalido() {
            when(objectMapper.map(clienteBase, ClienteEntity.class)).thenReturn(entityBase);

            @SuppressWarnings("unchecked")
            ConstraintViolation<ClienteEntity> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("El Nombre es obligatorio");
            when(validator.validate(entityBase)).thenReturn(Set.of(violation));

            StepVerifier.create(clienteAdapter.crearCliente(clienteBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("El Nombre es obligatorio"))
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
        @DisplayName("Debe actualizar cliente existente")
        void debeActualizarExistente() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityBase));
            when(objectMapper.map(entityBase, Cliente.class)).thenReturn(clienteBase);
            when(validator.validate(any(ClienteEntity.class))).thenReturn(Collections.emptySet());
            when(objectMapper.map(clienteBase, ClienteEntity.class)).thenReturn(entityBase);
            when(asyncTable.putItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.editarCliente(clienteBase))
                    .expectNextMatches(c -> "CLI-001".equals(c.getId()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe fallar si el cliente no existe")
        void debeFallarSiNoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.editarCliente(clienteBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("El cliente no existe"))
                    .verify();
        }
    }

    // ====================================================================
    //  actualizarSaldoYFondo – Suscripción
    // ====================================================================
    @Nested
    @DisplayName("actualizarSaldoYFondo() – Suscripción")
    class ActualizarSaldoSuscripcion {

        @Test
        @DisplayName("Debe debitar saldo y agregar fondo a lista")
        void debeDebitarYAgregarFondo() {
            ClienteEntity entityConSaldo = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(500000.0)
                    .fondosSuscritos(null).build();

            ClienteEntity entityActualizada = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(425000.0)
                    .fondosSuscritos(List.of("FONDO-1")).build();

            Cliente clienteEsperado = Cliente.builder()
                    .id("CLI-001").nombre("Yerid").saldo(425000.0)
                    .fondosSuscritos(List.of("FONDO-1")).build();

            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityConSaldo));
            when(asyncTable.updateItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityActualizada));
            when(objectMapper.map(entityActualizada, Cliente.class)).thenReturn(clienteEsperado);

            StepVerifier.create(clienteAdapter.actualizarSaldoYFondo("CLI-001", 425000.0, "FONDO-1", true))
                    .expectNextMatches(c -> c.getSaldo() == 425000.0
                            && c.getFondosSuscritos().contains("FONDO-1"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("No debe duplicar fondo si ya está suscrito")
        void noDuplicarFondo() {
            ClienteEntity entityYaSuscrito = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(425000.0)
                    .fondosSuscritos(new ArrayList<>(List.of("FONDO-1"))).build();

            ClienteEntity entityResultado = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(350000.0)
                    .fondosSuscritos(List.of("FONDO-1")).build();

            Cliente clienteResultado = Cliente.builder()
                    .id("CLI-001").saldo(350000.0)
                    .fondosSuscritos(List.of("FONDO-1")).build();

            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityYaSuscrito));
            when(asyncTable.updateItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityResultado));
            when(objectMapper.map(entityResultado, Cliente.class)).thenReturn(clienteResultado);

            StepVerifier.create(clienteAdapter.actualizarSaldoYFondo("CLI-001", 350000.0, "FONDO-1", true))
                    .expectNextMatches(c -> c.getFondosSuscritos().size() == 1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe lanzar error si el cliente no existe")
        void debeFallarClienteNoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(clienteAdapter.actualizarSaldoYFondo("NO-EXISTE", 0.0, "F1", true))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("Cliente no encontrado"))
                    .verify();
        }
    }

    // ====================================================================
    //  actualizarSaldoYFondo – Cancelación
    // ====================================================================
    @Nested
    @DisplayName("actualizarSaldoYFondo() – Cancelación")
    class ActualizarSaldoCancelacion {

        @Test
        @DisplayName("Debe reembolsar saldo y remover fondo de la lista")
        void debeReembolsarYRemoverFondo() {
            ClienteEntity entitySuscrita = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(425000.0)
                    .fondosSuscritos(new ArrayList<>(List.of("FONDO-1"))).build();

            ClienteEntity entityDesuscrita = ClienteEntity.builder()
                    .id("CLI-001").nombre("Yerid").saldo(500000.0)
                    .fondosSuscritos(null).build();

            Cliente clienteDesuscrito = Cliente.builder()
                    .id("CLI-001").saldo(500000.0)
                    .fondosSuscritos(null).build();

            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entitySuscrita));
            when(asyncTable.updateItem(any(ClienteEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityDesuscrita));
            when(objectMapper.map(entityDesuscrita, Cliente.class)).thenReturn(clienteDesuscrito);

            StepVerifier.create(clienteAdapter.actualizarSaldoYFondo("CLI-001", 500000.0, "FONDO-1", false))
                    .expectNextMatches(c -> c.getSaldo() == 500000.0
                            && c.getFondosSuscritos() == null)
                    .verifyComplete();
        }
    }

    // ====================================================================
    //  listarClientes
    // ====================================================================
    @Nested
    @DisplayName("listarClientes()")
    class ListarClientes {

        @Test
        @DisplayName("Debe retornar listado paginado (skip/take)")
        @SuppressWarnings("unchecked")
        void debeRetornarPaginado() {
            // 1. Datos de prueba
            ClienteEntity e1 = ClienteEntity.builder().id("C1").nombre("Uno").saldo(100.0).preferenciaNotificacion("EMAIL").build();
            ClienteEntity e2 = ClienteEntity.builder().id("C2").nombre("Dos").saldo(200.0).preferenciaNotificacion("SMS").build();

            Cliente c1 = Cliente.builder().id("C1").nombre("Uno").saldo(100.0).build();
            Cliente c2 = Cliente.builder().id("C2").nombre("Dos").saldo(200.0).build();

            // 2. Crear la página con el tipo explícito
            Page<ClienteEntity> page = Page.create(List.of(e1, e2));

            // 3. SOLUCIÓN AL ERROR DE COMPILACIÓN:
            // Forzamos el tipo del suscriptor para que coincida con Page<ClienteEntity>
            when(asyncTable.scan(any(java.util.function.Consumer.class)))
                    .thenAnswer(inv -> PagePublisher.create((org.reactivestreams.Subscriber<? super Page<ClienteEntity>> s) ->
                            reactor.core.publisher.Flux.just(page).subscribe(s)));

            // 4. Mapeos
            when(objectMapper.map(e1, Cliente.class)).thenReturn(c1);
            when(objectMapper.map(e2, Cliente.class)).thenReturn(c2);

            // 5. Verificación
            StepVerifier.create(clienteAdapter.listarClientes(0, 10))
                    .expectNext(c1)
                    .expectNext(c2)
                    .verifyComplete();
        }
    }
}