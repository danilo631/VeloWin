package dev.velowin.app.engine;

/**
 * Minimal X11 setup header parser. No graphics, event handling or X server is
 * implemented here. A successful handshake indicates only that the X server
 * accepted the (unauthenticated) connection on the local TCP port.
 */
public final class X11Protocol {
    public enum Reply { ACCEPTED, DENIED, AUTHENTICATE, NOT_X11 }
    private X11Protocol() {}

    /** X11 setup request, little-endian, protocol 11.0, no auth token. */
    public static byte[] setupRequest() {
        return new byte[]{0x6c,0,11,0,0,0,0,0,0,0,0,0};
    }

    public static Reply parseReply(byte[] header) {
        if(header==null || header.length<8)return Reply.NOT_X11;
        int major=(header[2]&255)|((header[3]&255)<<8);
        int minor=(header[4]&255)|((header[5]&255)<<8);
        if(major!=11 || minor>2)return Reply.NOT_X11;
        int status=header[0]&255;
        if(status==1)return Reply.ACCEPTED;
        if(status==0)return Reply.DENIED;
        if(status==2)return Reply.AUTHENTICATE;
        return Reply.NOT_X11;
    }

    public static String description(Reply result) {
        switch(result) {
            case ACCEPTED:return "X11 respondeu e aceitou handshake sem credenciais "
                +"(isso não prova janelas Wine).";
            case DENIED:return "Servidor X11 identificado, porém acesso negado. "
                +"Não desative autenticação de um servidor exposto à rede.";
            case AUTHENTICATE:return "Servidor X11 respondeu, mas exige autenticação "
                +"que o VeloWin ainda não negocia.";
            default:return "Porta TCP abriu, mas não respondeu como X11 versão 11.";
        }
    }
}
