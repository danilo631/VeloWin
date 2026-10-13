import dev.velowin.app.engine.X11Protocol;

public class X11ProtocolTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static byte[] response(int success,int major,int minor){
        return new byte[]{(byte)success,0,(byte)major,(byte)(major>>>8),
            (byte)minor,(byte)(minor>>>8),0,0};
    }
    public static void main(String[] args){
        byte[] request=X11Protocol.setupRequest();
        check(request.length==12,"X11 setup length");
        check(request[0]=='l' && request[2]==11 && request[3]==0,"11.0 LE");
        check(X11Protocol.parseReply(response(1,11,0))==X11Protocol.Reply.ACCEPTED,
            "accepted X11");
        check(X11Protocol.parseReply(response(0,11,0))==X11Protocol.Reply.DENIED,
            "auth denied");
        check(X11Protocol.parseReply(response(2,11,0))==X11Protocol.Reply.AUTHENTICATE,
            "challenge");
        check(X11Protocol.parseReply(response(1,10,0))==X11Protocol.Reply.NOT_X11,
            "wrong major");
        check(X11Protocol.parseReply(new byte[0])==X11Protocol.Reply.NOT_X11,
            "empty");
        check(X11Protocol.parseReply(null)==X11Protocol.Reply.NOT_X11,
            "null");
        System.out.println("X11ProtocolTest PASS");
    }
}
