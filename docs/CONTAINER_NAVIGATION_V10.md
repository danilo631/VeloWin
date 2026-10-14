# VeloWin — navegação dentro do contêiner e caminho X11

## O que esta branch implementa agora

VeloWin passa a ter uma **tela de navegação de arquivos Android nativa**, acessada pela ação **Explorar arquivos do contêiner** dentro do console de cada jogo, ou no contêiner de diagnóstico sem jogo. Esta tela é original do VeloWin e foi inspirada no fluxo de gerenciador de arquivos do Winlator, sem copiar código nem recursos visuais.

- **Jogo**: pasta `bionic-workspace-v1/games/<UUID>` privada da sessão, gravável. Aqui é possível criar pastas e **importar um arquivo selecionado** pelo Android Storage Access Framework, inclusive DLLs/assets (até 384 MiB por arquivo).
- **Workspace**: navegação somente leitura da estrutura privada Bionic. Através desta aba **não é permitido modificar prefixos e arquivos do motor**.
- **Prefixo**: abre os arquivos realmente extraídos de `prefixPack.txz` por UUID quando existem; somente leitura.
- **C: Wine**: abre o `drive_c` real identificado pelo marcador `.velowin-prefix.json`. Só aparece quando o diretório é reconhecido; somente leitura.
- **Logs**: mostra os diagnósticos do contêiner, com prévia textual limitada.
- **Wine Bionic**: navega os arquivos do WCP x86_64 instalado; somente leitura.

Os botões **voltar**, **atualizar**, **nova pasta**, **importar** e **voltar ao console** funcionam dentro da Activity. Pastas são abertas por toque, arquivos oferecem metadados e, quando a extensão é textual, visualização limitada a 16 KiB. O navegador não executa arquivos, não usa shell, não expõe arquivos de outros aplicativos, não abre links simbólicos, bloqueia traversal, limita a lista a 300 entradas por pasta e recusa sobrescrever durante a importação.

A cópia pelo SAF **não monta automaticamente um diretório Windows inteiro**, não atualiza arquivos de jogos já em outro local, não edita C: nem declara um arquivo Windows executável. O usuário continua responsável pela origem do conteúdo importado. A persistência de prefixo e saves existentes não foi alterada.

## Como testar no Android

1. Baixe o APK da execução verde do workflow `VeloWin Android` para esta branch e instale sem limpar os dados existentes. APKs debug com assinaturas diferentes podem exigir backup e reinstalação.
2. Na **Biblioteca**, abra as opções do jogo → **Entrar no contêiner**.
3. Toque em **Explorar arquivos do contêiner**. Entre em **Jogo**, crie `Testes` usando **+ Pasta**, entre na pasta e pressione **Importar**; selecione um pequeno `.txt` com o seletor Android.
4. Toque no arquivo para ler o conteúdo; volte usando a navegação; teste as abas **Workspace**, **Logs**, **Prefixo** e **C: Wine** (as duas últimas só aparecem depois de preparar Wineprefix).
5. Volte ao console e execute `wine preflight`, `wine --version`, `prefix prepare`, `wineboot -u`, `wine cmd /c ver`, `win32 test` — todos seguem com políticas e confirmações anteriores.
6. Compartilhe o log do console e relate se o arquivo importado permanece após sair e reabrir o contêiner.

## Termux:X11 / Xorg Bionic — arquitetura estudada, não integrada neste APK

Referência auditada em **termux/termux-x11** `fa3a8b430e2896a19f44c99a9cb056254615ae06`: servidor X.org real desenvolvido em Android NDK, com `libXlorie.so`, código JNI, `LorieView`/SurfaceView, camada de entrada e processo `CmdEntryPoint`. A build depende de fontes/submódulos Xorg, XKB, pixman e muitos componentes. O `TMPDIR` determina a pasta de sockets X11 Unix e o pacote tradicional espera acesso aos sockets entre clientes e servidor.

**Ponto de atenção:** no VeloWin, o processo Wine é executado no UID privado `dev.velowin.app`, separado do aplicativo Termux:X11. O Termux:X11 de instalação normal não disponibiliza automaticamente o socket Unix do servidor ao processo Wine do VeloWin. A opção atual `DISPLAY=127.0.0.1:0` requer um servidor **explicitamente** configurado para TCP loopback e é só um experimento; o Termux:X11 padrão não deve ser descrito como compatível sem uma ponte IPC validada.

**Integração real recomendada em trabalho posterior:**
1. Escolher uma revisão fixa do Termux:X11, clonar com todos os submódulos e registrar licenças de cada componente.
2. Adicionar módulo Android/NDK integrado ao processo/aplicativo VeloWin ou serviço próprio sob o mesmo UID, com root do X11 controlado pelo VeloWin. **Não copiar o pacote Termux e presumir que executa com outro UID**.
3. Criar display `:N` com socket Unix em `getFilesDir()/bionic-workspace-v1/tmp/.X11-unix`, alinhado ao `TMPDIR` da sessão Wine; implementar confinamento, limpeza, shutdown e autenticação de clientes.
4. Conectar a Surface Android a um renderer real do servidor e traduzir MotionEvent/KeyEvent/gamepad para X11. Integrar ciclo de vida com Surface, rotação, múltiplas janelas e teclado IME.
5. Verificar `winex11.drv` do WCP selecionado, inicializar Wineprefix, abrir janela Windows **real** e fotografar/registrar log no aparelho. Só depois integrar som e DXVK/Vulkan.
6. Auditar a compatibilidade de licenças do projeto inteiro. **Termux:X11 está sob GPL-3.0** e suas dependências têm licenças próprias; integrar e distribuir componentes GPL no APK combinado exige cumprir seus termos, incluindo a disponibilidade do código-fonte correspondente. A licença MIT atual do VeloWin **não é uma autorização para redistribuir componentes GPL sem cumprir a GPL**.

A arquitetura acima é uma especificação de integração. **Não existe um servidor Termux:X11 embutido no APK desta branch, nem uma janela Windows em SurfaceView**; testes de navegação Android não comprovam isso.

## Winlator — referências de arquitetura, sem copiar implementação

Estudado `brunodev85/winlator-app` `3981d86efa4f333b2a34a7da8b6521476cd8c8b9`:

- `ContainerFileManagerFragment.java` implementa navegação por unidade Wine e gerenciador de arquivos.
- `XServerDisplayActivity.java`, `XServerView.java` e `xserver/` implementam um servidor X11 e um visualizador interativo no mesmo app.
- `XEnvironment`/componentes separam inicialização do servidor, áudio e programa Windows.

O VeloWin adotou **apenas o conceito** de unidade C:, múltiplas áreas por contêiner, navegação e controle independente do processo Wine. Winlator-app apresenta licença LGPL-2.1 no repositório, mas muitos componentes podem ter licenças adicionais. Permissão para consultar código **não elimina obrigações de atribuição e relicenciamento** para cópia literal.

## Bloqueios restantes

A execução Win32 ainda depende de validação em hardware: `wine --version`, `wineboot -u`, `wine cmd /c ver`, `VELO_WIN32_SMOKE_OK`. Sem um servidor X11 efetivo dentro do VeloWin, este explorador é **Android nativo**, não o gerenciador de arquivos Windows nem um desktop Wine. Isso é intencional: uma interface navegável útil e testável não deve ser confundida com compatibilidade gráfica Windows.
