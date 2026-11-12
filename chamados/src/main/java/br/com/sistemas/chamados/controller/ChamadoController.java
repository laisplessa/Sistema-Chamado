
package br.com.sistemas.chamados.controller;

import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestParam;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.service.ChamadoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    // CRIAR CHAMADO
    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(
        @Valid @RequestBody ChamadoRequest dto
    ) {
        ChamadoResponse criado = service.criar(dto);

        return ResponseEntity
            .created(URI.create("/chamados/" + criado.id()))
            .body(criado);
    }

    // LISTAR CHAMADOS
    @GetMapping
    public Page<ChamadoResponse> listar(
        @RequestParam(required = false) StatusChamado status,
        @RequestParam(required = false) Prioridade prioridade,
        @RequestParam(required = false) Long clienteId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.listar(status, prioridade, clienteId, pageable);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(@PathVariable Long id,
        @Valid @RequestBody ComentarioRequest dto) {
        return ResponseEntity.status(201).body(service.comentar(id, dto));
    }

    // BUSCAR CHAMADO POR ID
    @GetMapping("/{id}")
    public ChamadoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    // ATUALIZAR CHAMADO
    @PutMapping("/{id}")
    public ChamadoResponse atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ChamadoRequest dto
    ) {
        return service.atualizar(id, dto);
    }

    // EXCLUIR CHAMADO
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);

        return ResponseEntity.noContent().build();
    }

    // ATRIBUIR TÉCNICO AO CHAMADO
    @PatchMapping("/{id}/atribuir")
    public ChamadoResponse atribuirTecnico(
        @PathVariable Long id,
        @Valid @RequestBody AtribuirTecnicoRequest dto
    ) {
        return service.atribuirTecnico(id, dto);
    }

    // ALTERAR STATUS DO CHAMADO
    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(
        @PathVariable Long id,
        @Valid @RequestBody AlterarStatusRequest dto
    ) {
        return service.alterarStatus(id, dto);
    }
}
