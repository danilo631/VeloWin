# VeloWin v22 — híbrido funcional, por componentes

## Por que não usar o APK standalone v21 como motor principal

O APK v21 compilado sem submódulo WinNative não tinha XServer,
JNI da superfície Wine, DRI3, compositor DXVK/Vulkan, áudio Win32
nem controles Win32 integrados. Compilar um APK sozinho **não** equivale
a iniciar o desktop Windows. A v22 volta à base v19 funcional (inclui
a recuperação de tela preta da v18) e **não** incorpora como motor de
jogos o shader GLES 3 experimental da v21.

## Base obrigatória até sua extração, sem depender da interface WinNative

| Componente em execução | Origem upstream verificável | Ação v22 |
|---|---|---|
| Wine/Proton, instalação ImageFS, prefixo e containers | WinNative / Winlator Bionic, fontes GPLv3 | Preservado |
| X11, XServerDisplayActivity, JNI Surface e DRI3 | WinNative / Winlator, fontes GPLv3 | Preservado com hooks restritos |
| Áudio PulseAudio/ALSA/DirectAudio | WinNative e autores de seus subcomponentes | Preservado |
| Teclado, touch e controle/Gamepad | WinNative / Winlator / componentes de terceiros | Preservado |
| DXVK, VKD3D, WineD3D, seleção de driver | Pacotes/motores comunitários já integrados | Preservado |
| OpenFlow/DIS compute Vulkan e frame pacing | Contribuição GPLv3 de qwertypower e componentes do compositor | Preservado, sem LSFG proprietário |
| SGSR1 Vulkan espacial (shader BSD-3-Clause) | Qualcomm SGSR1 adaptado pelo upstream | **Ligado ao X11 real** via VeloSR |
| DynaRec Box64 | ptitSeb/box64 (MIT) integrado à base | Receitas opcionais VeloBox64 |
| Modo Mali WineD3D + reversão | Código VeloWin, abordagens divulgadas no Winlator-Mali e Mobox | Mantido |
| Home, biblioteca, VeloSettings, diagnóstico e VeloBench | Módulos originais VeloWin | Preservados e ampliados |
| Controle de estado de sessão e engine port | :velowin-core original VeloWin | Segue separado do Android e do upstream |

### Fontes e atribuições

- Motor base: https://github.com/WinNative-Emu/WinNative, revisão fixa
  `8409085f706acbc0749881d9bf1e85f2f347dfd4`, GPL-3.0.
- Winlator: https://github.com/brunodev85/winlator
  (o motor derivado é compilado a partir do código fonte licenciado).
- Mobox: https://github.com/olegos2/mobox
  (conceito de presets Box64/Termux, **nenhum script copiado**).
- Box64: https://github.com/ptitSeb/box64/blob/main/docs/USAGE.md
  (`BOX64_DYNAREC_BIGBLOCK`, `SAFEFLAGS`, `STRONGMEM`).
- Winlator Mali: https://github.com/ar37-rs/winlator_mali
  (abordagens de fallback WineD3D/Mali, com compatibilidade verificada).
- Qualcomm SGSR1: shader BSD-3-Clause e créditos originais preservados
  no código fonte do motor.
- OpenCV DIS + implementação GPU qwertypower: licenças e crédito
  preservados em `WinNative-CREDITS.md` e `WinNative-EMULATOR_CREDITS.md`
  incluídos no APK pelo `tools/prepare-winnative.py`.

**Não há cópia cega** de implementações com licenses incompatíveis.
A distribuição combinada, enquanto mantiver o motor GPLv3, continua
sujeita à GPLv3 e precisa fornecer código-fonte correspondente.

## Integração técnica que roda dentro do Wine/XServer

