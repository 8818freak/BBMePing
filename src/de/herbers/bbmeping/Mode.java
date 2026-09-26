package de.herbers.bbmeping;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;

/**
 * Der fuer die Regel-Engine massgebliche "Modus, in dem sich das Telefon
 * gerade befindet" (Mathias' Formulierung) - aus Klingelton-Modus UND
 * Nicht-stoeren-Zustand zusammengefasst, damit ein aktives "Nicht stoeren"
 * (das Benachrichtigungen ganz unterdrueckt, unabhaengig vom Klingelton-
 * Regler) sich wie SILENT verhaelt, nicht wie der zufaellig eingestellte
 * Klingelton-Modus darunter.
 */
enum Mode {
    NORMAL, VIBRATE, SILENT;

    static Mode current(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int filter = nm.getCurrentInterruptionFilter();
            // ALL = Nicht-stoeren aus; alles andere (PRIORITY/ALARMS/NONE)
            // blendet normale Nachrichten aus - fuer unsere Zwecke wie lautlos.
            if (filter != NotificationManager.INTERRUPTION_FILTER_ALL
                    && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                return SILENT;
            }
        }
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return NORMAL;
        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT: return SILENT;
            case AudioManager.RINGER_MODE_VIBRATE: return VIBRATE;
            default: return NORMAL;
        }
    }

    String label(Context ctx) {
        switch (this) {
            case SILENT: return ctx.getString(R.string.mode_silent);
            case VIBRATE: return ctx.getString(R.string.mode_vibrate);
            default: return ctx.getString(R.string.mode_normal);
        }
    }
}
