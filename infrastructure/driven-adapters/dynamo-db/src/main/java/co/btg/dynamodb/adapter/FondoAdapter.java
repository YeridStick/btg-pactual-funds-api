package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.FondoEntity;
import co.btg.dynamodb.helper.TemplateAdapterOperations;
import co.btg.model.common.BusinessException;
import co.btg.model.fondo.Fondo;
import co.btg.model.fondo.gateways.FondoRepository;
import jakarta.validation.Validator;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class FondoAdapter extends TemplateAdapterOperations<Fondo, String, FondoEntity> implements FondoRepository {

    private final DynamoDbAsyncTable<FondoEntity> tableInstance;
    private final Validator validator;

    public FondoAdapter(DynamoDbEnhancedAsyncClient connectionFactory, ObjectMapper mapper, Validator validator) {
        super(connectionFactory, mapper, d -> validateAndMapToDomain(d, mapper, validator), "Fondos");
        this.tableInstance = connectionFactory.table("Fondos", TableSchema.fromBean(FondoEntity.class));
        this.validator = validator;
    }

    @Override
    public Mono<Fondo> findById(String id) {
        return this.getById(id);
    }

    @Override
    public Flux<Fondo> findAllFondo(int page, int size) {
        int totalToRead = (page + 1) * size;

        return Flux.from(tableInstance.scan(s -> s.limit(totalToRead)))
                .flatMapIterable(p -> p.items()) // Abrimos las páginas que vengan de la DB
                .skip((long) page * size)// Saltamos los registros de páginas previas
                .take(size)                      // Cortamos el flujo en el tamaño exacto
                .map(entity -> mapper.map(entity, Fondo.class));
    }

    @Override
    public Mono<Fondo> crearFondo(Fondo fondo) {
        FondoEntity entity = mapper.map(fondo, FondoEntity.class);
        var violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            return Mono.error(new BusinessException("Error de validación: " + violations.iterator().next().getMessage()));
        }
        return this.save(fondo);
    }

    @Override
    public Mono<Fondo> editarFondo(Fondo fondo) {
        FondoEntity entity = mapper.map(fondo, FondoEntity.class);
        var violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            return Mono.error(new BusinessException("Error de validación: " + violations.iterator().next().getMessage()));
        }
        return this.save(fondo);
    }

    private static Fondo validateAndMapToDomain(FondoEntity entity, ObjectMapper mapper, Validator validator) {
        return Optional.ofNullable(entity)
                .filter(e -> validator.validate(e).isEmpty())
                .map(e -> mapper.map(e, Fondo.class))
                .orElseThrow(() -> new BusinessException("Datos de fondo inválidos en la base de datos"));
    }
}