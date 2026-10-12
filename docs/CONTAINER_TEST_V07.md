# VeloWin 0.7 — entrar no contêiner e testar no celular

## O que o teste Box64 já demonstra

Quando o app exibe `Box64 arm64 ... with Dynarec`, `Código 0`, isso confirma **apenas** a execução do binário ARM64 Box64 e o sucesso do comando `-v`. Não valida Wine, DLL, wineboot, Surface, Direct3D ou jogo.

## Instalação da versão principal

1. Abra o [workflow Android](https://github.com/danilo631/VeloWin/actions/workflows/android.yml).
2. Selecione uma execução de `main` na qual **verify** e **apk** estejam verdes.
3. Baixe **Artifacts → velowin-v07-container-box64-debug**; extraia e instale o APK. O Box64 está incluído nesse APK principal.
4. Se ocorrer conflito com uma versão debug anterior assinada com outra chave, desinstalar pode apagar dados; faça backup antes.

## Abrir um contêiner

**Sem jogo:** em **Runtimes → Abrir console de contêiner (sem jogo)**. Execute `status`, `box64 -v` e, após preparar Wine, `wine --version`.

**Por jogo:**
1. Em Runtimes, obtenha e instale um WCP **Wine Android/Bionic x86_64** da fonte GameNative, com hash validado. Não use Linux/glibc Kron4ek nem ARM64EC no console Box64.
2. Na **Biblioteca**, adicione um `.exe` (SAF do Android) e abra as configurações para escolher a versão **Wine Bionic x86_64** e backend **BOX64**. Também existe seleção automática conservadora quando nenhum runtime foi escolhido.
3. Toque **Entrar no contêiner** no cartão do jogo.
4. Use **Preparar prefixo** (`prefix prepare`); a operação extrai `prefixPack.txz` do Wine instalado para um diretório privado do jogo. Não executa `wineboot`.
5. Rode **Wine --version**, depois **CMD /c ver**. Use também o botão **Testar Win32**, que executa um PE x86_64 compilado do código próprio do VeloWin no GitHub Actions. Somente **código 0 com o marcador `VELO_WIN32_SMOKE_OK`** comprova que esse programa de teste foi executado por Wine. São processos de teste Bionic/Box64; o resultado deve ser interpretado pela saída/código, não pela existência do botão.
6. **Wineboot -u** exige consentimento e pode alterar os arquivos do prefixo. O processo direto termina em no máximo 30 segundos; wineserver/filhos podem sobreviver, portanto há **Encerrar Wine** (`wineserver -k`).
7. **Testar EXE** exige consentimento e copia apenas o executável escolhido para a área privada do app, com limite de 384 MiB. Recursos/DLLs externos da pasta original **não** são copiados.
8. Compartilhe o log em **Compartilhar log**; registre o texto completo do erro sem tokens ou informações pessoais.

## Comandos permitidos

```text
help
status
files
box64 -v
wine --version
prefix prepare
prefix status
wine cmd /c ver
win32 test
wineboot -u
run exe
wineserver -k
clear
```

O console restringe os comandos e **não oferece** `sh`, `bash`, `sudo`, instalação de pacotes externos, comandos arbitrários ou terminal Linux. Não interpreta texto digitado como shell.

## Falhas esperadas e como interpretar

- **Box64 presente, Wine ausente:** preparar pacote Bionic x86_64 na aba Runtimes.
- **Wine ARM64EC:** requer outro pipeline/FEX; Box64 x86_64 não pode lançá-lo.
- **Arquivo EXE não acessível:** reimporte via seletor SAF.
- **Wineprefix ausente:** executar `prefix prepare` no contêiner do jogo.
- **Bibliotecas Bionic não encontradas ou loader ausente:** analisar o log. Um pacote de Wine baixado não instala automaticamente todas as dependências nativas.
- **`nodrv_CreateWindow`, `DISPLAY`, `X11`:** falta a ponte gráfica Windows/X11. **Nenhuma janela Windows é apresentada nesta versão.**
- **`wineboot` travado:** cancelar o processo direto e tentar `wineserver -k`. Processos filhos podem precisar do encerramento do app.
- **EXE inicia e não mostra janela:** falta servidor gráfico Wine integrado. Não é sucesso de jogo.

## Segurança, isolamento e licenças

Este contêiner é um **workspace de diretórios privado** do Android e os processos herdam permissões do aplicativo; não é virtualização, sandbox de kernel ou rootfs completo. Um EXE de origem desconhecida pode ser malicioso. Não teste executáveis que não sejam seus ou de fonte confiável.

O Box64 mantém seus avisos MIT; os pacotes Wine e componentes adicionais mantêm suas próprias licenças. O APK principal não distribui jogos, DXVK, VKD3D nem um servidor gráfico Wine.

## Critérios de sucesso do marco

Os testes de GitHub Actions demonstram que código Java/NDK compila, componentes estão incluídos e comandos permitidos têm políticas testadas. Um teste **no telefone** precisa mostrar o código e a saída de `wine --version` e `wine cmd /c ver` para confirmar que o Wine avançou de fato. O funcionamento de uma interface de jogo só pode ser comprovado após a integração da ponte gráfica real.
