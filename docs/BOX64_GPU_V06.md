# VeloWin 0.6 — Box64 no APK principal e testes reais de Vulkan WSI

## Integração Box64 Bionic

O workflow **VeloWin Android** agora compila a revisão fixa de Box64 (`c6a8c4472f74f9ad204d35195f084b5f996b12e3`) para Android AArch64/Bionic usando o NDK 27.2. O ELF é validado com llvm-readelf, exigindo CPU AArch64, linker /system/bin/linker64 e libc.so Android. O binário auditado passa a fazer parte **do APK padrão** como lib/arm64-v8a/libvelobox64.so. Um checksum SHA-256 e aviso MIT do Box64 são disponibilizados como artefato.

O Box64 agora está *empacotado*, mas isso não comprova execução em um aparelho. O autoteste `box64 -v` na guia Sistema tenta executá-lo com timeout. Para Wine Bionic x86_64, o app também oferece `wine --version` quando um WCP GameNative foi preparado. O motor Windows ainda exige X11, wineboot, áudio, entrada, DLLs Direct3D e testes físicos.

## Testar no Android sem root

1. Abra GitHub Actions → VeloWin Android (branch main), baixe **velowin-v06-box64-debug**, extraia o ZIP e instale o APK.
2. No app, use **Sistema → Testar Box64 Bionic (incluído no APK)**. Registre código de saída, logs e diagnóstico.
3. Em Runtimes, baixe um WCP x86_64 Bionic com SHA-256 publicado, extraia o pacote e use **Testar Wine --version**. Isto pode falhar por dependências Bionic, mesmo com Box64 presente.
4. Em **Sistema → Testar apresentação Vulkan nativa (WSI)**, o laboratório cria uma VkSurfaceKHR real, cria um VkDevice com suporte a VK_KHR_swapchain, tenta criar uma VkSwapchainKHR e verifica o número de imagens. O laboratório destrói todos os recursos após o teste.
5. Use a seção GPU e APIs e o relatório compartilhável para registrar Mali, Adreno, Xclipse ou PowerVR e a disponibilidade de VkDevice e swapchain.
6. Teste controles virtuais/gamepad e AudioTrack; esses laboratórios ainda não alimentam aplicativos Windows.

## Política de drivers

**Mali, Immortalis, Xclipse e PowerVR:** manter o driver Vulkan do Android. **Turnip não é compatível com essas GPUs.** **Adreno:** Vulkan do Android por padrão; Turnip só quando existir uma build específica testada e com licença, ICD/ABI e GPU compatíveis. Turnip não está embutido no APK; o menu bloqueia seleção fictícia. Nenhum driver do sistema é substituído.

**DXVK e VKD3D-Proton** ainda não estão integrados como camadas Windows. A criação de VkDevice ou VkSwapchainKHR não prova compatibilidade com DXVK, Direct3D 12 ou Wine. Testar Vulkan WSI não significa renderizar frames de jogos: o laboratório ainda não envia command buffers, não chama vkQueuePresentKHR e não desenha janelas Wine.

## Próximos marcos

1. Validar Box64 e Wine -v em um telefone ARM64 real, depois implementar wineboot em prefixo privado.
2. Integrar um servidor X11 ligado à Surface Android, comprovando janela Windows real.
3. Mapear controles para XInput/Win32 e áudio Wine para Android.
4. Distribuir DXVK/VKD3D com ABI/licença/extensões verificadas.
5. Testar jogos e registrar resultados reais em Mali e Adreno, sem alegar ganhos de FPS sintéticos.

Fontes: Box64 (https://github.com/ptitSeb/box64), GameNative Wine (https://github.com/GameNative/proton-wine), DXVK (https://github.com/doitsujin/dxvk), VKD3D-Proton (https://github.com/HansKristian-Work/vkd3d-proton).
