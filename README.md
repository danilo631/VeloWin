# VeloWin

**VeloWin** é um aplicativo Android independente para gerenciamento de jogos Windows e desenvolvimento de uma pilha de compatibilidade sem depender do código do Winlator. A experiência privilegia biblioteca simples, perfis automáticos, diagnóstico integrado e recursos avançados opcionais — inspiração de usabilidade em apps como GameHub, mas com código, marca e UI próprios.

## VeloWin 0.3.5-dev — Android/Bionic + diagnóstico e perfis

**Funcionalidades concretas desta revisão:**

- **Wine Android/Bionic experimental**: busca de versões [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) para **x86-64 ou ARM64EC**, validação do digest SHA-256 publicado nos assets GitHub, download seguro, registro local e instalação de arquivos WCP Zstandard/TAR isolada com controle de paths/symlinks, perfil `profile.json`, ELF e rollback.
- **Linux/glibc separado**: mantém downloads e extração Wine WoW64 de Kron4ek do VeloWin 0.2, sem misturar os ambientes.
- **Teste Wine controlado**: no APK experimental, após preparar o WCP Bionic x86-64, o botão **Testar Wine --version** tenta executar Wine via Box64 por até 9s. É um autoteste, não execução de jogo.
- **Box64 ARM64/Bionic**: novo [workflow experimental](.github/workflows/box64-bionic.yml) compila uma revisão fixa do upstream com Android NDK, inspeciona linker Bionic/ELF e produz APK diagnóstico com o tradutor embutido. No app, há autoteste opcional `box64 -v`. O APK de depuração padrão **não inclui Box64**.
- **Vulkan nativo**: `app/src/main/cpp/native_probe.c` interroga a biblioteca Vulkan Android via JNI para identificar GPU, versão de API e algumas extensões. Vulkan funcionando não significa DXVK ou VKD3D ativo.
- **Controles, áudio e tela**: laboratório de toque/gamepad e áudio por `AudioTrack`, mais **Surface OpenGL ES 2.0** com contador observacional de quadros (não mede FPS de jogos).
- **Perfis por jogo**: Compatibilidade/Equilibrado/Desempenho com configurações Box64 DynaRec documentadas no upstream, persistidas por jogo; escolha automática conservadora do Vulkan do sistema, sem Turnip em Mali.
- **Ambiente privado Bionic**: prepara diretórios seguros `home/tmp/logs/cache/prefixes/mounts`, sem root. Esse workspace **não é rootfs glibc** nem inicialização de Wine.
- **Supervisor de diagnósticos**: coleta limitada de stdout/stderr sem deadlock de pipe, encerramento por timeout, variáveis de ambiente auditáveis, análise de falhas comuns Bionic/ELF/X11/Vulkan.
- **Preflight fail-closed**: relatório detalha Box64, Wine Bionic, Vulkan e pontes de execução ainda ausentes. O VeloWin **não executa EXEs** nesta fase.

## APKs pelo GitHub Actions

- **[VeloWin Android](https://github.com/danilo631/VeloWin/actions/workflows/android.yml)**: testes Java/Python, JNI Vulkan e APK debug padrão, artefato `velowin-v035-debug`.
- **[Experimental Box64 Bionic Integration](https://github.com/danilo631/VeloWin/actions/workflows/box64-bionic.yml)**: compila Box64 upstream para AArch64/Bionic, confere ABI, insere `libvelobox64.so` e produz o artefato `velowin-box64-bionic-experimental`.
- **[Verify Bionic Proton WCP Layout](https://github.com/danilo631/VeloWin/actions/workflows/bionic-wcp-inspect.yml)**: confere checksum de um release GameNative real, tipo Zstandard/TAR, perfil e layouts. Não executa binários desconhecidos.

Baixe o artefato ZIP do workflow concluído, extraia o APK e instale. Para testes Box64, use apenas o build experimental. Não confunda APK compilado ou autoteste de versão com compatibilidade de jogos.

## O que ainda falta para jogar

A execução **de Wine e de jogos Windows ainda não está integrada/testada end-to-end**. Faltam preparar e testar Wineprefix/\`wineboot\`, resolver bibliotecas Android, roteamento do display Windows/X11 para uma Surface Android, áudio Wine → Android, entrada Wine, DXVK/VKD3D-Proton reais, lifecycle de processos e testes físicos Mali/Adreno. O aplicativo não tem acesso a Steam nem distribui jogos.

O pacote Bionic WCP extraído é só um conjunto de arquivos; executar esse Wine sem a pilha correta pode falhar. Os jobs em Ubuntu não substituem testes no celular.

Confira a [auditoria de fontes Winlator, Mobox e GameHub](docs/REFERENCE_IMPLEMENTATIONS.md). Consulte [Bionic detalhado](docs/BIONIC.md), [Arquitetura](docs/ARCHITECTURE.md), [Bases externas e licenças](docs/UPSTREAM.md), [Roadmap](docs/ROADMAP.md) e [Instalador Linux/glibc](docs/WINE_INSTALLER.md).

## Build local

JDK 17, Android SDK 35, Android Gradle Plugin 8.9.2, Gradle 8.11.1, NDK 27.2 e CMake 3.22.1:

```bash
git clone https://github.com/danilo631/VeloWin.git
cd VeloWin
gradle :app:assembleDebug
```

Código original do aplicativo sob licença [MIT](LICENSE). Licenças de componentes externos devem ser respeitadas. O VeloWin não é associado a GameSir, GameHub, Valve, WineHQ, Winlator ou GameNative.
