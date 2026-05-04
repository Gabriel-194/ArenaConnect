# Visão Geral do Sistema CarmaqControl

> Observação: o nome solicitado para o título foi mantido como "CarmaqControl", mas o projeto analisado no workspace se identifica como **ArenaConnect** em `pom.xml`, `application.properties`, frontend e Docker. Este relatório não altera código e registra apenas comportamento evidenciado nos arquivos analisados.

## Resumo Executivo

O sistema parece ser um SaaS para gestão e reserva de arenas esportivas. Ele possui três perfis principais: `SUPERADMIN`, `ADMIN` e `CLIENTE` (`src/main/java/com/example/Domain/RoleEnum.java:3`). O `SUPERADMIN` administra usuários, arenas e visão financeira global; o `ADMIN` gerencia uma arena, suas quadras, agendamentos, contratos mensalistas, dashboards e relatórios; o `CLIENTE` busca arenas, reserva quadras, acompanha reservas e contratos mensalistas.

A arquitetura é composta por um backend Spring Boot com PostgreSQL e multitenancy por schema (`src/main/resources/application.properties:13`), um frontend React/Vite (`arena-connect-frontend/package.json:7`, `arena-connect-frontend/package.json:16`) e integrações externas com Asaas, Google OAuth, SMTP Gmail, ViaCEP e Groq.

## Tecnologias Identificadas

- **Backend:** Java 17 (`pom.xml:30`), Spring Boot (`pom.xml:7`), Spring MVC/Web (`pom.xml:92`), Spring Security (`pom.xml:88`), Spring Data JPA (`pom.xml:38`), JDBC (`pom.xml:80`), Bean Validation (`pom.xml:84`), Lombok e schedulers Spring (`src/main/java/com/example/ArenaConnectApplication.java:6`).
- **Banco:** PostgreSQL (`pom.xml:95`, `src/main/resources/application.properties:3`) com estratégia de multitenancy por schema (`src/main/resources/application.properties:13`).
- **Autenticação:** JWT via cookie HTTP-only chamado `accessToken` (`src/main/java/com/example/Service/JwtService.java:62`, `src/main/java/com/example/Service/JwtService.java:64`) e login Google via Google OAuth (`src/main/java/com/example/Service/AuthService.java:202`).
- **Frontend:** React 19, Vite, React Router, Axios, React OAuth Google, Leaflet/React Leaflet, Recharts e styled-components (`arena-connect-frontend/package.json:13`-`arena-connect-frontend/package.json:21`).
- **Pagamentos:** Asaas sandbox configurado em propriedades (`src/main/resources/application.properties:25`) e serviço dedicado `AsaasService`.
- **E-mail:** Spring Mail com SMTP Gmail (`src/main/resources/application.properties:33`-`src/main/resources/application.properties:38`).
- **IA/Chatbot:** API Groq com modelo `llama-3.3-70b-versatile` (`src/main/java/com/example/Service/GroqService.java:84`).
- **Relatórios:** OpenPDF no backend (`pom.xml:132`) e endpoints de relatório em `ReportController`.
- **Infra local/container:** Docker Compose com serviços `db`, `backend` e `frontend` (`docker-compose.yml:5`, `docker-compose.yml:17`, `docker-compose.yml:39`).

## Estrutura do Projeto

