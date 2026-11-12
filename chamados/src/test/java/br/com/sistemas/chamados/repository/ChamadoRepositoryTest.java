package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.dto.*;
import br.com.sistemas.chamados.entity.*;
import br.com.sistemas.chamados.exception.*;
import br.com.sistemas.chamados.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({ChamadoService.class, ClienteService.class, TecnicoService.class, CategoriaService.class})
class ChamadoRepositoryTest {
    @Autowired ChamadoService service;
    @Autowired ClienteService clienteService;
    @Autowired TecnicoService tecnicoService;
    @Autowired CategoriaService categoriaService;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaRepository categorias;
    @Autowired TecnicoRepository tecnicos;
    @Autowired ChamadoRepository chamados;
    @Autowired ComentarioRepository comentarios;
    @Autowired EntityManager em;
    Cliente cliente;
    Categoria categoria;
    Tecnico tecnico;

    @BeforeEach
    void preparar() {
        cliente = clientes.saveAndFlush(new Cliente("Ana", "ana@teste.com", "123"));
        categoria = categorias.saveAndFlush(new Categoria("Rede", "Conectividade"));
        tecnico = tecnicos.saveAndFlush(new Tecnico("Bia", "bia@teste.com", Perfil.TECNICO));
    }

    ChamadoRequest request(Prioridade prioridade) {
        return new ChamadoRequest("Sem conexão", "Não conecta à rede", prioridade, cliente.getId(), categoria.getId());
    }

