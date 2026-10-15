package com.winlator.cmod.velowin;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.winlator.cmod.runtime.display.environment.ImageFs;
import com.winlator.cmod.runtime.display.environment.ImageFsInstaller;
import com.winlator.cmod.runtime.system.SessionKeepAliveService;
import dev.velowin.core.VeloInstallCoordinator;

/** VeloWin-owned runtime installation/repair UI.
 * Actual ZSTD archive extraction is preserved from the functional engine;
 * filesystem preparation + concurrency control are independent Velo modules.
 *
 * Runs explicit, confirmed operations only; never deletes a Wineprefix.
 */
public final class VeloRuntimeInstallActivity extends Activity {
    private final int background=0xff0a1521,ink=0xffe3f7f7,accent=0xff68dfbf;
    private TextView status;
    private ProgressBar progress;
    private Button install;
    private volatile boolean working;

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String content,int size){
        TextView t=new TextView(this);
        t.setText(content);t.setTextColor(ink);t.setTextSize(size);
        t.setPadding(0,dp(8),0,dp(8));return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(background);
        layout.setPadding(dp(22),dp(22),dp(22),dp(20));
        TextView title=text("VELO / INSTALAÇÃO",22);
        title.setTextColor(accent);layout.addView(title);
        layout.addView(text("Instalador e reparador VeloWin para ImageFS, Wine Bionic, "
            +"bibliotecas e drivers. Preserva HOME, prefixos e runtimes Wine "
            +"adicionais instalados no contêiner. O compositor X11/áudio e o "
            +"motor gráfico da v22 permanecem integrados.",13));
        status=text("",14);layout.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgress(0);
        layout.addView(progress,new LinearLayout.LayoutParams(-1,dp(12)));
        install=new Button(this);
        install.setAllCaps(false);install.setText("Instalar / reparar ImageFS");
        install.setOnClickListener(v->confirmInstall());
        layout.addView(install);
        Button back=new Button(this);
        back.setText("Voltar");back.setAllCaps(false);
        back.setOnClickListener(v->finish());layout.addView(back);
        setContentView(layout);
        updateState();
    }
    private void updateState(){
        boolean ready=ImageFs.find(this).isUpToDate();
        long mb=getFilesDir().getUsableSpace()/(1024L*1024L);
        status.setText((ready?"Ambiente principal instalado e válido.":"ImageFS ausente ou incompleto.")
            +"\nEspaço disponível: "+mb+" MiB."
            +"\nWineprefix e saves: preservados durante reparação."
            +"\nProteção: bloqueio de instalações simultâneas."
            +"\nInstalador: sem extração de arquivos arbitrários.");
        install.setEnabled(!working&&!VeloInstallCoordinator.isImageFsBusy());
    }
    private void confirmInstall(){
        if(working||VeloInstallCoordinator.isImageFsBusy()){
            new AlertDialog.Builder(this).setMessage("Outra instalação já está em andamento.")
                .setPositiveButton("OK",null).show();
            return;
        }
        if(SessionKeepAliveService.isSessionActive()){
            new AlertDialog.Builder(this)
                .setTitle("Há um jogo Windows aberto")
                .setMessage("Encerre o Wine/XServer antes de reparar o ambiente.")
                .setPositiveButton("OK",null).show();
            return;
        }
        long free=getFilesDir().getUsableSpace();
        if(free<1300L*1024L*1024L){
            new AlertDialog.Builder(this)
                .setTitle("Espaço insuficiente")
                .setMessage("A instalação completa exige espaço temporário para descompactação. "
                    +"Libere pelo menos 1,3 GiB antes de prosseguir; "
                    +"saves e Wineprefix não serão apagados.")
                .setPositiveButton("OK",null).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Instalar ou reparar o runtime VeloWin?")
            .setMessage("Os arquivos do sistema Linux e drivers serão reinstalados. "
                +"Seus diretórios HOME, contêineres e instalações Wine opcionais "
                +"permanecerão preservados. Um reparo pode demorar e não "
                +"deve ser interrompido enquanto os arquivos são gravados.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Continuar",(d,w)->installNow())
            .show();
    }
    private void installNow(){
        if(working)return;
        working=true;
        install.setEnabled(false);
        status.setText("Preparando atualização segura do ImageFS...");
        ImageFsInstaller.installFromAssets(this,new ImageFsInstaller.ProgressListener(){
            @Override public void onProgress(int value){
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    progress.setProgress(value);
                    status.setText("Instalando runtime VeloWin: "+value+"%");
                });
            }
            @Override public void onFinished(boolean reported){
                runOnUiThread(()->{
                    working=false;
                    if(isFinishing()||isDestroyed())return;
                    boolean verified=reported&&ImageFs.find(VeloRuntimeInstallActivity.this)
                        .isUpToDate();
                    progress.setProgress(verified?100:0);
                    updateState();
                    new AlertDialog.Builder(VeloRuntimeInstallActivity.this)
                        .setTitle(verified?"Runtime instalado":"Instalação não concluída")
                        .setMessage(verified?
                            "Sistema instalado. Execute um contêiner para validar Wine/X11.":
                            "A instalação falhou ou foi recusada. Verifique o espaço e os logs "
                                +"antes de tentar novamente. Seus prefixos não foram removidos.")
                        .setPositiveButton("OK",null).show();
                });
            }
        });
    }
}
