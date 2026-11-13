# Sistema de Agendamento de Barbearia

Projeto acadêmico em **arquitetura em camadas**, com Java 8+ no backend e HTML/CSS/JavaScript puro no frontend. O servidor HTTP pertence ao JDK. O Maven gerencia a dependência H2 e gera um JAR executável contendo o driver do banco.

## Abrir no IntelliJ

1. Instale um **JDK** (8 ou superior), não apenas JRE.
2. Extraia o pacote e abra a pasta do projeto (a que contém o `pom.xml`) em **File → Open**.
3. Importe como projeto Maven quando solicitado e selecione o JDK em **Project Structure → Project SDK**.
4. Execute `br.com.navalha.Main` pelo botão verde ao lado de `main`.
5. Acesse **http://localhost:8080** no navegador. Não abra o HTML como arquivo: ele precisa da API Java.

Não é preciso instalar Maven globalmente: o IntelliJ pode usar seu Maven integrado. A primeira importação precisa baixar os plugins Maven. Se a porta estiver ocupada, informe `8081` em **Run → Edit Configurations → Program arguments**.

No PowerShell, dentro da pasta do projeto, execute `./iniciar.cmd`. Ele compila e inicia o sistema e o console do banco. É necessário um JDK 8+; a primeira execução precisa de internet para baixar Maven e dependências. Não precisa instalar Maven globalmente.

Alternativa: `./mvnw.cmd package`, seguido de `java -jar target/barbearia-1.0.0.jar`. No Linux/macOS use `sh mvnw package`.

## Banco H2 e console

O banco é iniciado junto com `Main`. Abra **http://localhost:8082** e preencha:

- Driver Class: `org.h2.Driver`
- JDBC URL: `jdbc:h2:file:./data/barbearia` (substitua o exemplo `jdbc:h2:~/test` do console)
- User Name: `sa`
- Password: deixe vazio na configuração local padrão.

Clique em **Connect** e execute `SELECT * FROM agendamento ORDER BY inicio;`.

O arquivo fica em `data/barbearia.mv.db`, relativo à pasta de execução. Use a raiz do projeto como **Working directory** no IntelliJ para sempre abrir o mesmo banco. Reiniciar preserva os dados. O `.gitignore` exclui os arquivos do banco: cada integrante tem seu banco local. Compartilhar o código pelo Git não compartilha agendamentos.

A tabela e o índice são criados por `src/main/resources/db/schema.sql`, sem apagar dados existentes. Para backup, encerre a aplicação antes de copiar a pasta `data`. `BARBEARIA_DB_PASSWORD` permite configurar uma senha; depois de criar o banco, use a mesma senha nas próximas execuções. Não grave senhas no Git.

Portas: sistema 8080; console H2 8082. Se necessário, execute `java -Dbarbearia.console.port=8083 -jar target/barbearia-1.0.0.jar 8081`. Pare o programa anterior antes de iniciar a nova versão.

O console é uma ferramenta administrativa local: alterações SQL diretas não passam pelas regras do serviço. Faça os agendamentos pela interface do sistema. Não há migração automática de dados que estavam somente na memória da versão anterior.

## Camadas e dependências

```text
Navegador (resources/web)
        │ HTTP
presentation/http/AgendamentoController
        │ chama casos de uso
application/service/AgendamentoService
        ├── domain (entidades e enums)
        └── application/port/AgendamentoRepository (interface)
                         ▲ implementa
infrastructure/persistence/JdbcAgendamentoRepository

Main: cria os objetos e injeta as dependências pelo construtor.
```

| Pacote | Responsabilidade | Não deve fazer |
|---|---|---|
| `domain` | Entidade imutável, valores do catálogo, cálculo do fim e sobreposição | Conhecer HTTP, service, JDBC ou armazenamento |
| `application/dto` | Contrato de entrada do caso de uso | Manipular HTTP ou HTML |
| `application/service` | Validar entradas, expediente, conflito e executar casos de uso | Depender de repositório concreto ou apresentação |
| `application/port` | Definir operações de persistência necessárias à aplicação | Implementar SQL ou regras |
| `infrastructure/persistence` | Armazenar e recuperar entidades | Validar horário, cliente ou negócio |
| `presentation/http` | Receber HTTP, converter entrada e formatar respostas | Acessar repositório ou decidir disponibilidade |
| `resources/web` | Exibir, coletar entradas e chamar API | Decidir regras de negócio ou persistir dados |

A interface pertence à aplicação; o adaptador de dados depende desse contrato. Assim, a aplicação não depende da infraestrutura. O domínio não tem dependências de outras camadas. `Main` é a exceção intencional: é a raiz de composição, responsável por conectar os objetos, sem implementar regras.

### Classes

