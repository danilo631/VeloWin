# VeloWin — evolução do motor

## v0.1 — aplicação Android independente
- [x] Pacote dev.velowin.app, código próprio, biblioteca SAF e perfis
- [x] GPU/OpenGL/Vulkan Android e downloads Wine verificados
- [x] Compilação Android CI

## v0.2 — preparação real do filesystem Wine
- [x] Instalar Wine TAR/XZ com SHA-256, limites de tamanho, links relativos e diretório de staging
- [x] Detectar ELF principal e impedir instalação Bionic no pipeline glibc
- [x] Detectar formato PE de um jogo, filtrar favoritos, personalizar títulos e notas
- [x] Persistência robusta com escrita atômica, substituição reversível e confirmação de download
- [x] Diagnóstico RAM/bateria/térmico, exportar relatório, novo APK no CI
- [ ] Executar testes end-to-end de extração em Android físico e arquivar resultados Mali/Adreno

## v0.3 — motor nativo real (independente de Winlator)
- [ ] Escolher e empacotar rootfs com ABI declarada, versão fixa e manifesto assinado
- [ ] Compilar Box64 para AArch64 / integrar tradutor com verificações de DLL/ELF e ambiente
- [ ] Verificar dependências glibc e preparar prefixo Wine de modo reprodutível
- [ ] Implementar serviço de execução isolado, logs, encerramento e tratamento de falhas
- [ ] Interface gráfica Windows: X server/display Android, toque/controle, áudio
- [ ] Vulkan nativo + DXVK opcional e VKD3D-Proton quando extensões permitirem
- [ ] Testar executável Windows simples, depois games de referência Mali e Adreno

## v1.0 — distribuição
- [ ] Backups/migração de biblioteca e prefixos; atualizações assinadas
- [ ] Estabilidade térmica e melhorias de frame pacing medidas
- [ ] APK de release com assinatura e matrizes de suporte
