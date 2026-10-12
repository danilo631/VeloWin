# VeloWin · Bionic Engine v0.3-dev

## Filosofia de produto inspirada no GameHub

A inspiração é de experiência, não cópia de UI/código: instalar, importar jogo, obter **perfil conservador de um toque**, mostrar somente opções úteis por padrão e diagnósticos objetivos de compatibilidade. A interface é totalmente própria.

## Linux/glibc ≠ Android/Bionic

| Peça | Bionic (prioridade) | glibc (legado do v0.2) |
|---|---|---|
| libc | Android `libc.so` / linker64 | Linux `libc.so.6` / ld-linux |
| Wine | GameNative `proton-...-x86_64.wcp` ou ARM64EC | Kron4ek `wine-...-amd64-wow64.tar.xz` |
| Tradutor | Box64 nativo ARM64; FEX requer integração separada | Box64 compilado para Linux ARM64 |
| Rootfs | Diretórios Bionic privados + dependências Android/Termux compatíveis | Rootfs glibc completo com libs Linux |
| Graphics | Host Android Vulkan via JNI, X11/WSI futuro | Mesa/glibc ou adaptador host |
| Áudio | AudioTrack Android já testável; ponte Wine ainda pendente | PulseAudio/glibc pendente |

**Nunca extrair um pacote Bionic usando o instalador Wine glibc v0.2.** Um arquivo WCP GameNative contém um formato e manifesto específicos e requer verificação antes de instalar. O VeloWin v0.3 só baixa e verifica SHA-256 dos WCP — não extrai nem executa esses arquivos. Os hashes vêm do campo `assets[].digest` do GitHub, fornecido pelo mesmo canal de download, não por uma assinatura separada do publisher.

## Funcionalidades implementadas e instrumentadas

- **JNI Vulkan nativo**: cria uma instância real Vulkan Android, enumera GPU, extensão swapchain/timeline/memory-budget, limites e anisotropia; libera a instância.
- **Laboratório Android**: Canvas com toque visual, joystick AXIS_X/Y, botões de controle e AudioTrack PCM. Não são interfaces Wine ainda.
- **Workspace Bionic**: diretórios internos `home, tmp, logs, cache, prefixes, mounts`, sem root e sem glibc; *não* é Wineprefix inicializado.
- **Catálogo GameNative**: releases Proton Wine Android para x86_64 e ARM64EC, URL permitida, SHA-256, espaço livre, TLS e CDN validada. Download privado e cancelável.
- **Perfis automáticos**: para jogos x86/x64, preferir Box64 Bionic, Vulkan do sistema e preservação de dados por jogo, sem overclock.
- **Build Box64**: GitHub Actions experimental faz checkout de commit upstream `c6a8c4472f74f9ad204d35195f084b5f996b12e3`, compila com NDK r27c, verifica ELF AArch64 / linker Bionic e empacota um APK diagnóstico separado. O apk normal **não** inclui Box64.
- **Autoteste Box64**: nos APKs experimentais, executa somente `box64 -v` como subprocesso com timeout, sem abrir EXEs ou Wine.

## Integração faltante antes de jogos reais

A execução de Wine e de jogos Windows permanece bloqueada enquanto não houver, efetivamente:
1. Instalador WCP robusto (tar.xz ou zstd, `profile.json`, caminhos, symlinks e dependências ELF).
2. Verificação real do tradutor e lançamento da arquitetura Wine correta com prefixo Windows.
3. Ponte X11 ou outra implementação de janelas Windows para Surface Android.
4. Áudio real do Wine/WinMM/PulseAudio para Android, não apenas o som de teste AudioTrack.
5. Mapeamento efetivo de gamepad/teclado/toque para eventos Windows.
6. DXVK/VKD3D-Proton redistribuíveis com licenças e extensões Vulkan comprovadas.
7. Testes em telefone ARM64 real, incluindo Mali e Adreno. GitHub Actions em Ubuntu **não** roda Wine Android ARM64.

## Fonte upstream

- [Box64](https://github.com/ptitSeb/box64): MIT, CMake possui opções Android e Termux; próprio upstream alerta limitações de executáveis Linux no Android native Termux.
- [FEX](https://github.com/FEX-Emu/FEX): MIT, engine AArch64 que não substitui Box64 Bionic automaticamente.
- [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases): WCP Android/Bionic Proton experimental.
- [DXVK](https://github.com/doitsujin/dxvk), [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton): camadas Windows D3D → Vulkan; Vulkan 1.3 não garante todas as extensões.
- [GameSir GameHub](https://gamesir.com/pt-BR/support/downloads): referência conceitual de experiência centralizada. O VeloWin não é afiliado à GameSir.

## O que os testes realmente provam

Testes Java verificam políticas de ABI, SHA, nomes de pacotes e prontidão do engine; testes Android build verificam que Vulkan JNI/CMake compila. O job Box64 apenas faz cross-compilation e inspeciona ELF/linker — **nenhum desempenho FPS ou Wine startup é medido**.
