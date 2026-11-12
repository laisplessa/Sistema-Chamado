package br.com.sistemas.chamados.controller;

import br.com.sistemas.chamados.dto.*;
import br.com.sistemas.chamados.entity.*;
import br.com.sistemas.chamados.exception.*;
import br.com.sistemas.chamados.service.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ClienteController.class, TecnicoController.class, CategoriaController.class, ChamadoController.class})
class ApiControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ClienteService clientes;
    @MockitoBean TecnicoService tecnicos;
    @MockitoBean CategoriaService categorias;
    @MockitoBean ChamadoService chamados;

    ChamadoResponse chamado() {
        return new ChamadoResponse(1L, "Rede", "Sem conexão", LocalDateTime.now(), null,
            Prioridade.ALTA, StatusChamado.ABERTO, 1L, 1L, null, List.of());
    }

    @Test
    void criaRecursosComLocation() throws Exception {
        given(clientes.criar(any())).willReturn(new ClienteResponse(1L, "Ana", "ana@teste.com", null));
        given(tecnicos.criar(any())).willReturn(new TecnicoResponse(1L, "Bia", "bia@teste.com", Perfil.TECNICO));
        given(categorias.criar(any())).willReturn(new CategoriaResponse(1L, "Rede", null));
        given(chamados.criar(any())).willReturn(chamado());
        String[][] entradas = {
            {"clientes", "{\"nome\":\"Ana\",\"email\":\"ana@teste.com\"}"},
            {"tecnicos", "{\"nome\":\"Bia\",\"email\":\"bia@teste.com\",\"perfil\":\"TECNICO\"}"},
            {"categorias", "{\"nome\":\"Rede\"}"},
            {"chamados", "{\"titulo\":\"Rede\",\"descricao\":\"Sem conexão\",\"prioridade\":\"ALTA\",\"clienteId\":1,\"categoriaId\":1}"}
        };
        for (String[] entrada : entradas) {
            mvc.perform(post("/" + entrada[0]).contentType(MediaType.APPLICATION_JSON).content(entrada[1]))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/" + entrada[0] + "/1"))
                .andExpect(jsonPath("$.id").value(1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/clientes", "/tecnicos", "/categorias", "/chamados", "/chamados/1/comentarios"})
    void rejeitaDadosInvalidos(String rota) throws Exception {
        mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes").isNotEmpty());
        then(chamados).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/clientes", "/tecnicos", "/categorias", "/chamados"})
    void recursoInexistenteRetorna404(String rota) throws Exception {
        given(clientes.buscar(-1L)).willThrow(new RecursoNaoEncontradoException("Não encontrado"));
        given(tecnicos.buscar(-1L)).willThrow(new RecursoNaoEncontradoException("Não encontrado"));
        given(categorias.buscar(-1L)).willThrow(new RecursoNaoEncontradoException("Não encontrado"));
        given(chamados.buscar(-1L)).willThrow(new RecursoNaoEncontradoException("Não encontrado"));
        mvc.perform(get(rota + "/-1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicidadeRetorna409() throws Exception {
        given(clientes.criar(any())).willThrow(new RegraNegocioException("E-mail duplicado"));
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Ana\",\"email\":\"ana@teste.com\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value("E-mail duplicado"));
    }

    @Test
    void filtrosEPaginacaoSaoEncaminhadosAoServico() throws Exception {
        var pagina = PageRequest.of(1, 5, Sort.by("titulo").descending());
        given(chamados.listar(StatusChamado.ABERTO, Prioridade.ALTA, 1L, pagina))
            .willReturn(new PageImpl<>(List.of(chamado()), pagina, 6));
        mvc.perform(get("/chamados").param("status", "ABERTO").param("prioridade", "ALTA")
            .param("clienteId", "1").param("page", "1").param("size", "5").param("sort", "titulo,desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.content[0].id").value(1));
        then(chamados).should().listar(StatusChamado.ABERTO, Prioridade.ALTA, 1L, pagina);
    }

    @Test
    void listaChamadosDoClienteETrataClienteInexistente() throws Exception {
        given(chamados.listarPorCliente(eq(1L), any())).willReturn(new PageImpl<>(List.of(chamado())));
        given(chamados.listarPorCliente(eq(-1L), any())).willThrow(new RecursoNaoEncontradoException("Cliente inexistente"));
        mvc.perform(get("/clientes/1/chamados")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].clienteId").value(1));
        mvc.perform(get("/clientes/-1/chamados")).andExpect(status().isNotFound());
    }

    @Test
    void criaComentarioEListaNoDetalhe() throws Exception {
        var comentario = new ComentarioResponse(2L, "Investigando", LocalDateTime.now(), "Bia", 1L);
        given(chamados.comentar(eq(1L), any())).willReturn(comentario);
        var c = chamado();
        given(chamados.buscar(1L)).willReturn(new ChamadoResponse(c.id(), c.titulo(), c.descricao(), c.dataAbertura(),
            null, c.prioridade(), c.status(), c.clienteId(), c.categoriaId(), null, List.of(comentario)));
        mvc.perform(post("/chamados/1/comentarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"texto\":\"Investigando\",\"autor\":\"Bia\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.chamadoId").value(1));
        mvc.perform(get("/chamados/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.comentarios[0].texto").value("Investigando"));
    }

    @Test
    void rotasDeAcaoValidamDadosEConflitos() throws Exception {
        given(chamados.atribuirTecnico(eq(1L), any())).willReturn(chamado());
        given(chamados.alterarStatus(eq(1L), any())).willThrow(new RegraNegocioException("Chamado fechado"));
        mvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":1}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ABERTO\"}"))
            .andExpect(status().isConflict());
        for (String acao : List.of("atribuir", "status")) {
            mvc.perform(patch("/chamados/1/" + acao).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        }
    }
}
