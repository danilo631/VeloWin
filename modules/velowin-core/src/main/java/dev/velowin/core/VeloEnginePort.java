package dev.velowin.core;

/** Stable Android-free VeloWin API boundary.
 * Changing XServer/Box64 engine no longer changes the launcher contract.
 */
public interface VeloEnginePort {
    enum LaunchKind { WINDOWS_DESKTOP, WINDOWS_PROGRAM, LINUX_SESSION }
    final class LaunchRequest {
        public final int containerId;
        public final LaunchKind kind;
        public final String executable;
        public LaunchRequest(int containerId,LaunchKind kind,String executable) {
            if(containerId<1)throw new IllegalArgumentException("containerId");
            if(kind==null)throw new IllegalArgumentException("kind");
            if(kind==LaunchKind.WINDOWS_PROGRAM
                &&(executable==null||executable.trim().isEmpty()))
                throw new IllegalArgumentException("program");
            this.containerId=containerId;this.kind=kind;
            this.executable=executable==null?"":executable;
        }
    }
    /** Throws on unavailable runtime; must never fake success. */
    void launch(LaunchRequest request);
}
