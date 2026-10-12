# VeloWin

**VeloWin** é um novo aplicativo Android independente para organizar jogos Windows, investigar compatibilidade e preparar componentes Wine de forma verificável. Não depende de código, APK, submódulo ou assets do Winlator.

## VeloWin 0.2.0 — nova fase

A interface oferece:
- **Biblioteca**: importar executáveis Windows pelo seletor do Android, verificar o cabeçalho PE x86/x64, pesquisar, marcar favoritos, renomear, registrar anotações e configurar GPU/tradutor/Wine por jogo.
- **Wine e Proton**: buscar releases de [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds/releases), baixar apenas arquivos Linux x86_64 WoW64 com nome/URL/tamanho/SHA-256 validados, e **preparar o filesystem Wine** a partir de TAR/XZ com extração isolada, proteção contra path traversal e links externos, limites de extração e instalação atômica.
- **Sistema**: consultar GPU/OpenGL ES, a versão Vulkan anunciada pelo Android, RAM, espaço livre, bateria e estado térmico; compartilhar um relatório sem root/ADB.
- **Persistência robusta**: biblioteca em JSON com `AtomicFile` e atualizações por cópia, downloads substituídos com rollback, armazenamento privado e limpeza dos arquivos temporários.
- **GitHub Actions**: testes Java/Python e APK debug Android.

### Instalação do APK

Abra [GitHub Actions → VeloWin Android](https://github.com/danilo631/VeloWin/actions/workflows/android.yml), selecione a execução da `main` e, após os jobs aprovados, baixe **Artifacts → velowin-v02-debug**. Extraia o ZIP e instale o APK. A assinatura debug pode diferir de futuras versões de produção.

### Utilizar Wine

1. Abra **Runtimes → Buscar versões no GitHub**, escolha um Proton Wine Linux WoW64 com hash.
2. Toque **Baixar**. A validação SHA-256 protege contra arquivos truncados ou alterados em relação à descrição publicada do release.
3. Quando aparecer no inventário, toque **Preparar arquivos Wine**. A pasta temporária recebe o conteúdo TAR/XZ, o instalador valida a estrutura e `bin/wine` ELF x86_64; só depois move a pasta para a instalação final.
4. O botão **Remover preparação** apaga apenas os arquivos extraídos. O botão **Remover download** apaga o arquivo comprimido depois de remover a preparação.
5. Na biblioteca, selecione o pacote Wine preparado para um jogo. A configuração é salva, mas ainda não inicia o jogo.

**Limite importante:** preparar Wine **não instala um motor Windows funcional**. VeloWin ainda não integra o tradutor Box64/FEX, rootfs Linux/glibc, Wineprefix, janelas, áudio/entrada, DXVK/VKD3D e Vulkan de execução. O botão jogar continuará mostrando o diagnóstico, não abrindo executáveis. O APK pode compilar e mesmo assim falhar na extração de algum TAR que use entradas não suportadas. É necessário testar em Android real.

## Bases avaliadas

[Box64](https://github.com/ptitSeb/box64) • [FEX](https://github.com/FEX-Emu/FEX) • [Wine](https://gitlab.winehq.org/wine/wine) • [DXVK](https://github.com/doitsujin/dxvk) • [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton) • [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds). Esses projetos ainda **não são motores integrados** no APK v0.2. O parser Java Commons Compress e XZ estão declarados no Gradle.

Veja [Arquitetura](docs/ARCHITECTURE.md), [Auditoria de bases](docs/UPSTREAM.md), [Plano de execução](docs/ROADMAP.md) e [Instalador Wine](docs/WINE_INSTALLER.md).

## Compilar

JDK 17, Android SDK 35, Gradle 8.11.1, Android Gradle Plugin 8.9.2.

```bash
git clone https://github.com/danilo631/VeloWin.git
cd VeloWin
gradle :app:assembleDebug
```

Código de aplicação próprio sob [MIT](LICENSE); dependências externas permanecem com suas licenças. VeloWin não é associado a Winlator, Valve, WineHQ, Box64, FEX ou GameHub.
