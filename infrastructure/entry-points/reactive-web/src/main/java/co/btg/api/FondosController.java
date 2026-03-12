package co.btg.api;

import co.btg.api.dto.ApiResponse;
import co.btg.model.fondo.Fondo;
import co.btg.model.transaccion.Transaccion;
import co.btg.usecase.fondos.FondosUseCase;
import co.btg.usecase.transaccion.TransaccionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/fondos")
@RequiredArgsConstructor
@Tag(name = "Fondos", description = "Servicios para la gestión de fondos, suscripciones y consultas de historial")
public class FondosController {

    private final FondosUseCase fondosUseCase;
    private final TransaccionUseCase transaccionUseCase;

    @Operation(
            summary = "Suscribir cliente a un fondo",
            description = "Valida el saldo mínimo (75k) y vincula al cliente con el fondo seleccionado."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suscripción procesada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Saldo insuficiente o datos inválidos", content = @Content)
    })
    @PostMapping("/suscribir")
    public Mono<ApiResponse<Transaccion>> suscribir(
            @RequestParam(name = "clienteId") String clienteId,
            @RequestParam(name = "fondoId") String fondoId) {

        return fondosUseCase.suscribirClienteAFondo(clienteId, fondoId)
                .map(tx -> ApiResponse.<Transaccion>builder()
                        .mensaje("Suscripción exitosa al fondo")
                        .data(tx)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }

    @Operation(summary = "Cancelar suscripción", description = "Desvincula al cliente del fondo y retorna el capital invertido.")
    @PostMapping("/cancelar")
    public Mono<ApiResponse<Transaccion>> cancelar(
            @RequestParam(name = "clienteId") String clienteId,
            @RequestParam(name = "fondoId") String fondoId) {

        return fondosUseCase.cancelarSuscripcion(clienteId, fondoId)
                .map(tx -> ApiResponse.<Transaccion>builder()
                        .mensaje("Cancelación exitosa")
                        .data(tx)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }

    @Operation(summary = "Consultar historial de transacciones", description = "Retorna todas las operaciones de apertura y cierre de un cliente.")
    @GetMapping("/historial/{clienteId}")
    public Mono<ApiResponse<List<Transaccion>>> consultarHistorial(
            @PathVariable("clienteId") String clienteId) {

        return transaccionUseCase.obtenerHistorialPorCliente(clienteId)
                .collectList()
                .map(lista -> ApiResponse.<List<Transaccion>>builder()
                        .mensaje("Historial obtenido correctamente")
                        .data(lista)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }

    @Operation(
            summary = "Catálogo de fondos disponibles",
            description = "Lista todos los fondos de inversión con paginación optimizada (skip/take)."
    )
    @GetMapping("/all")
    public Mono<ApiResponse<List<Fondo>>> consultarAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        return fondosUseCase.obtenerFondosDisponibles(page, size)
                .collectList()
                .map(lista -> ApiResponse.<List<Fondo>>builder()
                        .mensaje("Catálogo de fondos disponibles")
                        .data(lista)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }

    @Operation(
            summary = "Crear fondo",
            description = "Registra un nuevo fondo de inversión. Requiere nombre, monto mínimo y categoría."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Fondo creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<Fondo>> crearFondo(@RequestBody Fondo fondo) {
        return fondosUseCase.crearFondo(fondo)
                .map(f -> ApiResponse.<Fondo>builder()
                        .mensaje("Fondo creado exitosamente")
                        .data(f)
                        .codigo(HttpStatus.CREATED.value())
                        .build());
    }

    @Operation(
            summary = "Editar fondo",
            description = "Actualiza nombre, monto mínimo y/o categoría de un fondo existente."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fondo actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Fondo no encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public Mono<ApiResponse<Fondo>> editarFondo(
            @PathVariable("id") String id,
            @RequestBody Fondo fondo) {
        return fondosUseCase.editarFondo(id, fondo)
                .map(f -> ApiResponse.<Fondo>builder()
                        .mensaje("Fondo actualizado exitosamente")
                        .data(f)
                        .codigo(HttpStatus.OK.value())
                        .build());
    }
}
