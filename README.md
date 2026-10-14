# VeloWin Native

**Aplicativo Android para executar contêineres Windows, construído sobre o motor completo
do [WinNative](https://github.com/WinNative-Emu/WinNative) (GPL-3.0).**

> **A branch v14 muda a base do app de verdade.** O protótipo
> `ContainerLabActivity`/Termux:Lorie foi removido da árvore principal.
> Seu histórico Git permanece acessível para auditoria e reversão;
> nenhum componente desse laboratório entra no APK VeloWin Native.

## O que funciona no motor adotado

O motor upstream inclui integração real de XServer, Surface Android,
teclado/mouse/touch/controles, ImageFS, gerenciador de contêineres, Wine/Proton,
Box64, áudio, perfis e instalação de componentes. É **o código do motor upstream
sendo compilado como app VeloWin**, não um conjunto de botões de diagnóstico.

Abra o aplicativo, conclua o assistente de instalação do ImageFS e acesse
**Containers → Add Container → Run**. O próprio motor apresenta o ambiente
gráfico e gerencia seus arquivos/jogos. Também é possível importar EXEs e
configurar o perfil de compatibilidade dentro do aplicativo.

**Isso não implica que todos os jogos Windows rodem**; desempenho, GPU,
DLLs e Wine dependem do hardware e dos componentes instalados.

## Fonte e construção

A revisão do motor é fixa:
`WinNative-Emu/WinNative@8409085f706acbc0749881d9bf1e85f2f347dfd4`,
sob `third_party/winnative`. Esse upstream reúne códigos e abordagens
do Winlator, GameNative, Mesa, Box64, Wine e outros. O Termux:X11 estudado
nas versões anteriores permanece separado: o APK Native utiliza **somente
o XServer integrado do WinNative**, não duas stacks X11 competindo.

Use JDK 17, Android SDK 35, NDK 27.3.13750724, CMake 3.22.1, Rust com
target `aarch64-linux-android` e Git LFS:

```bash
git clone --recurse-submodules https://github.com/danilo631/VeloWin.git
cd VeloWin
git checkout feature/v14-native-clean-main
git submodule update --init --recursive third_party/winnative
rustup target add aarch64-linux-android
bash tools/build-native.sh
```

A saída local é
`third_party/winnative/app/build/outputs/apk/standard/debug/standard.apk`.

No GitHub Actions, use **[VeloWin Full Native Runtime](https://github.com/danilo631/VeloWin/actions/workflows/velowin-native.yml)**.
O artefato, **se o workflow estiver verde**, é
`VeloWin-Native-Full-Container-debug`. Nenhum artefato deve ser anunciado
como funcional antes de passar pela compilação e teste Android.

## Identidade e separação

- `applicationId = dev.velowin.app` e nome **VeloWin**;
- Java/Kotlin e JNI mantêm o namespace consolidado `com.winlator.cmod`,
  para não quebrar chamadas nativas e registros internos;
- o flavor `standard` não usa nomes de pacotes de benchmarks ou jogos;
- códigos originais MIT do VeloWin permanecem rastreáveis no histórico;
- o APK combinado contém créditos e texto integral da GPL upstream.

O **gerenciador de contêineres nativo** substitui o protótipo de laboratório
anterior. Não transferimos automaticamente prefixos, jogos ou saves entre
`app/src/main/java/dev/velowin` e o novo motor: o layout e a estrutura
de dados são diferentes. Não desinstale versões antigas sem backup.

## GPU e drivers

Em GPUs **Mali**, preserve os drivers do Android e teste os modos
compatíveis expostos pelo runtime; não selecione Turnip (driver Adreno)
como se fosse um driver Mali. DXVK requer suporte Vulkan apropriado;
VKD3D-Proton necessita de extensões adicionais, e WineD3D pode servir de
alternativa quando adequado. O Wayland específico do WinNative tem
condições restritivas ligadas a Adreno; use X11 em Mali.

## Licenças e contribuição

**O APK construído com a base WinNative está sujeito à GPL-3.0.**
Não basta apenas mencionar autores: todo o código-fonte correspondente,
scripts de build e avisos necessários devem ser disponibilizados. Os
fontes e avisos estão preservados na revisão fixada e copiados para a
pasta de avisos dentro do APK.

- [Plano de implementação e checklist de validação](docs/NATIVE_ENGINE_V14.md)
- [WinNative — GPLv3, créditos completos](https://github.com/WinNative-Emu/WinNative/blob/8409085f706acbc0749881d9bf1e85f2f347dfd4/CREDITS.md)
- [WinNative — créditos de emuladores](https://github.com/WinNative-Emu/WinNative/blob/8409085f706acbc0749881d9bf1e85f2f347dfd4/EMULATOR_CREDITS.md)
- [Winlator](https://github.com/brunodev85/winlator)
- [Termux:X11](https://github.com/termux/termux-x11)
- [DXVK](https://github.com/doitsujin/dxvk)
- [VKD3D-Proton](https://github.com/HansKristian-Work/vkd3d-proton)
- [Mesa](https://mesa3d.org/)

**Status de engenharia:** o novo runtime é uma integração de fontes consolidadas;
um build aprovado ainda precisa de teste físico para comprovar que contêiner,
Wine e jogos abrem em cada aparelho.
