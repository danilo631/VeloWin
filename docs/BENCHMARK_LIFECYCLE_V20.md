# VeloBench: execução delimitada, separação de amostras e exportação

A v19 já incluía uma cena GLES 3 própria com cubos rotativos e iluminação.
Esta revisão corrige uma falha estatística: trocar de carga usava a mesma
janela circular de amostras, contaminando o FPS reportado. O objeto
`GpuBenchMetrics` agora é reiniciado a cada execução, inclusive após
recriação de contexto EGL. A interface executa no máximo 30 segundos
por carga (8, 48 ou 128 cubos), permite parada manual e mantém até
12 resultados no histórico local.

O JSON e CSV são serializados por `VeloBenchReport` no módulo independente
`:velowin-core`. Os valores incluem média, p95/p99 e estimativa de 1% low
a partir do p99. Os arquivos são exportados pelo seletor Android SAF
(`ACTION_CREATE_DOCUMENT`), sem permissão de armazenamento irrestrita.

**Limite importante:** os intervalos coletados no `GLSurfaceView` incluem
`glFinish` e a cadência de CPU/GPU observada pelo GL thread. Não
equivalem à cadência efetivamente apresentada pelo SurfaceFlinger.
Não afirmamos frames sintéticos DIS nem ganhos DXVK, Vulkan ou Wine a
partir desse benchmark. Os campos `presented_fps` e
`generated_fps` permanecem `null`.

O pipeline real OpenFlow/DIS e a correção de janela Wine da v18 não são
alterados por este commit. A Home mantém o mesmo botão de VeloBench e
os mesmos botões de Wine; não há migração destrutiva de prefixos.

Fonte de referência metodológica:
https://github.com/glmark2/glmark2 (GPLv3). Nenhum arquivo desse projeto
foi incorporado: a cena GLES Android existente continua código VeloWin.

Validação requerida em aparelho: comparar 8/48/128 objetos em sessões de
30 segundos, conferir relatório JSON/CSV e observar se Winefile e DIS
ainda iniciam. Compilação do APK não prova esses testes físicos.
