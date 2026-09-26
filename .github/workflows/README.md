# MiniEventos

Plugin Paper (Spigot API) para o servidor Semi Anarquia que implementa o
sistema de mini eventos automaticos descrito na especificacao: inscricoes
via `/evento`, sorteio entre os modos **Survival** e **PvP com Kits**, e
tudo o que precisa acontecer para isso ser seguro (inventarios, comandos
bloqueados, desconexao, morte/respawn, recompensa).

## Decisao de arquitetura (leia antes de instalar)

A especificacao fala em "servidor exclusivo de eventos" mas tambem em
"mundo exclusivo/separado". Este plugin implementa a partida em um
**mundo dedicado dentro do mesmo servidor** (nao um segundo processo de
servidor via BungeeCord/Velocity). Essa e, de longe, a forma padrao e
confiavel de fazer isso: evita ter que sincronizar inventario entre dois
servidores via proxy/banco de dados, que e muito mais complexo e fragil.
Se o seu plano era mesmo ter uma maquina/processo fisicamente separada,
me avise e eu adapto (mas normalmente nao e necessario para o que foi
descrito).

## Requisitos

- Servidor **Paper 1.20.x** (Java 17+). Deve funcionar em forks
  compativeis (Purpur etc.), mas foi escrito contra a API do Paper.
- **Geyser + Floodgate** instalados no seu servidor/proxy para os
  jogadores Bedrock. O plugin **nao precisa saber disso**: para o Bukkit,
  um jogador Bedrock conectado via Geyser/Floodgate e um `Player` comum,
  entao `/evento`, kits, teleports, etc. funcionam para os dois lados sem
  nenhum codigo especial. So certifique-se de que o Geyser/Floodgate ja
  estao rodando antes de testar com jogadores Bedrock.
- Um mundo dedicado para o evento (voce cria, ex. com Multiverse-Core ou
  copiando uma pasta de mundo e carregando com `/mv import`, ou do jeito
  que preferir - o plugin so precisa que o mundo exista e esteja
  carregado).
- Um plugin de crates que aceite um comando de console para dar chave
  (CMI, Crazycrates, ExcellentCrates, etc.) - so ajustar o comando no
  config.yml.

## Build

Este pacote contem o codigo-fonte completo (Maven). Como o ambiente onde
o codigo foi gerado nao tem acesso a internet, **nao foi possivel
compilar/testar o `.jar` aqui**. Para gerar o jar:

```bash
cd MiniEventos
mvn clean package
```

O arquivo final aparece em `target/MiniEventos.jar`. Recomendo fortemente
testar primeiro em um servidor de teste antes de usar em produção,
já que o código não pôde ser compilado neste ambiente.

## Instalação

1. Coloque `MiniEventos.jar` em `plugins/` e reinicie/reload o servidor.
   Isso vai gerar `plugins/MiniEventos/config.yml`.
2. Crie (ou carregue) o mundo que sera usado exclusivamente pelo evento.
3. Configure tudo com os comandos de admin abaixo (todos usam a sua
   localizacao atual, entao va ate o lugar certo antes de digitar).

### Checklist de configuracao inicial

```
/eventoadmin setmundo <nome_do_mundo_do_evento>
/eventoadmin setspawnprincipal        (fique no spawn do servidor principal)
/eventoadmin addspawn survival        (repita em varios pontos do mundo do evento)
/eventoadmin addspawn survival_arena  (repita nos pontos da arena final "de Bedrock")
/eventoadmin addspawn pvpkits         (repita em varios pontos do mundo do evento)
```

Adicione **varios** pontos de spawn para cada lista (o plugin distribui os
jogadores entre eles para nao empilhar todo mundo no mesmo bloco). Se você
não adicionar nenhum, todos vão para o spawn padrão do mundo do evento —
funciona, mas todo mundo cai empilhado no mesmo lugar.

Depois, edite `plugins/MiniEventos/config.yml` para:
- ajustar os itens do `modo-pvpkits.kit` (o padrao ja vem com um kit
  completo de exemplo, incluindo um kit estilo "Mace" trocando a espada
  se preferir);
