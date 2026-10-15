package dev.velowin.app.hardware;

import android.app.ActivityManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;

/** Advisory measurements, not privileged performance controls. */
public final class DeviceHealth {
    private DeviceHealth() {}

    public static String snapshot(Context context) {
        StringBuilder out = new StringBuilder("Dispositivo: ")
            .append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
            .append("\nAndroid API: ").append(Build.VERSION.SDK_INT);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            out.append("\nMemória RAM: ").append(gib(info.totalMem))
                .append(" GiB total, ").append(gib(info.availMem))
                .append(" GiB disponível");
            if (info.lowMemory) out.append("\nSistema sob pressão de memória.");
        }
        long free = context.getFilesDir().getUsableSpace();
        out.append("\nEspaço disponível para runtimes: ").append(gib(free)).append(" GiB");
        if (free < 2L * 1024 * 1024 * 1024)
            out.append("\nAviso: espaço baixo para instalar Wine.");
        BatteryManager battery =
            (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (battery != null) {
            int percent = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (percent >= 0 && percent <= 100) out.append("\nBateria: ").append(percent).append("%");
        }
        if (Build.VERSION.SDK_INT >= 29) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                int level = pm.getCurrentThermalStatus();
                String[] statuses = {"normal", "leve", "moderado", "severo",
                    "crítico", "emergência", "desligamento"};
                if (level >= 0 && level < statuses.length)
                    out.append("\nEstado térmico Android: ").append(statuses[level]);
                if (level >= PowerManager.THERMAL_STATUS_SEVERE)
                    out.append("\nReduza carga gráfica: o aparelho relata aquecimento.");
            }
        }
        return out.toString();
    }

    private static String gib(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f", bytes / (1024.0*1024*1024));
    }
}
