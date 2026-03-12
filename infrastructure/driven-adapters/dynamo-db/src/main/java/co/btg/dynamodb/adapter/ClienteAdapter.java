package co.btg.dynamodb.adapter;

import co.btg.dynamodb.entity.ClienteEntity;
import co.btg.dynamodb.helper.TemplateAdapterOperations;
import co.btg.model.cliente.Cliente;
import co.btg.model.cliente.gateways.ClienteRepository;
import co.btg.model.common.BusinessException;
import jakarta.validation.Validator;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteAdapter extends TemplateAdapterOperations<Cliente, String, ClienteEntity> implements ClienteRepository {

    private final Validator validator;
    private final DynamoDbAsyncTable<ClienteEntity> tableInstance;

    public ClienteAdapter(DynamoDbEnhancedAsyncClient connectionFactory,
                          ObjectMapper mapper,
                          Validator validator) {
        super(connectionFactory, mapper, d -> validateAndMapToDomain(d, mapper, validator), "Clientes");
        this.validator = validator;
        this.tableInstance = connectionFactory.table("Clientes", TableSchema.fromBean(ClienteEntity.class));
    }

    @Override
    public Mono<Cliente> guardarSaldo(Cliente cliente) {
        ClienteEntity entity = mapper.map(cliente, ClienteEntity.class);
        var violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            return Mono.error(new BusinessException("Error de validación: " + violations.iterator().next().getMessage()));
        }

        return this.save(cliente);
    }

    @Override
    public Mono<Cliente> obtenerCliente(String id) {
        return this.getById(id)
                .switchIfEmpty(Mono.error(new BusinessException("El cliente con ID " + id + " no existe en el sistema")));
    }

    @Override
    public Flux<Cliente> listarClientes(int page, int size) {
        int totalToRead = (page + 1) * size;

        return Flux.from(tableInstance.scan(s -> s.limit(totalToRead)))
                .flatMapIterable(p -> p.items())
                .skip((long) page * size)
                .take(size)
                .map(entity -> mapper.map(entity, Cliente.class));
    }

    @Override
    public Mono<Cliente> crearCliente(Cliente cliente) {
        return validarYGuardar(cliente);
    }

    @Override
    public Mono<Cliente> editarCliente(Cliente cliente) {
        return this.getById(cliente.getId())
                .switchIfEmpty(Mono.error(new BusinessException("El cliente no existe")))
                .flatMap(existente -> validarYGuardar(cliente));
    }

    private Mono<Cliente> validarYGuardar(Cliente cliente) {
        ClienteEntity entity = mapper.map(cliente, ClienteEntity.class);
        var violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            return Mono.error(new BusinessException("Error de validación: " +
                    violations.iterator().next().getMessage()));
        }
        return this.save(cliente);
    }

    @Override
    public Mono<Cliente> actualizarSaldoYFondo(String clienteId, Double nuevoSaldo, String fondoId, boolean esSuscripcion) {
        Key key = Key.builder().partitionValue(clienteId).build();
        return Mono.fromFuture(tableInstance.getItem(key))
                .switchIfEmpty(Mono.error(new BusinessException("Cliente no encontrado")))
                .map(entity -> aplicarMovimiento(entity, nuevoSaldo, fondoId, esSuscripcion))
                .flatMap(entity -> Mono.fromFuture(tableInstance.updateItem(entity)))
                .map(e -> mapper.map(e, Cliente.class))
                .onErrorMap(software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException.class,
                        e -> new BusinessException("Concurrencia detectada: El registro fue modificado por otra transacción."));
    }

    /**
     * Funcion encargada de mutar el estado de la entidad de forma segura .
     */
    private ClienteEntity aplicarMovimiento(ClienteEntity entity, Double nuevoSaldo, String fondoId, boolean esSuscripcion) {
        entity.setSaldo(nuevoSaldo);

        List<String> fondos = Optional.ofNullable(entity.getFondosSuscritos())
                .map(java.util.ArrayList::new)
                .orElseGet(java.util.ArrayList::new);

        if (esSuscripcion && !fondos.contains(fondoId)) {
            fondos.add(fondoId);
        } else if (!esSuscripcion) {
            fondos.remove(fondoId);
        }

        entity.setFondosSuscritos(fondos.isEmpty() ? null : fondos);

        return entity;
    }

    private static Cliente validateAndMapToDomain(ClienteEntity entity, ObjectMapper mapper, Validator validator) {
        return Optional.ofNullable(entity)
                .filter(e -> validator.validate(e).isEmpty())
                .map(e -> mapper.map(e, Cliente.class))
                .orElseThrow(() -> new BusinessException("Datos de cliente inconsistentes en la base de datos"));
    }
}