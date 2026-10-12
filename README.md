# VeloWin

**VeloWin** é um novo aplicativo Android independente, com código e identidade próprios, para organizar jogos Windows e preparar componentes de compatibilidade de forma segura. Este repositório não é um fork de Winlator nem inclui seus submódulos ou assets.

## Estado atual — 0.1.0 (foundation)

- Aplicativo Android próprio em **Java 17**, com interface escura minimalista, navegação Biblioteca / Runtimes / Sistema / Sobre.
- Biblioteca local de instaladores e jogos Windows via seletor SAF, sem permissão de armazenamento global.
- Perfis por jogo, persistência atômica e diagnóstico de GPU/OpenGL/Vulkan e ABI Android.
- Busca de builds Wine/Proton **Linux amd64 WoW64** via releases do GitHub, com checksum SHA-256, limites e verificação de origem.
- Arquivos baixados ficam em armazenamento privado do aplicativo; não são executados ou extraídos automaticamente.
- CI para testes de políticas e compilação Android debug.

**Ainda não executa jogos Windows:** o motor nativo (Wine/Box64/FEX, rootfs, DXVK/VKD3D e integração gráfica) é um módulo independente a construir e testar. A interface mostra este estado explicitamente. Baixar um Wine não instala um motor completo.

## Arquitetura

`dev.velowin.app` / `app/src/main/java/dev/velowin/app` contém módulos próprios: `library`, `runtime`, `hardware`, `engine` e `ui`. O aplicativo parte das APIs oficiais do Android; a interface e as regras de segurança são originais.

Consulte [Arquitetura](docs/ARCHITECTURE.md), [Bases e licenças](docs/UPSTREAM.md) e [Roadmap](docs/ROADMAP.md).

## Compilar

JDK 17, Android SDK 35, Android Gradle Plugin 8.9.2 e Gradle 8.11.1.

```bash
gradle :app:assembleDebug
```

Também é possível executar **Actions → VeloWin Android → Run workflow** para compilar no GitHub. O APK debug aparece em **Artifacts → velowin-debug** após o job passar.

**Nota:** a assinatura debug não é uma assinatura release distribuível. A compatibilidade dos futuros motores nativos deverá ser validada em dispositivos reais Mali/Adreno.

## Projeto independente e licenciamento

Código próprio sob [MIT](LICENSE). Pacotes opcionais externos **não estão** incluídos nesta fase e possuem licenças individuais. VeloWin não é associado a Valve, WineHQ, FEX, Box64, Winlator ou GameHub.
