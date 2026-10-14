# VeloWin v17 — Runtime Backends, VeloFlow e direção modular

## Arquitetura-alvo: VeloWin, não um clone do WinNative

A v17 mantém a interface original `VeloWinHomeActivity` e incorpora **duas
novas unidades originais** em `overlays/velowin-app`:

1. `VeloRuntimePlan`: política **Java pura** (sem Context/Android/WinNative)
   que responde quais modos podem rodar em um contêiner concreto.
2. `VeloRuntimeBridge`: único adaptador da nova política para
   `Container`, `ContentsManager`, `ImageFs` e `LinuxRuntime`.
   Ele não converte Wineprefix nem altera caminhos sem validação.

Os módulos substituem o acoplamento de seleção de runtime da Home à
implementação concreta upstream, sem anunciar que o XServer, Wine e Box64
já foram removidos da base legada. O próximo passo real de desacoplamento
é uma ABI/IPC estável que permita carregar motores de contêiner
separadamente. Até isso estar testado, a distribuição continua sujeita
à GPLv3 correspondente e ao código-fonte fixado.

```text
VeloWin (UI própria / GameHub-style)
  │
  ├── velowin-runtime-policy/        [agora VeloRuntimePlan.java: código independente]
  │     ├── BIONIC_BOX64            [Wine x86_64 existente]
  │     ├── BIONIC_ARM64EC_FEX      [Wine ARM64EC + FEX existente]
  │     ├── GLIBC_PROOT_GAMESCOPE   [LinuxFS/PRoot já instalado]
  │     └── EXTERNAL_GLIBC          [reservado, não habilitado]
  │
  ├── velowin-runtime-adapter/       [agora VeloRuntimeBridge.java]
  │     └── Winlator/WinNative JNI + XServer + ContainerManager
  │
  ├── velowin-framegen/
  │     ├── FrameGenPolicy.java     [original: Hz e temperatura]
  │     ├── veloflow_pacing.h       [original C11: teto físico]
  │     └── OpenFlow/DIS Vulkan     [código GPL de terceiros, mantido]
  │
  ├── velowin-gpu/
  │     └── MaliProfile.java        [reversível, sem Turnip]
  │
  └── Wine/Box64/FEX/PRoot/LinuxFS  [motores de terceiros; não reinventados]
```

## Backend de execução, com condições verificáveis

| Caminho | Checagem | Ação |
| --- | --- | --- |
| **Bionic + Box64** | ImageFS instalado, binário ou pacote Box64, Wineprefix **x86_64** | Define emulador Box64 para x86 e x86_64 e X11, salva no próprio contêiner |
| **Wine ARM64EC + FEX** | ImageFS, pacote FEXCore e prefixo previamente criado como **arm64ec** | Define FEX para emulação x86 do Wine ARM64EC, sem embrulhar o Wine ARM64 nativo no Box64 |
| **Linux glibc + PRoot + Gamescope** | Contêiner **já Linux**, LinuxFS/Gamescope instalado, executáveis PRoot e loader; não pressupõe driver para Mali | Reutiliza o backend de sessão Linux já existente; nunca transforma um prefixo Bionic em glibc |
| **glibc externo separado** | Nenhum provedor de rootfs verificado nesta revisão | **Não pode ser selecionado**; apenas um ponto explícito de extensão futura |

Isto evita uma das falhas recorrentes da comunidade: executar ARM64EC
como x86_64 via Box64, ou supor que um rootfs PRoot instalado é um Wineprefix.
A interface mostra por que um backend está indisponível, preserva os dados
e mantém acesso às configurações avançadas do motor.

### Implementação futura do glibc externo

Exige pacote rootfs ARM64 com checksum/licença, loader glibc,
Wine da ABI correta, proc/sys/dev binds privados, launcher sem shell,
redirecionamento de X11 + áudio, teardown de processos e testes de
instalação/rollback. **Não** oferecemos um botão habilitado sem isso.
LinuxFS do motor já é um caminho glibc/PRoot concreto, condicionado ao
driver GPU real.

## VeloFlow v17: eficiência e comportamento térmico

O algoritmo DIS/OpenFlow permanece open source upstream; o
`veloflow_pacing.h` é uma **nova política escrita para o VeloWin**,
testável no host C11 e integrada com patches estritos:

- Usa a **frequência do painel** (e não um suposto FPS de saída) para
  limitar FPS-alvo. Não gera acima do refresh real.