- `Agendamento`: entidade imutável, com identidade UUID, início, fim e teste de sobreposição.
- `Expediente`: regra única de funcionamento (seg a sáb, 09h às 19h, passo de 30 min). Usada pelo serviço na validação e na listagem de horários.
- `Servico` e `Barbeiro`: catálogos fixos do projeto; preços usam `BigDecimal`.
- `CriarAgendamentoCommand`: dados de entrada independentes do transporte.
- `AgendamentoService`: criar, listar, cancelar e consultar horários. Usa `Clock` injetado para testes determinísticos.
- `AgendamentoRepository`: interface de persistência (`listar`, `listarPorBarbeiroNoDia`, `salvar`, `excluir`). A consulta por barbeiro e dia evita carregar a tabela inteira.
- `JdbcAgendamentoRepository`: implementação SQL com `DataSource`, `PreparedStatement` e fechamento de conexões em `try-with-resources`.
- `DatabaseConfig`: configura H2 e executa o schema inicial.
- `H2Console`: inicia o console web do H2 (fora do `Main`).
- `InMemoryAgendamentoRepository`: alternativa em memória, preservada para testes ou demonstrações.
- `AgendamentoController`: adaptador HTTP, sem acesso à infraestrutura.
- `JsonPresenter`: serialização das respostas, incluindo escape de caracteres.
- `StaticController`: entrega somente os arquivos estáticos permitidos.
- Exceções da aplicação: comunicam regra inválida e registro não encontrado sem depender de status HTTP.

## Regras

Nome de 3 a 80 caracteres; telefone com DDD de 10 ou 11 dígitos; datas válidas e futuras; segunda a sábado, das 09h às 19h; início a cada 30 minutos. Corte e barba duram 30 minutos; combo dura 60. O atendimento precisa terminar até 19h. O mesmo barbeiro não pode ter intervalos sobrepostos. Barbeiros diferentes podem atender simultaneamente. Cancelar libera o horário. O backend sempre revalida ao gravar. Fuso de negócio: `America/Sao_Paulo`.

## API

| Método e rota | Entrada / comportamento |
|---|---|
| GET `/api/catalogo` | Serviços, preços e barbeiros |
| GET `/api/agendamentos?data=2027-01-04` | Lista; omita data para consultar todos |
| GET `/api/horarios?data=2027-01-04&servico=COMBO&barbeiro=ANDRE` | Inícios disponíveis |
| POST `/api/agendamentos` | Formulário `application/x-www-form-urlencoded`: cliente, telefone, servico, barbeiro, data, horario |
| DELETE `/api/agendamentos/{uuid}` | Cancela um registro |

Criação retorna 201; cancelamento 204; entrada/regra inválida 400; registro inexistente 404; falha inesperada 500. Respostas com conteúdo usam JSON.

## Arquitetura com banco

`Main` injeta `JdbcAgendamentoRepository` no serviço através da interface `AgendamentoRepository`. O serviço e o domínio não importam JDBC nem H2. O controller continua chamando somente o serviço. `DatabaseConfig` concentra a configuração específica de H2; o repositório recebe um `javax.sql.DataSource` e contém apenas SQL e mapeamento.

O sistema usa uma instância de serviço, com operações sincronizadas. Isso protege a verificação e gravação de reservas dentro desse processo. Para múltiplas instâncias ou outro banco, será necessário implementar uma transação que bloqueie a agenda do barbeiro durante a consulta e inserção; uma restrição UNIQUE apenas no horário inicial não evita sobreposição de durações diferentes. A conexão H2 em arquivo não usa AUTO_SERVER e mantém o banco restrito a um processo da aplicação.

Para PostgreSQL ou MySQL, configure outro DataSource, adapte o schema e valide a compatibilidade do adaptador JDBC. Não coloque SQL no serviço ou no controller. O catálogo ainda é fixo; quando se tornar editável, guarde preço e duração contratados nos registros históricos.

## Divisão entre três integrantes

1. **Apresentação:** HTML/CSS/JS, controllers HTTP e serialização. Usa somente os casos de uso.
2. **Domínio e aplicação:** entidades, contratos, DTO, regras, exceções e testes. Não importa infraestrutura ou apresentação.
3. **Dados e integração:** adaptador JDBC, schema SQL, configuração do H2, configuração em Main, documentação e Git.

Combinem os contratos antes de trabalhar. Usem branches por tarefa e revisem alterações juntos. Cada integrante pode explicar os limites de sua camada e como ela se conecta às outras.

## Git

Na pasta do projeto: `git init`, `git add .`, `git commit -m "Implementa agenda Java em camadas"`. Depois criem um repositório vazio no GitHub, adicionem o endereço com `git remote add origin ENDERECO` e enviem com `git push -u origin main` após `git branch -M main`. O `.gitignore` exclui arquivos do IntelliJ e compilados. Nenhuma conta remota é necessária para executar localmente.

## Limites

O H2 mantém dados no computador que executa o Java. Não há autenticação de clientes nem compartilhamento do banco entre computadores. Sistema e console são destinados ao uso acadêmico local. Agendamentos antigos que só existiam na memória não são recuperáveis depois do encerramento daquela versão.

## Verificação manual

1. Inicie com `./iniciar.cmd` e crie um agendamento futuro pela interface.
2. No console H2, consulte `SELECT * FROM agendamento;` e confirme o registro.
3. Encerre com Ctrl+C e inicie novamente: o registro deve continuar na agenda.
4. Cancele pelo sistema e consulte novamente: o registro deve ter sido excluído.
5. Tente reservar horários sobrepostos para o mesmo barbeiro: o serviço deve rejeitar.

Referência do banco: https://h2database.com/html/tutorial.html
