# VeloWin: motor Bionic com filosofia de console

## Inspiração de UX, não cópia de marca

A referência é o fluxo de **biblioteca → jogar**, com instalação guiada, perfis automáticos por GPU, controle de versões e diagnósticos compreensíveis. Não copiar identidade visual, assets, código ou serviços privados do GameHub.

## Compatibilidade e decisões técnicas

- **Wine Bionic x86_64 + Box64 ARM64**: fluxo separado de Wine Linux/glibc.
- **Wine Bionic ARM64EC + FEXCore**: fluxo distinto; não tratar Box64 como substituto universal de FEX.
- **Rootfs**: Bionic não implica rootfs glibc; construir workspace Android privado com dependências Bionic, manifestos e paths fixos.
- **Display**: integrar servidor X11 Android/Surface, janelas e lifecycle; um EGL probe não substitui a ponte.
- **Áudio**: inicializar e testar Wine ALSA/PulseAudio → AudioTrack/AAudio, latência e retomada.
- **Controles**: mapear KeyEvent/MotionEvent, gamepad, toque e cursor para o servidor de janelas.
- **Vulkan**: preferir ICD nativo para Mali; Turnip somente em GPUs suportadas. Vulkan anunciado pelo Android não garante DXVK.
- **DXVK**: copiar DLLs apenas depois de verificar arquitetura, versão e dependências.
- **VKD3D-Proton**: validar Vulkan 1.3 e extensões necessárias antes de habilitar Direct3D 12.
- **Segurança**: versões e hashes fixos, sem executar arquivos WCP apenas por terem sido baixados; nunca executar binários glibc no pipeline Bionic.

## Implementação incremental

`BionicSessionPlan` modela os pré-requisitos por sessão e recusa combinações incoerentes. É uma camada **de validação**, não um lançador. A execução real exige integração de binários ARM64, X11, áudio e input; os testes JVM validam as combinações, mas não demonstram execução de jogos.

## Projetos de referência

- https://github.com/utkarshdalal/GameNative
- https://github.com/ptitSeb/box64
- https://github.com/FEX-Emu/FEX
- https://github.com/GameNative/proton-wine
- https://github.com/doitsujin/dxvk
- https://github.com/HansKristian-Work/vkd3d-proton

Acompanhar licenças de cada componente antes de incluir binários no APK.
