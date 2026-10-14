# VeloWin v16 — VeloFlow nativo + compatibilidade Mali

Esta revisão reduz **dependências compiladas** do motor WinNative, sem
substituir XServer, Wine e Box64 por telas falsas.

## Mudanças reais

- **O motor de shaders Lossless Scaling/LSFG foi removido da build**:
  `tools/strip-lsfg.py` tira os ~15+ arquivos nativos da árvore de build,
  remove os vínculos CMake de `lsfg/*`, exclui seu tradutor DXBC e
  desativa o instalador Java de DLL, o menu LSFG e a ativação por configurações
  antigas. Não são redistribuídos shaders comerciais.
- O XServer e o compositor Wayland ainda têm chamadas para a antiga API
  `VkrLsfg`; um adaptador minúsculo **criado pelo VeloWin** devolve
  `unsupported`/nenhum quadro, evitando reescrever de uma só vez o motor
  enorme de `vk_renderer.c`. Isso **não é LSFG funcional**. Uma próxima
  etapa deve remover também os tipos e funções legados do core.
- FrameGen operacional: **OpenFlow / DIS Vulkan** da comunidade WinNative
  e qwertypower, com integração, configurações e seleção de perfis VeloWin.
  Não há alegação de que todo o algoritmo de optical flow foi escrito pelo
  VeloWin; o VeloFlow original nesta revisão é a orquestração, integração,
  regras de compatibilidade e retirada do backend legado.
- **Perfil Mali independente** (`MaliProfile.java`): seleciona X11,
  wrapper de sistema, WineD3D, 1280x720 e `mesa_glthread=false`;
  evita instalar Turnip/Adreno, desativa FrameGen até teste da GPU e
  preserva as configurações anteriores para reversão. Box64/FEX,
  memória e Vulkan não recebem falsos overrides globais.
- A interface própria VeloWin oferece o botão de **perfil Mali** por
  contêiner, aplicar e restaurar, com mensagens de aviso.
- Testes puros JVM para idempotência, fallback e restauração, Python
  para contratos do código fonte, CI compilando XServer nativo sem LSFG.

## Base técnica e créditos

- **Winlator** (LGPL-2.1) — estrutura de contêiner, X11/Surface/Wine.
- **WinNative** (GPL-3.0) — código do motor efetivamente
  compilado, com submódulo e revisão fixos.
- **Mobox** (código/scripts sob os termos do repositório): inspiração
  para perfis por jogo, componentes Wine e fallback gráfico; não
  incorporamos scripts Termux, proot ou invocação root.
- **winlator_mali** — referência de compatibilidade via VirGL/WineD3D,
  variáveis conservadoras e escolha explícita de wrapper.
- **Mesa PanVK para Mali/kbase** — projeto comunitário experimental
  (FristOneRR-Panvk-Driver). **Não instalado automaticamente**:
  compatibilidade depende de geração de GPU, versão do kernel/kbase,
  wrapper Vulkan e hardware. PanVK beta não é Turnip, nem um driver
  universal Mali. Cada release exigirá licenciamento e verificação
  de hash antes de entrar no gerenciador.
- **OpenFlow/DIS**, qwertypower e demais autores: crédito e GPL
  preservados no fonte do motor e nos avisos do APK.
- **Termux:X11** — estudado como arquitetura alternativa, mas
  não distribuímos uma segunda pilha XServer que disputaria a Surface.

Esta revisão é uma **redução real e verificável da dependência de LSFG**
e introduz políticas próprias. Ela ainda não desacopla todo o monolito
WinNative. Dividir o XServer e Wine em bibliotecas independentes exige
isolar API, JNI, manifests e recursos e deve ser validado com APK e
jogos a cada etapa. Não inventar independência onde ainda há código
fonte upstream.

## Galaxy A34 5G / Mali

Começar por **Mali compatibilidade**, WineD3D e 1280x720. Se
`wine cmd /c ver`, `wine notepad` e janelas funcionarem, testar
DXVK Vulkan com o wrapper real da GPU — não assumir extensões
Vulkan por nome de SoC. OpenFlow usa compute e pode ser pesado
para GPUs Mali, por isso deve permanecer opcional e não deve ser
ativado apenas por detecção genérica de Vulkan.

O perfil salva o driver, wrapper DX, env, resolução e estado DIS.
Ao restaurar, recupera esses valores. A antiga opção proprietária
LSFG permanece desligada, mesmo que um perfil antigo a tenha usado.

## Testes de aceitação

1. GitHub Actions: verificar testes Java/Python, CMake sem o
   diretório LSFG, `libwinlator.so` compilada e APK com UI VeloWin.
2. No telefone: criar contêiner, entrar no desktop Windows,
   abrir/mover Notepad, fechar e reentrar; testar gamepad/som.
3. Aplicar e restaurar Mali, comparar logs e selecionar DXVK
   apenas se driver Vulkan suportar o jogo.
4. Ativar OpenFlow/DIS e comparar **quadros reais e interpolados
   apresentados** e latência. FPS alvo não comprova frame generation.
5. Testar saída, recuperação de prefixo e saves sem alterações.
6. Relatar falhas com logs e qual driver foi selecionado.

Os APKs de CI provam compilação, não hardware.
A `main` deve ser preservada até testes físicos.
