# Arquitetura

`Main` mantém a fachada exigida pelo bootstrap/JNI e publica um único `ClientContext`. O contexto é dono dos serviços de módulos, settings, clãs, estatísticas, persistência e despacho. A fábrica conclui `setup()` explicitamente depois do construtor; os padrões são capturados nesse momento, antes da carga do arquivo.

O primeiro tick estabelece a thread do cliente. Comandos vindos da UI entram em `ClientThreadExecutor`; o tick drena a fila antes de executar módulos. A thread de render é dona de `WorkspaceState` e não lê entidades Java. Ela recebe snapshots protegidos pela bridge. Callbacks de pacotes permanecem no transporte Netty, com snapshots de módulos e ownership explícito de mensagens/promises.

`SettingCodec` é o contrato entre armazenamento e UI: valida tipos, choices e números finitos; listas/intervalos usam arrays JSON. Keybinds consultam o valor do módulo. `SaveLoad` lê tudo antes de aplicar; suspende autosave durante parsing, substitui coleções de clãs explicitamente e grava em arquivo temporário com rename atômico quando suportado.

## UI

- `theme.hpp`: cores, espaçamento e duração das transições.
- `state.hpp/.cpp`: categoria, seleção, favoritos, busca, escala e preferências.
- `components.hpp/.cpp`: ícones vetoriais, toggle e cartões reutilizáveis.
- `settings.hpp/.cpp`: controles por tipo, captura de atalhos e grupos do inspetor.
- `nebula_gui.cpp`: navegação e composição adaptável do workspace.
- `hud.cpp`: overlays existentes, independentes do menu.

Categoria e seleção não se sobrescrevem. O inspetor anima a ocupação do espaço em 180 ms. Em telas menores ele ocupa a área inferior, preservando a navegação e a grade. Cartões usam regiões distintas para corpo, toggle e favorito; o corpo não captura o clique dos controles.

## Nativo e build

CMake coordena Maven, verificação de JARs, empacotamento e DLL. O Make expõe os comandos de trabalho. Uma dependência explícita serializa a produção Java antes dos consumidores nativos. ImGui e MinHook são compilados separadamente dos fontes próprios.

`ScopedLock` e `ScopedJniEnv` não são copiáveis. Referências globais temporárias de classes são liberadas por `GlobalClassRefs`; os componentes que retêm classes fazem suas próprias referências. A interação existente com processos não foi ampliada. O encerramento do render/JVM ainda depende do ciclo de vida do host; unload/reload ao vivo permanece um caso de validação manual.