- `src/main/java/com/example`: backend Spring Boot.
- `src/main/java/com/example/Controller`: controllers REST. Principais endpoints aparecem em `AgendamentoController`, `ArenaController`, `AuthController`, `ContratoMensalistaController`, `UserController`, `WebhookController`, `ChatbotController`, `EmailController`, `ReportController`, `QuadraController` e `NotificacaoController`.
- `src/main/java/com/example/Service`: regras de negócio e integrações, incluindo autenticação, arenas, quadras, agendamentos, mensalistas, Asaas, e-mail, IA, notificações e relatórios.
- `src/main/java/com/example/Models`: entidades JPA. As principais são `Users`, `Arena`, `Quadra`, `Agendamentos`, `ContratoMensalista`, `AgendamentoHistorico` e `Notificacao`.
- `src/main/java/com/example/Repository`: repositórios JPA e implementações customizadas por schema.
- `src/main/java/com/example/Multitenancy`: `TenantContext` e `TenantFilter`, responsáveis por resolver o tenant/schema.
- `src/main/java/com/example/Scheduler`: rotinas automáticas para finalizar/cancelar reservas, reconciliar pagamentos e gerar cobranças mensalistas.
- `src/main/resources/sql`: scripts SQL de dados iniciais e template de schema por arena.
- `src/main/resources/db/migration`: migration de índices de performance.
- `arena-connect-frontend/src/Pages`: páginas principais do frontend.
- `arena-connect-frontend/src/Components`: modais, navegação, sidebar, chatbot, autenticação Google e proteção de rotas.
- `arena-connect-frontend/src/utils`: estrutura auxiliar `ArenaBST.js`, usada no frontend para busca local de arenas.
- `.agents/rules/arenaconnect.md`: arquivo de regras existe, mas não contém instruções além de metadados (`.agents/rules/arenaconnect.md:1`).

## Módulos Principais

- **Autenticação e autorização:** `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` e `PrivateRoute`.
- **Usuários e cadastro:** `UserController`, `UserService`, `UserRegistrationDTO`, `PartnerRegistrationDTO`, `ModalUser`, `ModalPartners`, `Register` e `Login`.
- **Arenas e multitenancy:** `ArenaController`, `ArenaService`, `ArenaRepository`, `TenantFilter`, `TenantContext`, `HomeClient` e `ModalArena`.
- **Quadras:** `QuadraController`, `QuadraService`, `QuadraRepositoryImpl`, `Quadras` e `ModalCourts`.
- **Agendamentos/reservas:** `AgendamentoController`, `AgendamentoService`, `AgendamentoRepositoryImpl`, `Agendamentos`, `AgendamentoHistorico`, `Agendamentos.jsx`, `ClientsAgendamentos.jsx` e `ModalBooking.jsx`.
- **Contratos mensalistas:** `ContratoMensalistaController`, `ContratoMensalistaService`, `ContratoMensalistaRepositoryImpl` e `ContratoMensalista`.
- **Pagamentos:** `AsaasService`, `WebhookController`, `PaymentReconciliationScheduler` e campos Asaas nas entidades de arena, usuário, agendamento e contrato.
- **Notificações:** `NotificacaoController`, `NotificacaoService` e `Notificacao`.
- **Dashboards e relatórios:** `Dashboard.jsx`, `SuperAdmin.jsx`, `ReportController`, `ReportService`, DTOs de dashboard e OpenPDF.
- **Chatbot/IA:** `ChatbotController`, `GroqService` e `Chatbot.jsx`.
- **Recuperação de senha:** `EmailController`, `EmailService`, `RateLimitService` e `ForgotPasswordModal.jsx`.

## Fluxos Importantes

### Fluxo de login e sessão

O login por e-mail/senha ocorre em `/api/auth/login` (`src/main/java/com/example/Controller/AuthController.java:27`). O serviço busca o usuário por e-mail, bloqueia usuários inativos, valida senha com `PasswordEncoder` e cria uma sessão JWT em cookie (`src/main/java/com/example/Service/AuthService.java:51`, `src/main/java/com/example/Service/AuthService.java:74`). O token inclui `userId`, `email`, `role`, `nome`, `idArena` e `arenaSchema` quando disponíveis (`src/main/java/com/example/Service/JwtService.java:31`-`src/main/java/com/example/Service/JwtService.java:43`).

O frontend valida a sessão em `PrivateRoute.jsx` chamando `/api/auth/validate` com `withCredentials` (`arena-connect-frontend/src/Components/PrivateRoute.jsx:14`). A resposta define o redirecionamento por perfil: `ADMIN -> /home`, `SUPERADMIN -> /homeSuperAdmin`, `CLIENTE -> /homeClient` (`src/main/java/com/example/Service/AuthService.java:135`).

### Autorização por perfil

