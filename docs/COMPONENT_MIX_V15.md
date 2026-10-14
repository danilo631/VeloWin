# VeloWin v15 — integração modular dos projetos da comunidade

## Objetivo corrigido

A v14 empacotava o aplicativo WinNative quase sem mudanças. Não era o
produto que o VeloWin deveria ser. A v15 usa a base de emulação como
**motor de terceiros**, mas substitui a Activity launcher por uma
**interface original** do VeloWin e mantém regras de perfil independentes
no repositório principal. O submódulo upstream é fixado e pode ser
atualizado após revisão técnica de API e licenças.

| Camada | Módulo efetivo | Autor/origem |
| --- | --- | --- |
| Home e criação de contêiner | `overlays/velowin-app/VeloWinHomeActivity.java` | VeloWin original, UX inspirada nos launchers de jogos, incluindo GameHub |
| Seleção de FrameGen | `FrameGenPolicy.java` | VeloWin original |
| Contêiner, wineprefix, Wine, Box64/FEX | `ContainerManager`, `ContainerCreation`, `XServerDisplayActivity` e ImageFS | WinNative, derivado de Winlator/GameNative; créditos na árvore fonte |
| X11, render, entrada e áudio | `XServerComponent`, `VulkanRenderer`, `XServerDisplayActivity` e serviços associados | Winlator/WinNative e colaboradores |
| FrameGen sem shader comercial | `winlator/vk/framegen` e `winlator/vk/lsfg` (OpenFlow/DIS) | WinNative + autores OpenFlow; o código faz parte do componente upstream |
| DXVK/VKD3D/WineD3D/Mesa | Gerenciador de componentes do runtime | upstream (veja os avisos de licença de cada pacote) |
| Termux:X11 | Repositório pesquisado; **sem** componentes no APK v15 | Termux:X11 GPLv3, referência para futuras experiências independentes |

Não copiamos GameHub ou redistribuímos seu código/propriedades.
Os padrões de experiência de usuário (biblioteca, iniciar rápido e perfis
por jogo) são ideias, não uma alegação de integração comercial.

## Como as partes se conectam realmente

1. A Activity `VeloWinHomeActivity` é compilada junto aos fontes Android
   da revisão upstream, mas é mantida no diretório do VeloWin; o script
   troca o `MAIN/LAUNCHER` no manifesto, deixando o menu avançado
   WinNative como uma Activity interna.
2. `ContentsManager.syncContents()` fornece componentes locais;
   `ContainerCreation.newestInstalledRuntime` seleciona Wine/Proton
   instalado; `createContainerAsync` cria um prefixo verdadeiro.
3. **Entrar no contêiner** lança `XServerDisplayActivity` com
   `container_id` do contêiner existente, sem comando de shell, sem
   contornar o fluxo integrado de ativação de ambiente.
4. O perfil DIS grava `disFrameGen=1`, `frameGen=0` e
   `disFrameGenTargetFps` no próprio contêiner. O renderer upstream
   lê esses valores na sessão, configura OpenFlow/DIS e usa Vulkan.
   A UI exige ao menos a declaração Vulkan no Android; o renderer
   também precisa validar suporte real da GPU.
5. O perfil padrão para o VeloWin em GPU Mali é X11. Não ativamos
   Turnip em Mali nem Wayland de Adreno automaticamente.

## Fluxo testável (dispositivo Android)

Primeiro instalar o APK v15 **da compilação aprovada**, abrir o
assistente de ImageFS e instalar Wine/Box64. Testar:

- Criar contêiner, sair da tela, retornar e conferir persistência.
- Tocar **Entrar no contêiner** e observar desktop Wine renderizado
  (prova requer tela visível e controles funcionando, não só PID).
- Abrir uma janela Notepad e mover/fechar com toque, mouse e teclado.
- Configurar **OpenFlow/DIS** desligado e ligado num jogo com movimento
  conhecido, comparar GPU, FPS real, quadros apresentados, consumo,
  latência, estabilidade e eventuais artefatos.
- Trocar DXVK/WineD3D no menu avançado apenas com o componente correto
  e comparar logs de inicialização Vulkan; testar som e gamepad.
- Testar sair, reabrir, manter jogos/saves e reverter FrameGen. Fazer
  cópia dos dados antes de trocar APKs de assinaturas diferentes.

## Integridade do código e licenças

Código original VeloWin: `overlays/velowin-app`, inserido na build pelo
`tools/prepare-winnative.py`. Upstream: WinNative GPLv3 fixado em
`8409085f706acbc0749881d9bf1e85f2f347dfd4`, seus submódulos
Git e objetos LFS. `WinNative-CREDITS.md` e
`WinNative-EMULATOR_CREDITS.md` são incluídos no APK com GPL integral;
`LICENSE-NATIVE.md` identifica as obrigações do produto combinado.
Alterações posteriores devem preservar os respectivos avisos dos
componentes Winlator LGPL-2.1, Termux:X11 GPLv3 (não embutido), DXVK,
VKD3D-Proton, Mesa e FrameGen OpenFlow.

**Shaders LSFG comerciais não fazem parte do VeloWin**. O motor upstream
contém código para ler shaders da cópia própria do usuário, mas o
painel original do VeloWin oferece inicialmente apenas DIS, que não
precisa deles.

## Limitações

Uma build verde comprova a compilação e os contratos; não pode atestar
hardware, Wine/Box64 x86_64, Direct3D, FPS interpolado ou jogo específico.
Não há garantia de FrameGen em todas as GPUs Vulkan, especialmente Mali
sem extensões adequadas. O nome do FPS selecionado é **meta de saída**,
não taxa medida. Só publicar como funcional após evidência em hardware.
