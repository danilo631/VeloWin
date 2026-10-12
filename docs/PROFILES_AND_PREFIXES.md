# VeloWin 0.4 — biblioteca, prefixos por jogo e compatibilidade

Inspirado no fluxo **biblioteca e perfis** (GameHub/Winlator) e **ambientes versionados, Dynarec configurável e diagnóstico** (Mobox), com código Android original e sem copiar identidade, assets ou partes proprietárias dos apps citados.

## Onde a versão 0.4 avança

1. Adicione um `.exe` via Android Storage Access Framework. O VeloWin analisa a arquitetura PE, mas não copia automaticamente todos os arquivos do jogo.
2. Em **Runtimes**, faça download e **prepare Wine Android/Bionic** compatível com seu processador (a variante x86_64 é destinada ao caminho experimental Box64). O Wine Linux/glibc continua em pasta distinta e **não é usado** na sessão Bionic.
3. Na biblioteca, abra as opções do jogo → **Selecionar pacote Wine** → escolha uma versão **Android Bionic** já extraída.
4. Toque **Preparar prefixo Windows**. O VeloWin extrai `prefixPack.txz` para `files/bionic-workspace-v1/prefixes/<game UUID>`, sem root e sem executar Wine. Jogos diferentes recebem pastas próprias.
5. Configure **Box64 Dynarec**, **resolução virtual**, **FPS alvo** e **Direct3D**. São **preferências persistidas**, não parâmetros já aplicados a jogos.
6. Use **Testar Wine deste jogo** no APK experimental Box64 para executar apenas `wine --version` (limite de 9s). **Pré-verificação de sessão** informa componentes ausentes.
7. Use **Compartilhar perfil** para gerar um relatório textual sem incluir URI privada, nomes de caminhos ou arquivos do dispositivo.

## Segurança do prefixPack

- Diretório privado e subpasta por UUID canônico impedem path traversal via nome de jogo.
- O arquivo `prefixPack.txz` deve estar **dentro** de um Wine Bionic cujo manifesto é válido.
- Extração streaming TAR/XZ, tamanho total máximo, número máximo de entradas e tamanho por arquivo.
- Caminhos relativos canonizados, duplicados proibidos, links simbólicos estritamente relativos, sem dispositivos, pipes ou hardlinks.
- Gravação em `.prefix-stage-<UUID>` e movimentação para a pasta final somente após presença de `system.reg` e `drive_c`.
- Um cancelamento remove o staging sem seguir symlinks. Em encerramentos inesperados, a ação **Limpar prefixos de jogos interrompidos** recupera espaço.
- O pacote de referência GameNative `proton-11.0-2-20261010` foi inspecionado no CI sem executar binários; o template usa a pasta `.wine/`, C: aponta para `../drive_c` e Z: aponta para `/`. Por segurança, o VeloWin **não instala o link Z: → /**, impedindo acesso à raiz do Android. C: é mantido como link relativo e restrito à pasta do jogo.
- O marcador `TEMPLATE_ONLY` registra a origem do Wine e o UUID do jogo, não afirma que `wineboot` executou.
- Apagar o prefixo é uma operação explícita que pode apagar saves e configurações. Remover apenas o jogo da biblioteca **não** apaga os dados privados por segurança.

## Renderização e desempenho

| Camada | Fato comprovado | Ainda não integrado |
| --- | --- | --- |
| Vulkan Android | JNI chama `vkCreateInstance`, enumera GPU e tenta criar dispositivo lógico/fila/command pool | Nenhuma superfície Vulkan de jogo, WSI DXVK ou integração com Wine |
| OpenGL ES | Laboratório desenha triângulo Android | X11/GL Wine não conectado |
| DXVK | Opção por jogo registrada | DLLs, Vulkan WSI, validação de extensões e carregamento |
| VKD3D-Proton | Opção por jogo registrada | D3D12, Vulkan 1.3/extensões requeridas e Wine carregando DLLs |
| FPS alvo | Preferência 0/30/45/60/90/120 FPS | Frame limiter/telemetria por jogo |
| Prefixo | Template extraído e isolado | `wineboot`, wineserver, processos, áudio e X11 reais |

**O aplicativo ainda não abre jogos Windows.** Um template Wineprefix, um Box64 `-v` e um Vulkan `vkCreateDevice` bem-sucedidos não comprovam execução de jogos. Para isso será necessária a integração e teste do servidor X11 Android e suas pontes de áudio/entrada, com dispositivo real.

## Bases e licenciamento

- [Mobox](https://github.com/olegos2/mobox) (conceito de gerenciamento de pacote e Dynarec)
- [Winlator](https://github.com/brunodev85/winlator) (conceito de container/configuração por jogo)
- [GameNative](https://github.com/GameNative/proton-wine) e GameHub (fluxo orientado a jogos/instalação guiada)
- [Box64](https://github.com/ptitSeb/box64), [FEX](https://github.com/FEX-Emu/FEX), [DXVK](https://github.com/doitsujin/dxvk), [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton)

Esses projetos e seus ativos têm licenças próprias; são referências e bases potencialmente integráveis, não foram incorporados como se fossem código original do VeloWin.
