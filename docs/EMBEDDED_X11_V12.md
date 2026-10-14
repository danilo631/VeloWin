# VeloWin v12 — inclusão experimental do XServer Xorg/NDK no projeto Android

## O que esta branch realmente contém

Este experimento vai além de baixar um APK externo: o repositório VeloWin
agora fixa Termux:X11 upstream como git submodule em
`third_party/termux-x11` (commit
`fa3a8b430e2896a19f44c99a9cb056254615ae06`).
O projeto Gradle inclui `:lorie` e `:shell-loader:stub`, e o módulo
`app` declara `implementation project(':lorie')`.

O workflow Android inicializa submódulos recursivos, instala o Android NDK
29.0.14206865 além do NDK 27 exigido pelo Box64, prepara dependências Xorg
(bison/flex/autotools) e tenta produzir **o APK VeloWin com o código-fonte
XServer/NDK integrado**. O manifesto de integração mantém o launcher VeloWin
como principal, restringe as Activities e receivers do addon e usa um
`VeloWinApplication` derivado de `LorieApp`.

A compilação isolada de `:lorie:assembleDebug` foi confirmada pela execução
https://github.com/danilo631/VeloWin/actions/runs/38500834971 .
Isso **não** comprova o build Android combinado da branch, nem Wine
apresentado na Surface. Veja o resultado da própria branch v12 antes de
anunciar APK funcional.

## Arquitetura para o próximo nível de execução

1. Isolar Xorg em processo Android dedicado para que `dix_main()` e seu
   encerramento não terminem a UI VeloWin.
2. Garantir `XKB_CONFIG_ROOT` e fontes/XKB disponíveis **dentro do UID
   privado do VeloWin**. Criar socket `TMPDIR/.X11-unix/X0` com permissões
   consistentes com a sessão Bionic/Box64, sem depender de UID do Termux.
3. Iniciar, parar, supervisionar e reiniciar o servidor com logs e sem
   vazar processos ou sockets de sessões anteriores.
4. Vincular a View de apresentação `LorieView` a uma Activity original
   VeloWin, encaminhando toque, mouse/teclado, rotação e Surface lifecycle.
5. Somente após confirmação do endpoint X11, configurar o `DISPLAY`
   no ambiente Wine, testar `winex11.drv`, `wineboot -u` e abrir uma
   janela `notepad` observável no próprio VeloWin.
6. Validar áudio Wine e compatibilidade DXVK/WineD3D/VKD3D separadamente.
   Nenhum teste de GPU, FPS ou jogo pode ser inferido de uma compilação.

## Licenças e créditos obrigatórios

- Termux:X11: GPL-3.0, fonte e histórico:
  https://github.com/termux/termux-x11/tree/fa3a8b430e2896a19f44c99a9cb056254615ae06
- X.Org Server e bibliotecas auxiliares: inspecionar licenças de cada
  submódulo referenciado pelo upstream, inclusive Xorg, pixman, libepoxy.
- VeloWin mantém a autoria do código original MIT; a **distribuição
  combinada com código GPLv3** requer uma política de licença compatível,
  avisos e entrega dos fontes correspondentes do conjunto no momento da
  redistribuição. Não ocultar licenças de terceiros.

O usuário autorizou estudo e reutilização, mas permissão pessoal
não revoga obrigações de licença dos titulares upstream.

**Não usar esta branch como release principal até que compile, instale,
inicie o XServer sob o UID correto e apresente uma janela Wine no telefone.**
