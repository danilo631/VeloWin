# VeloWin

**VeloWin** é um aplicativo Android independente para gerenciamento de jogos Windows e desenvolvimento de uma pilha de compatibilidade sem depender do código do Winlator. A experiência privilegia biblioteca simples, perfis automáticos, diagnóstico integrado e recursos avançados opcionais — inspiração de usabilidade em apps como GameHub, mas com código, marca e UI próprios.

## VeloWin 0.7.0-dev — entrar no contêiner Bionic e testar Wine no Android

O APK principal mantém **Box64 Bionic incluído**. Na **Biblioteca**, cada jogo agora oferece **Entrar no contêiner**. Na aba **Runtimes**, existe um console de diagnóstico mesmo sem jogo.

O console implementado de fato executa **Box64 -v**, **Wine --version** e, com Wine Bionic x86_64 instalado e Wineprefix do jogo preparado, permite tentar **wine cmd /c ver**, **wineboot -u** e **run exe** (somente após confirmação). Há cancelamento, saída limitada, histórico local, compartilhamento de log e cópia restrita via SAF de um EXE autorizado pelo usuário. Comandos arbitrários de shell não são aceitos.

O **contêiner é um workspace privado Android/Bionic, não um Linux rootfs nem uma sandbox de kernel**. Wine/EXE podem retornar erros por ausência de X11, DLLs ou áudio. **A compilação do APK e o teste Box64 -v não comprovam que jogos Windows funcionam**; veja [Teste de contêiner 0.7](docs/CONTAINER_TEST_V07.md).

## VeloWin 0.6.0-dev — Box64 integrado ao APK principal, drivers e teste Vulkan WSI

**Novidade no workflow principal:** agora o próprio **VeloWin Android** compila Box64 ARM64/Bionic em um commit upstream fixo, valida a arquitetura e o linker Android e distribui o APK padrão **velowin-v06-box64-debug** com `libvelobox64.so`. Isso elimina a necessidade de baixar um APK experimental separado para ter o tradutor; o autoteste `box64 -v` permanece disponível em **Sistema**.

**Driver Vulkan testável:** o JNI/NDK agora cria e destrói uma **VkSwapchainKHR real** numa Surface Android, validando mais do que a existência da extensão. O sistema identifica GPUs Mali/Adreno/Xclipse/PowerVR, verifica a criação de VkDevice e informa se a troca de driver é suportada. **Turnip continua indisponível no APK** e não pode ser selecionado como driver ativo.

**Também permanecem** os perfis por jogo, Wine Bionic WCP verificado, Wineprefix por jogo, controles multitouch/gamepad e laboratórios de áudio. `wine --version` e Box64 `-v` são testes reais de processo no Android quando executados no aparelho, mas **não demonstram jogos Windows funcionais**. Faltam o servidor gráfico Wine/X11, wineboot testado, pontes de áudio/entrada e as camadas DXVK/VKD3D.

Leia [Box64 + GPU v0.6](docs/BOX64_GPU_V06.md) para testar o APK e entender a matriz de drivers.

## VeloWin 0.5.0-dev — Vulkan WSI + controle virtual real no Android

Esta versão adiciona laboratórios de capacidades nativas sem misturar funcionalidades Android com alegações de execução Windows.

- **Vulkan na superfície do Android**: um novo laboratório NDK/JNI cria e consulta `VkSurfaceKHR`, verifica filas de apresentação, extensões de superfície e suporte a `VK_KHR_swapchain` na GPU real. **Não** cria a swapchain nem apresenta frames de jogos.
- **Gamepad e controle virtual multitouch**: novo painel original com joystick, D-pad, A/B/X/Y, START/BACK e shoulders, além de entrada USB/Bluetooth; uma classe Java independente combina entradas físicas e touch num snapshot de botões/analógicos/gatilhos semelhante ao XInput.
- **Deadzone e integridade de input**: tratamento de analógicos, hat switch, gatilhos, valores inválidos, liberação de botões ao mudar de aplicativo e preservação da outra fonte durante uso misto de touch+controle.
- **Regressões Java e CI NDK**: testes de mapeamento, atualização de AndroidManifest, nova Activity e código Vulkan nativo, com APK no GitHub Actions.

Para testar no telefone: **Sistema → Testar apresentação Vulkan nativa (WSI)** e **Sistema → Testar controles virtuais e gamepad**. Nenhuma dessas funções injeta eventos Win32, inicializa DXVK ou inicia jogos. Mais detalhes: [Laboratórios Vulkan/entrada v0.5](docs/VULKAN_WSI_INPUT_V05.md).

## VeloWin 0.4.0-dev — Bionic, prefixos independentes e perfis completos

**Verificação real do WCP:** o GitHub Actions confirmou o pacote de referência GameNative contendo `.wine/drive_c`, `system.reg` e unidades Wine. O instalador preserva C: e omite o link Z: que apontaria para a raiz do Android. Mais detalhes em [Prefixos e perfis](docs/PROFILES_AND_PREFIXES.md).

### Novidades concretas da 0.4

