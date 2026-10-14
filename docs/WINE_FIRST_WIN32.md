# Primeiro Win32 no VeloWin — auditoria e logs preservados

Esta etapa aprimora a versão **0.8.0-dev** sem anunciar compatibilidade com jogos.

## Mudanças verificáveis por código

- Novo comando e botão **Auditar Wine** (`wine preflight`). Lê arquivos já instalados no dispositivo e confere o ELF x86_64 com interpretador Bionic de `bin/wine` e `bin/wineserver`; confirma diretórios de módulos e o template, sem executar programas do pacote.
- O console e o teste `Wine --version` compartilham a mesma construção de ambiente (`WineLaunchEnvironment`). `PATH` começa pelo `bin` do WCP Bionic, sem encaminhar o PATH de Termux, Linux/glibc ou binários importados. `WINELOADER` e, quando presente, `WINESERVER` apontam para a instalação escolhida. `BOX64_LD_LIBRARY_PATH` aponta para as bibliotecas x86_64 do WCP. Não configuramos `LD_LIBRARY_PATH` do linker ARM64 do Android.
- `BoundedProcess` continua sem shell e drenando stdout/stderr, mas retém o início e o **fim** da saída (até 8 KiB em cada extremidade). A saída intermediária omitida é identificada. Isto evita perder marcadores ou erros finais quando Wine gera muitas mensagens.
- O diagnóstico guiado passa a informar a auditoria de arquivos antes de iniciar `wine --version`, sem substituir testes reais. Ele **não** executa automaticamente `wineboot`.

## Testar no Android

Instale o APK de uma execução **verde** do workflow Android desta branch; o APK ainda mantém a versão 0.8.0-dev. Entre em **Runtimes**, instale um WCP **x86_64 Bionic** e abra **Contêiner de diagnóstico** (sem jogo), ou entre pelo jogo correspondente.

Execute, nesta ordem, no console de comandos permitidos:

```text
status
wine preflight
box64 -v
wine --version
prefix prepare
wineboot -u
wine cmd /c ver
win32 test
wineserver -k
```

- `wine preflight`: cole a saída inteira. **SIM** indica somente que o ELF foi encontrado e possui ABI/linker compatíveis.
- `wine --version`: anote código, duração e saída final; não significa que CMD ou prefixo funcionam.
- `wineboot -u`: autorização explícita; pode modificar o prefixo, falhar sem X11 ou deixar processos filhos ativos. O estado `TEMPLATE_ONLY` **não** é promovido a `INITIALIZED` por este teste.
- `wine cmd /c ver`: cole retorno integral e mensagens de linker/depêndencias.
- `win32 test`: apenas código 0 **e** linha exata `VELO_WIN32_SMOKE_OK` confirmam este pequeno programa PE de console. Prova de programa Windows inicializado, **não** prova janela/áudio/jogos.
- `wineserver -k`: encerramento solicitado; não demonstra término de todos os processos que Wine possa ter criado.

Use **Compartilhar log** e envie o arquivo completo da execução. Se houver truncamento, a saída informa a região omitida. Não cole dados sensíveis de pastas de jogos nem arquivos pessoais.

## Bloqueio gráfico separado

O fluxo gráfico atual depende de uma ponte de X11 externa opt-in: o `bridge status` verifica apenas protocolo/porta em loopback. Nenhum `SurfaceView` exibe uma janela Wine ainda. A referência mais viável para *pesquisa* é o servidor Android/NDK do [Termux:X11](https://github.com/termux/termux-x11) (GPLv3), mas o projeto usa seu app e pacote companion próprios, normalmente com Unix sockets. Integrar o servidor ao VeloWin requer engenharia de ciclo de vida, IPC, autenticação, renderização, entrada e conformidade GPLv3; não basta copiar a UI nem ativar `DISPLAY`. O suporte TCP atual também não significa que o Termux:X11 padrão exponha a porta 6000.

Não há testes físicos neste PR. Somente CI Android e testes Java/estáticos são automatizados. DXVK/VKD3D, áudio Wine e integração real de jogos continuam pendentes.
