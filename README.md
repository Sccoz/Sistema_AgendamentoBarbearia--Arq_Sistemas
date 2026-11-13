# Sistema de Agendamento de Barbearia

Projeto acadêmico em **arquitetura em camadas**, com Java 8+ no backend e HTML/CSS/JavaScript puro no frontend. Sem frameworks ou bibliotecas de execução externas. O servidor HTTP pertence ao JDK; o Maven organiza o projeto e gera o JAR.

## Abrir no IntelliJ

1. Instale um **JDK** (8 ou superior), não apenas JRE.
2. Extraia o pacote e abra a pasta `barbearia-java` em **File → Open**.
3. Importe como projeto Maven quando solicitado e selecione o JDK em **Project Structure → Project SDK**.
4. Execute `br.com.navalha.Main` pelo botão verde ao lado de `main`.
5. Acesse **http://localhost:8080** no navegador. Não abra o HTML como arquivo: ele precisa da API Java.

Não é preciso instalar Maven globalmente: o IntelliJ pode usar seu Maven integrado. A primeira importação precisa baixar os plugins Maven. Se a porta estiver ocupada, informe `8081` em **Run → Edit Configurations → Program arguments**.

Terminal com Maven: `mvn package`, seguido de `java -jar target/barbearia-1.0.0.jar`.

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
infrastructure/persistence/InMemoryAgendamentoRepository

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
- `Servico` e `Barbeiro`: catálogos fixos do projeto; preços usam `BigDecimal`.
- `CriarAgendamentoCommand`: dados de entrada independentes do transporte.
- `AgendamentoService`: criar, listar, cancelar e consultar horários. Usa `Clock` injetado para testes determinísticos.
- `AgendamentoRepository`: interface de persistência.
- `InMemoryAgendamentoRepository`: implementação em mapa, com cópias nas consultas.
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

## Evoluir para banco de dados

1. Criar `JdbcAgendamentoRepository implements AgendamentoRepository` dentro de `infrastructure/persistence`.
2. Configurar um `DataSource` e injetá-lo nesse adaptador. Manter credenciais fora do Git.
3. Usar `PreparedStatement`, `try-with-resources` e mapeamento entre linhas e entidades. Preservar UUID e `LocalDateTime` na conversão.
4. Em `Main`, trocar somente a criação do adaptador e fornecer a configuração do banco. Controller e service continuam dependendo dos mesmos contratos.
5. Antes de executar múltiplos servidores, implementar exclusão concorrente no banco com transações/bloqueios ou restrições de intervalo; adaptar o contrato para criação atômica se necessário. O `synchronized` atual protege apenas uma instância do serviço, não processos separados.

Sugestão inicial de tabela: `agendamento(id, cliente, telefone, servico, barbeiro, inicio)`, com PK em `id` e índice em `(barbeiro, inicio)`. Uma restrição UNIQUE em `(barbeiro, inicio)` **não impede sobreposição de durações diferentes**. Ao tornar o catálogo editável, modelar serviço e barbeiro como entidades/tabelas e guardar preço e duração contratados no agendamento histórico.

## Divisão entre três integrantes

1. **Apresentação:** HTML/CSS/JS, controllers HTTP e serialização. Usa somente os casos de uso.
2. **Domínio e aplicação:** entidades, contratos, DTO, regras, exceções e testes. Não importa infraestrutura ou apresentação.
3. **Dados e integração:** adaptador em memória, futura implementação JDBC, configuração em Main, documentação e Git.

Combinem os contratos antes de trabalhar. Usem branches por tarefa e revisem alterações juntos. Cada integrante pode explicar os limites de sua camada e como ela se conecta às outras.

## Git

Na pasta do projeto: `git init`, `git add .`, `git commit -m "Implementa agenda Java em camadas"`. Depois criem um repositório vazio no GitHub, adicionem o endereço com `git remote add origin ENDERECO` e enviem com `git push -u origin main` após `git branch -M main`. O `.gitignore` exclui arquivos do IntelliJ e compilados. Nenhuma conta remota é necessária para executar localmente.

## Limites

Os dados ficam **na memória do processo Java**, compartilhados pelos navegadores que acessam esse servidor, e são perdidos ao encerrar o programa. Não há localStorage. O servidor escuta apenas na máquina local; não há autenticação ou autorização. O projeto é uma base acadêmica e não deve ser exposto na internet com dados reais sem evolução de segurança e persistência.

## Testes

`src/test/java/br/com/navalha/AgendamentoServiceTest.java` contém um executor de testes sem bibliotecas externas. No IntelliJ, execute seu `main`. Ele lança `AssertionError` em falhas, independentemente da opção `-ea`. O Maven compila esse arquivo, mas o Surefire não executa automaticamente esse executor: execute seu main explicitamente.
