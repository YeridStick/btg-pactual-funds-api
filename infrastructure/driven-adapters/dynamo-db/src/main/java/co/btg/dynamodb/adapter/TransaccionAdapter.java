package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.TransaccionEntity;
import co.btg.dynamodb.helper.TemplateAdapterOperations;
import co.btg.model.transaccion.Transaccion;
import co.btg.model.transaccion.gateways.TransaccionRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class TransaccionAdapter extends TemplateAdapterOperations<Transaccion, String, TransaccionEntity> implements TransaccionRepository {

    private final DynamoDbAsyncTable<TransaccionEntity> tableInstance;

    public TransaccionAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper) {
        super(connectionFactory, mapper, d -> mapper.map(d, Transaccion.class), "Transacciones");
        this.tableInstance = connectionFactory.table("Transacciones", TableSchema.fromBean(TransaccionEntity.class));
    }

    @Override
    public Flux<Transaccion> consultarHistorialPorCliente(String clienteId) {
        DynamoDbAsyncIndex<TransaccionEntity> index = this.tableInstance.index("ClienteIndex");

        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clienteId).build()
        );

        return Flux.from(index.query(r -> r.queryConditional(queryConditional)))
                .flatMapIterable(page -> page.items())
                .map(entity -> mapper.map(entity, Transaccion.class));
    }
}