O backend usa Spring Security stateless, CSRF desabilitado e filtros JWT + tenant (`src/main/java/com/example/config/SecurityConfig.java:46`, `src/main/java/com/example/config/SecurityConfig.java:49`, `src/main/java/com/example/config/SecurityConfig.java:68`-`src/main/java/com/example/config/SecurityConfig.java:69`). Endpoints de auth, cadastro, webhook, e-mail e chatbot estão públicos (`src/main/java/com/example/config/SecurityConfig.java:55`-`src/main/java/com/example/config/SecurityConfig.java:59`). Contratos mensalistas têm regras explícitas por role (`src/main/java/com/example/config/SecurityConfig.java:60`-`src/main/java/com/example/config/SecurityConfig.java:63`).

No frontend, as rotas administrativas, de cliente e de superadmin passam por `PrivateRoute` (`arena-connect-frontend/src/main.jsx:35`-`arena-connect-frontend/src/main.jsx:43`). Isso é uma proteção de navegação, mas a autorização efetiva depende do backend.

### Fluxo multi-tenant

O projeto usa `public` para dados globais e schemas por arena para dados operacionais. `TenantFilter` ignora endpoints públicos e tenta resolver o schema primeiro pelo header `X-Tenant-ID`, depois pelo cookie `accessToken` (`src/main/java/com/example/Multitenancy/TenantFilter.java:64`, `src/main/java/com/example/Multitenancy/TenantFilter.java:100`-`src/main/java/com/example/Multitenancy/TenantFilter.java:121`). Se a arena estiver inativa, retorna HTTP 402 com mensagem de bloqueio (`src/main/java/com/example/Multitenancy/TenantFilter.java:77`-`src/main/java/com/example/Multitenancy/TenantFilter.java:80`).

No frontend cliente, `ModalBooking` envia `X-Tenant-ID` com o ID da arena selecionada ou do agendamento em edição (`arena-connect-frontend/src/Components/ModalBooking.jsx:36`-`arena-connect-frontend/src/Components/ModalBooking.jsx:38`). Os repositórios customizados executam `SET search_path` ou montam queries com schema validado por regex, como em `AgendamentoRepositoryImpl` e `QuadraRepositoryImpl` (`src/main/java/com/example/Repository/Custom/AgendamentoRepositoryImpl.java:29`, `src/main/java/com/example/Repository/Custom/QuadraRepositoryImpl.java:18`).

### Fluxo de cadastro de parceiro/arena

O cadastro de parceiro chama `UserService.registerPartner` (`src/main/java/com/example/Service/UserService.java:82`). Ele valida dados pessoais e de arena, cria uma arena inicialmente inativa (`src/main/java/com/example/Service/UserService.java:100`), chama `ArenaService.cadastrarArena`, cria o admin com role `ADMIN` e integra com Asaas para customer, wallet e subscription (`src/main/java/com/example/Service/UserService.java:106`, `src/main/java/com/example/Service/UserService.java:117`, `src/main/java/com/example/Service/UserService.java:127`-`src/main/java/com/example/Service/UserService.java:133`).

Ao cadastrar uma arena, o backend gera `schemaName` a partir do nome, salva a arena, cria o schema e aplica `schema-template.sql` (`src/main/java/com/example/Service/ArenaService.java:71`, `src/main/java/com/example/Service/ArenaService.java:86`, `src/main/java/com/example/Service/ArenaService.java:95`). O template cria `quadras`, `agendamentos`, `contratos_mensalistas` e índices por tenant (`src/main/resources/sql/schema-template.sql:6`, `src/main/resources/sql/schema-template.sql:31`, `src/main/resources/sql/schema-template.sql:75`, `src/main/resources/sql/schema-template.sql:92`-`src/main/resources/sql/schema-template.sql:94`).

### Fluxo de busca e reserva de quadra

O cliente busca arenas em `HomeClient.jsx`, usando geolocalização do navegador se disponível e chamando `/api/arena` (`arena-connect-frontend/src/Pages/HomeClient.jsx:31`, `arena-connect-frontend/src/Pages/HomeClient.jsx:81`). O backend pode buscar arenas recentes, por texto ou por distância usando latitude/longitude (`src/main/java/com/example/Repository/ArenaRepository.java:22`, `src/main/java/com/example/Repository/ArenaRepository.java:47`).

