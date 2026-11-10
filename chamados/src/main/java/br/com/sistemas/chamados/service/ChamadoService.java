
package br.com.sistemas.chamados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;

    public ChamadoService(
        ChamadoRepository repository,
        ClienteRepository clienteRepository,
        CategoriaRepository categoriaRepository
    ) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
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
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(buscarEntity(id));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                "Não é permitido alterar um chamado fechado"
            );
        }

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

        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                "Não é permitido excluir um chamado fechado"
            );
        }

        repository.delete(chamado);
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"
            ));
    }
}
