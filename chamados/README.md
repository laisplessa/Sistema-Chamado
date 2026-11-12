# Sistema de Chamados

O Sistema de Chamados foi desenvolvido para organizar solicitações de suporte e facilitar o acompanhamento dos atendimentos. Ele reúne, em um só lugar, os clientes, os técnicos responsáveis e as informações de cada chamado, permitindo acompanhar uma solicitação desde a abertura até o fechamento.

A proposta é tornar o atendimento mais claro e organizado. Ao registrar um chamado, é possível informar o problema, definir sua prioridade e classificá-lo em uma categoria. Durante o atendimento, a equipe pode atribuir um técnico, atualizar a situação da solicitação e registrar comentários sobre o que está sendo feito.

## O que você encontra no sistema

### Dashboard

O dashboard apresenta uma visão geral dos atendimentos. Ele mostra quantos chamados estão abertos, em atendimento, aguardando o cliente e fechados, além da distribuição entre essas situações e dos cinco chamados abertos mais recentemente.

Os números são calculados a partir dos registros existentes. A tela também oferece atalhos para os cadastros e para a abertura de um novo chamado.

### Clientes

Nesta área, é possível cadastrar as pessoas que solicitam atendimento, informando nome, e-mail e, se necessário, telefone. Também é possível buscar, editar e excluir cadastros, além de consultar os chamados de cada cliente.

O e-mail identifica o cliente no cadastro e não pode ser repetido entre clientes.

### Categorias

As categorias ajudam a organizar os chamados por assunto ou área de atendimento. Por exemplo, uma equipe pode cadastrar categorias como rede, equipamentos e sistemas para classificar suas solicitações.

Cada categoria possui um nome e pode receber uma descrição. A tela permite consultar, buscar, cadastrar, editar e excluir categorias. O sistema verifica se o nome já está cadastrado para evitar duplicidades.

### Técnicos

A área de técnicos reúne as pessoas responsáveis pelos atendimentos. Cada cadastro possui nome, e-mail e um perfil, que pode ser técnico ou administrador. É possível buscar, cadastrar, editar e excluir esses registros.

O e-mail não pode se repetir entre técnicos. Nesta versão, o perfil é apenas uma informação do cadastro: o sistema não possui tela de login nem diferencia permissões de acesso.

### Chamados

A tela de chamados reúne as solicitações registradas. É possível filtrar os resultados por situação, prioridade e cliente, além de ordenar a listagem pelos chamados mais recentes ou mais antigos. Os resultados são divididos em páginas para facilitar a consulta.

Para abrir um chamado, é necessário informar um título, descrever a solicitação, selecionar o cliente e a categoria e definir a prioridade como baixa, média, alta ou crítica. O chamado começa com a situação “Aberto”, e a data de abertura é registrada automaticamente.

No detalhe do chamado, ficam disponíveis a descrição do problema, os dados do atendimento, o técnico responsável e os comentários. Enquanto o chamado não estiver fechado, é possível editar suas informações, atribuir um técnico e alterar sua situação. O cliente informado na abertura não pode ser trocado.

## Como funciona o atendimento

Para começar, cadastre pelo menos um cliente e uma categoria. Com esses registros disponíveis, já é possível abrir uma solicitação. Cadastre também um técnico para que ele possa ser atribuído ao chamado.

O atendimento pode ser acompanhado pelas situações “Aberto”, “Em atendimento”, “Aguardando cliente” e “Fechado”. Para colocar um chamado em atendimento, é necessário atribuir um técnico antes.

Os comentários permitem registrar atualizações, dúvidas e informações sobre a solicitação. Cada comentário contém o texto, o nome de quem escreveu e a data do registro, preenchida automaticamente. Eles aparecem em ordem cronológica no detalhe do chamado.

Ao fechar o chamado, o sistema registra a data de fechamento. A partir desse momento, ele fica disponível somente para consulta: não é possível editar seus dados, trocar o técnico, alterar a situação, adicionar comentários ou excluí-lo.

## Cuidados com os cadastros

O sistema verifica os campos obrigatórios, o formato dos e-mails e os limites de tamanho dos textos. Quando alguma informação está incorreta ou uma operação não é permitida, a interface apresenta uma mensagem para orientar a correção.

As exclusões pedem confirmação. Cadastros de clientes, categorias ou técnicos que estejam vinculados a chamados podem ter sua exclusão impedida para preservar as informações dos atendimentos. Quando um chamado pode ser excluído, seus comentários também são removidos.

## Sobre o projeto

O projeto utiliza Java e Spring Boot no backend, com uma interface construída em HTML, CSS e JavaScript. As telas se comunicam com a API da própria aplicação, utilizando os mesmos dados e respeitando as regras de negócio do servidor.

A interface foi pensada para ser simples de usar, com navegação entre os módulos, identificação visual das prioridades e situações e adaptação para computadores e celulares. O projeto também possui testes automatizados que verificam a persistência dos dados, as respostas da API e as principais regras do atendimento.

Nesta versão, os dados ficam em um banco H2 em memória. Isso significa que os cadastros, chamados e comentários são mantidos enquanto a aplicação está em execução e são perdidos quando ela é encerrada ou reiniciada.