Ao selecionar uma arena, `ModalBooking` busca quadras ativas, horários disponíveis, reserva ou assina contrato mensalista (`arena-connect-frontend/src/Components/ModalBooking.jsx:50`, `arena-connect-frontend/src/Components/ModalBooking.jsx:96`, `arena-connect-frontend/src/Components/ModalBooking.jsx:188`, `arena-connect-frontend/src/Components/ModalBooking.jsx:217`). O backend cria agendamento com status `PENDENTE`, define duração de uma hora, valida funcionamento/disponibilidade, cria cobrança Asaas, salva no schema da arena e salva histórico no `public` (`src/main/java/com/example/Service/AgendamentoService.java:105`, `src/main/java/com/example/Service/AgendamentoService.java:112`, `src/main/java/com/example/Service/AgendamentoService.java:118`, `src/main/java/com/example/Service/AgendamentoService.java:120`).

### Fluxo de pagamento

Reservas usam `AsaasService.createPaymentWithSplit`, com split de 90% para a carteira da arena (`src/main/java/com/example/Service/AsaasService.java:158`, `src/main/java/com/example/Service/AsaasService.java:170`). Quando o webhook Asaas recebe `PAYMENT_RECEIVED` ou `PAYMENT_CONFIRMED`, tenta confirmar primeiro uma reserva pelo `paymentId`; se não for reserva e houver `subscription`, ativa a arena por mais um mês (`src/main/java/com/example/Controller/WebhookController.java:36`, `src/main/java/com/example/Controller/WebhookController.java:47`, `src/main/java/com/example/Controller/WebhookController.java:57`-`src/main/java/com/example/Controller/WebhookController.java:58`).

Também há reconciliação automática de pagamentos pendentes e correção de divergências entre histórico e schema do tenant (`src/main/java/com/example/Scheduler/PaymentReconciliationScheduler.java:47`, `src/main/java/com/example/Scheduler/PaymentReconciliationScheduler.java:82`).

### Fluxo de dashboards e relatórios

O dashboard administrativo consome faturamento anual, status de agendamentos, estatísticas de quadras e movimentações recentes (`arena-connect-frontend/src/Pages/Dashboard.jsx:36`, `arena-connect-frontend/src/Pages/Dashboard.jsx:51`, `arena-connect-frontend/src/Pages/Dashboard.jsx:90`, `arena-connect-frontend/src/Pages/Dashboard.jsx:104`). Também baixa relatório em PDF por `/api/relatorio/dashboard` (`arena-connect-frontend/src/Pages/Dashboard.jsx:119`). O relatório superadmin é acessado por `SuperAdmin.jsx` via `/api/relatorio/superadmin` (`arena-connect-frontend/src/Pages/SuperAdmin.jsx:182`).

### Fluxo de chatbot e cadastro conversacional

O chatbot é público em `/api/chatbot/message` (`src/main/java/com/example/Controller/ChatbotController.java:30`) e usa Groq com contexto de sistema declarando fluxos de cadastro de cliente e parceiro (`src/main/java/com/example/Service/GroqService.java:18`). Se a IA retornar `[REGISTER_CLIENT_CMD]`, o controller interpreta o JSON e chama `userService.registrarCliente` (`src/main/java/com/example/Controller/ChatbotController.java:35`-`src/main/java/com/example/Controller/ChatbotController.java:49`). Se retornar `[OPEN_PARTNER_MODAL]`, o frontend abre o modal de parceiro (`arena-connect-frontend/src/Components/Chatbot.jsx:65`).

## Regras de Negócio Identificadas

