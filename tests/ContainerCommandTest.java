import dev.velowin.app.engine.ContainerCommand;

public class ContainerCommandTest {
    private static void ok(boolean yes,String label) {
        if(!yes) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        ok(ContainerCommand.parse("  WINE  --VERSION  ") == ContainerCommand.Action.WINE_VERSION,"Wine");
        ok(ContainerCommand.parse("Box64 -v") == ContainerCommand.Action.BOX64_VERSION,"Box64");
        ok(ContainerCommand.parse("wine cmd /c ver") == ContainerCommand.Action.CMD_VERSION,"cmd");
        ok(ContainerCommand.parse("wineboot -u") == ContainerCommand.Action.WINEBOOT,"boot");
        ok(ContainerCommand.parse("run exe") == ContainerCommand.Action.RUN_EXE,"exe");
        ok(ContainerCommand.parse("wine cmd /c del c:\\\\*") == null,"shell-like denied");
        ok(ContainerCommand.parse("rm -rf /") == null,"shell denied");
        ok(ContainerCommand.parse("wine cmd /c ver; touch /x") == null,"operator denied");
        ok(ContainerCommand.parse("wine --version\nbox64 -v") == null,"multicommand denied");
        ok(ContainerCommand.parse("wine --version && echo hi") == null,"AND denied");
        ok(ContainerCommand.requiresConsent(ContainerCommand.Action.RUN_EXE),"exe consent");
        ok(ContainerCommand.requiresConsent(ContainerCommand.Action.WINEBOOT),"boot consent");
        ok(!ContainerCommand.requiresConsent(ContainerCommand.Action.WINE_VERSION),"version no consent");
        ok(ContainerCommand.manual().contains("wineserver -k"),"documented");
        System.out.println("ContainerCommandTest PASS");
    }
}
