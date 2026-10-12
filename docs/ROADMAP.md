# VeloWin roadmap

## 0.1.0 — Fundação própria
- [x] Repositório independente / aplicação `dev.velowin.app`
- [x] Interface escura minimalista e navegação
- [x] Biblioteca de jogos Windows via Android SAF
- [x] Perfis locais de tradutor e driver por jogo
- [x] Relatório GPU/GLES/Vulkan
- [x] Wine/Proton Linux WoW64: catálogo GitHub, download verificado, registro e remoção
- [x] Testes de política/estruturas e pipeline de build
- [ ] Testar APK em hardware Android real

## 0.2.0 — Runtime adapter
- [ ] ABI documentada para motor Box64/glibc e outro Bionic/FEX quando apropriado
- [ ] Pacotes versionados e manifestos com assinatura confiável
- [ ] Extrator seguro de rootfs/Wine com proteção contra symlink e traversal
- [ ] Teste automático do binário e dependências antes de instalar
- [ ] Logs estruturados de criação de prefixo Wine, rollback e cancelamento

## 0.3.0 — Execução
- [ ] Ponte nativa Android Vulkan/driver System
- [ ] Implementar execução de Wine através de Box64/FEX sob runtime compatível
- [ ] DXVK/VKD3D-Proton configurados por jogo e capability probe
- [ ] Entrada de teclado/toque/controle, áudio, display e gestão de processos

## 1.0 — Estabilidade e distribuição
- [ ] Testes de jogos reais em dispositivos Mali/Adreno
- [ ] Limites térmicos e previsibilidade de frame pacing
- [ ] Exportar/importar contêiner com backup
- [ ] APK release assinado, atualizações verificadas e publicação de licenças