- **Prefixos por jogo**: novo instalador Android para `prefixPack.txz` dos WCP Wine Bionic já preparados, com staging isolado por UUID, TAR/XZ streaming, proteção de links e verificação de `system.reg` e `drive_c`. Essa preparação **não executa wineboot** e não demonstra compatibilidade Windows.
- **Wine Bionic selecionável por jogo**: variante x86_64 ou ARM64EC explicitamente vinculada ao perfil, sem confusão com os antigos arquivos Linux/glibc.
- **Perfil de console**: resolução virtual, FPS alvo, Dynarec Box64 e renderizador Direct3D (WineD3D, DXVK ou VKD3D-Proton como preferências). Parâmetros gráficos ainda não são aplicados a um jogo sem motor integrado.
- **Teste e diagnóstico no contexto do jogo**: Box64 → Wine --version somente em APK experimental compatível; pré-verificação de sessão, exportação do perfil e limpeza de instalações de prefixo interrompidas.
- **Vulkan nativo mais profundo**: laboratório JNI tenta criar instância, dispositivo lógico, fila gráfica e command pool; não há renderização Vulkan de jogos, DXVK ou VKD3D nesta etapa.
- **Verificações automatizadas**: testes Java para ABI Bionic, seleção de Wine e perfis; contratos estáticos de isolamento da extração e build Android no GitHub Actions.

Veja [Prefixos e perfis](docs/PROFILES_AND_PREFIXES.md) e [projeto do motor Bionic](docs/BIONIC_ENGINE_V03.md).

## VeloWin 0.3.5-dev — Android/Bionic + diagnóstico e perfis

**Funcionalidades concretas desta revisão:**

- **Wine Android/Bionic experimental**: busca de versões [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) para **x86-64 ou ARM64EC**, validação do digest SHA-256 publicado nos assets GitHub, download seguro, registro local e instalação de arquivos WCP Zstandard/TAR isolada com controle de paths/symlinks, perfil `profile.json`, ELF e rollback.
- **Linux/glibc separado**: mantém downloads e extração Wine WoW64 de Kron4ek do VeloWin 0.2, sem misturar os ambientes.
- **Teste Wine controlado**: no APK experimental, após preparar o WCP Bionic x86-64, o botão **Testar Wine --version** tenta executar Wine via Box64 por até 9s. É um autoteste, não execução de jogo.
- **Box64 ARM64/Bionic**: novo [workflow experimental](.github/workflows/box64-bionic.yml) compila uma revisão fixa do upstream com Android NDK, inspeciona linker Bionic/ELF e produz APK diagnóstico com o tradutor embutido. No app, há autoteste opcional `box64 -v`. O APK principal da v0.6 **inclui Box64**. Compilar não confirma que executa jogos.
- **Vulkan nativo**: `app/src/main/cpp/native_probe.c` interroga a biblioteca Vulkan Android via JNI para identificar GPU, versão de API e algumas extensões. Vulkan funcionando não significa DXVK ou VKD3D ativo.
- **Controles, áudio e tela**: laboratório de toque/gamepad e áudio por `AudioTrack`, mais **Surface OpenGL ES 2.0** com contador observacional de quadros (não mede FPS de jogos).
- **Perfis por jogo**: Compatibilidade/Equilibrado/Desempenho com configurações Box64 DynaRec documentadas no upstream, persistidas por jogo; escolha automática conservadora do Vulkan do sistema, sem Turnip em Mali.
- **Ambiente privado Bionic**: prepara diretórios seguros `home/tmp/logs/cache/prefixes/mounts`, sem root. Esse workspace **não é rootfs glibc** nem inicialização de Wine.
- **Supervisor de diagnósticos**: coleta limitada de stdout/stderr sem deadlock de pipe, encerramento por timeout, variáveis de ambiente auditáveis, análise de falhas comuns Bionic/ELF/X11/Vulkan.
- **Preflight fail-closed**: relatório detalha Box64, Wine Bionic, Vulkan e pontes de execução ainda ausentes. O VeloWin **não executa EXEs** nesta fase.

## APKs pelo GitHub Actions

- **[VeloWin Android](https://github.com/danilo631/VeloWin/actions/workflows/android.yml)**: testes Java/Python, JNI Vulkan e APK debug padrão, artefato `velowin-v07-container-box64-debug`.
- **[Experimental Box64 Bionic Integration](https://github.com/danilo631/VeloWin/actions/workflows/box64-bionic.yml)**: compila Box64 upstream para AArch64/Bionic, confere ABI, insere `libvelobox64.so` e produz sob demanda o artefato `velowin-box64-standalone-diagnostic`; o APK principal já contém Box64.
- **[Verify Bionic Proton WCP Layout](https://github.com/danilo631/VeloWin/actions/workflows/bionic-wcp-inspect.yml)**: confere checksum de um release GameNative real, tipo Zstandard/TAR, perfil e layouts. Não executa binários desconhecidos.

Baixe o artefato ZIP do workflow concluído, extraia o APK e instale. A v0.6 inclui Box64 no APK principal; use o build experimental isolado apenas para auditoria. Não confunda APK compilado ou autoteste de versão com compatibilidade de jogos.

## O que ainda falta para jogar

A execução **de Wine e de jogos Windows ainda não está integrada/testada end-to-end**. O template de Wineprefix agora pode ser extraído por jogo; ainda faltam executar e testar \`wineboot\`, resolver bibliotecas Android, roteamento do display Windows/X11 para uma Surface Android, áudio Wine → Android, entrada Wine, DXVK/VKD3D-Proton reais, lifecycle de processos e testes físicos Mali/Adreno. O aplicativo não tem acesso a Steam nem distribui jogos.

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
