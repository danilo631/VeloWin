package dev.velowin.app.engine;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

/**
 * Per-session opt-in for an externally hosted loopback X11/PulseAudio service.
 * Connectivity only proves the port accepts TCP, NOT an authenticated X11 or
 * PulseAudio protocol handshake or Wine graphical compatibility.
 */
public final class LocalBridgeManager {
    private LocalBridgeManager(){}

    private static SharedPreferences store(Context ctx) {
        return ctx.getSharedPreferences("velowin-session-bridges-v1",Context.MODE_PRIVATE);
    }

    private static String key(String gameId){
        if(gameId==null)throw new IllegalArgumentException("Sessão sem ID.");
        UUID value=UUID.fromString(gameId);
        if(!value.toString().equals(gameId.toLowerCase(java.util.Locale.ROOT)))
            throw new IllegalArgumentException("Identificador de sessão não canônico.");
        return value.toString();
    }

    public static BridgeProfile get(Context ctx,String sessionId) {
        try {
            String id=key(sessionId);
            SharedPreferences p=store(ctx);
            int display=p.getInt(id+".display",-1);
            boolean pulse=p.getBoolean(id+".pulse",false);
            return BridgeProfile.of(display,pulse);
        }catch(IllegalArgumentException | ClassCastException ignored){
            return BridgeProfile.disabled();
        }
    }

    public static void put(Context ctx,String sessionId,BridgeProfile profile)
        throws IOException {
        String id=key(sessionId);
        if(profile==null)throw new IOException("Configuração vazia.");
        if(!store(ctx).edit().putInt(id+".display",profile.display)
            .putBoolean(id+".pulse",profile.pulseAudio).commit())
            throw new IOException("Falha ao salvar preferência de X11/áudio.");
    }

    public static boolean isListening(int port) {
        if(port<=0)return false;
        try(Socket client=new Socket()) {
            client.connect(new InetSocketAddress("127.0.0.1",port),900);
            return true;
        }catch(IOException | SecurityException failure) {return false;}
    }

    public static String inspectX11(int port) {
        if(port<6000 || port>6009)return "DISPLAY fora do intervalo permitido.";
        try(Socket client=new Socket()) {
            client.connect(new InetSocketAddress("127.0.0.1",port),900);
            client.setSoTimeout(1200);
            client.getOutputStream().write(X11Protocol.setupRequest());
            client.getOutputStream().flush();
            byte[] header=new byte[8];
            int read=0;
            while(read<header.length) {
                int n=client.getInputStream().read(header,read,header.length-read);
                if(n<0)break;
                read+=n;
            }
            return X11Protocol.description(X11Protocol.parseReply(
                read==header.length?header:null));
        }catch(IOException | SecurityException failure){
            return "Servidor X11 TCP indisponível em 127.0.0.1:"+port+".";
        }
    }

    public static String probe(BridgeProfile profile) {
        StringBuilder b=new StringBuilder("Pontes TCP locais — sem autenticar protocolos:\n");
        if(profile.x11Enabled()) {
            b.append("X11 127.0.0.1:").append(profile.x11Port())
                .append(": ").append(inspectX11(profile.x11Port()))
                .append("\n");
        }else b.append("X11: desativado\n");
        if(profile.pulseAudio) {
            b.append("PulseAudio 127.0.0.1:4713: ")
                .append(isListening(profile.pulsePort())?"PORTA ACESSÍVEL":"SEM SERVIDOR")
                .append("\n");
        }else b.append("PulseAudio: desativado\n");
        b.append("Handshake X11 não comprova janelas Wine, som Win32 ou jogos. ")
            .append("Use apenas servidores locais que você controla.");
        return b.toString();
    }
}
