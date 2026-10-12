# VeloWin · Bionic Engine v0.3-dev

## Filosofia de uso inspirada no GameHub

Biblioteca como centro do aplicativo, **perfil recomendado com um toque**, controles reconhecidos automaticamente, modos avançados opcionais, download guiado e estado de prontidão explícito. É uma inspiração conceitual de experiência; nenhum código, layout ou marca proprietária é reutilizado.

## Cadeia de execução pretendida

```text
Jogo Windows (.exe) - PE x64 ou x86
    ↓ perfil por jogo
Wine/Proton compilado para Android Bionic (GameNative)
    ↓ ELF x86_64 via Box64 AArch64 ou ARM64EC/FEX quando integrado
Android libc Bionic + dependências compatíveis
    ↓
Windows display (X11/Surface Android)    [PENDENTE]
Wine audio → host Android                [PENDENTE]
Wine keyboard/gamepad → Android          [PENDENTE]
D3D → DXVK/VKD3D → Vulkan Android        [PENDENTE]
    ↓
Jogo renderizado e áudio no Android      [AINDA NÃO FUNCIONA]
```

**Bionic não é glibc.** Wine Linux em `wine-*-amd64-wow64.tar.xz` e Wine Android GameNative `.wcp` não são intercambiáveis. Em Bionic não é preciso instalar um rootfs Linux glibc só para obter libc; ainda são necessárias bibliotecas Wine/Box64/X11 e isolamento. O `BionicWorkspace` prepara diretórios privados Android, mas não cria um rootfs Linux nem um prefixo Windows.

## Funcionalidades implementadas

1. **Vulkan JNI Android**: `vkCreateInstance`, `vkEnumeratePhysicalDevices`, propriedades da GPU, extensões swapchain/timeline/memory-budget; libera corretamente instância Vulkan. Nem todas as extensões exigidas por DXVK são consultadas.
2. **AudioTrack**: emite tom curto PCM para verificar o áudio nativo do Android, separado da saída Wine.
3. **Gamepad e touch**: recebe eventos `MotionEvent` e `KeyEvent` e desenha o movimento no Canvas Android, ainda sem converter para API de entrada Windows.
4. **Catálogo GameNative**: valida nomes `proton-x.y-z-...wcp`, URL GitHub canônica, tamanho e `assets[].digest=sha256:...`; baixa só por TLS/CDN permitida e armazena localmente.
5. **Instalador WCP**: confirmado no workflow real que o pacote Proton 11.0-2 x86_64 de 10/10/2026 é **Zstandard TAR**, com `profile.json`, `bin/wine` link simbólico para `../lib/wine/x86_64-unix/wine`, pastas `bin/lib` e `prefixPack.txz`. O `BionicWineInstaller` revalida SHA-256 e examina links, caminhos, tamanho, manifesto, presença da imagem ELF de arquitetura esperada e só então instala atomicamente. A extração ainda não foi validada num aparelho físico.
6. **Build Box64 Bionic**: workflow experimental compila o commit upstream `c6a8c4472f74f9ad204d35195f084b5f996b12e3` para ARM64 com NDK 27.2, confirma linker Android e dependência `libc.so`, e cria um APK experimental. Os testes de CI **não são um teste real do Box64 em aparelho**.
7. **Teste de inicialização Wine controlado**: o APK Box64 experimental oferece **Wine --version** apenas para WCP x86-64 validado, com confirmação, sem shell e timeout de 9 segundos. O diagnóstico ainda não foi executado em aparelho Android físico.
8. **Pré-requisitos de launch**: os bloqueios são explícitos: prefixo Wine não preparado, Wine display/audio/input ausentes, DXVK e compatibilidade de GPU por jogo ainda não implementados. O botão Jogar não executa binários.

## Segurança e limitações

- Sem root, ADB, overclock, acesso amplo à galeria ou instalação silenciosa de drivers.
- Todos os dados extraídos permanecem na pasta privada do aplicativo; a extração não executa conteúdo WCP.
- Sem assinatura independente do upstream: o campo `assets[].digest` confere os bytes distribuídos no **mesmo** GitHub release.
- WCP TAR: dispositivos, hardlinks e caminhos absolutos são proibidos; links relativos são avaliados lexicalmente e criados só depois de todos os arquivos comuns.
- Instalações são atômicas, falhas temporárias são recuperáveis, versões instaladas ficam separadas por tag.
- Ainda pode haver incompatibilidade de um pacote específico com o instalador; o CI verificou o arquivo WCP real em ambiente Linux, não a extração dentro de Android.
- Box64 upstream com `ANDROID=ON` pode assumir bibliotecas/prefixos Termux. Compilar o binário e executar `-v` não prova que Wine Bionic funcione.

## Matrizes de suporte

| GPU | Configuração inicial | Observação |
|---|---|---|
| Mali / Immortalis | Vulkan Android do sistema | não tentar Turnip |
| Adreno | Vulkan Android do sistema | Turnip será opcional após teste |
| Xclipse / PowerVR | Vulkan Android do sistema | suporte DXVK/VKD3D depende de extensões |
| Sem Vulkan | WineD3D futuramente | sem aceleração D3D/Vulkan garantida |

## Referências

[Box64](https://github.com/ptitSeb/box64) · [FEX](https://github.com/FEX-Emu/FEX) · [GameNative Proton Wine](https://github.com/GameNative/proton-wine/releases) · [DXVK](https://github.com/doitsujin/dxvk) · [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton) · [GameSir GameHub](https://gamesir.com/pt-BR/support/downloads).

O conteúdo **não** representa afiliação com essas marcas.