- Alvo zero segue adaptativo e considera a taxa de quadros-fonte
  informada ao apresentador. Sem fonte conhecida, não inventa uma taxa.
- Impõe um teto conservador de 240 Hz e rejeita Hz inválidos.
- `FrameGenPolicy.safeTarget` recebe a temperatura Android no momento
  da seleção: limita a 60 FPS sob condição térmica moderada e a 30 FPS
  sob condição severa; a seleção adaptativa continua dinâmica.
- O patch injeta a política nos caminhos C da apresentação DIS e
  no renderizador X11 Vulkan, não apenas na UI.
- O LSFG removido na v16 **não retorna**. Shaders comerciais não são
  baixados ou redistribuídos. O usuário pode desativar o DIS.

**Limitação:** a consulta térmica da UI é instantânea. Se a temperatura
mudar durante a sessão, o limitador nativo respeita o painel, mas ainda
não recebe eventos térmicos contínuos. Integração posterior deve usar
Thermal API / ATemperature ou callbacks Android e histérese para não
oscilar nos frames.

## Pesquisa da comunidade (base das decisões)

- r/EmulationOnAndroid: relatos Mali/Exynos sobre Bionic + wrapper de
  sistema, WineD3D/VirGL vs DXVK e compatibilidade muito dependente
  do jogo, sem equivaler uma configuração universal a garantia.
- r/winlator: testes/versões Box64 e FEX, incluindo mudanças de
  desempenho entre SoCs e games.
- Mobox (olegos2/mobox, arquivo): glibc via Termux, limites de
  compatibilidade e caminhos de Wine sem copiar a stack inteira.
- Winlator e forks Mali: uso dos drivers corretos e perfis por jogo;
  Turnip é para Adreno, não Mali.
- FEX-Emu/FEX e ptitSeb/box64: projetos de tradução ARM64;
  versionamento e compatibilidade de ABI são essenciais.
- GameHub: somente referência de UX e perfis, sem código proprietário.

Fontes da comunidade são hipóteses de engenharia, não benchmarks
controlados. Não instalar versões aleatórias de Mesa, DXVK ou rootfs
em produção sem manifestos, hashes e compatibilidade do aparelho.

## Qualidade, licenças e testes

- `tests/VeloRuntimePlanTest.java`: arquitetura incompatível,
  ausência de pacotes, PRoot incompleto, Mali e rootfs externo bloqueado.
- `tests/VeloFlowPacingTest.c`: 60/90/120/240 Hz, NaN, FPS adaptativo.
- `tests/FrameGenPolicyTest.java`: estado térmico e refresh da UI.
- `tests/test_v17_runtime.py`: contratos das APIs e alinhamento
  entre build local e GitHub Actions.
- Pipeline GitHub Actions aplica, em ordem, os patches de
  `prepare-winnative.py`, `strip-lsfg.py`, `patch-v17.py` antes de
  compilar os binários. `tools/build-native.sh` segue a **mesma** ordem.
- A fonte upstream atual permanece fixa no commit identificado em
  `README.md`. As alterações originais ficam nos overlays do VeloWin.
- Redistribuir o conjunto exige disponibilizar código-fonte
  correspondente, licenças GPLv3 e avisos dos diversos componentes.

## Aceitação real Android

Instalar o APK v17 **somente após build verde** e fazer backup.

1. Instalar ImageFS e um Wine/Proton; criar contêiner x86_64.
2. Em **Método de execução**, verificar Bionic + Box64 habilitado e
   ARM64EC indisponível para o prefixo x86_64.
3. Testar Wine Notepad e navegação gráfica. Em prefixo ARM64EC separado,
   só habilitar FEX se o pacote estiver realmente instalado.
4. Para Linux PRoot/Gamescope, usar um contêiner Linux previamente
   instalado pelo painel avançado e dispositivo/driver compatível.
5. Na Mali, comparar WineD3D e DXVK com fallback; não ativar Turnip.
6. Ativar DESLIGADO, ADAPTATIVO e alvo fixo VeloFlow.
   Medir **quadros reais versus gerados**, latência e artefatos, FPS
   apresentados e temperatura. FrameGen não melhora FPS do jogo
   automaticamente.
7. Validar saída/retorno do contêiner, arquivos e saves; anexar logs
   sem tokens.

**Build verde ≠ desktop Wine comprovado ≠ desempenho garantido.**
