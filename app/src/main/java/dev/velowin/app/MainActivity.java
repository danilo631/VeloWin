package dev.velowin.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.CheckBox;
import android.app.ActivityManager;
import android.os.Build;
import android.os.PowerManager;
import dev.velowin.app.library.PeInspector;
import dev.velowin.app.runtime.WinePackageInstaller;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.velowin.app.engine.EngineGate;
import dev.velowin.app.hardware.GpuInspector;
import dev.velowin.app.library.Game;
import dev.velowin.app.library.GameStore;
import dev.velowin.app.runtime.ReleaseEntry;
import dev.velowin.app.runtime.RuntimeCatalog;
import dev.velowin.app.runtime.RuntimeDownloader;
import dev.velowin.app.runtime.RuntimeStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Original VeloWin Android frontend. No Winlator source, launcher or native binaries. */
public final class MainActivity extends Activity {
    private static final int PICK_GAME = 124;
    private static final int BACKGROUND = 0xFF0C0F14;
    private static final int SURFACE = 0xFF171C24;
    private static final int STROKE = 0xFF303945;
    private static final int WHITE = 0xFFF2F5FA;
    private static final int MUTED = 0xFF98A6B9;
    private static final int ACCENT = 0xFF9EC2FF;

    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final AtomicBoolean cancelling = new AtomicBoolean(false);
    private GameStore games;
    private RuntimeStore runtimes;
    private LinearLayout content;
    private int tab = 0;
    private volatile boolean busy = false;
    private boolean favoritesOnly = false;
    private String gameFilter = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        games = new GameStore(this);
        runtimes = new RuntimeStore(this);
        if (state != null) tab = state.getInt("tab", 0);
        draw();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("tab", tab);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        cancelling.set(true);
        worker.shutdownNow();
        super.onDestroy();
    }

    private int dp(float amount) {
        return Math.round(getResources().getDisplayMetrics().density * amount);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(size);
        result.setTextColor(color);
        if (bold) result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        result.setLineSpacing(dp(2), 1f);
        return result;
    }

    private GradientDrawable rounded(int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        if (border != 0) bg.setStroke(dp(1), border);
        return bg;
    }

    private LinearLayout vertical() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        return root;
    }

    private LinearLayout card() {
        LinearLayout v = vertical();
        v.setPadding(dp(16), dp(16), dp(16), dp(16));
        v.setBackground(rounded(SURFACE, STROKE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        content.addView(v, params);
        return v;
    }

    private void add(LinearLayout parent, String value, int size, int color,
                     boolean bold, int bottomDp) {
        TextView t = text(value, size, color, bold);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(bottomDp);
        parent.addView(t, params);
    }

    private Button action(LinearLayout parent, String label, Runnable work, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(primary ? BACKGROUND : WHITE);
        b.setBackground(rounded(primary ? ACCENT : STROKE, 0, 10));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(12), dp(13), dp(12), dp(13));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(9);
        parent.addView(b, params);
        b.setOnClickListener(v -> work.run());
        return b;
    }

    private void section(String title, String subtitle) {
        add(content, title, 22, WHITE, true, 4);
        add(content, subtitle, 13, MUTED, false, 18);
    }

    private void draw() {
        LinearLayout root = vertical();
        root.setBackgroundColor(BACKGROUND);

        LinearLayout header = vertical();
        header.setPadding(dp(22), dp(18), dp(22), dp(11));
        TextView brand = text("V / VELOWIN", 16, WHITE, true);
        brand.setLetterSpacing(0.14f);
        header.addView(brand);
        add(header, "Windows, do seu jeito.", 11, MUTED, false, 0);
        root.addView(header);

        View line = new View(this);
        line.setBackgroundColor(STROKE);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        content = vertical();
        content.setPadding(dp(18), dp(22), dp(18), dp(24));
        scroller.addView(content);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(SURFACE);
        String[] labels = {"Biblioteca", "Runtimes", "Sistema", "Sobre"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView button = text(labels[i], 12, tab == i ? ACCENT : MUTED, tab == i);
            button.setGravity(Gravity.CENTER);
            button.setPadding(dp(2), dp(15), dp(2), dp(15));
            button.setBackground(rounded(tab == i ? STROKE : SURFACE, 0, 8));
            nav.addView(button, new LinearLayout.LayoutParams(0, -2, 1f));
            button.setOnClickListener(v -> { tab = index; draw(); });
        }
        root.addView(nav);
        setContentView(root);

        if (tab == 0) renderLibrary();
        else if (tab == 1) renderRuntimes();
        else if (tab == 2) renderSystem();
        else renderAbout();
    }

    private void renderLibrary() {
        section("Biblioteca", "Seus jogos Windows, organizados em um único lugar.");
        LinearLayout head = card();
        add(head, "Coleção local", 17, WHITE, true, 6);
        add(head, "Adicione o executável .exe pelo seletor seguro do Android. "
            + "O VeloWin guarda o acesso ao arquivo, sem copiar sua coleção.", 13, MUTED, false, 6);
        action(head, "+ Adicionar jogo", this::pickGame, true);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setTextSize(14);
        search.setHint("Buscar jogos...");
        search.setText(gameFilter);
        search.setPadding(dp(12), dp(12), dp(12), dp(12));
        search.setBackground(rounded(STROKE, 0, 10));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        searchParams.topMargin = dp(12);
        head.addView(search, searchParams);
        CheckBox favorites = new CheckBox(this);
        favorites.setText("Mostrar apenas favoritos");
        favorites.setTextSize(13);
        favorites.setTextColor(MUTED);
        favorites.setButtonTintList(ColorStateList.valueOf(ACCENT));
        favorites.setChecked(favoritesOnly);
        head.addView(favorites);

        if (games.isDamaged()) {
            LinearLayout error = card();
            add(error, "Biblioteca precisa de recuperação", 16, WHITE, true, 7);
            add(error, "O registro anterior não pôde ser lido e foi preservado. "
                + "Novas gravações estão bloqueadas para evitar perda de dados.", 13, MUTED, false, 0);
            return;
        }

        List<Game> all = games.all();
        if (all.isEmpty()) {
            LinearLayout empty = card();
            add(empty, "Nenhum jogo adicionado", 16, WHITE, true, 5);
            add(empty, "Os jogos aparecerão aqui com seu perfil e diagnóstico de execução.",
                13, MUTED, false, 0);
        }
        final ArrayList<LinearLayout> renderedCards = new ArrayList<>();
        final ArrayList<Game> renderedGames = new ArrayList<>();
        for (Game game : all) {
            LinearLayout item = card();
            renderedCards.add(item);
            renderedGames.add(game);
            add(item, (game.favorite ? "★  " : "") + game.title, 17, WHITE, true, 5);
            add(item, "PE " + game.architecture + "   •   CPU: " + game.backend
                + "\nVulkan: " + game.graphics + (game.runtimeTag.isEmpty() ? "" : "   •   Wine: " + game.runtimeTag),
                12, MUTED, false, 0);
            action(item, "Abrir opções", () -> gameMenu(game), false);
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence t, int a, int c, int z) {}
            @Override public void onTextChanged(CharSequence t, int start, int before, int count) {
                gameFilter = t.toString();
                filterGames(renderedCards, renderedGames);
            }
            @Override public void afterTextChanged(Editable t) {}
        });
        favorites.setOnCheckedChangeListener((button, checked) -> {
            favoritesOnly = checked;
            filterGames(renderedCards, renderedGames);
        });
        filterGames(renderedCards, renderedGames);
        LinearLayout note = card();
        add(note, "Execução de jogos", 15, WHITE, true, 6);
        add(note, EngineGate.reason(this, all.isEmpty() ? null : all.get(0)),
            12, MUTED, false, 0);
    }

    private void filterGames(List<LinearLayout> cards, List<Game> titles) {
        String query = gameFilter.toLowerCase(Locale.ROOT).trim();
        for (int i = 0; i < cards.size(); i++) {
            Game g = titles.get(i);
            boolean match = (!favoritesOnly || g.favorite)
                && (query.isEmpty() || g.title.toLowerCase(Locale.ROOT).contains(query)
                    || g.architecture.toLowerCase(Locale.ROOT).contains(query));
            cards.get(i).setVisibility(match ? View.VISIBLE : View.GONE);
        }
    }

    private void pickGame() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, PICK_GAME); }
        catch (ActivityNotFoundException error) {
            message("Arquivos", "O Android não encontrou um seletor de arquivos.");
        }
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != PICK_GAME || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = "";
        try (Cursor c = getContentResolver().query(uri,
            new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        }
        catch (Exception ignored) {}

        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".exe")) {
            message("Arquivo não reconhecido", "Selecione um arquivo .exe do jogo.");
            return;
        }
        for (Game old : games.all()) {
            if (old.uri.equals(uri.toString())) {
                message("Jogo duplicado", "Esse executável já está na biblioteca.");
                return;
            }
        }
        final String title = name.replaceFirst("(?i)\\.exe$", "");
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        catch (SecurityException permission) {
            message("Permissão de arquivo", "Este seletor não permite acesso persistente ao EXE.");
            return;
        }
        worker.execute(() -> {
            try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                String arch = PeInspector.inspect(input);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Game item = new Game(title, uri.toString());
                    item.architecture = arch;
                    try { games.put(item); draw(); }
                    catch (IOException error) { message("Falha ao salvar jogo", errorText(error)); }
                });
            }
            catch (Exception failure) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed())
                        message("Arquivo EXE inválido", errorText(failure));
                });
            }
        });
    }

    private void gameMenu(Game game) {
        new AlertDialog.Builder(this).setTitle(game.title)
            .setItems(new String[]{"Informações e jogar", "Configurar tradutor",
                "Configurar Vulkan", "Selecionar pacote Wine", "Renomear jogo",
                "Anotações", game.favorite ? "Remover dos favoritos" : "Adicionar aos favoritos",
                "Verificar acesso ao arquivo", "Remover da biblioteca"}, (d, which) -> {
                switch (which) {
                    case 0: message("Motor não disponível", EngineGate.reason(this, game)); break;
                    case 1: chooseBackend(game); break;
                    case 2: chooseGraphics(game); break;
                    case 3: chooseWine(game); break;
                    case 4: editGameText(game, false); break;
                    case 5: editGameText(game, true); break;
                    case 6:
                        game.favorite = !game.favorite;
                        saveGame(game);
                        break;
                    case 7: checkGameAccess(game); break;
                    case 8: confirmRemoveGame(game); break;
                }
            })
            .setNegativeButton("Fechar", null).show();
    }

    private void saveGame(Game game) {
        try { games.put(game); draw(); }
        catch (IOException error) { message("Falha", errorText(error)); }
    }

    private void editGameText(Game game, boolean notes) {
        EditText input = new EditText(this);
        input.setSingleLine(!notes);
        input.setText(notes ? game.notes : game.title);
        input.setSelectAllOnFocus(!notes);
        LinearLayout holder = vertical();
        holder.setPadding(dp(20), dp(8), dp(20), dp(2));
        holder.addView(input);
        new AlertDialog.Builder(this).setTitle(notes ? "Anotações do jogo" : "Renomear jogo")
            .setView(holder).setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                String text = input.getText().toString().trim();
                if (text.length() > (notes ? 1500 : 100))
                    text = text.substring(0, notes ? 1500 : 100);
                if (!notes && text.isEmpty()) {
                    message("Nome inválido", "O título não pode ficar vazio.");
                    return;
                }
                if (notes) game.notes = text; else game.title = text;
                saveGame(game);
            }).show();
    }

    private void chooseWine(Game game) {
        List<RuntimeStore.Installed> local = runtimes.all();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();
        labels.add("Automático (quando houver motor)");
        tags.add("");
        for (RuntimeStore.Installed entry : local) {
            if (!WinePackageInstaller.installed(this, entry)) continue;
            labels.add(entry.tag + " (arquivos extraídos)");
            tags.add(entry.tag);
        }
        new AlertDialog.Builder(this).setTitle("Wine preferido")
            .setSingleChoiceItems(labels.toArray(new String[0]),
                Math.max(0, tags.indexOf(game.runtimeTag)), (d, selected) -> {
                    game.runtimeTag = tags.get(selected);
                    saveGame(game);
                    d.dismiss();
                })
            .setNegativeButton("Cancelar", null).show();
    }

    private void checkGameAccess(Game game) {
        worker.execute(() -> {
            String result;
            try (java.io.InputStream in = getContentResolver().openInputStream(Uri.parse(game.uri))) {
                String arch = PeInspector.inspect(in);
                result = "Acesso de leitura ativo. Arquitetura PE: " + arch;
            }
            catch (Exception error) {
                result = "Arquivo não disponível: " + errorText(error)
                    + ". Adicione novamente pelo seletor de arquivos.";
            }
            final String details = result;
            runOnUiThread(() -> { if (!isDestroyed()) message("Verificação do jogo", details); });
        });
    }

    private void chooseBackend(Game game) {
        String[] items = {"Box64 (previsto)", "FEX (previsto)"};
        new AlertDialog.Builder(this).setTitle("Tradutor para " + game.title)
            .setSingleChoiceItems(items, "FEX".equals(game.backend) ? 1 : 0, (d, index) -> {
                game.backend = index == 0 ? "BOX64" : "FEX";
                try { games.put(game); draw(); }
                catch (IOException error) { message("Falha", errorText(error)); }
                d.dismiss();
            }).setNegativeButton("Cancelar", null).show();
    }

    private void chooseGraphics(Game game) {
        String[] options = {"Sistema (padrão seguro)", "Turnip (Adreno, a validar)"};
        new AlertDialog.Builder(this).setTitle("Driver para " + game.title)
            .setSingleChoiceItems(options, "TURNIP".equals(game.graphics) ? 1 : 0, (d, index) -> {
                game.graphics = index == 0 ? "SYSTEM" : "TURNIP";
                try { games.put(game); draw(); }
                catch (IOException error) { message("Falha", errorText(error)); }
                d.dismiss();
            }).setNegativeButton("Cancelar", null).show();
    }

    private void confirmRemoveGame(Game game) {
        new AlertDialog.Builder(this).setTitle("Remover " + game.title + "?")
            .setMessage("Somente a entrada na biblioteca será removida. O arquivo original "
                + "do jogo permanece no armazenamento do Android.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Remover", (d, w) -> {
                try { games.remove(game.id); draw(); }
                catch (IOException error) { message("Falha", errorText(error)); }
            }).show();
    }

    private void renderRuntimes() {
        section("Runtimes", "Componentes baixados de fontes abertas e verificáveis.");
        LinearLayout intro = card();
        add(intro, "Wine e Proton", 17, WHITE, true, 7);
        add(intro, "Catálogo: Kron4ek/Wine-Builds. Builds Linux x86_64 WoW64 "
            + "com SHA-256 publicado. Pacotes Android/Bionic incompatíveis "
            + "são excluídos do catálogo.", 13, MUTED, false, 4);
        action(intro, "Buscar versões no GitHub", this::findReleases, true);

        LinearLayout availability = card();
        add(availability, "Motor de execução", 16, WHITE, true, 6);
        add(availability, "Preparação Wine disponível: após baixar, você pode extrair "
            + "o pacote em uma pasta isolada, com SHA-256 e verificação de ELF. "
            + "Isso não inclui Box64/FEX, rootfs, DXVK ou execução de jogos.",
            12, MUTED, false, 0);

        LinearLayout downloaded = card();
        add(downloaded, "Pacotes verificados", 16, WHITE, true, 8);
        if (runtimes.isDamaged()) {
            add(downloaded, "Registro danificado. Arquivos preservados; downloads bloqueados.",
                12, MUTED, false, 0);
        }
        else {
            List<RuntimeStore.Installed> entries;
            try { entries = runtimes.all(); }
            catch (Exception error) {
                add(downloaded, "Não foi possível acessar o armazenamento privado.",
                    12, MUTED, false, 0);
                return;
            }
            if (entries.isEmpty()) add(downloaded, "Nenhum pacote baixado.", 12, MUTED, false, 0);
            for (RuntimeStore.Installed entry : entries) {
                add(downloaded, entry.tag + "  ·  " + entry.bytes / 1048576 + " MB",
                    13, WHITE, true, 4);
                boolean prepared = WinePackageInstaller.installed(this, entry);
                add(downloaded, prepared ? "Arquivos Wine preparados · motor não disponível"
                    : "Download SHA-256 verificado · ainda não preparado",
                    11, MUTED, false, 1);
                if (!prepared)
                    action(downloaded, "Preparar arquivos Wine", () -> prepareWine(entry), true);
                else
                    action(downloaded, "Remover preparação", () -> removeWinePreparation(entry), false);
                action(downloaded, "Remover download", () -> removeRuntime(entry), false);
            }
        }
    }

    private void findReleases() {
        if (busy) { message("Operação em andamento", "Conclua a operação atual."); return; }
        busy = true;
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Consultando GitHub…");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
        worker.execute(() -> {
            try {
                ArrayList<ReleaseEntry> results = RuntimeCatalog.search();
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (isFinishing() || isDestroyed()) return;
                    String[] labels = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) labels[i] = results.get(i).toString();
                    new AlertDialog.Builder(this).setTitle("Escolher versão")
                        .setItems(labels, (d, selected) -> confirmDownload(results.get(selected)))
                        .setNegativeButton("Cancelar", null).show();
                });
            }
            catch (Exception error) {
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (!isFinishing() && !isDestroyed())
                        message("Não foi possível listar releases", errorText(error));
                });
            }
        });
    }

    private void confirmDownload(ReleaseEntry entry) {
        new AlertDialog.Builder(this).setTitle(entry.toString())
            .setMessage("Origem: GitHub / Kron4ek\nSHA-256: " + entry.sha256
                + "\n\nO pacote será baixado com verificação SHA-256. Depois você "
                + "poderá preparar os arquivos Wine isoladamente, sem executá-los. "
                + "Jogos exigem um motor ARM64 adicional, ainda não incluído.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Baixar", (d, w) -> downloadRuntime(entry)).show();
    }

    private void downloadRuntime(ReleaseEntry entry) {
        if (busy) { message("Operação em andamento", "Conclua o download atual."); return; }
        busy = true;
        cancelling.set(false);
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Baixando " + entry.tag);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
        dialog.setCancelable(false);
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancelar",
            (d, which) -> cancelling.set(true));
        dialog.show();
        worker.execute(() -> {
            try {
                RuntimeDownloader.download(getApplicationContext(), entry, cancelling,
                    percent -> runOnUiThread(() -> {
                        if (!isDestroyed() && !isFinishing()) dialog.setProgress(percent);
                    }));
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (!isDestroyed() && !isFinishing()) {
                        draw();
                        message("Download verificado", "SHA-256 confirmado. "
                            + "Toque em Preparar arquivos Wine para extrair com segurança. "
                            + "O motor de execução nativo continua em desenvolvimento.");
                    }
                });
            }
            catch (Exception error) {
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (!isDestroyed() && !isFinishing())
                        message("Download não concluído", errorText(error));
                });
            }
        });
    }

    private void prepareWine(RuntimeStore.Installed entry) {
        if (busy) { message("Operação em andamento", "Conclua a operação atual."); return; }
        if (runtimes.isDamaged()) {
            message("Registro inválido", "Runtimes precisam de recuperação antes de instalar.");
            return;
        }
        busy = true;
        cancelling.set(false);
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Preparando Wine " + entry.tag);
        dialog.setMessage("Validando checksum e descompactando sem executar binários…");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancelar",
            (d, which) -> cancelling.set(true));
        dialog.show();

        worker.execute(() -> {
            try {
                WinePackageInstaller.install(getApplicationContext(), entry, cancelling,
                    count -> runOnUiThread(() -> {
                        if (!isDestroyed() && !isFinishing())
                            dialog.setMessage("Verificando e extraindo… " + count + " entradas");
                    }));
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (isDestroyed() || isFinishing()) return;
                    draw();
                    message("Arquivos Wine preparados",
                        "A instalação isolada foi concluída e o executável ELF validado. "
                            + "A execução ainda requer o motor Box64/FEX, rootfs e Vulkan.");
                });
            }
            catch (Exception error) {
                runOnUiThread(() -> {
                    busy = false;
                    dialog.dismiss();
                    if (!isDestroyed() && !isFinishing())
                        message("Falha ao preparar Wine", errorText(error));
                });
            }
        });
    }

    private void removeWinePreparation(RuntimeStore.Installed entry) {
        if (busy) { message("Operação em andamento", "Conclua a operação atual."); return; }
        new AlertDialog.Builder(this).setTitle("Remover arquivos Wine preparados?")
            .setMessage("O arquivo original baixado será preservado. "
                + "Nenhum jogo na biblioteca será apagado.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Remover", (d, w) -> {
                busy = true;
                worker.execute(() -> {
                    Exception failure = null;
                    try { WinePackageInstaller.uninstall(getApplicationContext(), entry); }
                    catch (Exception e) { failure = e; }
                    final Exception error = failure;
                    runOnUiThread(() -> {
                        busy = false;
                        if (isFinishing() || isDestroyed()) return;
                        draw();
                        if (error != null) message("Falha ao remover", errorText(error));
                    });
                });
            }).show();
    }

    private void removeRuntime(RuntimeStore.Installed info) {
        new AlertDialog.Builder(this).setTitle("Remover " + info.tag + "?")
            .setMessage("O arquivo baixado será excluído. Seus jogos serão preservados.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Remover", (d, w) -> {
                try { runtimes.remove(info); draw(); }
                catch (IOException error) { message("Falha", errorText(error)); }
            }).show();
    }

    private void renderSystem() {
        section("Sistema", "Diagnóstico do dispositivo e compatibilidade gráfica.");
        LinearLayout card = card();
        add(card, "GPU e APIs", 17, WHITE, true, 8);
        TextView status = text("Consultando GPU…", 13, MUTED, false);
        card.addView(status);
        worker.execute(() -> {
            GpuInspector.Info info = GpuInspector.inspect(getApplicationContext());
            runOnUiThread(() -> {
                if (!isDestroyed() && tab == 2) {
                    status.setText("Renderizador  " + info.renderer
                        + "\nFabricante  " + info.vendor
                        + "\nOpenGL ES  " + info.opengl
                        + "\nVulkan  " + info.vulkan
                        + "\nABIs  " + info.abi
                        + "\n\n" + info.recommendation);
                }
            });
        });
        LinearLayout note = card();
        add(note, "Política de segurança", 16, WHITE, true, 8);
        add(note, "VeloWin não altera frequências da GPU, root, ADB ou "
            + "bibliotecas do sistema. A disponibilidade real de Vulkan precisa "
            + "ser testada com um motor carregado.", 13, MUTED, false, 0);
    }

    private void renderAbout() {
        section("Sobre o VeloWin", "Uma plataforma Android original, aberta e modular.");
        LinearLayout card = card();
        add(card, "VeloWin 0.1.0", 18, WHITE, true, 7);
        add(card, "Interface própria, biblioteca de jogos, seleção de perfis, "
            + "diagnóstico gráfico e downloads verificáveis de Wine/Proton. "
            + "O módulo de execução está em desenvolvimento.", 13, MUTED, false, 10);
        action(card, "Código-fonte no GitHub",
            () -> browse("https://github.com/danilo631/VeloWin"), false);
        action(card, "Arquitetura e roadmap",
            () -> browse("https://github.com/danilo631/VeloWin/blob/main/docs/ARCHITECTURE.md"), false);
        LinearLayout independence = card();
        add(independence, "Projeto independente", 15, WHITE, true, 6);
        add(independence, "Sem reutilizar o código do Winlator. "
            + "Componentes de terceiros serão integrados somente após "
            + "análise de licença, ABI e segurança.", 12, MUTED, false, 0);
    }

    private void browse(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException error) { message("Link", url); }
    }

    private void message(String title, String detail) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(detail)
            .setPositiveButton("Entendi", null).show();
    }

    private String errorText(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage()
            : exception.getClass().getSimpleName();
    }
}