- Usuários têm roles fixas `SUPERADMIN`, `ADMIN` e `CLIENTE` (`src/main/java/com/example/Domain/RoleEnum.java:3`).
- Usuário inativo não consegue logar (`src/main/java/com/example/Service/AuthService.java:60`).
- Login redireciona por role (`src/main/java/com/example/Service/AuthService.java:135`).
- Arena com `dataExpiracao` vencida é marcada como inativa durante validação de sessão (`src/main/java/com/example/Service/AuthService.java:168`).
- Arena inativa bloqueia requisições de tenant com status 402 e mensagem de pagamento pendente (`src/main/java/com/example/Multitenancy/TenantFilter.java:77`-`src/main/java/com/example/Multitenancy/TenantFilter.java:80`).
- Cadastro de parceiro cria arena inativa e só a ativação por pagamento de assinatura parece liberá-la depois; esta relação está evidenciada por `arena.setAtivo(false)` e webhook de assinatura, mas a UX completa de pagamento inicial é possível/não confirmada (`src/main/java/com/example/Service/UserService.java:100`, `src/main/java/com/example/Controller/WebhookController.java:57`).
- Arena exige nome, CNPJ e CEP; CNPJ precisa ter 14 dígitos e ser único (`src/main/java/com/example/Service/ArenaService.java:142`).
- Cadastro de usuário exige nome, e-mail válido, senha igual à confirmação, CPF com 11 dígitos e e-mail/CPF únicos (`src/main/java/com/example/Service/UserService.java:165`).
- Quadra exige nome, tipo e valor/hora; ao cadastrar, fica ativa (`src/main/java/com/example/Service/QuadraService.java:37`, `src/main/java/com/example/Service/QuadraService.java:53`).
- Horários disponíveis consideram dias de funcionamento, abertura/fechamento da arena e agendamentos não cancelados (`src/main/java/com/example/Service/AgendamentoService.java:63`).
- Reservas têm duração fixa de 1 hora (`src/main/java/com/example/Service/AgendamentoService.java:116`).
- Reservas novas entram como `PENDENTE`, geram cobrança Asaas e notificação (`src/main/java/com/example/Service/AgendamentoService.java:115`, `src/main/java/com/example/Service/AgendamentoService.java:120`).
- Cliente só pode cancelar seus próprios agendamentos; não pode marcar como `FINALIZADO` ou `CONFIRMADO` (`src/main/java/com/example/Service/AgendamentoService.java:300`-`src/main/java/com/example/Service/AgendamentoService.java:311`).
- Reagendamento é bloqueado para status `CANCELADO` ou `FINALIZADO` (`src/main/java/com/example/Service/AgendamentoService.java:149`).
- Webhook de pagamento confirma agendamento no schema da arena e no histórico público (`src/main/java/com/example/Service/AgendamentoService.java:364`).
- Agendamentos vencidos são finalizados automaticamente a cada 10 minutos (`src/main/java/com/example/Scheduler/AgendamentoScheduler.java:41`).
- Reservas pendentes são canceladas automaticamente por falta de pagamento; a regra usa `LocalDateTime.now().plusMinutes(30)`, o que parece indicar intenção de janela de 30 minutos, mas o critério exato deve ser revisado porque usa tempo futuro como limite (`src/main/java/com/example/Scheduler/AgendamentoScheduler.java:104`).
- Contrato mensalista calcula jogos restantes no mês, aplica desconto da arena e gera cobrança Asaas (`src/main/java/com/example/Service/ContratoMensalistaService.java:51`, `src/main/java/com/example/Service/ContratoMensalistaService.java:68`, `src/main/java/com/example/Service/ContratoMensalistaService.java:88`).
- Cobranças mensalistas recorrentes são geradas todo dia 20 às 03:00 para o mês seguinte (`src/main/java/com/example/Scheduler/MensalistaScheduler.java:42`).
- Split Asaas configurado em 90% para a arena; o comentário/cálculo financeiro indica 10% para plataforma (`src/main/java/com/example/Service/AsaasService.java:170`, `src/main/java/com/example/Service/AsaasService.java:208`).

## Banco de Dados

### Estrutura global em `public`

