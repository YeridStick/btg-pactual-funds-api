package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.TransaccionEntity;
import co.btg.model.transaccion.Transaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionAdapter – Pruebas unitarias")
class TransaccionAdapterTest {

    @Mock
    private DynamoDbEnhancedAsyncClient enhancedClient;

    @Mock
    @SuppressWarnings("unchecked")
    private DynamoDbAsyncTable<TransaccionEntity> asyncTable;

    @Mock
    @SuppressWarnings("unchecked")
    private DynamoDbAsyncIndex<TransaccionEntity> asyncIndex;

    @Mock
    private ObjectMapper objectMapper;

    private TransaccionAdapter transaccionAdapter;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("Transacciones"), any(TableSchema.class))).thenReturn(asyncTable);

        transaccionAdapter = new TransaccionAdapter(enhancedClient, objectMapper);
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Debe guardar transacción correctamente")
        void debeGuardarTransaccion() {
            Transaccion tx = Transaccion.builder()
                    .id("TX-001")
                    .clienteId("CLI-001")
                    .fondoId("FPV-001")
                    .tipo("APERTURA")
                    .monto(75000.0)
                    .timestamp(System.currentTimeMillis())
                    .build();

            TransaccionEntity entity = TransaccionEntity.builder()
                    .id("TX-001")
                    .clienteId("CLI-001")
                    .fondoId("FPV-001")
                    .tipo("APERTURA")
                    .monto(75000.0)
                    .build();

            when(objectMapper.map(tx, TransaccionEntity.class)).thenReturn(entity);
            when(asyncTable.putItem(any(TransaccionEntity.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(transaccionAdapter.save(tx))
                    .expectNextMatches(t -> "TX-001".equals(t.getId())
                            && "APERTURA".equals(t.getTipo()))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("consultarHistorialPorCliente()")
    class ConsultarHistorial {

        @Test
        @DisplayName("Debe retornar transacciones del cliente usando GSI ClienteIndex")
        void debeRetornarHistorial() {
            TransaccionEntity e1 = TransaccionEntity.builder()
                    .id("TX-001").clienteId("CLI-001").fondoId("F1")
                    .tipo("APERTURA").monto(75000.0).timestamp(1000L).build();

            TransaccionEntity e2 = TransaccionEntity.builder()
                    .id("TX-002").clienteId("CLI-001").fondoId("F2")
                    .tipo("CANCELACION").monto(50000.0).timestamp(2000L).build();

            Transaccion t1 = Transaccion.builder()
                    .id("TX-001").clienteId("CLI-001").fondoId("F1")
                    .tipo("APERTURA").monto(75000.0).timestamp(1000L).build();

            Transaccion t2 = Transaccion.builder()
                    .id("TX-002").clienteId("CLI-001").fondoId("F2")
                    .tipo("CANCELACION").monto(50000.0).timestamp(2000L).build();

            Page<TransaccionEntity> page = Page.create(List.of(e1, e2));

            when(asyncTable.index("ClienteIndex")).thenReturn(asyncIndex);
            when(asyncIndex.query(any(java.util.function.Consumer.class)))
                    .thenReturn(SdkPublisher.adapt(reactor.core.publisher.Flux.just(page)));

            when(objectMapper.map(e1, Transaccion.class)).thenReturn(t1);
            when(objectMapper.map(e2, Transaccion.class)).thenReturn(t2);

            StepVerifier.create(transaccionAdapter.consultarHistorialPorCliente("CLI-001"))
                    .expectNextMatches(t -> "APERTURA".equals(t.getTipo()) && t.getMonto() == 75000.0)
                    .expectNextMatches(t -> "CANCELACION".equals(t.getTipo()) && t.getMonto() == 50000.0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Debe retornar vacío cuando el cliente no tiene transacciones")
        void debeRetornarVacioCuandoSinTransacciones() {
            Page<TransaccionEntity> emptyPage = Page.create(Collections.emptyList());

            when(asyncTable.index("ClienteIndex")).thenReturn(asyncIndex);
            when(asyncIndex.query(any(java.util.function.Consumer.class)))
                    .thenReturn(SdkPublisher.adapt(reactor.core.publisher.Flux.just(emptyPage)));

            StepVerifier.create(transaccionAdapter.consultarHistorialPorCliente("CLI-SIN-TX"))
                    .verifyComplete();
        }
    }
}
