package de.herbers.bbmeping;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Listet die Toene der tatsaechlich installierten BBM-Enterprise-App direkt
 * auf, ueber ihre eigenen Ressourcen-Namen (aus dem Decompile bekannt, hier
 * nur als Text-Liste - keine Audiodatei wird kopiert oder mitgeliefert).
 * Aufgeloest wird ausschliesslich zur Laufzeit gegen die Resourcen der
 * jeweils gerade installierten BBM-Version (Resources.getIdentifier), so
 * dass sich das automatisch an ein BBM-Update anpasst: Namen, die es nicht
 * mehr gibt, tauchen einfach nicht mehr auf.
 *
 * Die entstehende android.resource://-URI verweist dauerhaft auf die fremde
 * App und ihre Datei - es wird nichts davon in dieser App gespeichert.
 */
final class InstalledSounds {

    static final String BBM_PACKAGE = "com.bbm.enterprise";

    /** Aus dem Decompile der aktuellen BBM-Enterprise-Version bekannte
     *  res/raw-Namen (Stand 2026); nur Namen, keine Dateien. */
    private static final String[] CANDIDATE_RAW_NAMES = {
            "high_priority",
            "bbm_notification",
            "contentment",
            "bbm_incoming_call",
            "bbm_outgoing_call",
            "bbm_end_call",
            "voice_recording_start",
            "voice_recording_stop",
    };

    static final class NamedSound {
        final String rawName;
        final Uri uri;
        NamedSound(String rawName, Uri uri) { this.rawName = rawName; this.uri = uri; }
    }

    private InstalledSounds() {}

    static boolean isBbmInstalled(PackageManager pm) {
        try {
            pm.getPackageInfo(BBM_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Alle Kandidaten, die sich in der gerade installierten BBM-Version
     *  tatsaechlich aufloesen lassen - leer, wenn BBM fehlt oder keiner der
     *  bekannten Namen (mehr) existiert. */
    static List<NamedSound> resolve(PackageManager pm) {
        List<NamedSound> out = new ArrayList<>();
        Resources bbmRes;
        try {
            bbmRes = pm.getResourcesForApplication(BBM_PACKAGE);
        } catch (Throwable t) {
            return out;
        }
        for (String name : CANDIDATE_RAW_NAMES) {
            int id = bbmRes.getIdentifier(name, "raw", BBM_PACKAGE);
            if (id != 0) {
                out.add(new NamedSound(name, Uri.parse("android.resource://" + BBM_PACKAGE + "/" + id)));
            }
        }
        return out;
    }

    /** Liest den eigentlich sprechenden Namen fuer einen bekannten Kandidaten
     *  (fuer die Anzeige); unbekannte/neue Namen einfach unveraendert. */
    static String friendlyName(android.content.Context ctx, String rawName) {
        switch (rawName) {
            case "high_priority": return ctx.getString(R.string.bbmsound_high_priority);
            case "bbm_notification": return ctx.getString(R.string.bbmsound_notification);
            case "contentment": return ctx.getString(R.string.bbmsound_contentment);
            case "bbm_incoming_call": return ctx.getString(R.string.bbmsound_incoming_call);
            case "bbm_outgoing_call": return ctx.getString(R.string.bbmsound_outgoing_call);
            case "bbm_end_call": return ctx.getString(R.string.bbmsound_end_call);
            case "voice_recording_start": return ctx.getString(R.string.bbmsound_voice_start);
            case "voice_recording_stop": return ctx.getString(R.string.bbmsound_voice_stop);
            default: return rawName;
        }
    }

    static boolean isBbmOwnUri(String soundUri) {
        return soundUri != null && soundUri.startsWith("android.resource://" + BBM_PACKAGE + "/");
    }
}
