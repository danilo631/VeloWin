# VeloWin — pesquisa de implementações maduras

Esta análise compara conceitos de arquitetura, não copia implementações. Todas as alterações em VeloWin são código original sob a licença deste repositório.

## Referências

| Fonte | Evidência upstream | Decisão prática adotada |
| --- | --- | --- |
| [Winlator oficial (brunodev85/winlator)](https://github.com/brunodev85/winlator) | Wine + Box86/Box64; perfis por jogo; recomendação de estabilidade para Unity; configuração de latência ALSA/PulseAudio | Biblioteca e configurações isoladas por título, presets com significado, diagnósticos explícitos |
| [Mobox (olegos2/mobox)](https://github.com/olegos2/mobox) | Wine e Box64 em Termux; gerenciamento de Wine, dynarec e logs; Termux-X11/Input Bridge; projeto arquivado em 20/06/2025 | Gerenciar pacotes + logs e separar camadas, mas **não depender** de scripts \`curl | sh\`, fontes arquivadas ou copiar componentes |
| [GameSir GameHub](https://gamesir.com/pt-BR/support/downloads) | Um aplicativo para downloads, atualizações e perfis de controle | Fluxo centrado na biblioteca, perfis automáticos de um toque e diagnóstico antes de iniciar |
| [Box64 docs/USAGE.md](https://github.com/ptitSeb/box64/blob/main/docs/USAGE.md) | \`BOX64_DYNAREC_SAFEFLAGS\`, \`BIGBLOCK\`, \`STRONGMEM\`, \`CALLRET\` e outras variáveis documentadas com comportamento/limites | Três configurações auditáveis em \`Box64Tuning.java\`, salvas por jogo. Sem alegação de ganho de FPS |
| [FEX](https://github.com/FEX-Emu/FEX) | Emulador AArch64 e recompiler x86-64 | Avaliação futura como backend separado; não assumimos interoperabilidade Bionic com o Box64 atual |
| [GameNative/proton-wine](https://github.com/GameNative/proton-wine/releases) | WCP Bionic com hash SHA-256, perfis e arquivos ELF próprios | Catálogo Bionic distinto de Kron4ek glibc; instalação de filesystem sem ativação automática |
| [DXVK](https://github.com/doitsujin/dxvk) e [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton) | Bibliotecas Direct3D para Vulkan com requisitos de extensões, software e hardware | Não habilitar apenas pela presença de Vulkan; inspecionar capacidades e aguardar integração |

## Experiência do usuário: comportamento implementado

**Biblioteca → perfil → diagnóstico → runtime.** A pessoa escolhe um jogo e um preset, sem precisar aprender todos os parâmetros DynaRec. O sistema mantém Wine Bionic e Linux/glibc em pastas separadas; um pacote obtido não é confundido com instalação/execução. O painel de diagnóstico mostra se falta Box64 Bionic, Wine Bionic, display, som ou tradutor D3D.

Os três modos foram montados a partir da documentação do Box64:
- Compatibilidade: SAFEFLAGS=2, BIGBLOCK=0, STRONGMEM=1, CALLRET=0.
- Equilibrado: SAFEFLAGS=1, BIGBLOCK=2, STRONGMEM=0, CALLRET=2.
- Desempenho: SAFEFLAGS=1, BIGBLOCK=3, STRONGMEM=0, CALLRET=2.

Essas recomendações só alteram o ambiente do autoteste Wine nesta fase. Perfis salvos para jogos ainda não lançam executáveis Windows.

## Camadas que continuam faltando

1. Instalação/configuração de bibliotecas Bionic runtime e execução de wineboot/wineserver com gerenciamento de processo.
2. Servidor de janelas Windows/X11 integrado a SurfaceView Android. O laboratório GLES é apenas uma prova de Surface Android.
3. Mapeamento de eventos Android para entrada Win32 e áudio Wine/ALSA/PulseAudio para Android.
4. Driver gráfico de jogo e WSI Vulkan, DXVK/VKD3D empaquetados legalmente com verificação de extensões.
5. Testes físicos com jogos autorizados, registros de falhas e medições reais de estabilidade em Mali e Adreno.

## Licenciamento, segurança e distribuição

O Winlator possui componentes sob LGPL e outras licenças. Mobox distribui scripts e componentes sob condições próprias; não assume-se licença universal para copiar. GameHub/GameSir é aplicativo de terceiros, sem qualquer afiliação. Não usar código ou arte proprietária nem incorporar scripts remotos sem auditoria.

O VeloWin limita o download a fontes com URLs aceitas e hashes públicos e isola os diagnósticos. Isso não substitui assinatura independente do publicador nem verificação de segurança de todo binário. Builds experimentais Box64 usam commit fixado e levam seu aviso MIT correspondente.

**Status:** CI valida compilação e algumas políticas, mas só testes no hardware Android podem provar suporte a Wine, Vulkan WSI e jogos.
