package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;
import br.com.sistemas.chamados.entity.Comentario;

public record ComentarioResponse(Long id, String texto, LocalDateTime data, String autor, Long chamadoId) {
    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(), comentario.getTexto(),
            comentario.getData(), comentario.getAutor(), comentario.getChamado().getId());
    }
}
