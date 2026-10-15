# VeloWin — Android ARM64 independente

Projeto aberto de execução e pesquisa de aplicativos Windows no Android.
Esta branch **v21 standalone** compila código Android próprio, sem
checkout, submódulo ou patch monolítico do WinNative.

## O que funciona nesta branch

- Aplicativo Android `dev.velowin.app`, biblioteca de jogos e importação SAF.
- Catálogo e instalação verificável de pacotes Wine Bionic, prefixos e
  comandos de diagnóstico; Wine gráfico ainda não integrado.
- Compilação Box64 Bionic ARM64 fixada por commit via GitHub Actions,
  com análise ELF e licença MIT (depende da execução verde do CI).
- VeloBench GLES 3: cubos iluminados renderizados em 3D com cargas reais,
  medições de tempo de quadro, histórico e exportação JSON/CSV.
- **VeloSpatial**: renderização de cena em FBO com 77%, 67% ou 50% da
  resolução, reconstrução espacial adaptativa e nitidez limitada.
  Shader de autoria VeloWin inspirado na finalidade do FSR 1, **não**
  é EASU/RCAS oficial nem FSR 2/3.
- **VeloTemporal**: interpolação GLES experimental entre quadros reais
  usando busca de correspondência local e warping bidirecional,
  somente no benchmark. Possui contagem separada de quadros 3D,
  intermediários e chamadas GL, limitações térmicas e de memória.
  **Não** se conecta à apresentação de jogos Wine/DXVK.

## Limitação fundamental

O novo APK **não possui ainda XServer nativo integrado ao Wine**.
WinNative fornecia JNI/X11, superfície, input e áudio. Remover seu
checkout tornou o build independente, mas **não substituiu essas
funcionalidades**. Não há alegação de que o desktop Windows já abre
nesta branch. O VeloFlow do benchmark não é substituto direto do
OpenFlow/DIS Vulkan testado no aparelho.

O motor gráfico com Wine real está preservado na branch
`feature/v19-core-module-gpu-benchmark` e na v18 comprovada.
Não desinstale essa versão sem backup dos contêineres, saves e prefixos.

## Build

- Java 17, Gradle 8.11.1, Android SDK 35, NDK 27.2.12479018.
- `gradle :app:assembleDebug` monta o aplicativo standalone;
  a pipeline `.github/workflows/velowin-standalone.yml` também
  compila e embute o Box64 Bionic do commit pinado.
- Sem root. APK debug não é assinado com a chave do release;
  a compatibilidade de atualização in-place deve ser verificada.

Consulte `docs/ARCHITECTURE.md` e
`docs/STANDALONE_V21.md` para arquitetura, licenças,
métodos de teste e lacunas.
