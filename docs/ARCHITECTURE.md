# VeloWin — arquitetura inicial

## Objetivo

VeloWin não é uma renomeação de Winlator. É uma **aplicação Android original**, com IDs, UI, modelos, dependências e pipeline próprios. Nenhum arquivo do Winlator é copiado ou usado como submódulo. A estratégia de interoperabilidade é integrar progressivamente motores abertos existentes, sempre respeitando licença, ABI e configurações Vulkan.

### Componentes incluídos e testáveis

```text
VeloWin / dev.velowin.app
├── MainActivity.java          UI escura, biblioteca, runtimes, sistema, sobre
├── library/
│   ├── Game.java              Modelo de jogo e escolhas por título
│   └── GameStore.java         JSON + AtomicFile + SAF URI persistente
├── runtime/
│   ├── ReleasePolicy.java     Identidade, nome, versão, checksum, URL, tamanho
│   ├── RuntimeCatalog.java    Leitura da API pública de releases
│   ├── RuntimeDownloader.java HTTPS + SHA-256 + download atômico/cancelável
│   ├── RuntimeStore.java      Registro de arquivos verificados em pasta privada
│   └── ReleaseEntry.java
├── hardware/
│   └── GpuInspector.java      Contexto GLES2 temporário + informação Vulkan Android
└── engine/
    └── EngineGate.java        Recusa execução enquanto backend não existe
```

## Contrato do motor futuro

O próximo módulo `engine-native` deverá construir uma ABI Android ARM64 real, com:
1. Tradutor de x86_64 (**Box64** no caminho glibc ou **FEX** onde compatível).
2. Rootfs com ABI inequívoca: Android **Bionic** ou Linux **glibc**, sem misturar bibliotecas.
3. Implementação de Wine e preparação de prefixo Windows idempotente, com detecção de erros.
4. Ponte gráfica para Vulkan nativo Android e, conforme capacidade da GPU, DXVK / VKD3D-Proton / WineD3D.
5. Namespace de processos e filesystem por jogo, logs, cancelamento, limites térmicos e limpeza.
6. Testes com executáveis Windows reais em Mali e Adreno antes de habilitar o botão **Jogar**.

A versão 0.1.0 **não contém o motor**. O arquivo Wine/Proton baixado por HTTPS é mantido **sem extração ou execução**. Concluir o download não significa que o Wine esteja instalado para rodar jogos.

## Arquitetura de segurança

- Sem root, ADB, acesso irrestrito ao armazenamento ou comandos de overclock.
- `ACTION_OPEN_DOCUMENT` guarda URI com permissão read persistível; não copia EXEs.
- Dados de biblioteca e de runtimes usam `AtomicFile`, preservando estado antigo em falhas.
- Catálogo de GitHub somente Kron4ek, nomes x86_64 WoW64, hash SHA-256 publicado, tamanho e domínio permitido.
- Downloads com limite de tamanho e `.part`; arquivos inválidos são descartados.
- Fonte de binário de terceiros não é assumida confiável por ser open source; em fase posterior, sandbox e verificação de assinatura/manifesto serão essenciais.
- Download SHA-256 garante integridade **conforme o manifesto publicado no mesmo release**; não substitui uma assinatura independente de autor.

## Compilação e suporte

Java 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2, compileSdk 35, minSdk 26. Android 8+. O app depende somente das bibliotecas do Android nessa fase, reduzindo conflitos transitivos Kotlin. A evolução para Jetpack só ocorrerá quando melhorar comprovadamente a qualidade.

O desenho mantém `GameStore`, `RuntimeStore` e políticas isolados da UI. A camada gráfica não deve alterar drivers do sistema; deve preferir Vulkan do Android e permitir configuração por jogo.
