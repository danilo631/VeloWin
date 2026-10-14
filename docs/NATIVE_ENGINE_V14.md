# VeloWin Native v14 — migração para o motor WinNative/Winlator

## Decisão técnica

Esta branch **aposenta como produto o console de diagnóstico VeloWin 0.8,
EmbeddedX11Service, bridge TCP e a Activity própria de teste**. A inclusão
isolada de Xorg/Lorie no APK anterior não formava um ambiente Wine gráfico
com lifecycle/controles/Surface integrados. A nova aplicação distribuída
será construída com o motor completo WinNative, mantido em submódulo Git
para permitir auditoria, updates controlados e cumprimento GPL.

O WinNative deriva da família Winlator/Bionic e oferece
`XServerDisplayActivity`, `XServerComponent`, `ContainerManager`,
ImageFS, gerenciador de ambientes, arquivos, painel, controles e áudio
que já cooperam na mesma aplicação. O projeto mantém a própria pilha
XServer, não incorpora o processo Termux:Lorie experimental; **usar dois
XServers seria conflito, não uma melhoria**.

Revisão fixa do WinNative:
`8409085f706acbc0749881d9bf1e85f2f347dfd4`.
Os submódulos C++ do upstream também são fixados via seu `.gitmodules`.
Permissões e licenças são preservadas.

## Arquitetura real do aplicativo

```text
VeloWin Android APK (dev.velowin.app)
  ├── UnifiedActivity, primeiro assistente, biblioteca de jogos
  ├── ContainersScreen / ContainerManager / ImageFS
  │    ├── wineprefix, drives, arquivos do contêiner
  │    └── processos Wine / Box64 / Proton
  ├── XServerDisplayActivity
  │    ├── XServerComponent + XConnectorEpoll
  │    ├── GL/Vulkan renderer + Android Surface
  │    └── touch/mouse/teclado/controle
  ├── audio (PulseAudio/ALSA/DirectAudio conforme versão/configuração)
  ├── graphics drivers and DXVK/VKD3D/WineD3D components
  └── assets Linux ImageFS do upstream via Git LFS
```

Isso é **código upstream completo** dentro do APK e não uma descrição
de funcionalidades implementadas do zero pelo VeloWin.

A ferramenta `tools/prepare-winnative.py` confere o commit, licença
e três componentes críticos antes de preparar a build. Ela altera
somente o `applicationId` do flavor standard e o recurso de nome para
VeloWin; preserva pacotes Java/Kotlin/JNI para garantir a ABI upstream.
Copia `LICENSE`, `CREDITS.md` e `EMULATOR_CREDITS.md` para os
assets de avisos legais do aplicativo, junto ao MIT das contribuições
originais do VeloWin.

## Build reproduzível e rastreabilidade

- Git submodule `third_party/winnative` fixo;
- Git LFS obrigatório para `imagefs*.tzst` e pacotes;
- JDK17, SDK35, NDK 27.3.13750724, CMake3.22.1, Rust Android;
- build `./gradlew :app:assembleStandardDebug` **dentro do upstream**;
- conferência pós-build por `aapt dump badging` para exigir
  `dev.velowin.app` e label `VeloWin`;
- audit da licença GPL dentro do APK e SHA-256 publicado;
- downloads externos e Native package resolvidos pelo upstream;
- artefato `VeloWin-Native-Full-Container-debug` somente se verde.

Workflow `.github/workflows/velowin-native.yml` é o principal desta
branch. O workflow antigo `android.yml` para o protótipo permanece
disponível apenas nas branches de legado. A `main` não muda antes
de comprovar build.

## Roteiro de teste físico

1. Instale o APK nativo aprovado. APKs antigos usam outra assinatura
   e/ou aplicativo; não apague dados sem backup.
2. Na primeira abertura execute a instalação do **ImageFS** do upstream.
   Mantenha Wi-Fi e espaço livre porque os dados podem superar 2 GiB.
3. Abra **Containers**, selecione **Add Container**, configure resolução
   e compatibilidade e use **Run**. Espera-se abrir o desktop Wine
   renderizado, **não apenas um console de comando**.
4. Teste tocar ícones, arrastar janela, teclado virtual, cursor e controles.
5. Importe um executável Windows de teste de origem confiável e valide
   que é iniciado dentro do desktop. Meça log, FPS, temperatura e bateria.
6. Confirme saída e reentrada sem perder prefixo/saves; depois altere
   DXVK, WineD3D e VKD3D-Proton **se a GPU suportar**.
7. Se não abrir, compartilhe logcat e logs de sessão sem tokens/chaves.

### Situação GPU Mali

Mali/Exynos não usa Turnip (Adreno). Preferir X11 e os recursos
que o driver do Android oferecer. Não atribuir suporte Wayland ou
Vulkan 1.3 universal. O upstream explica o caminho específico de
Adreno em `docs/WAYLAND-DISPLAY.md`.

## Licenças e origem

- WinNative: GPLv3 (https://github.com/WinNative-Emu/WinNative)
- Winlator: upstream de código WinNative, avisos de componentes individualizados
  (https://github.com/brunodev85/winlator)
- Termux:X11: GPLv3 — referência, não vinculada ao APK Native
  (https://github.com/termux/termux-x11)
- GameNative, Wine, Box64, FEX, Mesa, DXVK, VKD3D-Proton, SDL,
  libretro e contribuintes: termos preservados na árvore WinNative,
  `CREDITS.md` e `EMULATOR_CREDITS.md` correspondentes.
- VeloWin pré-v14: arquivos próprios cobertos pela licença MIT
  original, sem afirmar que o produto combinado seja MIT.

**Condições:** a distribuição do APK binário precisa acompanhar uma
forma adequada de obter os fontes correspondentes à versão exata,
incluindo submódulos, patches, ferramentas e avisos. Uma simples
atribuição não substitui requisitos copyleft.

## Limitações ainda não validadas

A construção upstream funciona para o projeto original, mas modificar
`applicationId` pode revelar hardcodes e diferenças de compatibilidade
em serviços, autenticação e caminhos de dados. Passar no compilador
ou no aapt **não comprova** funcionamento no telefone. A validação em
hardware é o gate para mesclar a nova base na `main`.

**Esta é uma migração de motor**, não migração automática de dados:
o layout de prefixos antigos do VeloWin v0.8 é diferente. Não realizar
merge de saves/dados de usuários sem estratégia de migração e rollback.