- ajustar `recompensa.comando` para o comando exato do seu plugin de
  crates (o padrao e `crates give %player% Eventos 1` - troque pelo
  comando real, ex. do CMI seria algo como `cmi crate give %player% Eventos 1`).

## Comandos

| Comando | Quem pode usar | O que faz |
|---|---|---|
| `/evento` | Qualquer jogador | Entra na rodada de inscricoes atual (so funciona durante a janela de 3 min) |
| `/eventoadmin abrir` | `eventos.admin` | Abre as inscricoes agora, sem esperar a proxima hora |
| `/eventoadmin iniciar [survival\|pvpkits]` | `eventos.admin` | Forca o inicio imediato (usa os inscritos, ou todos os online se ninguem se inscreveu ainda - util para testar) |
| `/eventoadmin encerrar` | `eventos.admin` | Encerra a partida atual na marra e devolve todo mundo |
| `/eventoadmin status` | `eventos.admin` | Mostra estado atual, modo, quantidade de vivos/inscritos, mundo configurado |
| `/eventoadmin reload` | `eventos.admin` | Recarrega o config.yml |
| `/eventoadmin setmundo <nome>` | `eventos.admin` | Define o mundo do evento |
| `/eventoadmin setspawnprincipal` | `eventos.admin` | Define o spawn "de volta" (sua localizacao) |
| `/eventoadmin addspawn <survival\|survival_arena\|pvpkits>` | `eventos.admin` | Adiciona sua localizacao atual a lista de spawns daquele tipo |

## Como cada regra da especificacao foi resolvida

- **Ciclo de 1h / inscricoes de 3 min / `/evento` fechado depois disso**:
  `EventManager` controla um estado (`AGUARDANDO` → `INSCRICOES_ABERTAS` →
  `EM_ANDAMENTO`); o comando `/evento` so aceita inscricao no segundo
  estado, e o estado muda para `EM_ANDAMENTO` no instante exato em que os
  3 minutos acabam, antes mesmo do modo ser sorteado - entao ninguem entra
  na rodada depois disso (Bug 8).
- **Inventario nao pode ir para o evento / deve voltar certinho**:
  `PlayerDataManager` tira uma "foto" completa (inventario, armadura,
  offhand, vida, fome, XP, modo de jogo) antes de limpar o jogador, e
  devolve exatamente isso quando ele sai da partida (por vitoria, morte,
  desconexao ou encerramento forcado). Essa foto tambem e salva em disco
  (`pendentes.yml`) assim que a partida comeca, como rede de seguranca:
  se o servidor cair no meio do evento, ao voltar o plugin devolve os
  itens automaticamente no proximo login de cada jogador.
- **PvP desativado por 10 min, depois liberado**: `SurvivalMode` liga uma
  flag `pvpPermitido`; `PvpListener` cancela dano entre dois
  *participantes do evento* enquanto essa flag estiver falsa. Dano de
  mobs nunca e tocado.
- **Bussola aos 20 min**: uma tarefa repetida a cada segundo recalcula,
  para cada jogador vivo, quem e o jogador vivo mais proximo e aponta a
  bussola para ele.
- **Arena final aos 30 min**: os jogadores vivos sao teleportados para os
  pontos cadastrados com `/eventoadmin addspawn survival_arena`.
- **Kits + borda de 500 blocos encolhendo**: `PvpKitsMode` entrega o kit
  configurado, espera um pequeno preparo (`tempo-preparo-segundos`, padrao
  10s) e so entao ativa o PvP e comeca a encolher a borda do mundo do
  evento (`WorldBorder#setSize`) ate o tamanho final configurado, ao longo
  do tempo configurado.
