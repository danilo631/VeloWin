import dev.velowin.app.engine.FailureClassifier;
public final class FailureClassifierTest {
    static void ok(boolean ok,String name){if(!ok)throw new AssertionError(name);}
    public static void main(String[] a){
        ok(FailureClassifier.explain("CANNOT LINK EXECUTABLE",1,false).contains("linker"),"bionic loader");
        ok(FailureClassifier.explain("wrong ELF class",1,false).contains("Arquitetura"),"ABI");
        ok(FailureClassifier.explain("library libfoo.so not found",1,false).contains("Biblioteca"),"missing so");
        ok(FailureClassifier.explain("Cannot open display",1,false).contains("X11"),"display");
        ok(FailureClassifier.explain("DXVK Vulkan failed",1,false).contains("Vulkan"),"graphics");
        ok(FailureClassifier.explain("",-1,true).contains("tempo limite"),"timeout");
        ok(FailureClassifier.explain("wine-11.0",0,false).contains("não comprova"),"false success");
        System.out.println("FailureClassifierTest PASS");
    }
}
