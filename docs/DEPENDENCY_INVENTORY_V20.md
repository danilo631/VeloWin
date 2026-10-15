# Inventário de dependências do APK VeloWin v19/v20

Este inventário é deliberadamente restrito ao que o código e o workflow
permitem verificar. Versões internas contidas no ImageFS precisam ser
lidas dos artefatos; **não são adivinhadas**. O pin do checkout WinNative
é `8409085f706acbc0749881d9bf1e85f2f347dfd4`.

| Componente | Origem real no build atual | Licença / verificação | Acoplamento | Extraído? | Evidência disponível |
|---|---|---|---|---|---|
| UI Home e Settings | overlays/velowin-app | Código VeloWin; combinado GPLv3 | Classes do app herdado ainda usadas na criação | UI própria, **sim** | CI Gradle v19; aparelho pendente |
| Contrato de lançamento | modules/velowin-core VeloEnginePort | Original VeloWin | Sem Android nem upstream | **Sim** | Testes JVM |
| Ciclo de estados | VeloSessionLifecycle e VeloSessionJournal | Original VeloWin | Adaptador mínimo no XServer | **Parcial** | Testes JVM e source, hardware pendente |
| ContainerManager e ImageFS | third_party/winnative | WinNative GPLv3 / créditos anexados | **Alto** | **Não** | Build do checkout completo |
| Wine/Proton, prefixos e Winefile | ImageFS e código de runtime herdado | Wine LGPL e avisos por pacote, auditar versões | **Alto** | **Não** | v18 física positiva, v20 pendente |
| Box64 Bionic | runtime/ativos do WinNative | Box64 MIT e créditos | **Alto** | **Não** | Binário e Wine v18; versões por APK |
| FEX / ARM64EC | Perfis de runtime e componentes herdados | FEX MIT, demais por pacote | Condicional | **Não** | Viabilidade por instalação; física pendente |
| glibc / PRoot | Fluxos opcionais v17 | Avisos individuais dos pacotes | Condicional | **Não** | Não declarar backend nativo completo |
| XServer, JNI, Surface e input | WinNative/Winlator | GPLv3 e avisos upstream específicos | **Alto** | **Não** | v18 corrigiu janela negra |
| Áudio | Backend herdado | Dependente do componente empacotado | **Alto** | **Não** | CI não comprova reprodução física |
| OpenFlow/DIS Vulkan | Renderer herdado e patch VeloFlow | Licença/aviso do autor preservados no upstream | **Alto** | Política separada; kernel **não** | Proprietário confirmou FG v18 |
| Código proprietário LSFG | Antigo upstream (não compilado) | Não distribuir shaders comerciais | ABI de compatibilidade desabilitada | Shaders e fontes **removidos pelo script** | Auditoria de fontes CI |
| DXVK, VKD3D, WineD3D | Componentes do ImageFS | Licenças individuais a conferir por versão | Acoplamento gráfico | **Não** | Disponibilidade não equivale à compatibilidade GPU |
| Perfil Mali | overlays/velowin-app/MaliProfile.java | Original VeloWin | API de Container upstream | **Parcial** | Testes JVM, sem benchmark físico novo |
| VeloBench GLES3 | overlays/velowin-app/VeloBenchActivity.java | Original VeloWin | GLES Android, não Wine | **Sim** | Compilação Java, GPU física pendente |
| Metrologia e JSON/CSV | modules/velowin-core | Original VeloWin | Zero dependências Android | **Sim** | Testes JVM e source |
| glmark2 | Referência metodológica somente | GPLv3 [upstream](https://github.com/glmark2/glmark2) | Nenhum arquivo incorporado | Não se aplica | Código da cena escrito para VeloWin |
| Termux:X11, Mobox e GameHub | Referências estudadas | Verificar individualmente; GameHub tem componentes proprietários | Nenhum checkout da aplicação | Não se aplica | Não confundir inspiração com incorporação |

## Portas necessárias para remover o checkout WinNative

- Separação dos serviços de contêiner, ImageFS, execução de processo,
  montagem de discos e guarda de prefixos sem perda de dados.
- Extração documentada de JNI/X11/Wayland, Vulkan WSI, áudio e controles,
  com versões fixadas por commit e testes de equivalência.
- Verificação de licenças das versões concretas redistribuídas,
  incluindo correspondência entre APK, fontes e artefatos.
- Regressão física: container desktop Wine, mouse, toque, teclado,
  Winefile, DXVK/WineD3D, áudio e VeloFlow, antes de remover símbolos.

**O checkout WinNative ainda é obrigatório para gerar um APK executável
de jogos Windows nesta revisão.**
