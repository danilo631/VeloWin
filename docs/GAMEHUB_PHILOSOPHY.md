# VeloWin — experiência GameHub, implementação independente

A inspiração é **produto**, não reutilização de interface, assets ou código do GameHub.

## Experiência
- **Jogar primeiro**: biblioteca personalizada, favoritos, busca e poucas decisões iniciais.
- **Configuração adaptativa por jogo**: escolher Compatibilidade, Equilibrado ou Desempenho; sugerir tradutor, API e driver do Android com explicações.
- **Bionic primeiro**: Wine Android/Bionic e Box64 Android/Bionic pertencem ao mesmo fluxo; não combinar Wine Linux/glibc sem container/rootfs dedicado.
- **Driver System como padrão**: especialmente para Mali/Xclipse. Turnip não é driver para Mali; instalar drivers terceiros só após validar GPU/ABI.
- **Diagnóstico antes de execução**: relatórios Vulkan, controle e áudio são testes de host, não evidência de jogo Windows pronto.
- **Reprodutibilidade**: binários externos fixados por commit, hashes, licenças e CI de ABI; nenhuma DLL proprietária distribuída sem permissão.
- **Falhar com informações úteis**: se faltar bridge de display/Wineprefix/DXVK, exibir qual componente impede iniciar em vez de executar um binário arbitrário.

## O que existe neste projeto
- Wine Bionic WCP com inspeção, arquivo de origem e extração segura; Workspace Android.
- Prova de vida opcional do Box64 Bionic em APK **experimental** do GitHub Actions; Wine `--version` apenas com pacote compatível.
- Laboratório Android de touch, controle e AudioTrack e Vulkan nativo pelo NDK.
- Recomendação de perfil por GPU, RAM, Vulkan e arquitetura PE; não instala DXVK nem inicia um Windows game.

## Lacunas para execução real
1. Obter APK experimental Box64 validado em dispositivo real, sem confundir compilação CI com execução ARM.
2. Preparar prefixo Wine Bionic com dependências e runner isolados, testes de processo e logs.
3. Implementar servidor de janelas compatível com Wine (SurfaceView, composição e input), não apenas canvas de teste.
4. Ponte de áudio e controle entre Wine e Android (AudioTrack e Gamepad API isolados não bastam).
5. Tradutor Direct3D via DXVK/VKD3D com bibliotecas x86 compatíveis, WSI Vulkan do Android e checagem de extensões.
6. Rodar testes end-to-end por hardware real (Mali e Adreno), com jogos autorizados e executáveis de referência.

GameHub e Valve são marcas de terceiros. VeloWin não afirma associação.

## Próxima implementação: práticas externas avaliadas (v0.3.5)
A partir do Winlator: preferir configurações por jogo e perfis de estabilidade, manter diagnóstico de latência e política conservadora. Do Mobox: modos Box64 específicos, seleção de Wine, separação do contêiner e ferramentas de log; **Mobox upstream está arquivado desde junho de 2025**. Da experiência GameHub: ações simples por jogo, tela central de suporte, menos opções obrigatórias. Não copiar código, arte, marca ou assets de terceiros.

Foram adicionados: DynaRec por jogo, saída capturada de processos com timeout, análise de causas mais frequentes e demonstração de renderização Android em Surface. O último não é um servidor de janelas Windows e não constitui um motor de jogo.
