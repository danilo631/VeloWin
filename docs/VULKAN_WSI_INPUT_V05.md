# VeloWin 0.5 — Vulkan WSI Android + controles de console

Esta etapa adota os princípios de **Mobox** (componentes intercambiáveis e diagnóstico), **Winlator** (configurações por título) e **GameHub** (experiência guiada e foco na biblioteca) usando apenas código próprio de aplicativo e respeitando as licenças de dependências.

## Laboratório Vulkan nativo: o que verifica realmente

1. Abre uma **SurfaceView Android**, não uma janela Wine.
2. JNI usa **ANativeWindow_fromSurface** e cria uma **VkInstance** com \`VK_KHR_surface\` e \`VK_KHR_android_surface\`.
3. Cria uma **VkSurfaceKHR** ligada à Surface Android.
4. Procura uma GPU com fila \`VK_QUEUE_GRAPHICS_BIT\` que suporta **present** nessa superfície.
5. Consulta \`VK_KHR_swapchain\`, os formatos de superfície, modos de apresentação, dimensões e limites de imagens.
6. Destrói a VkSurfaceKHR, a VkInstance e libera a ANativeWindow mesmo em falhas.

**Não cria swapchain, não envia comandos de renderização, não inicializa DXVK ou VKD3D e não desenha janelas Wine.** Uma GPU pode passar essa verificação e continuar incompatível com as extensões necessárias a determinados jogos.

### Testar no telefone

Abra **Sistema → Testar apresentação Vulkan nativa (WSI)**. Se Vulkan estiver disponível, observe GPU, suporte de fila de apresentação, quantidade de formatos e modos. Dispositivos sem Vulkan ou sem extensão obrigatória devem relatar a indisponibilidade, sem travar o aplicativo. Repita com mudança de orientação da tela e após alternar entre aplicativos para avaliar o ciclo de vida.

## Laboratório de controle virtual + gamepad

Novo laboratório Android **Sistema → Testar controles virtuais e gamepad**:

- Desenha joystick e botões A, B, X, Y, LB, RB, START e BACK.
- Aceita **multitouch**: cada dedo atualiza o conjunto total de botões pressionados.
- Recebe botões físicos de gamepad USB/Bluetooth pelos \`KeyEvent\` Android.
- Recebe analógicos, gatilhos e hat-switch pelos \`MotionEvent\` Android.
- Converte eventos para uma estrutura Java \`ControllerState\` semelhante a XInput, com bitmasks de botões, analógicos normalizados, deadzone de 0,16 e gatilhos no intervalo 0–1.
- Mantém fontes físicas e touch separadas, combinando seus estados sem liberar botões da outra fonte por engano.
- Libera os estados ao sair para o background e limita valores inválidos como \`NaN\` e infinito.

**Não injeta eventos Win32, X11 ou Wine.** A estrutura já facilita conectar um futuro mapeador de teclado/mouse/XInput no backend, que precisará de implementações e testes específicos. Não requer root, permissão de acessibilidade ou overlays sobre outros aplicativos.

### Como validar

1. Teste um dedo no joystick e outro no botão A. Ambos devem aparecer simultaneamente.
2. Levante um dedo; apenas os estados desse contato devem deixar de ser marcados.
3. Conecte um gamepad, pressione A/B, mova os dois analógicos, D-pad e gatilhos.
4. Misture touch + gamepad. Os eventos de um não devem remover os estados do outro.
5. Alterne para outro aplicativo e volte: botões não devem permanecer pressionados.

## Código, segurança e CI

- \`app/src/main/cpp/vulkan_wsi.c\`: WSI de consulta com Vulkan NDK.
- \`app/src/main/java/dev/velowin/app/hardware/VulkanWsiLabActivity.java\`: SurfaceView e lifecycle.
- \`app/src/main/java/dev/velowin/app/engine/ControllerState.java\`: modelo multiplataforma, Java puro, snapshots imutáveis.
- \`app/src/main/java/dev/velowin/app/hardware/AndroidGamepadMapper.java\`: eventos de controles Android.
- \`app/src/main/java/dev/velowin/app/hardware/VirtualControllerLabActivity.java\`: desenho e multi-toque do controle.
- \`tests/ControllerStateTest.java\`: regressões para flags, fusão touch/físico, deadzone, gatilhos e liberação.
- \`tests/test_v05.py\`: presença/isolamento JNI, Activity, manifesto e fluxo de entrada.
- GitHub Actions compila C nativo (NDK) e Android; isso **não substitui testes em hardware real**.

## Próximos requisitos para um jogo Windows

- Completar fluxo Box64 → Wine Bionic e executar \`wineboot\` em Wineprefix isolado, com supervisor de processos e logs.
- Integrar display X11/Windows a uma Surface Android. O laboratório WSI não cria um X11 server.
- Fazer ponte real áudio Wine → Android e eventos \`ControllerState\` → Win32/X11/XInput.
- Integrar DLLs DXVK/VKD3D e drivers Vulkan apropriados por GPU, com versões fixas, licenças e validação de extensões.
- Rodar programa Windows mínimo em ARM64 real com janela, áudio e input; depois jogos de referência Mali e Adreno.

**Não trate VeloWin 0.5 como emulador de jogos pronto.** Nesta versão os laboratórios têm funções reais do Android, porém faltam as pontes Windows e o motor completo.
