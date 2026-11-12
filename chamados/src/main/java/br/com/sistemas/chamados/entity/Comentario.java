package br.com.sistemas.chamados.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comentarios")
public class Comentario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 2000)
    private String texto;
    @Column(nullable = false, updatable = false)
    private LocalDateTime data;
    @Column(nullable = false, length = 100)
    private String autor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    protected Comentario() {}

    public Comentario(String texto, String autor, Chamado chamado) {
        this.texto = texto;
        this.autor = autor;
        this.chamado = chamado;
        this.data = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public LocalDateTime getData() { return data; }
    public String getAutor() { return autor; }
    public Chamado getChamado() { return chamado; }
}
