package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.FondoEntity;
import co.btg.model.common.BusinessException;
import co.btg.model.fondo.Fondo;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FondoAdapter – Pruebas unitarias")
class FondoAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient enhancedClient;

    @Mock
    @SuppressWarnings("unchecked")
    private DynamoDbAsyncTable<FondoEntity> asyncTable;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;

    private FondoAdapter fondoAdapter;

    private Fondo fondoBase;
    private FondoEntity entityBase;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("Fondos"), any(TableSchema.class))).thenReturn(asyncTable);

        fondoAdapter = new FondoAdapter(enhancedClient, objectMapper, validator);

        fondoBase = Fondo.builder()
                .id("FPV-001")
                .nombre("FPV_BTG_PACTUAL_RECAUDADORA")
                .montoMinimo(75000.0)
                .categoria("FPV")
                .build();

        entityBase = FondoEntity.builder()
                .id("FPV-001")
                .nombre("FPV_BTG_PACTUAL_RECAUDADORA")
                .montoMinimo(75000.0)
                .categoria("FPV")
                .build();
    }

    // ====================================================================
    //  findById
    // ====================================================================
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Debe retornar el fondo cuando existe")
        void debeRetornarFondoCuandoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(entityBase));
            when(objectMapper.map(entityBase, Fondo.class)).thenReturn(fondoBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());

            StepVerifier.create(fondoAdapter.findById("FPV-001"))
                    .expectNextMatches(f -> "FPV-001".equals(f.getId())
                            && f.getMontoMinimo() == 75000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar vacío cuando no existe")
        void debeRetornarVacioCuandoNoExiste() {
            when(asyncTable.getItem(any(Key.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(fondoAdapter.findById("NO-EXISTE"))
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
        @DisplayName("Debe crear fondo con validación exitosa")
        void debeCrearFondo() {
            when(objectMapper.map(fondoBase, FondoEntity.class)).thenReturn(entityBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());
            when(asyncTable.putItem(any(FondoEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(fondoAdapter.crearFondo(fondoBase))
                    .expectNextMatches(f -> "FPV_BTG_PACTUAL_RECAUDADORA".equals(f.getNombre()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe rechazar fondo con datos inválidos")
        void debeRechazarFondoInvalido() {
            when(objectMapper.map(fondoBase, FondoEntity.class)).thenReturn(entityBase);

            @SuppressWarnings("unchecked")
            ConstraintViolation<FondoEntity> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("El nombre del fondo no puede estar vacío");
            when(validator.validate(entityBase)).thenReturn(Set.of(violation));

            StepVerifier.create(fondoAdapter.crearFondo(fondoBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("El nombre del fondo no puede estar vacío"))
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
        @DisplayName("Debe actualizar fondo con validación exitosa")
        void debeActualizarFondo() {
            when(objectMapper.map(fondoBase, FondoEntity.class)).thenReturn(entityBase);
            when(validator.validate(entityBase)).thenReturn(Collections.emptySet());
            when(asyncTable.putItem(any(FondoEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(fondoAdapter.editarFondo(fondoBase))
                    .expectNextMatches(f -> f.getId().equals("FPV-001"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe rechazar con monto mínimo inválido")
        void debeRechazarConMontoInvalido() {
            when(objectMapper.map(fondoBase, FondoEntity.class)).thenReturn(entityBase);

            @SuppressWarnings("unchecked")
            ConstraintViolation<FondoEntity> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("El monto mínimo debe ser mayor a cero");
            when(validator.validate(entityBase)).thenReturn(Set.of(violation));

            StepVerifier.create(fondoAdapter.editarFondo(fondoBase))
                    .expectErrorMatches(e -> e instanceof BusinessException
                            && e.getMessage().contains("El monto mínimo debe ser mayor a cero"))
                    .verify();
        }
    }

    // ====================================================================
    //  findAllFondo
    // ====================================================================
    @Nested
    @DisplayName("findAllFondo()")
    class FindAllFondo {

        @Test
        @DisplayName("Debe retornar la página correcta cuando hay suficientes registros")
        void debeRetornarPaginaCorrecta() {
            FondoEntity entity1 = FondoEntity.builder().id("F-001").nombre("Fondo A").montoMinimo(50000.0).categoria("FPV").build();
            FondoEntity entity2 = FondoEntity.builder().id("F-002").nombre("Fondo B").montoMinimo(75000.0).categoria("FIC").build();
            Fondo fondo1 = Fondo.builder().id("F-001").nombre("Fondo A").montoMinimo(50000.0).categoria("FPV").build();
            Fondo fondo2 = Fondo.builder().id("F-002").nombre("Fondo B").montoMinimo(75000.0).categoria("FIC").build();

            Page<FondoEntity> page = mock(Page.class);
            when(page.items()).thenReturn(List.of(entity1, entity2));

            PagePublisher<FondoEntity> pagePublisher = mock(PagePublisher.class);
            when(asyncTable.scan(any(java.util.function.Consumer.class))).thenReturn(pagePublisher);
            doAnswer(inv -> {
                org.reactivestreams.Subscriber<Page<FondoEntity>> subscriber = inv.getArgument(0);
                subscriber.onSubscribe(mock(org.reactivestreams.Subscription.class));
                subscriber.onNext(page);
                subscriber.onComplete();
                return null;
            }).when(pagePublisher).subscribe(any(org.reactivestreams.Subscriber.class));

            when(objectMapper.map(entity1, Fondo.class)).thenReturn(fondo1);
            when(objectMapper.map(entity2, Fondo.class)).thenReturn(fondo2);

            StepVerifier.create(fondoAdapter.findAllFondo(0, 2))
                    .expectNext(fondo1)
                    .expectNext(fondo2)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe aplicar skip correcto en página 1")
        void debeAplicarSkipEnPagina1() {
            FondoEntity e1 = FondoEntity.builder().id("F-001").nombre("Fondo A").montoMinimo(50000.0).categoria("FPV").build();
            FondoEntity e2 = FondoEntity.builder().id("F-002").nombre("Fondo B").montoMinimo(75000.0).categoria("FIC").build();
            FondoEntity e3 = FondoEntity.builder().id("F-003").nombre("Fondo C").montoMinimo(100000.0).categoria("FPV").build();
            FondoEntity e4 = FondoEntity.builder().id("F-004").nombre("Fondo D").montoMinimo(200000.0).categoria("FIC").build();
            Fondo f3 = Fondo.builder().id("F-003").nombre("Fondo C").montoMinimo(100000.0).categoria("FPV").build();
            Fondo f4 = Fondo.builder().id("F-004").nombre("Fondo D").montoMinimo(200000.0).categoria("FIC").build();

            Page<FondoEntity> page = mock(Page.class);
            when(page.items()).thenReturn(List.of(e1, e2, e3, e4));

            PagePublisher<FondoEntity> pagePublisher = mock(PagePublisher.class);
            when(asyncTable.scan(any(java.util.function.Consumer.class))).thenReturn(pagePublisher);
            doAnswer(inv -> {
                org.reactivestreams.Subscriber<Page<FondoEntity>> subscriber = inv.getArgument(0);
                subscriber.onSubscribe(mock(org.reactivestreams.Subscription.class));
                subscriber.onNext(page);
                subscriber.onComplete();
                return null;
            }).when(pagePublisher).subscribe(any(org.reactivestreams.Subscriber.class));

            when(objectMapper.map(e3, Fondo.class)).thenReturn(f3);
            when(objectMapper.map(e4, Fondo.class)).thenReturn(f4);

            StepVerifier.create(fondoAdapter.findAllFondo(1, 2))
                    .expectNext(f3)
                    .expectNext(f4)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay registros")
        void debeRetornarListaVaciaSinRegistros() {
            Page<FondoEntity> page = mock(Page.class);
            when(page.items()).thenReturn(Collections.emptyList());

            PagePublisher<FondoEntity> pagePublisher = mock(PagePublisher.class);
            when(asyncTable.scan(any(java.util.function.Consumer.class))).thenReturn(pagePublisher);
            doAnswer(inv -> {
                org.reactivestreams.Subscriber<Page<FondoEntity>> subscriber = inv.getArgument(0);
                subscriber.onSubscribe(mock(org.reactivestreams.Subscription.class));
                subscriber.onNext(page);
                subscriber.onComplete();
                return null;
            }).when(pagePublisher).subscribe(any(org.reactivestreams.Subscriber.class));

            StepVerifier.create(fondoAdapter.findAllFondo(0, 5))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si la página pedida supera los registros disponibles")
        void debeRetornarVacioCuandoPaginaSuperaRegistros() {
            FondoEntity e1 = FondoEntity.builder().id("F-001").nombre("Fondo A").montoMinimo(50000.0).categoria("FPV").build();

            Page<FondoEntity> page = mock(Page.class);
            when(page.items()).thenReturn(List.of(e1));

            PagePublisher<FondoEntity> pagePublisher = mock(PagePublisher.class);
            when(asyncTable.scan(any(java.util.function.Consumer.class))).thenReturn(pagePublisher);
            doAnswer(inv -> {
                org.reactivestreams.Subscriber<Page<FondoEntity>> subscriber = inv.getArgument(0);
                subscriber.onSubscribe(mock(org.reactivestreams.Subscription.class));
                subscriber.onNext(page);
                subscriber.onComplete();
                return null;
            }).when(pagePublisher).subscribe(any(org.reactivestreams.Subscriber.class));

            StepVerifier.create(fondoAdapter.findAllFondo(1, 2))
                    .verifyComplete();
        }
    }
}