```
VeloWinHomeActivity / VeloSettingsActivity
                  |
       VeloEnginePort (Java sem Android)
                  |
       VeloEngineAdapter
                  |
       XServerDisplayActivity real
           /                 \
   VeloHybridGraphics     VeloBox64Bridge
           |                 |
   VeloUpscalePlan       VeloDynarecPlan
           |                 |
    SGSR1 Vulkan          EnvVars do Wine
           |                 |
         DRI3         Box64 -> Wine/Proton
           \                 /
            XServer -> Surface Android
                      |
          DIS FrameGen (se ativado)
                      |
             Painel físico / input / áudio
```

Os hooks são adicionados por `tools/patch-v22.py`, após os patches
v17 (pacing) e v18 (recuperação). Exigem **duas âncoras exatas**;
falham no CI se a revisão base mudar. O launcher não substitui o XServer.

### VeloSR: reconstrução espacial real do desktop Wine

No VeloSettings, selecionar o modo:
- Herdar: preserva todas as opções SGSR nativas originais.
- Desativar: força resolução nativa sem SGSR no contêiner.
- Qualidade: escala de entrada 75%, modo SGSR1=3.
- Equilibrado: escala 66,7%, modo SGSR1=4.
- Performance: escala 50%, modo SGSR1=6.

O SGSR1 executa o shader Vulkan da base WinNative sobre a imagem
real do convidado. O código novo apenas seleciona o perfil por
contêiner, antes da criação do XServer. Na temperatura severa
(`PowerManager` >= 4), o override explícito recai em resolução
nativa no lançamento. Para atalhos com configurações gráficas próprias,
a seleção do atalho continua com prioridade, sem sobrescrita.

**SGSR1 não é AMD FSR1 EASU/RCAS nem FSR2/3**, embora cumpra
objetivo semelhante de reconstrução espacial e sharpening. SGSR1
não gera quadros intermediários: essa função continua no DIS Vulkan.

### VeloBox64: perfis inspirados em Mobox

Sem selecionar a opção `VeloBox64`, o motor preserva **todos** os
presets/variáveis Box64 existentes. Um perfil só injeta três opções
documentadas de DynaRec em sessões Box64/X11 explicitamente selecionadas:

| Receita | BIGBLOCK | SAFEFLAGS | STRONGMEM |
|---|---:|---:|---:|
| Compatibilidade | 0 | 2 | 2 |
| Equilibrado | 2 | 1 | 1 |
| Desempenho | 3 | 1 | 0 |

O modo desempenho pode ser incompatível com jogos que modificam
código em execução; compatibilidade pode reduzir velocidade. Não
injetar em FEX/ARM64EC, Linux/Gamescope ou instâncias de instalação
de dependências. Há opção "Herdar" para remover o override por completo.

## Riscos, testes e condições para mudar a base

1. CI source-contract: testes Java de preset/dynarec, testes Python de
   hooks, verificações de módulo e testes originais de tela preta/FrameGen.
2. CI native-apk: checkout pinado+LFS, execução obrigatória dos patches,
   compilação completa de Wine/XServer/SGSR1/DIS, APK e créditos.
3. Teste físico: entrar no desktop Wine normal e modo recuperação;
   abrir aplicação X11 real, mover cursor, digitar e tocar áudio.
4. Teste Mali: medir upscaling em cada perfil, verificar artefatos,
   estabilidade, temperatura, DXVK/WineD3D e desempenho do DIS real.
5. Teste de regressão: prefixos antigos intactos, atalhos, saves,
   mudança e restauração de perfis, tela preta e encerramento seguro.

Até cumprir o item 3, **não há comprovação de execução no Galaxy A34**;
um CI Android não executa Wine em aparelho real. A v19/v18 deve continuar
disponível como rollback. Não mesclar em main antecipadamente.

## Extração futura real do WinNative

Primeiro isolar armazenamento e launcher, depois dividir JNI Surface,
XServer, ponte DRI3 e apresentação Vulkan, por fim input, áudio e
interfaces de Wine/Box64/FEX. Cada módulo deve receber testes de
conformidade e manter a implementação funcional anterior como fallback.
O checkout WinNative pinado só pode ser removido depois de substituir
**cada** dependência realmente usada pela atividade gráfica.
