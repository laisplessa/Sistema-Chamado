
package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.repository.ComentarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ComentarioRepository comentarioRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;

    public ChamadoService(
        ChamadoRepository repository,
        ClienteRepository clienteRepository,
        CategoriaRepository categoriaRepository,
        TecnicoRepository tecnicoRepository,
        ComentarioRepository comentarioRepository
    ) {
        this.repository = repository;
        this.comentarioRepository = comentarioRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Cliente cliente = clienteRepository.findById(dto.clienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + dto.clienteId() + " não encontrado"
            ));

        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria " + dto.categoriaId() + " não encontrada"
            ));

        Chamado chamado = new Chamado(
            dto.titulo(),
            dto.descricao(),
            dto.prioridade(),
            cliente,
            categoria
        );

        Chamado salvo = repository.save(chamado);

        return ChamadoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listar() {
        return repository.findAll()
            .stream()
            .map(ChamadoResponse::de)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(StatusChamado status, Prioridade prioridade,
                                       Long clienteId, Pageable pageable) {
        Specification<Chamado> filtro = (root, query, cb) -> cb.conjunction();
        if (status != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (prioridade != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("prioridade"), prioridade));
        if (clienteId != null) filtro = filtro.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        return repository.findAll(filtro, pageable).map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente " + clienteId + " não encontrado");
        }
        return listar(null, null, clienteId, pageable);
    }

    @Transactional
    public ComentarioResponse comentar(Long id, ComentarioRequest dto) {
        Chamado chamado = buscarEntity(id);
        verificarChamadoAberto(chamado);
        Comentario comentario = new Comentario(dto.texto(), dto.autor(), chamado);
        chamado.getComentarios().add(comentario);
        return ComentarioResponse.de(comentarioRepository.save(comentario));
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);

        verificarChamadoAberto(chamado);

        Cliente cliente = clienteRepository.findById(dto.clienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + dto.clienteId() + " não encontrado"
            ));

        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria " + dto.categoriaId() + " não encontrada"
            ));

        if (!chamado.getCliente().getId().equals(cliente.getId())) {
            throw new RegraNegocioException(
                "Não é permitido alterar o cliente do chamado"
            );
        }

        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCategoria(categoria);

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntity(id);

        verificarChamadoAberto(chamado);

        repository.delete(chamado);
    }

    // REGRA 1: ATRIBUIR TÉCNICO
    @Transactional
    public ChamadoResponse atribuirTecnico(
        Long id,
        AtribuirTecnicoRequest dto
    ) {
        Chamado chamado = buscarEntity(id);

        verificarChamadoAberto(chamado);

        Tecnico tecnico = tecnicoRepository.findById(dto.tecnicoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Técnico " + dto.tecnicoId() + " não encontrado"
            ));

        chamado.setTecnico(tecnico);

        return ChamadoResponse.de(chamado);
    }

    // REGRA 2: ALTERAR STATUS
    @Transactional
    public ChamadoResponse alterarStatus(
        Long id,
        AlterarStatusRequest dto
    ) {
        Chamado chamado = buscarEntity(id);

        verificarChamadoAberto(chamado);

        StatusChamado novoStatus = dto.status();

        if (novoStatus == StatusChamado.EM_ATENDIMENTO
            && chamado.getTecnico() == null) {

            throw new RegraNegocioException(
                "É necessário atribuir um técnico antes de iniciar o atendimento"
            );
        }

        if (novoStatus == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }

        chamado.setStatus(novoStatus);

        return ChamadoResponse.de(chamado);
    }

    // MÉTODOS AUXILIARES
    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"
            ));
    }

    private void verificarChamadoAberto(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                "Não é permitido alterar um chamado fechado"
            );
        }
    }
}
