# VeloWin 0.8 — diagnóstico guiado Wine e pontes X11 locais

## O que foi integrado

O APK principal continua compilando **Box64 Bionic ARM64** do commit upstream fixado e empacota um programa Windows PE de console compilado do código-fonte do projeto. Agora o console possui:

1. **Diagnóstico guiado** (`diagnose`): testa sequencialmente **Box64 -v → Wine --version → template de Wineprefix → Wine cmd /c ver → Win32 Smoke**. Cada etapa usa processo real e código de saída. O teste Win32 só recebe confirmação se o processo terminar com código 0, sem cancelar/expirar, e produzir a linha exata `VELO_WIN32_SMOKE_OK`.
2. **Ponte local X11 opcional**: configuração de `DISPLAY=127.0.0.1:0`, `:1` ou `:2`, habilitada por sessão. Exige **servidor X11 TCP externo** em execução **no mesmo telefone**. VeloWin não contém um servidor de janelas X11 e não mostra janelas Wine internamente.
3. **Ponte local PulseAudio opcional**: envia `PULSE_SERVER=tcp:127.0.0.1:4713` ao processo Wine. Exige servidor PulseAudio externo em execução. O teste AudioTrack integrado ao Android **não** equivale a uma ponte Wine/Windows.
4. **Teste de portas** (`bridge status`): verifica conexões TCP de loopback com timeout curto. Uma porta aberta **não** confirma autenticação/protocolo X11/PulseAudio nem compatibilidade com Win32.
5. As opções X11/áudio são persistidas em preferências privadas por contêiner; nenhum host remoto arbitrário é aceito. Um contêiner sem jogo usa suas próprias preferências separadas das de cada jogo.
6. **Console e logs** continuam restringindo comandos, exigindo consentimento para executar Wine/Win32 no teste guiado e oferecendo cancelamento e compartilhamento do diagnóstico.

## Testar no Android sem X11

1. Instale o APK da execução verde de `main` no [GitHub Actions](https://github.com/danilo631/VeloWin/actions/workflows/android.yml), artefato **`velowin-v08-bionic-bridge-debug`**.
2. Acesse **Sistema → Box64 -v**. O código 0 confirma apenas que Box64 inicia.
3. Em **Runtimes**, baixe e prepare um WCP **Wine Bionic x86_64** e confira a integridade SHA-256.
4. Acesse **Runtimes → Abrir console de contêiner (sem jogo)** ou abra um jogo da **Biblioteca → Entrar no contêiner**. Se entrar por um jogo, selecione o pacote Wine Bionic e Box64 no perfil primeiro.
5. Pressione **Diagnóstico guiado**, leia o aviso e autorize. A sequência cria o template do prefixo privado se necessário, mas **não executa wineboot** nem executa o EXE do jogo.
6. Se o diagnóstico parar em Wine/CMD/Win32, use **Compartilhar log** para obter a mensagem exata, incluindo o primeiro linker/ABI/X11/DLL ausente. Para encerrar processos Wine eventualmente restantes, pressione **Encerrar Wine** (`wineserver -k`).

**Como interpretar:** sucesso do programa de teste Win32 comprova somente que o PE específico executou em modo console naquele aparelho. Não comprova Windows desktop, DirectX, áudio nem jogos.

## Experimentar interface gráfica com servidor X11 externo

Somente se você tiver instalado e iniciado um **servidor X11 compatível que aceite TCP IPv4 em loopback** no mesmo Android, por sua própria conta:

1. Verifique em qual display o servidor escuta, por exemplo `:0` na porta TCP 6000 ou `:1` na porta 6001.
2. No console do VeloWin, toque **Configurar X11 local** e escolha o display correspondente.
3. Pressione **Testar portas locais**. O teste apenas verifica se há uma conexão TCP possível.
4. Caso seu servidor ofereça PulseAudio TCP em loopback na porta 4713, habilite separadamente **Áudio Pulse local**. Não habilite sem um serviço confiável.
5. Use **CMD /c ver**, **Win32 Smoke** e, conscientemente, **Testar EXE** para observar comportamento. **Janelas só podem aparecer no servidor X11 externo**, nunca diretamente no console do VeloWin nesta versão.

O Wine do WCP precisa conter bibliotecas gráficas adequadas. X11 pode usar autenticação e negar a conexão mesmo com a porta aberta. Alguns servidores Android implementam apenas sockets Unix ou mecanismos próprios: nesses casos, o modo TCP não é compatível e deve permanecer desativado. Nunca exponha servidores X11/PulseAudio pela rede Wi-Fi sem autenticação; o VeloWin restringe os valores configurados a `127.0.0.1` mas não controla a configuração do aplicativo servidor.

## Limites reais

- Não há XServer nativo dentro do APK nem desenho da janela Wine em SurfaceView; o laboratório Vulkan WSI é independente de Wine.
- Não existem drivers Turnip, DXVK/VKD3D-Proton ou FEX funcionais no APK principal.
- Não há virtualização ou sandbox de kernel. Processos de terceiros executam com as permissões do app.
- O teste de processo é limitado no tempo e cancela o filho direto; processos Wine descendentes podem sobreviver.
- O console não monta jogos com DLLs/assets da pasta original: o botão **Testar EXE** copia somente um `.exe` selecionado via SAF.
- A opção de FPS/resolução do perfil do jogo ainda não altera o motor de renderização Windows.
- CI compila o APK e os testes Java; a execução Wine/servidor X11 exige testes em Android físico.

## Código relevante

`BridgeProfile`, `LocalBridgeManager`, `ContainerSession`, `Win32ProbeVerdict` e `ContainerLabActivity`. Consulte [roadmap](ROADMAP.md) para o trabalho necessário à experiência de console integrada.
