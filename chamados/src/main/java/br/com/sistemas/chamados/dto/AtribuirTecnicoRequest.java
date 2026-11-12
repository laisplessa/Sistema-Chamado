
package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.NotNull;

public record AtribuirTecnicoRequest(

    @NotNull(message = "O ID do técnico é obrigatório")
    Long tecnicoId

) {}
