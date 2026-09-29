package de.herbers.bbmeping;

import android.app.Application;

import de.herbers.common.DiagLog;
import de.herbers.common.Diagnostics;

/**
 * App-Einstieg fuer alle Prozesse (UI + Benachrichtigungs-Listener). Richtet
 * die gemeinsame Diagnose ein: logcat-Tag "BBMePingDiag" und einen
 * Absturz-Logger, der unbehandelte Ausnahmen mit vollem Stack ins
 * Diagnose-Protokoll schreibt (in der "Ueber"-Ansicht einsehbar).
 */
public class BbmePingApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        DiagLog.setTag("BBMePingDiag");
        Diagnostics.installCrashLogger(this);
    }
}