    @Test
    void persisteRelacionamentosEstadoInicialEComentariosOrdenados() {
        var criado = service.criar(request(Prioridade.ALTA));
        var primeiro = service.comentar(criado.id(), new ComentarioRequest("Investigando", "Bia"));
        var segundo = service.comentar(criado.id(), new ComentarioRequest("Aguardando retorno", "Ana"));
        em.flush();
        em.clear();
        var salvo = service.buscar(criado.id());
        assertThat(salvo.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(salvo.dataAbertura()).isNotNull();
        assertThat(salvo.dataFechamento()).isNull();
        assertThat(salvo.clienteId()).isEqualTo(cliente.getId());
        assertThat(salvo.categoriaId()).isEqualTo(categoria.getId());
        assertThat(salvo.comentarios()).extracting(ComentarioResponse::id).containsExactly(primeiro.id(), segundo.id());
        assertThat(salvo.comentarios().getFirst().autor()).isEqualTo("Bia");
        assertThat(salvo.comentarios().getFirst().data()).isNotNull();
        service.comentar(criado.id(), new ComentarioRequest("Novo retorno", "Ana"));
        assertThat(service.buscar(criado.id()).comentarios()).hasSize(3);
        service.excluir(criado.id());
        em.flush();
        assertThat(comentarios.findAll()).isEmpty();
    }

    @Test
    void filtraCombinacoesPaginaEOrdena() {
        var alta = service.criar(request(Prioridade.ALTA));
        var baixa = service.criar(request(Prioridade.BAIXA));
        service.alterarStatus(baixa.id(), new AlterarStatusRequest(StatusChamado.AGUARDANDO_CLIENTE));
        var outro = clientes.saveAndFlush(new Cliente("Outro", "outro@teste.com", null));
        service.criar(new ChamadoRequest("Outro", "Descrição", Prioridade.ALTA, outro.getId(), categoria.getId()));
        var pagina = PageRequest.of(0, 1, Sort.by("id").descending());
        for (StatusChamado status : new StatusChamado[]{null, StatusChamado.ABERTO}) {
            for (Prioridade prioridade : new Prioridade[]{null, Prioridade.ALTA}) {
                for (Long clienteId : new Long[]{null, cliente.getId()}) {
                    var resultado = service.listar(status, prioridade, clienteId, PageRequest.of(0, 20));
                    assertThat(resultado.getContent()).allSatisfy(c -> {
                        if (status != null) assertThat(c.status()).isEqualTo(status);
                        if (prioridade != null) assertThat(c.prioridade()).isEqualTo(prioridade);
                        if (clienteId != null) assertThat(c.clienteId()).isEqualTo(clienteId);
                    });
                    assertThat(resultado.getContent()).extracting(ChamadoResponse::id).contains(alta.id());
                }
            }
        }
        var resultado = service.listarPorCliente(cliente.getId(), pagina);
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
        assertThat(resultado.getContent()).extracting(ChamadoResponse::id).containsExactly(baixa.id());
        assertThat(service.listarPorCliente(cliente.getId(), pagina.next()).getContent())
            .extracting(ChamadoResponse::id).containsExactly(alta.id());
        assertThat(service.listar(StatusChamado.FECHADO, null, null, pagina)).isEmpty();
        var semChamados = clientes.saveAndFlush(new Cliente("Sem chamados", "sem@teste.com", null));
        assertThat(service.listarPorCliente(semChamados.getId(), pagina)).isEmpty();
    }

    @Test
    void validaTransicoesEImutabilidadeDeChamadoFechado() {
        var id = service.criar(request(Prioridade.ALTA)).id();
        assertThatThrownBy(() -> service.alterarStatus(id, new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO)))
            .isInstanceOf(RegraNegocioException.class);
        service.atribuirTecnico(id, new AtribuirTecnicoRequest(tecnico.getId()));
        assertThat(service.alterarStatus(id, new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO)).status())
            .isEqualTo(StatusChamado.EM_ATENDIMENTO);
        service.alterarStatus(id, new AlterarStatusRequest(StatusChamado.AGUARDANDO_CLIENTE));
        var fechado = service.alterarStatus(id, new AlterarStatusRequest(StatusChamado.FECHADO));
        em.flush();
        em.clear();
        assertThat(service.buscar(id).dataFechamento()).isCloseTo(fechado.dataFechamento(),
            within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThatThrownBy(() -> service.atualizar(id, request(Prioridade.BAIXA))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.atribuirTecnico(id, new AtribuirTecnicoRequest(tecnico.getId()))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.alterarStatus(id, new AlterarStatusRequest(StatusChamado.ABERTO))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.comentar(id, new ComentarioRequest("Texto", "Ana"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void impedeTrocaDeClienteEValidaReferencias() {
        var id = service.criar(request(Prioridade.ALTA)).id();
        var outro = clientes.saveAndFlush(new Cliente("Outro", "outro@teste.com", null));
        assertThatThrownBy(() -> service.atualizar(id, new ChamadoRequest("Título", "Descrição",
            Prioridade.ALTA, outro.getId(), categoria.getId()))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.criar(new ChamadoRequest("Título", "Descrição",
            Prioridade.ALTA, cliente.getId(), -1L))).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.atribuirTecnico(id, new AtribuirTecnicoRequest(-1L)))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void duplicidadeNosCadastrosENaoEncontrados() {
        assertThat(clientes.existsByEmail("ana@teste.com")).isTrue();
        assertThat(clientes.existsByEmailAndIdNot("ana@teste.com", cliente.getId())).isFalse();
        assertThat(tecnicos.existsByEmail("bia@teste.com")).isTrue();
        assertThat(categorias.existsByNome("Rede")).isTrue();
        assertThatThrownBy(() -> clienteService.criar(new ClienteRequest("Ana", "ana@teste.com", null))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> tecnicoService.criar(new TecnicoRequest("Bia", "bia@teste.com", Perfil.ADMIN))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> categoriaService.criar(new CategoriaRequest("Rede", null))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.buscar(-1L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.listarPorCliente(-1L, PageRequest.of(0, 20))).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.comentar(-1L, new ComentarioRequest("Texto", "Ana"))).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.criar(new ChamadoRequest("Título", "Descrição", Prioridade.ALTA, -1L, categoria.getId())))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
