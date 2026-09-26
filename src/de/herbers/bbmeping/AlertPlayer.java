package de.herbers.bbmeping;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Spielt ein AlertPattern ab: tonesPerBurst Toene im Abstand toneGapMs,
 * dann burstGapMs Pause, das Ganze burstCount mal, mit optional ansteigender
 * Lautstaerke (siehe AlertPattern.volumeForBurst). Jeder einzelne Ton laeuft
 * ueber USAGE_ALARM - der einzige Stream, den der Klingelton-Regler
 * "Lautlos" nicht stummschaltet (dieselbe, oeffentlich dokumentierte Technik
 * wie bei Wecker-Apps, keine Umgehung von Systemschutz).
 *
 * Nur eine Sequenz gleichzeitig - eine neue Ausloesung (oder das Entfernen
 * der ausloesenden Benachrichtigung, siehe NotificationCapture) bricht eine
 * noch laufende Sequenz ab.
 */
final class AlertPlayer {

    private static final String TAG = "BBMePing";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static String currentKey;

    private AlertPlayer() {}

    static synchronized void play(Context ctx, String notificationKey, Uri sound, AlertPattern pattern) {
        stop();
        if (sound == null || pattern.tonesPerBurst <= 0 || pattern.burstCount <= 0) return;
        currentKey = notificationKey;
        Context app = ctx.getApplicationContext();
        burst(app, notificationKey, sound, pattern, 0);
    }

    /** Bricht eine laufende Sequenz ab, wenn sie zu dieser Benachrichtigung
     *  gehoert (z.B. weil sie inzwischen gelesen/geloescht wurde). */
    static synchronized void stopIfMatches(String notificationKey) {
        if (notificationKey != null && notificationKey.equals(currentKey)) stop();
    }

    static synchronized void stop() {
        currentKey = null;
        handler.removeCallbacksAndMessages(null);
    }

    private static void burst(Context ctx, String key, Uri sound, AlertPattern pattern, int burstIndex) {
        if (!key.equals(currentKey) || burstIndex >= pattern.burstCount) return;
        float vol = pattern.volumeForBurst(burstIndex);
        tone(ctx, key, sound, vol, 0, pattern, burstIndex);
    }

    private static void tone(Context ctx, String key, Uri sound, float vol, int toneIndex,
                              AlertPattern pattern, int burstIndex) {
        if (!key.equals(currentKey) || toneIndex >= pattern.tonesPerBurst) {
            if (key.equals(currentKey)) {
                handler.postDelayed(() -> burst(ctx, key, sound, pattern, burstIndex + 1), pattern.burstGapMs);
            }
            return;
        }
        // Die Pause erst NACH dem tatsaechlichen Ende des Tons abwarten, nicht
        // nach einem festen Timer ab dem Start - sonst koennte sich ein Ton,
        // der laenger dauert als toneGapMs, mit dem naechsten ueberlappen
        // (beobachtet mit dem laengeren System-Standardton beim Testen).
        playOnce(ctx, sound, vol, () -> {
            if (!key.equals(currentKey)) return;
            handler.postDelayed(() -> tone(ctx, key, sound, vol, toneIndex + 1, pattern, burstIndex),
                    pattern.toneGapMs);
        });
    }

    private static void playOnce(Context ctx, Uri sound, float volume, Runnable onDone) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(ctx, sound);
            mp.setOnCompletionListener(m -> { m.release(); onDone.run(); });
            mp.setOnErrorListener((m, w, e) -> {
                try { m.release(); } catch (Throwable ignored) {}
                onDone.run();
                return true;
            });
            mp.prepare();
            // setVolume() VOR prepare() wird auf manchen Geraeten stillschweigend
            // ignoriert (beobachtet: Lautstaerke-Einstellung ohne Wirkung) -
            // deshalb erst nach dem Vorbereiten setzen.
            mp.setVolume(volume, volume);
            mp.start();
        } catch (Throwable t) {
            Log.w(TAG, "Ton konnte nicht abgespielt werden: " + sound, t);
            onDone.run(); // Sequenz trotzdem fortsetzen, nicht an einer kaputten Datei haengenbleiben
        }
    }
}
