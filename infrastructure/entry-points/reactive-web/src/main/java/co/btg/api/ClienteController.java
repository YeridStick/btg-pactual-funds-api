package co.btg.api;

import co.btg.api.dto.ApiResponse;
import co.btg.api.dto.ClienteRequest;
import co.btg.model.cliente.Cliente;
import co.btg.usecase.cliente.ClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Servicios para la gestión de clientes")
public class ClienteController {

    private final ClienteUseCase clienteUseCase;

    @GetMapping
    public Mono<ApiResponse<List<Cliente>>> listarClientes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return clienteUseCase.listarClientes(page, size)
                .collectList()
                .map(lista -> ApiResponse.<List<Cliente>>builder().mensaje("Listado de clientes").data(lista).codigo(HttpStatus.OK.value()).build());
    }

    @Operation(summary = "Crear nuevo cliente", description = "Registra un cliente. Si no se envía saldo, se asume $500.000 por defecto.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cliente creado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<Cliente>> crearCliente(@Valid @RequestBody ClienteRequest request) {

        Cliente nuevoCliente = Cliente.builder()
                .id(UUID.randomUUID().toString())
                .nombre(request.getNombre())
                .preferenciaNotificacion(request.getPreferenciaNotificacion().toUpperCase())
                .saldo(request.getSaldo())
                .build();

        return clienteUseCase.crearCliente(nuevoCliente)
                .map(c -> ApiResponse.<Cliente>builder()
                        .mensaje("Cliente creado exitosamente")
                        .data(c)
                        .codigo(HttpStatus.CREATED.value())
                        .build());
    }

    @Operation(summary = "Editar cliente")
    @PutMapping("/{id}")
    public Mono<ApiResponse<Cliente>> editarCliente(
            @PathVariable("id") String id,
            @Valid @RequestBody ClienteRequest request) {

        Cliente datosParaActualizar = Cliente.builder()
                .nombre(request.getNombre())
                .preferenciaNotificacion(request.getPreferenciaNotificacion().toUpperCase())
                .build();

        return clienteUseCase.editarCliente(id, datosParaActualizar)
                .map(c -> ApiResponse.<Cliente>builder()
                        .mensaje("Cliente actualizado exitosamente")
                        .data(c)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }
}