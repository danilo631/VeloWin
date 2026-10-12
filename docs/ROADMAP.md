# Roadmap de execução VeloWin

## v0.1 — aplicativo independente
- [x] UI original, biblioteca SAF, perfis, GPU e CI Android

## v0.2 — Wine Linux/glibc
- [x] Catálogo Kron4ek, download SHA-256, extração TAR/XZ de arquivos e rollback
- [x] Biblioteca de favoritos, cabeçalhos PE, notas e diagnósticos de hardware

## v0.3-dev — Bionic-first e infraestrutura nativa
- [x] Android NDK JNI Vulkan físico, informações reais de device/driver/algumas extensões
- [x] WCP GameNative x86_64/ARM64EC com SHA-256, catálogo e armazenamento separados
- [x] Confirmar WCP real Zstd TAR + `profile.json` em CI, implementar extrator seguro isolado
- [x] Box64 ARM64/Bionic compilado em workflow experimental, ABI e linker inspecionados
- [x] `box64 -v` com timeout nos APKs que embalem o tradutor, sem jogo
- [x] Tentativa controlada de `wine --version` com Box64 opcional e WCP Bionic extraído; validação física ainda pendente
- [x] AudioTrack e eventos reais de controles/touch em laboratório Android
- [x] Workspace Bionic privado e matriz de pré-requisitos fail-closed
- [x] Perfil conservador de um toque para jogos
- [ ] Instalação WCP e Box64 testados em Android físico

## v0.4 — motor Windows Android real
- [ ] Inicializar Wine Bionic/`wineboot` após validar dependências, logs e prefixPack
- [ ] Integrar Box64 de modo seguro ao processo host, separar perfil x86 de ARM64EC
- [ ] FEX/ARM64EC só após verificação de biblioteca e ABI
- [ ] Display Windows/X11 → Surface Android, entradas e UI de execução
- [ ] PulseAudio ou ponte Wine → Android AudioTrack
- [ ] Traduzir touch/gamepad Android para eventos Windows
- [ ] DXVK e VKD3D-Proton com validação granular de Vulkan e licenças
- [ ] Jogo Windows mínimo real: janela, som, teclado e encerramento
- [ ] Testes em Mali e Adreno com resultados e frame pacing

## v1.0
- [ ] Benchmarks reais, saves, backups, DRM considerations, experiência assistida, APK assinado

## v0.3.5-dev — diagnóstico reproduzível e experiência simplificada
- [x] Perfis Box64 DynaRec Compatibilidade/Equilibrado/Desempenho por jogo, variáveis baseadas em docs upstream
- [x] Supervisor de diagnóstico `--version` e `-v` com pipe assíncrono, limite de saída/tempo e limpeza
- [x] Erros de ABI/Bionic, libs, X11, Vulkan interpretados em recomendações claras
- [x] Laboratório Surface OpenGL ES com contador observacional de quadros em tela Android
- [x] Testes Java para processos, presets e falhas; contratos estáticos para laboratório Android
- [ ] Gráficos *Windows* em Surface Android (servidor X11/WSI), áudio e controle Wine reais
- [ ] `wineboot` e prefixo com gerenciamento seguro de filhos/encerramento, dependências verificadas
- [ ] Implementação de DXVK e VKD3D validada em jogos autorizados Mali/Adreno

## v0.5-dev — Vulkan Surface e laboratório de entrada
- [x] Consultar WSI de Surface Android real via JNI, incluindo fila de apresentação e requisitos swapchain
- [x] Laboratório original de controles virtuais multitouch com estados semelhantes ao XInput
- [x] Capturar eventos de gamepad Bluetooth/USB e combinar sem conflito com toque
- [x] Testes Java para deadzone, gatilhos, botões e liberação; build NDK/Android
- [ ] Criar swapchain Vulkan e apresentar frames reais de teste
- [ ] Servidor X11/Windows conectado à Surface Android e DLLs DXVK realmente integradas
- [ ] Traduzir estado XInput-like para eventos Win32/X11; áudio Wine para Android
- [ ] Executar `wineboot` e programas Windows mínimos em Android ARM64 real
