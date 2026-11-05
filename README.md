# Nebula Client

Base modular para um client nativo Windows x64 com camada Java 8, voltada à integração com Lunar Client e Minecraft 1.8.9. Inclui a ponte JNI, carregamento do payload, interface Dear ImGui, sistema de configurações e infraestrutura para módulos.

## Compilar

Requisitos: JDK **8**, Python 3, CMake 3.21+, GNU Make e GCC/G++ MinGW-w64 UCRT64. O primeiro build precisa de rede para baixar Maven 3.9.9 e as dependências. O wrapper verifica SHA-512 do Maven; o build verifica SHA-256 dos JARs declarados. `VERSION` define a versão Java e nativa.

No PowerShell, na raiz **deste projeto**:

```powershell
# Configure estas variáveis no PowerShell do Windows x64.
$env:JAVA_HOME = 'C:\caminho\do\jdk8'
$env:CC = 'C:\msys64\ucrt64\bin\gcc.exe'
$env:CXX = 'C:\msys64\ucrt64\bin\g++.exe'
make build
make test
```

Se usar o Make distribuído pelo MSYS2, substitua `make` por `mingw32-make`. `make help` lista os comandos; `make clean` remove apenas `build/` e `dist/` marcados como gerados. `make rebuild` limpa e recompila. O projeto não usa `build.py`, JARs locais de `lib/` nem diretórios da antiga árvore.

Saída: `dist/output.dll`, `dist/client.jar`, `dist/fonts/` e `dist/licenses/`. Preserve as fontes ao mover a distribuição. `build/` e `.cache/` são regeneráveis e ignorados pelo Git.

## Interface

- Navegação por Favoritos, Todos, Jogo, HUD e categorias realmente registradas.
- Busca por nome/descrição, cartões adaptáveis e favoritos persistentes.
- Corpo do cartão seleciona; toggle muda o estado; estrela muda o favorito.
- Inspetor lateral em janelas largas e empilhado em janelas menores; ambas as áreas têm rolagem independente.
- Sliders, inteiros, intervalos, booleanos, listas, cores, combos e atalhos usam os tipos reais enviados pelo Java.
- Ajustes avançados expansíveis e restauração dos padrões capturados antes de carregar a configuração.
- Escala de 0,8x a 1,5x no botão de preferências. A escala e os favoritos ficam em `nebula-ui.ini`, junto à DLL.

Esta distribuição fornece infraestrutura e módulos básicos para extensão. As opções podem ser adicionadas no registro de módulos sem incluir funcionalidades especializadas removidas desta base.

## Estrutura

```text
src/main/java/dev/nebula/client/  serviços, módulos, mappings, contratos JNI
src/main/resources/    mappings canônicos e avisos empacotados
tests/                 verificações Java e nativas
native/include/nebula/ interfaces por componente
native/src/            bridge, JVM, render, UI, core e security
native/third_party/    ImGui e MinHook, com licenças
native/assets/fonts/   fontes e licença OFL
tools/                 build, empacotamento e verificações
docs/                  arquitetura e procedencia
licenses/              licenças de dependências distribuídas
```

## Compatibilidade e validação

As assinaturas JNI, o namespace público Java `dev.nebula.client`, o formato existente do payload e os nomes dos artefatos foram preservados. Não há entrypoint Forge nem manifesto de Java Agent. Gson, LWJGL e Netty são fornecidos pelo jogo; somente ASM 9.5 é incorporado e relocado.

A configuracao fica em %USERPROFILE%/NebulaClient/config.txt. Escritas usam substituicao atomica; recarregar nao salva durante o parsing.

`make test` verifica bytecode ASM, descriptors de pacotes, persistência, concorrência, compatibilidade do payload e UI. Esses testes não substituem a validação manual dentro do Minecraft.

## Licenças

As licenças de ASM, Dear ImGui, MinHook e Sansation estão em `licenses/`; fontes e hashes de procedência estão em `docs/asset-sources.json`. A licença do código próprio deve ser escolhida pelo titular antes de anunciar uma licença para o repositório. Este projeto não atribui uma licença nova ao código do autor.

O repositório não inclui executáveis, logs, backups ou saídas locais de compilação.
