# VeloWin v13 — sessão X11 independente, aprendizados do WinNative

## Referência examinada

WinNative commit [`8409085f706acbc0749881d9bf1e85f2f347dfd4`](https://github.com/WinNative-Emu/WinNative/tree/8409085f706acbc0749881d9bf1e85f2f347dfd4), GPL-3.0.
A implementação do WinNative separa ambiente gráfico, audio, dispositivos e lançamento de programa;
`XEnvironment.startEnvironmentComponents()` inicializa serviços antes do guest e para-os na
ordem inversa. Seu documento `docs/WAYLAND-DISPLAY.md` descreve Wayland em Adreno/Turnip
com Wine que inclua o próprio `winewayland.so`, e fallback para X11. **Mali não deve
ser anunciada como compatível com esse compositor Wayland.**

Adotamos a **ordenação e a verificação explícita de prontidão** no motor original
VeloWin. Não copiamos os arquivos de frontend, comércio, DRM, driver ou launcher do WinNative.

## Implementado nesta branch

O servidor Xorg/NDK do Termux:X11, já adicionado no PR #9 como módulo GPLv3 `:lorie`,
passa a ter um host experimental `EmbeddedX11Service` registrado em
`android:process=":velowin_x11"`, `android:exported="false"`.
Essa separação é necessária: o upstream pode chamar `exit(dix_main(...))`,
que terminaria o processo da UI se executado na mesma JVM.

- O botão **Iniciar X11 NDK** na tela do contêiner solicita ao serviço isolado
  o início do Xorg em `:0` e sem TCP, compartilhando `TMPDIR` com Wine.
- **Tela X11** só abre a Activity gráfica do addon depois de um handshake
  real do protocolo 11 no Unix socket privado. Uma conexão TCP não é considerada prova.
- **Parar X11** encerra o serviço/processo Xorg separado; prefixos e arquivos de
  jogo não são apagados.
- O build inclui dados `xkb-data` licenciados para `XKB_CONFIG_ROOT`
  e os extrai com limites para o diretório privado Android `xkb-v1`.
- O diagnóstico `bridge status` já existente indica aceitação/recusa do
  protocolo X11. O comando `wine notepad` exige resposta `ACCEPTED` antes
  de tentar o processo gráfico. **Aceitação X11 não confirma uma janela visível**.
- Box64, Wineprefix, DXVK e VKD3D anteriores são preservados.

## Testar no Android

Instale **somente um APK com workflow aprovado** da branch v13.
A versão interna continua 0.8.0-dev; o número de branch não é uma release.
APK debug de assinatura diferente pode exigir backup e reinstalação.

1. Abra a Biblioteca → jogo → **Abrir contêiner Bionic** ou a tela
   de diagnóstico. Pressione **Iniciar X11 NDK**, confirme.
2. Execute `bridge status` e veja se surge `X11 ... aceitou handshake`.
   Se ausente, confira o logcat do processo/tag `VeloWin-X11`.
3. Abra **Tela X11**. A Activity gráfica upstream deve poder negociar
   a conexão com o servidor; se aparecer preta, registre o logcat da View.
4. Prepare Wine Bionic x86_64 e prefixo, e execute:

```text
wine preflight
wine --version
prefix prepare
wineboot -u
wine cmd /c ver
win32 test
bridge status
wine notepad
wineserver -k
```

5. Confirme **visualmente** a janela do Notepad dentro da área gráfica;
   depois pressione **Parar X11**. Envie o log do console e, se possível,
   `adb logcat -d -s VeloWin-X11:D AndroidRuntime:E Xlorie:D CmdEntryPoint:D`.

Não considere um retorno de código zero ou socket X11 como prova de renderização.
A presença da Surface no APK não comprova que os eventos de toque ou
gamepad chegaram ao Wine.

## Limitações conhecidas e riscos

- Serviço Android iniciado pela tela visível: restrições de background
  do Android podem terminar a sessão; **não é ainda foreground service persistente**.
- `CmdEntryPoint` e `LorieView` são componentes upstream complexos.
  O ciclo de vida Android e a conexão Binder entre processos ainda precisam de
  validação em dispositivo real. Um build aprovado não garante uma janela.
- O diretório XKB extraído pode falhar por layout de arquivos incompleto; o
  erro é bloqueante, não é mascarado.
- Wine Bionic, `winex11.drv`, Vulkan/Box64, PulseAudio e controles são
  etapas separadas. Não afirmar compatibilidade com jogos.
- A lógica de sessão Wine usa um prefixo por jogo existente, sem migração
  automática. Não misturar arm64ec/Winewayland com x86_64/Box64.
- X11 só aceita conexões no socket privado e o serviço não expõe TCP.

## Licenças e créditos

- **Termux:X11**, GPLv3, commit fixo
  `fa3a8b430e2896a19f44c99a9cb056254615ae06`:
  https://github.com/termux/termux-x11
- **WinNative**, GPLv3, arquitetura de ciclo de vida e seleção de display:
  https://github.com/WinNative-Emu/WinNative
- **Winlator**, componentes X11 e Wine, verifique licenças efetivas:
  https://github.com/brunodev85/winlator
- **xkb-data** (X.Org keyboard configuration): copyright/licenças
  incluídos em `assets/xkb-data-copyright.txt` no APK gerado, derivados
  do pacote do build Ubuntu. A versão instalada é registrada no artefato CI.

A distribuição de um APK com Xorg GPLv3 exige fornecer o código-fonte
correspondente e avisos necessários. A licença MIT dos arquivos originais
VeloWin não elimina as obrigações GPL dos componentes combinados.

**Status:** código do serviço e interface em revisão; os testes do CI
e a validação física determinam se o servidor funciona.
