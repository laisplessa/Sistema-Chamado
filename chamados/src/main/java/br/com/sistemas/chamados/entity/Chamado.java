
package br.com.sistemas.chamados.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "chamados")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataFechamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusChamado status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Tecnico tecnico;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("data ASC, id ASC")
    private List<Comentario> comentarios = new ArrayList<>();

    public List<Comentario> getComentarios() { return comentarios; }

    public Chamado() {}

    public Chamado(
        String titulo,
        String descricao,
        Prioridade prioridade,
        Cliente cliente,
        Categoria categoria
    ) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.cliente = cliente;
        this.categoria = categoria;
        this.status = StatusChamado.ABERTO;
        this.dataAbertura = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Tecnico getTecnico() {
        return tecnico;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    public void setStatus(StatusChamado status) {
        this.status = status;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setTecnico(Tecnico tecnico) {
        this.tecnico = tecnico;
    }
}
