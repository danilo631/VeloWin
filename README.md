# VeloWin — Modular Android Windows Gaming

**VeloWin** é um app Android autoral para criar ambientes Windows, instalar
componentes e executar programas e jogos. O projeto combina uma interface
própria, inspirada na praticidade de hubs de jogos, com motores open source
comprovados da comunidade.

## Arquitetura da versão v15

| Área | Fonte e implementação |
| --- | --- |
| **Home VeloWin** | Código original em `overlays/velowin-app/VeloWinHomeActivity.java`. Biblioteca, criar contêiner, entrar no desktop, gerenciamento rápido de FrameGen |
| **Perfis e segurança de FrameGen** | Código original em `FrameGenPolicy.java` |
| **X11, janelas e controles** | Motor Winlator, adaptado pela comunidade WinNative; compilado dos fontes do submódulo fixado |
| **Wine/Proton + Box64/FEX e áudio** | Serviços integrados do motor open source WinNative/Winlator |
| **Frame generation** | Pipeline Vulkan **OpenFlow/DIS** do WinNative, selecionado pelo VeloWin. Não emula FPS nem inclui shaders proprietários LSFG |
| **Drivers e pacotes** | Gerenciador upstream de Wine, DXVK, VKD3D-Proton, Mesa e outros componentes; GPU e arquitetura precisam ser compatíveis |
| **Termux:X11** | Fonte GPLv3 estudada nas versões experimentais. Não embutida junto ao XServer do Winlator para evitar duas stacks gráficas em conflito |
| **Experiência GameHub** | Referência de UX (acesso rápido aos jogos, perfis e execução). Sem copiar recursos proprietários nem alegar vínculo com o produto |

A base gráfica WinNative continua versionada por submódulo Git — ela é uma
**dependência de motor**, não mais a interface principal apresentada ao
usuário. O VeloWin tem seu próprio launcher, criado e mantido neste
repositório. O menu avançado ainda oferece o ambiente completo do motor.

## Como usar

1. Instale um APK **da branch v15 com GitHub Actions aprovado**.
2. Abra o VeloWin. Instale os arquivos ImageFS e runtimes Wine/Proton pelo
   assistente quando solicitado.
3. Use **Criar contêiner Windows**: ele seleciona um runtime instalado e chama
   o criador de prefixos real do motor.
4. Toque em **Entrar no contêiner / Desktop Wine**. O aplicativo inicia
   `XServerDisplayActivity` com o ID daquele contêiner, ligando a
   inicialização de Wine, Box64/FEX, XServer e os controles reais.
5. Em **Frame Generation e desempenho**, escolha desligado ou **OpenFlow/DIS**
   adaptativo, alvo 60, 90 ou 120 FPS. A seleção é armazenada por contêiner
   como `disFrameGen`/`disFrameGenTargetFps`. O renderizador decide o que
   suporta na GPU; o valor selecionado é uma meta, não FPS comprovado.
6. Em **Componentes, drivers e biblioteca**, configure DXVK, VKD3D,
   WineD3D, áudio e os demais componentes via menu avançado.

**GPU Mali:** usar X11 como padrão e drivers apropriados à Mali. Turnip é
feito para Adreno. Vulkan e suporte de compute precisam ser testados em
cada dispositivo.

## Como compilar

A versão do motor está fixada em
`WinNative-Emu/WinNative@8409085f706acbc0749881d9bf1e85f2f347dfd4`.
O script `tools/prepare-winnative.py` injeta o código **original** do
VeloWin no projeto Android fonte, registra a nova Activity como o único
launcher e preserva o Winlator/WinNative como motor, com créditos e
licenças. O source upstream não é copiado manualmente para a raiz.

Pré-requisitos: JDK 17, Git LFS, SDK 35, NDK 27.3.13750724, CMake 3.22.1,
Rust com target aarch64-linux-android e bastante espaço livre.

```bash
git clone https://github.com/danilo631/VeloWin.git
cd VeloWin
git checkout feature/v15-modular-velowin-gamehub
git submodule update --init --recursive third_party/winnative
rustup target add aarch64-linux-android
bash tools/build-native.sh
```

A execução CI **VeloWin Modular GameHub Runtime** exporta o artefato
`VeloWin-Modular-GameHub-FrameGen-debug`, **somente se a build passar**.
O APK usa `dev.velowin.app`. Não misture versões assinadas por chaves debug
diferentes sem backup prévio dos dados.

## Licenças, fontes e créditos

- **VeloWin (UI e configuração)**: código fonte original, distribuído sob
  GPL-3.0 no aplicativo combinado.
- **Winlator**: <https://github.com/brunodev85/winlator-app> (LGPL-2.1).
- **WinNative**: <https://github.com/WinNative-Emu/WinNative> (GPL-3.0),
  incluindo créditos a GameNative, Box64, Wine, Mesa, DirectAudio e outros
  componentes.
- **Termux:X11**: <https://github.com/termux/termux-x11> (GPL-3.0),
  referência separada, não embutida neste APK.
- **DXVK**: <https://github.com/doitsujin/dxvk>.
- **VKD3D-Proton**: <https://github.com/HansKristian-Work/vkd3d-proton>.
- **Mesa**: <https://mesa3d.org/>.

Os textos integrais das licenças e créditos upstream são incluídos como
assets. O código original MIT das versões anteriores preserva sua
atribuição; o **APK combinado permanece sujeito à GPLv3**. O projeto não
redistribui shaders proprietários de Lossless Scaling.

Veja [componentes e licenciamento detalhados](docs/COMPONENT_MIX_V15.md).

## O que já é validável

O código da nova Home, criação de contêiner, abertura do XServer real e
configuração do OpenFlow/DIS está conectado a APIs reais do upstream.
Passar nos testes e compilar o APK **não confirma** desktop Wine, áudio,
interpolação de quadros nem compatibilidade de jogos no telefone. Esses
recursos exigem testes em hardware, com log e observação da Surface.
