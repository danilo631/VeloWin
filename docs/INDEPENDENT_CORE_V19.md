# VeloWin v19 — módulo independente, VeloFlow e VeloBench 3D

## O que mudou

O usuário confirmou em um dispositivo Android que o **desktop Wine da v18
abriu** e que a geração de quadros DIS funcionou. A v19 parte desse
marco: não substitui o XServer/Bionic/Box64 por um frontend experimental
e não quebra o lançamento Wine comprovado.

A redução da dependência de WinNative é **arquitetural e verificável**,
não apenas rebranding:

| Antes | Depois |
| --- | --- |
| A Home chamava diretamente `XServerDisplayActivity` | Home e Settings falam com `VeloEnginePort`, uma API **sem Android e sem WinNative** |
| Configuração FrameGen exclusivamente ligada ao motor | `VeloFlowProfile` valida qualidade/resolução, métricas e regra térmica fora do monolito |
| Indicador simples de FPS | `GpuBenchMetrics` mede tempo médio, p95, p99 e 1% low de um benchmark 3D real |
| Sem medidor de GPU independente | `VeloBenchActivity` com renderização OpenGL ES 3 própria, três cargas e relatório |
| Fonte original espalhada dentro do app | `:velowin-core` é **um módulo Gradle Java próprio** com testes JVM sem dependência do Android |

**Limitação honesta:** a v19 ainda compila os motores do Winlator/
WinNative em `third_party/winnative` no commit fixado. A remoção
completa desse checkout **não está concluída**. Isso exige migrar
os serviços ImageFS, Box64/FEX, Wine, XServer, JNI, Vulkan, inputs e
áudio, um de cada vez, com testes físicos. Retirar o submódulo de uma
vez faria o jogo/desktop parar de iniciar. A separação aqui inaugura
a ABI/contrato que permitirá trocar os backends.

## FrameGen: correção de qualidade real

A tela antiga escrevia `disFrameGenScale=70`. O motor DIS interpreta
inteiros até 100 como **porcentagem de 720p**:
70 × 720/100 = 504, arredondado para o valor suportado 360.
Em vez de um perfil equilibrado, isso escolhia o **nível de maior custo
computacional** em todas as GPUs Mali.

O VeloWin v19 agora usa explicitamente:

- **180px Econômico** — recomendado para Mali e GPUs sensíveis a consumo
- **252px Equilibrado** — padrão escolhido para novos perfis
- **360px Qualidade** — exige folga de desempenho e temperatura

Perfis legados recebem leitura compatível (70 → 360) para não
surpreender usuários com mudança de qualidade. A qualidade é alterada
**por contêiner e somente por escolha explícita**. O mesmo OpenFlow/DIS
Vulkan, do autor comunitário atribuído, continua renderizando quadros.
O projeto NÃO retoma Lossless Scaling/LSFG nem distribui seus shaders.

O módulo `VeloFlowProfile` inclui regras térmicas, metas limitadas
pela frequência real do painel e histerese para recomendações, mas
**não inventa interpolação nova**. A qualidade DIS efetiva é do
algoritmo upstream.

## Benchmark 3D comunitário, sem dependências do WinNative

A atividade `VeloBenchActivity`, construída pelo VeloWin com
`android.opengl.GLES30` e `GLSurfaceView`, é inspirada no tipo de
cena de geometria/vertex e cubos do benchmark comunitário
[glmark2](https://github.com/glmark2/glmark2) (GPLv3; autoria de
Alexandros Frantzis, Jesse Barker, Ben Smith). O código da Activity
foi escrito especificamente para este projeto; **não é uma cópia
vendorizada do glmark2 nem uma pontuação glmark2 oficial**.

O teste contém **geometria 3D de verdade**: cubos de seis faces, 12
triângulos, iluminação no fragment shader, transformação por matriz,
rotação dinâmica, teste de profundidade, três níveis de chamadas de
desenho (8/48/128 cubos) e sincronização GPU pelo `glFinish` para
capturar a cadência do trabalho apresentado pelo GL thread.

Mede:
- FPS médio sobre os últimos até 240 quadros
- Tempo médio e p95 do frame
- 1% low aproximado derivado de p99
- Nome/versão do renderizador GLES e nível de carga
- Perfil VeloFlow **sugerido**, não aplicado automaticamente

Para gravar a sugestão, o usuário precisa executar o perfil Médio ou
Intenso durante pelo menos 80 amostras e tocar o botão de salvar.
Os valores são mantidos nas preferências locais do app e exibidos
como recomendação informativa ao alterar a qualidade do VeloFlow.

### O que o benchmark não mede

É **OpenGL ES nativo do Android**, não um jogo dentro do Wine. Não
mede velocidade de execução x86_64 do Box64, Vulkan/DXVK, a latência
do DIS Vulkan, nem frames realmente interpolados. Comparar
resultados entre aparelhos não garante o mesmo desempenho em jogos.
Ele serve para medir a capacidade gráfica geral e orientar
um primeiro perfil, não alegar aceleração fictícia.

## Licenças e créditos

- `velowin-core`, VeloBench e o adaptador: VeloWin, componentes
  originais desenvolvidos para o projeto, distribuídos no APK GPLv3
  combinado
- **Winlator**: LGPL-2.1, conforme repositório e termos próprios
- **WinNative**: GPL-3.0, fonte fixada mantida e atribuída
- **OpenFlow/DIS**: contribuição comunitária da DEVAR/qwertypower,
  avisos preservados no motor
- **glmark2**: referência de metodologia GPLv3, sem importar seus
  fontes para a Activity
- Wine, Box64, FEXCore, Mesa, DXVK, VKD3D e demais projetos mantêm
  seus avisos e licenças relevantes na distribuição

O código agregado deve respeitar as exigências de fonte correspondente
para qualquer distribuição de APK combinado.

## CI e testes

`VeloCoreTest.java` compila todas as classes próprias da biblioteca
sem import Android nem motor WinNative, validando limites de FPS,
clamp térmico, mapeamento do valor legado, latência p95/p99 e contrato
de lançamento. `test_v19_modular.py` verifica a incorporação
de módulo Gradle separado, chamadas GLES reais e ausência de
implementações fakes. Testes anteriores seguem ativos.

O script `tools/prepare-winnative.py` injeta o módulo
`:velowin-core` no build Android `app`, registra a Activity
3D no manifest e copia apenas o adaptador específico para
`com.winlator.cmod`. O script `tools/build-native.sh` e o
GitHub Actions continuam reproduzindo o pacote de produção.

### Próxima extração de engine (fases futuras)

1. Isolar descoberta/instalação de pacotes (`ContentsManager`) atrás
   de `VeloPackagePort` com hashes, assinaturas e manifestos
2. Extrair `ContainerManager` e persistência em dados VeloWin
   mantendo migração não destrutiva dos prefixos Wine
3. Implementar `VeloX11Port` e adaptador JNI para a Surface sem
   monolito de 700k linhas, incorporando bibliotecas Winlator LGPL
   compatíveis e créditos
4. Migrar Wine Bionic/Box64/FEX, drivers Vulkan Mali, áudio e input
   com testes X11 e GPU reais; somente então eliminar `WinNative`
   do build e revisar a licença do app combinado