- `users`: entidade `Users`, com `id_user`, `nome`, `role`, `email`, `senha_hash`, `cpf`, `ativo`, `id_arena`, `telefone`, `asaas_customer_id` (`src/main/java/com/example/Models/Users.java:12`-`src/main/java/com/example/Models/Users.java:54`).
- `arenas`: entidade `Arena`, com dados cadastrais, `schema_name`, status, horários, localização, dados Asaas, expiração e desconto mensalista (`src/main/java/com/example/Models/Arena.java:13`, `src/main/java/com/example/Models/Arena.java:34`-`src/main/java/com/example/Models/Arena.java:67`).
- `agendamentos_historico`: histórico global dos agendamentos por usuário/arena, com status, valores e IDs Asaas (`src/main/java/com/example/Models/AgendamentoHistorico.java:10`-`src/main/java/com/example/Models/AgendamentoHistorico.java:54`).
- `notificacoes`: notificações por usuário (`src/main/java/com/example/Models/Notificacao.java:10`-`src/main/java/com/example/Models/Notificacao.java:35`).
- `data.sql` cria um usuário `SUPERADMIN` inicial se ainda não existir (`src/main/resources/sql/data.sql:1`).

### Estrutura por tenant/schema de arena

- `quadras`: quadras com nome, tipo, valor/hora e ativo (`src/main/resources/sql/schema-template.sql:6`).
- `agendamentos`: reservas com quadra, usuário, início/fim, status, valor e dados Asaas (`src/main/resources/sql/schema-template.sql:31`).
- `contratos_mensalistas`: contratos recorrentes por usuário/quadra/dia/horário, valor pactuado, status e dados Asaas (`src/main/resources/sql/schema-template.sql:75`).

### Queries customizadas e índices

- Busca por distância usa fórmula Haversine diretamente no SQL em `ArenaRepository.findNearestWithDistance` (`src/main/java/com/example/Repository/ArenaRepository.java:22`).
- Busca recente/textual de arenas usa `LIKE` por nome/cidade (`src/main/java/com/example/Repository/ArenaRepository.java:47`).
- Repositórios customizados trocam `search_path` para consultar entidades no schema ativo (`src/main/java/com/example/Repository/Custom/AgendamentoRepositoryImpl.java:29`, `src/main/java/com/example/Repository/Custom/ContratoMensalistaRepositoryImpl.java:21`).
- Há índices planejados/criados para histórico global e comentários de índices por tenant (`src/main/resources/db/migration/V999__add_performance_indexes.sql:7`-`src/main/resources/db/migration/V999__add_performance_indexes.sql:29`).

## Pontos Críticos

