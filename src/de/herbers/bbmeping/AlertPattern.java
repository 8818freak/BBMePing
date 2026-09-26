package de.herbers.bbmeping;

import android.net.Uri;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ein vollstaendig einstellbares Ton-/Wiederholungsmuster: N Toene je Stoß,
 * eine Pause zwischen den Toenen innerhalb eines Stoßes, ein laengerer
 * Abstand zwischen den Stoessen, eine Anzahl Wiederholungen (Stoesse) und
 * eine Lautstaerke, optional von einem Start- zu einem Maximalwert
 * ansteigend ueber die Wiederholungen hinweg (Mathias' Wunsch: "Ton, Ton,
 * Ton, Ton, laengere Pause, dann wieder Ton, Ton, Ton, Ton, ...").
 *
 * Ein Muster gilt nur fuer bestimmte Telefon-Modi (sonst wuerde eine fuer
 * "lautlos" gedachte Alarmserie auch im Normalbetrieb ausloesen, wo der ganz
 * normale Benachrichtigungston der Quell-App ohnehin schon reicht).
 */
final class AlertPattern {

    int tonesPerBurst = 4;
    int burstCount = 3;
    int toneGapMs = 500;
    int burstGapMs = 4000;
    int volumeStartPct = 60;
    int volumeMaxPct = 100;
    boolean rampEnabled = true;
    Set<Mode> modes = EnumSet.of(Mode.SILENT);
    /** null = automatisch aus dem Benachrichtigungskanal der ausloesenden
     *  Nachricht uebernehmen (siehe SoundLocator) - der uebliche Fall.
     *  Nicht-null nur, wenn der Nutzer selbst eine eigene Datei gewaehlt hat. */
    String soundUri;

    AlertPattern copy() {
        AlertPattern p = new AlertPattern();
        p.tonesPerBurst = tonesPerBurst;
        p.burstCount = burstCount;
        p.toneGapMs = toneGapMs;
        p.burstGapMs = burstGapMs;
        p.volumeStartPct = volumeStartPct;
        p.volumeMaxPct = volumeMaxPct;
        p.rampEnabled = rampEnabled;
        p.modes = EnumSet.copyOf(modes);
        p.soundUri = soundUri;
        return p;
    }

    Uri soundUriParsed() {
        return soundUri == null ? null : Uri.parse(soundUri);
    }

    /** Lautstaerke (0..1) fuer den wievielten Stoss (0-basiert) - linear von
     *  volumeStartPct zu volumeMaxPct ueber alle Stoesse, oder konstant auf
     *  volumeStartPct, wenn rampEnabled aus ist. */
    float volumeForBurst(int burstIndex) {
        if (!rampEnabled || burstCount <= 1) return clamp01(volumeStartPct / 100f);
        float t = burstIndex / (float) (burstCount - 1);
        float pct = volumeStartPct + t * (volumeMaxPct - volumeStartPct);
        return clamp01(pct / 100f);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    // ---------- Serialisierung (SharedPreferences-Wert, ein String) ----------
    // Reihenfolge: tonesPerBurst|burstCount|toneGapMs|burstGapMs|
    // volumeStartPct|volumeMaxPct|rampEnabled(0/1)|modeBitmask|soundUri(oder leer)

    String encode() {
        int bits = 0;
        for (Mode m : modes) bits |= (1 << m.ordinal());
        StringBuilder sb = new StringBuilder();
        sb.append(tonesPerBurst).append('|')
          .append(burstCount).append('|')
          .append(toneGapMs).append('|')
          .append(burstGapMs).append('|')
          .append(volumeStartPct).append('|')
          .append(volumeMaxPct).append('|')
          .append(rampEnabled ? 1 : 0).append('|')
          .append(bits).append('|')
          .append(soundUri == null ? "" : soundUri);
        return sb.toString();
    }

    static AlertPattern decode(String s) {
        AlertPattern p = new AlertPattern();
        if (s == null || s.isEmpty()) return p;
        try {
            String[] f = s.split("\\|", -1);
            p.tonesPerBurst = Integer.parseInt(f[0]);
            p.burstCount = Integer.parseInt(f[1]);
            p.toneGapMs = Integer.parseInt(f[2]);
            p.burstGapMs = Integer.parseInt(f[3]);
            p.volumeStartPct = Integer.parseInt(f[4]);
            p.volumeMaxPct = Integer.parseInt(f[5]);
            p.rampEnabled = "1".equals(f[6]);
            int bits = Integer.parseInt(f[7]);
            p.modes = EnumSet.noneOf(Mode.class);
            for (Mode m : Mode.values()) if ((bits & (1 << m.ordinal())) != 0) p.modes.add(m);
            if (f.length > 8 && !f[8].isEmpty()) p.soundUri = f[8];
        } catch (Exception ignored) {
            return new AlertPattern();
        }
        return p;
    }
}
