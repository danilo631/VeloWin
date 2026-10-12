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