- **Comandos bloqueados durante a partida, mesmo para OP**: a permissao
  `eventos.admin.bypass` tem `default: false`, entao ela **nao** e dada a
  OPs automaticamente - so quem receber essa permissao explicitamente
  consegue usar comandos estando dentro do evento. Um admin que **nao**
  esteja participando da partida continua podendo usar `/eventoadmin`
  normalmente (o bloqueio so vale para quem esta dentro da partida).
- **Jogador desconecta durante o evento**: `PlayerQuitEvent` restaura o
  inventario e remove o jogador da partida na hora; no proximo login, uma
  checagem extra garante que ele nunca aparece de volta no mundo do
  evento nem recebe o kit de novo.
- **Morte/respawn sem bugs**: na morte, o jogador e removido da lista de
  "vivos" na hora (para a checagem de vencedor e a bussola reagirem
  imediatamente); no respawn, o plugin decide o destino (cama valida, se
  houver, senao o spawn principal) via `PlayerRespawnEvent#setRespawnLocation`,
  e so depois devolve o inventario original - nunca reaparece no mundo do
  evento.
- **Recompensa**: entregue via comando de console configuravel, com log
  de erro caso o comando falhe (nunca falha silenciosamente).

## Limitacoes e pontos de atencao (leia com calma)

- **Nao consegui compilar o projeto neste ambiente** (sem acesso a
  internet para baixar a API do Paper). Revisei o codigo com cuidado, mas
  recomendo testar em um servidor de teste antes de usar em producao,
  especialmente os fluxos de morte/respawn e desconexao, que dependem de
  timing do proprio Minecraft.
- **Reset do mundo do evento**: a especificacao pede isso "caso o plugin
  permita com seguranca" - eu **nao** implementei apagar/recriar
  automaticamente a pasta do mundo, porque isso e arriscado de fazer
  sem saber qual gerenciador de mundos voce usa (Multiverse, etc.) e pode
  corromper dados se feito na hora errada. Se quiser isso automatizado,
  recomendo usar o comando de reset do Multiverse-Core (`/mv regen` ou
  similar) numa rotina agendada externamente, ou me pedir para integrar
  diretamente com o Multiverse.
- **Imunidade de PvP cobre dano direto (espada, flecha, tridente etc.)**
  vindo de outro participante. Ela nao intercepta, por exemplo, uma nuvem
  de efeito de po´cao ou uma explosao de TNT acesa por um jogador. Para
  reforcar isso, considere tambem flags do WorldGuard no mundo do evento
  (ex. `mob-damage` normal, mas regioes especificas se quiser travar TNT).
- **`/eventoadmin iniciar`** para testes ignora o minimo de jogadores e
  pode iniciar uma partida "solo" - ela nao termina sozinha nesse caso
  (ninguem para matar), use `/eventoadmin encerrar` para parar o teste.
- Este projeto assume que os dois mundos (principal e do evento) estao
  no mesmo servidor Paper. Nao ha nenhuma integracao com BungeeCord/Velocity.

## Estrutura do codigo

```
com.semianarquia.minieventos
├── MiniEventosPlugin      (onEnable/onDisable, liga tudo)
├── EventManager           (estado, ciclo de 1h, inscricoes, inicio/fim de partida)
├── ConfigManager          (leitura/escrita do config.yml, spawns, kit)
├── PlayerDataManager       (snapshot de inventario + persistencia de emergencia)
├── PlayerSnapshot          (captura/restaura o estado de um jogador)
├── EventState / GameModeType (enums)
├── commands/
│   ├── EventoCommand       (/evento)
│   └── EventoAdminCommand  (/eventoadmin)
├── listeners/
│   ├── PvpListener             (janela de imunidade)
│   ├── CommandBlockListener    (bloqueio de comandos)
│   ├── QuitJoinListener        (desconexao / recuperacao de emergencia)
│   └── DeathRespawnListener    (eliminacao + respawn correto)
├── modes/
│   ├── GameModeRunner      (interface comum)
│   ├── SurvivalMode        (timers 10/20/30 min)
│   └── PvpKitsMode         (kit + borda encolhendo)
└── util/
    ├── MessageUtil
    └── RewardManager
```
