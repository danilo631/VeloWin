# VeloWin — bases consolidadas analisadas

**VeloWin 0.3-dev:** o APK convencional inclui Vulkan JNI próprio e Commons Compress/XZ/zstd-jni para extrair WCP GameNative. Um **workflow experimental** compila Box64 do commit upstream fixado e o empacota em APK separado, conservando a licença MIT em `app/src/main/assets/BOX64_LICENSE.txt`. O aplicativo convencional não contém Box64, Wine ou DXVK, e nenhum APK foi validado com jogos Windows.

Esta implementação da aplicação é própria. Os projetos abaixo foram conferidos no GitHub; **não** são copiados nem embalados como motores do VeloWin 0.2.

| Projeto | Papel técnico | Descobertas / decisão |
|---|---|---|
| [Box64](https://github.com/ptitSeb/box64) | Traduz ELF x86_64 para ARM64 | Código sob MIT; necessita compilação para alvo e configuração Linux/glibc. O tradutor não fornece Wine, Vulkan ou rootfs |
| [FEX](https://github.com/FEX-Emu/FEX) | Emulação x86/x64 em ARM64 | MIT; exige integração com ABI/runtime apropriado, não é substituição intercambiável automática do Box64 |
| [Wine](https://gitlab.winehq.org/wine/wine) | Tradução de WinAPI | Código predominantemente LGPL e licenças individuais; precisa de bibliotecas host e configuração de prefixo |
| [DXVK](https://github.com/doitsujin/dxvk) | D3D8/9/10/11 para Vulkan | Vulkan e extensões requeridas variam conforme versão; não presumir compatibilidade com Mali/Adreno |
| [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton) | D3D12 → Vulkan | Arquivos sob LGPL e outros avisos; requer extensa cobertura Vulkan, não basta instalar Wine |
| [Mesa](https://gitlab.freedesktop.org/mesa/mesa) | Driver Vulkan/OpenGL e layers | Turnip normalmente se destina a Adreno; GPUs Mali devem priorizar driver nativo do Android, não instalar Turnip automaticamente |
| [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds) | Pacotes Linux de Wine e Wine com patches Proton | A variante amd64 WoW64 evita multilib 32-bit, mas requer glibc e dependências Linux; hashes publicados no release |
| [GameNative/proton-wine](https://github.com/GameNative/proton-wine) | Wine e builds voltadas a Android/Bionic | ABI diferente de Linux/glibc; os WCP não são importados no instalador automático do VeloWin |

## Componentes efetivamente incluídos no 0.2

- Código Java Android original do VeloWin, licenciado sob MIT.
- [Apache Commons Compress 1.28.0](https://commons.apache.org/proper/commons-compress/): processamento seguro de streams TAR/XZ; Apache-2.0.
- [XZ for Java 1.10](https://tukaani.org/xz/java.html): descompressão XZ; conservar os termos próprios do projeto e avisos quando redistribuído.

**Não** está incluso qualquer Box64, FEX, Wine, DXVK, VKD3D, rootfs ou driver nativo no APK. Quando cada motor for incluído, atualizar licenças de redistribuição, referências de build, versão exata, testes de ABI e avisos.
