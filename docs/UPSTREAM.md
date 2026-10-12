# Bases abertas e licenciamento

O VeloWin foi desenvolvido do zero no nível da aplicação Android. Os projetos abaixo são **bases candidatas de integração**, não dependências incluídas ou executadas pelo APK v0.1:

| Projeto | Função planejada | Cuidados |
|---|---|---|
| [WineHQ](https://gitlab.winehq.org/wine/wine) | Camada de compatibilidade Win32/Win64 | Wine sob LGPL e outras licenças por arquivo; avaliar distribuição e mudanças |
| [Box64](https://github.com/ptitSeb/box64) | Execução de ELF x86_64 em ARM64 Linux | Compilação Android/ABI e licenças do upstream |
| [FEX](https://github.com/FEX-Emu/FEX) | Emulação x86/x64 em AArch64 Linux | MIT; integração no host Android depende de runtime e bibliotecas |
| [DXVK](https://github.com/doitsujin/dxvk) | Direct3D 8/9/10/11 em Vulkan | Respeitar licença e extensões Vulkan da GPU |
| [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton) | Direct3D 12 em Vulkan | Exige recursos Vulkan e hardware compatível |
| [Mesa](https://gitlab.freedesktop.org/mesa/mesa) | Drivers Linux e utilidades Vulkan/OpenGL | Drivers Turnip não atendem GPUs Mali por padrão |
| [Kron4ek/Wine-Builds](https://github.com/Kron4ek/Wine-Builds) | Pacotes Wine Linux WoW64, incluindo Wine com patches Proton | Downloads são verificados, mas não executados na versão 0.1 |

**Proton da Valve** integra mais do que um binário Wine. Um pacote de Wine com patches Proton não é o Steam Proton completo. Builds Android/Bionic de GameNative não devem ser confundidas com arquivos Wine Linux/glibc.

**Não incorporar código ou assets de Winlator, GameHub, GameNative ou terceiros sem licença verificada.** Ao distribuir bibliotecas de terceiros, manter os avisos de copyright e as licenças exigidas por cada componente. MIT neste repositório cobre somente as implementações próprias.

### Questões abertas antes de lançar o motor

- ABI e dependências para Wine no Android sem root.
- Fluxo de Vulkan do host e extensões BCn/WSI relevantes.
- Distribuição independente de rootfs com atualização segura e checksums publicados fora do canal do binário.
- Contêineres isolados com salvamento de jogos e permissões.
- Licenças de código e binários redistribuídos e conformidade com o Android.
