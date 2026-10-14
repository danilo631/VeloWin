package dev.velowin.app;

/**
 * The integrated GPLv3 Xorg/Lorie addon uses its own Android notification
 * and binder initialization. Keep the app identity under dev.velowin.app.
 * XServer launch and Wine/Surface routing require separate runtime validation.
 */
public final class VeloWinApplication extends com.termux.x11.LorieApp {
}
