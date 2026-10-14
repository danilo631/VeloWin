# VeloWin v18 — recuperação real de tela preta e interface independente

## Problema reportado

O contêiner abre a Surface X11 com **fundo preto e apenas o cursor se
move**. Isso prova que o Android consegue mostrar o cursor, **não**
que `Wine/Box64` produziu alguma janela. A causa exata depende do
logcat e dos executáveis disponíveis no prefixo; não existe evidência
de que o driver Mali seja sempre culpado.

O código original Winlator e o motor herdado têm um caminho de desktop:
`wine explorer /desktop=shell,... winhandler.exe "wfm.exe"`. A execução
VeloWin sem atalho passava `container_id` e dependia do gerenciador
`wfm.exe`, mesmo quando a preparação dos arquivos comuns falhava.
O caminho `ContainerManager.activateContainer` e a reparação
`ensureWinePrefixEssentialFiles` continuam sendo utilizados.

## Mudanças integradas à execução REAL de Wine

1. **`VeloBootTarget.java`**: seleciona `wfm.exe` quando ambos os
   executáveis estão presentes; senão `winefile.exe` do Wine, e,
   quando `winhandler.exe` não existe, `explorer.exe` nativo do Wine.
   O usuário também pode solicitar `Winefile` via perfil de
   recuperação persistido no contêiner. Isto não inicia outro Wine,
   apenas ajusta o argumento do executor original.
2. `tools/patch-v18.py` conecta esse módulo na chamada concreta de
   `XServerDisplayActivity.getWineStartCommand()`, **depois** do
   preparo do prefixo, e registra a escolha no Logcat `VeloWinSession`.
3. Após `ensureWinePrefixEssentialFiles()`, a Activity registra
   existência dos arquivos `wfm.exe`, `winhandler.exe` e `system.reg`.
4. Se nenhuma janela de aplicação for mapeada após 35 segundos,
   abre um alerta não destrutivo com **Diagnóstico / Continuar / Sair**.
   A detecção usa `firstGuestWindowShown`, não a visibilidade do
   cursor. Não apaga prefixos nem jogos e ignora sessões Linux/instaladores.
5. Falha de inicialização que antes exibia um toast efêmero recebe
   diálogo persistente e saída explícita. A exceção permanece no
   logcat/SessionLogWriter.

## O VeloWin deixa de abrir configurações do WinNative

- **`VeloSettingsActivity.java`**, código original: configura resolução
  540p/720p/900p/1080p, WineD3D/DXVK+VKD3D, X11/Wayland condicionado,
  PulseAudio/ALSA, preset Box64, inicialização Wine normal/essencial.
- A Home do VeloWin não possui mais botão que abre
  `UnifiedActivity`; telas autorais de configurações, diagnóstico e
  importação substituem esse caminho de uso comum.
- A Activity fornece **Recuperar tela preta**:
  1280x720, WineD3D, wrapper de sistema, X11, FrameGen desativado
  e fallback Winefile. Preserva configuração gráfica anterior
  via `MaliProfile`, que pode ser revertida.
- **`VeloSessionDiagnostics`** executa um preflight somente leitura:
  ImageFS, arquitetura, Wine, Box64, arquivos essenciais, prefixo
  Wine, backend X11, wrapper DirectX e sinaliza o que falta. O usuário
  pode copiar o diagnóstico.
- **Importação própria via Storage Access Framework** para
  `C:\\VeloWin-Imports`: nome restrito, bloqueio de links simbólicos,
  arquivos existentes e limite 512 MiB. Operações de cópia em executor,
  gravação temporária antes do rename.

A implementação de Wine, Box64/FEX, XServer, Vulkan e parte da
instalação de componentes **ainda usa a engine derivada de Winlator/
WinNative fixada em submódulo GPL**. As telas de usuário e o controle
de recuperação são originais. A interface WinNative continua compilada
internamente como dependência do motor; remover tudo de uma vez
invalidaria JNI e instaladores. O objetivo de independência progride
pelas interfaces e componentes separados, não por trocar o rótulo.

## Validação no Android (especialmente GPU Mali)

1. Fazer backup dos prefixos/saves. Instalar APK **com CI verde**.
2. Na Home, clicar em **Reparar tela preta** no contêiner original.
   Isso não altera a arquitetura Wine nem instala novo driver.
3. Entrar no desktop. Confirmar janela Winefile (fallback) e abertura
   de `winecfg.exe` ou `notepad.exe`, não apenas cursor.
4. Se ainda preto, após 35s abrir **Diagnóstico** e copiar o relatório.
   O aplicativo registra `VeloWinSession`, `ContainerLaunch` e
   `XServerDisplayActivity` no Android Logcat. Exemplo:
   `adb logcat -d -s VeloWinSession:I ContainerLaunch:D XServerDisplayActivity:E AndroidRuntime:E`.
5. Se Winefile funcionar, voltar ao desktop padrão desligando o
   fallback na tela de Configurações. Comparar WineD3D e DXVK somente
   com o driver Vulkan realmente suportado.
6. Validar teclado/mouse/touch, áudio, reentrada, importação de arquivos
   e persistência dos saves.

Sem logs do aparelho **não se pode provar** que a tela preta tinha
origem no `wfm.exe`; a correção cobre as falhas de arquivo e expõe
mais diagnóstico para outras causas como Wine, Box64, Vulkan ou prefixo.

## Próximas extrações reais de dependência

- Publicar `velowin-engine-api` com contratos de sessão e retorno
  de estado; o XServer/Winlator ficará atrás de um único adaptador.
- Separar o gerenciador de pacotes Wine/Box64 de `ContentsManager`
  num catálogo próprio com hashes, licenças e versionamento.
- Extrair input/áudio e painel para módulos compiláveis isolados,
  mantendo ABI e DLL paths.
- Regredir cada extração em CI e em aparelho Mali **antes** de mudar
  a `main`; nunca afirmar que FPS ou jogos foram testados só pelo
  compilador.

Licenciamento: Winlator LGPL-2.1, WinNative GPLv3, Wine,
Box64, Mesa, DXVK, VKD3D-Proton e componentes adicionais conforme
suas licenças individuais; código próprio VeloWin parte do
produto GPLv3 combinado.
