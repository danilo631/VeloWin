# VeloWin — Central de Componentes e camadas Direct3D (desenvolvimento)

Esta branch empilha as melhorias do PR #6 (Wine Bionic) e #7 (navegador privado).
Não habilita jogos por si só. O aplicativo mantém o ID `dev.velowin.app`
e a versão interna 0.8.0-dev enquanto aguarda validação física.

## Instalação direta dentro do aplicativo

Abra **Runtimes → Central de Componentes**. Os catálogos DXVK, VKD3D-Proton e
Termux:X11 consultam **exclusivamente a API de releases oficiais do GitHub**,
aceitam somente URLs formadas pelas políticas de origem, recusam releases
sem SHA-256 declarado, calculam novamente SHA-256 de todos os bytes baixados
e aplicam limites de tamanho antes da extração.

- **DXVK:** `doitsujin/dxvk`. O arquivo Windows `dxvk-X.Y.Z.tar.gz`
  é extraído em pasta privada, filtrando somente DLLs Windows PE x86/x64
  reconhecidas (não instala binários Linux nativos). Para ativação por jogo,
  no menu **Configurações → Instalar/remover DLLs gráficas**, o usuário autoriza
  cópia de arquivos PE **x64** para a pasta privada desse jogo e seleciona
  o perfil DXVK. O comando `run exe` passa a definir
  `WINEDLLOVERRIDES` exclusivamente para DLLs que o manifesto instalado
  comprovar como inalteradas. Outros jogos, prefixos e system32 não são
  modificados; qualquer colisão com DLL de usuário é recusada.
- **VKD3D-Proton:** `HansKristian-Work/vkd3d-proton`. A release oficial
  `tar.zst` é inspecionada como PE x64. Mesmo com Vulkan 1.3, várias extensões
  de GPU/driver podem faltar. A preparação de `d3d12.dll` e
  `d3d12core.dll` não atesta D3D12 em funcionamento.
- **WineD3D:** acompanha a distribuição Wine Bionic e é o padrão quando não
  há injeção. Usa OpenGL, que ainda depende da integração Wine/X11; não
  é um driver Vulkan nem pacote Mesa.
- **WineHQ:** o código oficial Wine não deve ser confundido com artefatos
  Android. **Wine Bionic** utilizável no pipeline Box64 continua no catálogo
  GameNative já existente em Runtimes. Pacotes Linux/glibc ficam isolados.
- **Mesa:** Mesa é uma coleção de drivers/implementações. Não existe
  "Mesa genérico" que possa substituir com segurança o driver Vulkan do
  Android. O VeloWin preserva o Vulkan do sistema para Mali/Xclipse/PowerVR
  e não oferece Turnip para GPUs incompatíveis. O suporte Android
  Adreno/Turnip exigirá binários Bionic corretos, loader/ICD e testes físicos.
- **Termux:X11:** pacote `termux-x11-universal-debug.apk` do release nightly
  oficial, verificado por hash, oferecido ao instalador Android com permissão
  temporária de leitura. Ele é um app **separado**, GPLv3. Instalar não
  significa que Wine do VeloWin já consegue acessar seu servidor:
  o Termux:X11 normalmente usa socket Unix no UID privado do Termux.

## Testar primeiro uma janela Wine

O console do contêiner agora suporta **`wine notepad`**, com confirmação.
É necessário preparar o prefixo, configurar X11 TCP em loopback e receber
resposta válida X11 (versão 11, status ACCEPTED). O comando lança Wine
Notepad pela pilha Box64/Bionic e retorna o log; ele **não prova**
apresentação numa Surface do VeloWin e não força TCP do Termux:X11.

Sequência recomendada no dispositivo:

```text
box64 -v
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

Se `bridge status` não detectar um servidor X11 real configurado para
TCP local, o comando da janela deve falhar de forma explícita.

## Próxima integração: servidor gráfico nativo no mesmo UID

**Estado atual:** ainda não há XServer embutido, renderizador Wine em
Surface Android, entrada X11 funcional ou áudio Wine. Isso exige mais
do que um pacote baixado. A implementação arquitetural mais viável é
uma variante do XServer GPLv3 do Termux:X11 (Xorg/NDK) ou uma integração
do stack XServer/renderer/conector do Winlator, com as bibliotecas
correspondentes, ciclo de vida, sockets UNIX privados e política de
licenças correta.

A base Termux:X11 original foi examinada em
`fa3a8b430e2896a19f44c99a9cb056254615ae06` e o Winlator-app em
`3981d86efa4f333b2a34a7da8b6521476cd8c8b9`.
Não se pode simplesmente copiar `libXlorie.so` sem portar a biblioteca
Android, os serviços, o JNI, o teclado/Surface e as dependências Xorg.
A redistribuição de código GPLv3 no APK combinado exige cumprimento
do copyleft e entrega dos fontes correspondentes. O Winlator-app é
LGPL-2.1 no arquivo principal, com dependências de licenças variadas.
O VeloWin não alega ter importado fontes desses projetos nesta branch.

## Limitações reais

- Instalar DXVK/VKD3D **não** substitui a GPU/driver do telefone.
- A importação atual do jogo copia apenas um EXE; diretórios completos de
  instalação e assets grandes ainda exigem integração adicional.
- Somente jogos PE x86_64 podem receber DLLs gráficas na implementação atual;
  x86/Box86 e FEX são pipelines separados.
- DXVK e VKD3D dependem da capacidade Vulkan real e de um servidor gráfico.
- Overlays são por jogo: não sobrescrevem DLLs existentes, não alteram
  prefixos Wine, e só podem ser removidos quando o checksum bate com o
  registro de instalação. Não apaga saves nem arquivos desconhecidos.
- APK debug instalado com outra chave de assinatura pode exigir backup antes
  de desinstalar a versão anterior, para preservar dados privados.
- SHA-256 publicado verifica os bytes da origem selecionada; **não equivale
  a uma assinatura de código independente**.

## Créditos e origem

- WineHQ: https://www.winehq.org/ e https://gitlab.winehq.org/wine/wine
- GameNative Wine Bionic: https://github.com/GameNative/proton-wine
- DXVK: https://github.com/doitsujin/dxvk
- VKD3D-Proton: https://github.com/HansKristian-Work/vkd3d-proton
- Termux:X11 (GPLv3): https://github.com/termux/termux-x11
- Winlator e XServer (LGPLv2.1 + componentes): https://github.com/brunodev85/winlator-app
- Mesa: https://mesa3d.org/
- Box64: https://github.com/ptitSeb/box64

Consulte os avisos e arquivos de licença de cada pacote antes de
redistribuí-lo. Esta branch não distribui fontes nem imagens proprietárias.
