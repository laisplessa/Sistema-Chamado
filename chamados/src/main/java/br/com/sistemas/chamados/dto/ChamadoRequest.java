
package br.com.sistemas.chamados.dto;

import br.com.sistemas.chamados.entity.Prioridade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150)
    String titulo,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 2000)
    String descricao,

    @NotNull(message = "Prioridade é obrigatória")
    Prioridade prioridade,

    @NotNull(message = "Cliente é obrigatório")
    Long clienteId,

    @NotNull(message = "Categoria é obrigatória")
    Long categoriaId

) {}
