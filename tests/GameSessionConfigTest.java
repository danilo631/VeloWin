import dev.velowin.app.engine.GameSessionConfig;
public final class GameSessionConfigTest {
    private static void require(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        require(GameSessionConfig.supportedResolution("1280x720"),"resolution");
        require(!GameSessionConfig.supportedResolution("1280x720; rm -rf /"),"injection");
        require("1280x720".equals(GameSessionConfig.resolution("../../")),"fallback");
        require(GameSessionConfig.fps(144)==60,"invalid FPS");
        require(GameSessionConfig.fps(0)==0,"unlimited");
        require(GameSessionConfig.graphics("VKD3D")==GameSessionConfig.Graphics.VKD3D,"D3D12");
        require(GameSessionConfig.graphics("turnip")==GameSessionConfig.Graphics.WINED3D,"fallback");
        require("960x540".equals(GameSessionConfig.recommendation(3L*1024*1024*1024,true,false)),"low RAM");
        System.out.println("GameSessionConfigTest PASS");
    }
}
