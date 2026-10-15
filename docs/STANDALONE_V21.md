# VeloWin standalone v21 — status e validação

## Substituição estrutural realizada

- Removidos do código compilado: `.gitmodules`, diretório
  `third_party/winnative`, `overlays/`, `tools/patch-v*.py`,
  `tools/prepare-winnative.py` e o workflow de build monolítico.
- O Android Gradle agora aponta diretamente para `:app` e
  `:velowin-core`, com `applicationId=dev.velowin.app`.
- Fonte do `:app`: aplicativo independente da branch main anterior
  à migração WinNative; persistência e instaladores originais VeloWin.
- `Box64` é baixado de fonte MIT e compilado por NDK no workflow,
  sem incorporar nenhuma classe ou binário WinNative.

## Renderização de verdade

O VeloBench usa geometria GLES3 com vértices, 12 triângulos por cubo,
matrizes 3D, profundidade e iluminação. VeloSpatial usa um FBO RGBA8
com depth renderbuffer para renderizar em resolução reduzida e shader
GLSL 300 ES para reconstruir usando gradientes, vizinhança direcional e
nitidez localmente limitada. Perfis: nativo, 77%, 67%, 50%.

VeloTemporal usa a imagem real anterior e a posterior em texturas
separadas, busca local por uma correspondência de luminância em 9
candidatos e calcula um quadro de meia-fase com um blend modulado
pela diferença das amostras para atenuar dupla borda. Um quadro real
é apresentado depois. Isto é interpolação experimental no benchmark,
**não um algoritmo FSR 3, nem optical flow avançado confiável para
jogos rápidos**. Não há motion vectors de jogos.

Referências abertas:
- https://github.com/GPUOpen-Effects/FidelityFX-FSR (FSR 1 MIT; ideia)
- https://github.com/glmark2/glmark2 (metodologia 3D GPLv3)
- https://github.com/ptitSeb/box64 (código MIT; commit pinado no CI)

## Auditoria honesta

| Critério | Status |
|---|---|
| Sem checkout WinNative | Sim, por árvore Git e build Gradle |
| APK Android compilável | CI (verificar run do HEAD) |
| Box64 embarcado | Apenas se etapa ELF/CI terminar com sucesso |
| Benchmark GLES3 e GLSL | Fontes compilados; execução GPU precisa de aparelho |
| FrameGen 2D experimental | Código implementado no benchmark; desempenho e artefatos não validados |
| Wine --version Bionic | Necessita pacote Wine instalado e teste físico |
| Janelas Wine/X11 | **Não implementado no APK standalone** |
| Vulkan FrameGen dentro de jogos | **Não implementado** |
| Desempenho Mali superior | **Não demonstrado** |

A v18/v19 preservada representa o rollback que possui desktop Wine e
DIS funcional confirmado pelo usuário. APKs debug podem usar assinaturas
diferentes, portanto faça backup antes de qualquer troca; NÃO desinstale
sem cópia validada dos dados e saves.
