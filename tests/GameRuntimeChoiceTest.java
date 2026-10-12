import dev.velowin.app.engine.GameRuntimeChoice;

public class GameRuntimeChoiceTest {
    private static void check(boolean condition,String name) {
        if(!condition)throw new AssertionError(name);
    }
    public static void main(String[] ignored) {
        String value=GameRuntimeChoice.bionic("proton-11.0-2-20261010","x86_64");
        GameRuntimeChoice choice=GameRuntimeChoice.fromStored(value);
        check(choice.kind==GameRuntimeChoice.Kind.BIONIC,"Bionic parsed");
        check("x86_64".equals(choice.arch),"arch roundtrip");
        check(GameRuntimeChoice.fromStored("").kind==GameRuntimeChoice.Kind.AUTO,"automatic");
        check(GameRuntimeChoice.fromStored("proton-10.0-1").kind==
              GameRuntimeChoice.Kind.GLIBC,"legacy glibc");
        check(GameRuntimeChoice.fromStored("BIONIC:foo:x86_64").kind==
              GameRuntimeChoice.Kind.INVALID,"bad tag");
        check(GameRuntimeChoice.fromStored("BIONIC:proton-11.0-2-20261010:arm64").kind==
              GameRuntimeChoice.Kind.INVALID,"bad ABI");
        check(GameRuntimeChoice.fromStored("../../").kind==
              GameRuntimeChoice.Kind.INVALID,"path traversal");
        System.out.println("GameRuntimeChoiceTest PASS");
    }
}
