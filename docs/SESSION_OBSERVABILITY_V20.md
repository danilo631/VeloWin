# Sessão Wine/X11: estados observados no VeloWin

Foi criado o motor de estados Java VeloSessionLifecycle dentro do módulo
independente velowin-core. VeloSessionJournal integra o launcher próprio
com pontos pontuais da Activity XServer usada no backend herdado.

O início da Activity mostra PREPARING, **não RUNNING**. O runtime real
emite STARTING_XSERVER, STARTING_WINE e WAITING_FOR_WINDOW durante o
preparo. RUNNING aparece apenas após o callback onFirstGuestWindow(),
que sinaliza a primeira janela real ou frame guest. O alerta de tela
preta após 35s não marca falsamente uma janela como funcional.
Ao destruir a Activity, a sessão é marcada STOPPED.

Estados são locais ao processo e não sobrevivem como falsos registros
RUNNING se o Android terminar o aplicativo. A máquina de estados evita
transições atrasadas para RUNNING após encerramento e não toca em arquivos
Wine, saves ou prefixos.

O motor nativo WinNative permanece necessário: esta é uma extração de
observabilidade, não a alegação de substituição do runtime. A validação
em dispositivo precisa conferir os eventos com os logs Wine/XServer.
