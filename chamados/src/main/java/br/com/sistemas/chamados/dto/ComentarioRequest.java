package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
    @NotBlank(message = "Texto é obrigatório") @Size(max = 2000) String texto,
    @NotBlank(message = "Autor é obrigatório") @Size(max = 100) String autor
) {}
