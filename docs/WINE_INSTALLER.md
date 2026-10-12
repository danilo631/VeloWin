# Preparação de Wine no VeloWin 0.2

## Fluxo real implementado

```text
GitHub Releases Kron4ek
  → filtro de assets Linux amd64 WoW64
  → verificação HTTPS + tamanho + SHA-256 publicado
  → arquivo comprimido no armazenamento privado
  → verificação SHA-256 novamente
  → descompactação XZ/TAR em diretório temporário exclusivo
  → política de caminhos TAR e symlinks relativos
  → verificação de estrutura da distribuição e bin/wine ELF64 x86_64
  → manifesto de instalação + commit atômico de pasta
  → seleção opcional por título no catálogo de jogos
```

A descompactação é **uma instalação de arquivos Wine**, não a instalação funcional do emulador de Windows. O Wine precisa de bibliotecas glibc compatíveis, rootfs, tradutor x86/ARM64, inicializador, display e outras camadas. Nenhum arquivo baixado é executado nesta fase.

## Ameaças mitigadas

- Caminhos absolutos, `..`, NUL, barras invertidas/escapes e nomes longos não são permitidos.
- Arquivos duplicados e escritos sob o caminho de um symlink previamente declarado são rejeitados.
- Tipos TAR especiais como dispositivos, FIFO e hardlinks não são aceitos.
- Links simbólicos só podem ser relativos e apontar lexicalmente para dentro da pasta extraída. São criados **depois** dos arquivos comuns, sem seguir links durante a escrita.
- Cada TAR tem limite de entradas, tamanho por arquivo e soma descompactada. O espaço livre é verificado durante o processo.
- A extração ocorre numa pasta temporária criada no armazenamento interno privado e é removida sem seguir symlinks em caso de erro.
- O download é re-hasheado antes de instalar. O ELF `bin/wine` deve ser x86_64, não ARM64/Bionic.
- A pasta final só recebe os arquivos após todas as verificações. O manifesto `.velowin.json` permite validar se uma preparação foi concluída.
- O arquivo original comprimido é preservado para retry e o botão apagar arquivos preparados mantém o arquivo de download.

## Limitações / risco residual

- A implementação aceita somente subconjunto conservador do formato TAR: algumas compilações Wine oficiais podem conter metadados/caminhos especiais e ser rejeitadas.
- Verificação SHA-256 comparada ao texto do **mesmo release** oferece integridade, não assinatura criptográfica independente do mantenedor.
- Scripts e bibliotecas incluídos no Wine não são executados nem analisados integralmente; componentes só deverão ser iniciados num runtime e isolamento testados.
- A extração não verifica ABI de cada biblioteca da distribuição. Valida apenas o bin/wine principal.
- Se o Android interromper abruptamente o processo, pode restar uma pasta `.staging-*` na pasta interna; ela não é reconhecida como versão instalada. O botão **Limpar preparações interrompidas** remove apenas essas pastas, sem apagar versões concluídas.
- A disponibilidade de symlinks, sistema de arquivos e armazenamento deve ser confirmada em dispositivos reais.

## Testes

`ArchiveSafetyTest`, `ElfVerifierTest`, `PeInspectorTest`, `ReleasePolicyTest`, `test_structure.py` e `test_v02.py` no GitHub Actions, seguidos pela montagem do APK Android. A extração end-to-end em hardware real ainda é uma etapa manual necessária.