- **Autenticação por cookie + CSRF desabilitado:** o backend usa cookie `accessToken` HTTP-only e `withCredentials` no frontend, com CSRF desabilitado (`src/main/java/com/example/config/SecurityConfig.java:46`, `src/main/java/com/example/Service/JwtService.java:64`). Risco de segurança deve ser analisado com profundidade; este relatório não confirma exploração.
- **Cookie sem `Secure`:** o JWT é criado com `cookie.setSecure(false)` (`src/main/java/com/example/Service/JwtService.java:65`). Pode ser intencional para ambiente local, mas é sensível em produção.
- **Webhook Asaas público:** `/api/webhook/**` está liberado (`src/main/java/com/example/config/SecurityConfig.java:56`) e `WebhookController` tem campo `asaasApiKey`, mas não foi encontrado uso dele para validar assinatura/token no handler analisado (`src/main/java/com/example/Controller/WebhookController.java:27`). Validação de autenticidade do webhook: não confirmada.
- **E-mail/reset de senha público:** `/api/email/**` está liberado (`src/main/java/com/example/config/SecurityConfig.java:57`); há `RateLimitService`, mas o armazenamento de token é em memória (`src/main/java/com/example/Service/EmailService.java:25`). Em restart, códigos somem; em múltiplas instâncias, comportamento distribuído não confirmado.
- **Multitenancy por `X-Tenant-ID`:** o tenant pode ser resolvido por header enviado pelo frontend (`src/main/java/com/example/Multitenancy/TenantFilter.java:102`). A associação entre usuário autenticado e arena do header deve ser revisada com atenção; validação explícita de vínculo no filtro não foi confirmada.
- **SQL por schema dinâmico:** há validação regex em alguns pontos antes de concatenar schema (`src/main/java/com/example/Repository/Custom/AgendamentoRepositoryImpl.java:29`), mas nem todos os métodos que montam SQL com schema chamam claramente `definirSchema` antes, como algumas queries em `QuadraRepositoryImpl` (`src/main/java/com/example/Repository/Custom/QuadraRepositoryImpl.java:54`). A segurança parece mitigada por origem do schema, mas precisa de revisão.
- **Pagamentos e consistência:** estado de pagamento depende de webhook, reconciliação periódica e histórico global. Isso é funcionalmente sensível porque status divergentes entre tenant e `public.agendamentos_historico` afetam reservas e dashboard (`src/main/java/com/example/Scheduler/PaymentReconciliationScheduler.java:82`).
- **Regra de cancelamento automático:** `cancelarReservasNaoPagas` usa `LocalDateTime.now().plusMinutes(30)` para buscar pendentes (`src/main/java/com/example/Scheduler/AgendamentoScheduler.java:104`). Possível bug de regra, não confirmado sem testes.
- **Chaves/configuração:** `.env` existe no workspace, mas este relatório não cita valores sensíveis. O projeto depende de variáveis como `JWT_SECRET`, `ASAAS_API_KEY`, `GOOGLE_CLIENT_ID`, SMTP e `GROQ_API_KEY` (`src/main/resources/application.properties:16`, `src/main/resources/application.properties:26`, `src/main/resources/application.properties:30`, `src/main/resources/application.properties:35`, `src/main/resources/application.properties:41`).
- **Frontend com URLs hardcoded:** várias telas chamam `http://localhost:8080` diretamente, e o Google Client ID aparece hardcoded em `main.jsx` (`arena-connect-frontend/src/main.jsx:22`). Para deploy, isso provavelmente exigirá configuração por ambiente.
- **Chatbot cadastra cliente a partir de resposta da IA:** o controller interpreta comando `[REGISTER_CLIENT_CMD]` e cadastra usuário (`src/main/java/com/example/Controller/ChatbotController.java:35`-`src/main/java/com/example/Controller/ChatbotController.java:49`). É uma área sensível por depender de output estruturado de LLM.
- **Dados iniciais de superadmin:** `data.sql` insere um superadmin fixo se não existir (`src/main/resources/sql/data.sql:1`). O hash impede conhecer senha pelo código, mas credenciais iniciais devem ser tratadas como área operacional sensível.

## Próximos Passos Recomendados

1. Fazer uma análise específica de segurança em autenticação cookie/JWT, CSRF, webhook Asaas e validação de tenant por header.
2. Revisar o fluxo de cadastro de parceiro do início ao desbloqueio da arena, especialmente pagamento inicial, `arena.ativo=false`, assinatura Asaas e retorno da URL de cobrança.
3. Validar com testes o ciclo completo de reserva: disponibilidade, criação, pagamento, webhook, cancelamento e finalização automática.
4. Revisar regras de mensalista: conflitos de agenda com reservas avulsas, cancelamento, renovação mensal e tratamento de pagamento confirmado.
5. Conferir consistência entre dados do schema tenant e `public.agendamentos_historico`, pois vários fluxos dependem dos dois.
6. Avaliar migrações reais do banco: há `schema-template.sql`, `data.sql` e uma migration de índices, mas não foi encontrada uma migration inicial completa para `public` neste levantamento.
7. Parametrizar URLs do frontend e client IDs por ambiente antes de produção.
8. Auditar uso de logs para evitar exposição de tokens, dados pessoais, payloads de pagamento ou mensagens de erro sensíveis.
9. Verificar cobertura de testes: foi encontrado apenas `ArenaConnectApplicationTests.java`; testes de negócio, integração e segurança não foram confirmados.
10. Revisar performance das buscas e schedulers conforme volume esperado de arenas, reservas, histórico e chamadas à API Asaas.
