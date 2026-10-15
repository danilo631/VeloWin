# VeloWin v23 — substituição incremental do motor e do instalador

A v23 parte da **v22, compilada integralmente em [Actions 38665420864](https://github.com/danilo631/VeloWin/actions/runs/38665420864)**. NÃO usa o APK de laboratório v21 como substituto do Wine.

## Código substituído nesta branch

| Operação concreta | Antes (WinNative base) | Agora (VeloWin v23) |
|---|---|---|
| Preparação para atualização ImageFS | `ImageFsInstaller.clearRootDir` apagava a árvore inteira, exceto `home` | `VeloImageFsMaintenance.prepare` controla a limpeza sem seguir links simbólicos; preserva também `opt/installed-wine` |
| Exclusão concorrente de arquivos ImageFS | `installFromAssets` não serializava chamadas | `VeloInstallCoordinator`: instalação única em andamento; liberação no `finally` |
| Publicação de pacotes Wine/Box64/FEX | `ContentsManager.finishInstallContent`: criava diretório final, tentava renomear, podia chamar erro **e sucesso** | `VeloContentTransaction.commit`: valida staging e destino, move atomicamente e só reporta sucesso após publicação |
| Remoção de componente em uso | `ContentsManager.removeContent` apagava o diretório sem verificar contêineres | `VeloInstalledRuntimeGuard`: bloqueia remoção enquanto a sessão está ativa ou um contêiner referencia a versão selecionada |
| Fluxo de reparação ImageFS | Interface e lógica WinNative | `VeloRuntimeInstallActivity`: confirmação explícita, barra de progresso, alerta de sessão ativa, espaço e verificação de ImageFS pronto |
| Quadros extras no presenter Vulkan DIS | Planejamento podia tentar adquirir continuamente imagens extras com o swapchain saturado | `VeloFlowCapacityGuard`: limita geração opcional após falhas de acquire e recupera gradualmente após 120 quadros reais estáveis |

A **extração de arquivos de runtime** TAR/ZSTD e os instaladores de
drivers/Wine antigos são preservados neste passo: substituímos o
controle de instalação, limpeza e publicação, NÃO declaramos que todas
as implementações de arquivos já foram migradas.

## Interface real entre os módulos

```text
VeloWinHomeActivity (motor gráfico funcional)
 ├─ VeloRuntimeInstallActivity  [VeloWin]
 │   └─ ImageFsInstaller       [extração GPL anterior]
 │      ├─ VeloInstallCoordinator [VeloWin próprio]
 │      └─ VeloImageFsMaintenance [VeloWin próprio]
 ├─ Containers + Wine/Box64/FEX + XServer [mantidos]
 │   ├─ ContentsManager [instalação parcial reescrita]
 │   │   ├─ VeloContentTransaction
 │   │   └─ VeloInstalledRuntimeGuard
 │   └─ Vulkan compositor + SGSR1 [mantidos]
 │      └─ OpenFlow/DIS [mantido]
 │         └─ VeloFlowCapacityGuard [NOVO controle nativo]
 └─ VeloBench GLES3 [medições existentes]
```

As integrações físicas ocorrem via `tools/patch-v23.py` depois dos
patches v17, v18 e v22. Os pontos de edição são fixados por âncoras
exatas e a pipeline falha se os formatos upstream não coincidirem.
Não há troca de nomes sem alteração efetiva nos métodos
`installFromAssets`, `finishInstallContent`, `removeContent`
e no `fg_present.c` real.

## Fontes estudadas e licença

- [DroidDeck](https://github.com/Droid-Deck/DroidDeck), GPL-3.0:
  controle de jobs de instalação, preservação de diretórios pessoais
  na atualização e recuperação/reinstalação (referência de arquitetura).
- [CronyX-](https://github.com/REF4IK/CronyX-), repositório
  **arquivado**, licença MIT no raiz: instalação e staging de
  componentes Wine; os subcomponentes podem possuir outras licenças.
- [Winlator-Ludashi](https://github.com/StevenMXZ/Winlator-Ludashi),
  MIT no raiz: `WineRuntimeGuard` e a ideia de proteger versões
  ainda selecionadas por contêineres; VeloWin reimplementou a lógica
  para a API diferente do motor base.
- [WinNative](https://github.com/WinNative-Emu/WinNative), GPLv3,
  revisão `8409085f706acbc0749881d9bf1e85f2f347dfd4`.
  Wine, X11, DXVK, áudio, SGSR1 e DIS continuam creditados e usados
  nesta versão para não regressar jogos.
- [Winlator](https://github.com/brunodev85/winlator),
  [Box64](https://github.com/ptitSeb/box64), [Mobox](https://github.com/olegos2/mobox):
  abordagens de execução com Wine/Bionic/Box64 e presets mantidas
  da v22.

O código novo das políticas e transações v23 é uma implementação
original adaptada a esses conceitos; **não** afirma copiar todo
DroidDeck, CronyX, Winlator-Ludashi ou portar os seus binários.
A distribuição com o motor WinNative continua **GPLv3**.
Não redistribuímos shaders LSFG proprietários.

## FrameGen real x laboratório

A `VeloFlowCapacityGuard` governa a capacidade do **DIS Vulkan
real** em `fg_present.c`; não gera frames sintéticos falsos e não
confunde FPS do monitor com FPS renderizado pelo jogo. Falhas na
aquisição da imagem reduzem a capacidade extra para no máximo uma;
após 120 quadros reais sem novas falhas, reabre uma imagem por janela
de 120 quadros. A apresentação do quadro real continua no caminho
original, assim como o cálculo optical flow do DIS. A adaptação só
atua nesse presenter DIS, não no FEX nem no benchmark GLES3.

Este controle é um mecanismo de proteção de latência sob pressão,
**não demonstra ganho de FPS em jogos**; mudanças no swapchain precisam
ser testadas em aparelhos Mali e Adreno.

## Portas obrigatórias antes de substituir v22/v19

1. CI de Java puro: integração, pasta HOME e Wine preservadas,
   remoção segura de links simbólicos, rename atomizado, concorrência.
2. CI de C11: throttle após misses, recuperação e capacidade zero.
3. Compilação Android com ImageFS, X11, Wine, Box64/FEX, Vulkan e assets.
4. Verificação física da **primeira janela real** (sem tela preta).
5. Testes de instalação de pacotes reais, upgrades, saves e remoção
   impedida quando runtime estiver em uso. Fallback da v22 disponível.
6. Medição do DIS e input latency com contador de quadros reais vs
   gerados em Mali; monitorar stutter sob pressão térmica.

PR continua em draft e a main intocada até essas provas. A branch
preserva o motor completo como baseline; outros componentes serão
migrados individualmente com testes de equivalência antes de excluir
a dependência.
