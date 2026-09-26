package de.herbers.bbmeping;

import android.app.NotificationChannel;
import android.net.Uri;
import android.os.Process;
import android.service.notification.NotificationListenerService;

import java.util.List;

/**
 * Besorgt die abzuspielende Tondatei, OHNE je selbst eine Kopie eines
 * fremden Tons zu besitzen oder zu verteilen (Mathias: "nicht den netten
 * Entwickler bestehlen"): der Kanal, ueber den die ausloesende Nachricht
 * kam, hat selbst einen konfigurierten Sound (den BBM Enterprise beim
 * Anlegen des Kanals gesetzt hat) - den lesen wir per API aus und spielen
 * genau diese Datei ab, die schon auf dem Geraet liegt (Teil der ohnehin
 * installierten BBM-Enterprise-App). Nichts davon wird kopiert, gecacht
 * oder ins Repo aufgenommen.
 *
 * Nur falls das je fehlschlaegt (z.B. der Kanal hat gar keinen Ton
 * hinterlegt), oder falls der Nutzer bewusst eine eigene Datei gewaehlt hat
 * (AlertPattern.soundUri gesetzt), wird stattdessen diese verwendet.
 */
final class SoundLocator {

    private SoundLocator() {}

    static Uri resolve(NotificationListenerService svc, String pkg, String channelId, AlertPattern pattern) {
        if (pattern.soundUriParsed() != null) return pattern.soundUriParsed();
        try {
            List<NotificationChannel> channels = svc.getNotificationChannels(pkg, Process.myUserHandle());
            if (channels != null) {
                for (NotificationChannel ch : channels) {
                    if (ch.getId().equals(channelId) && ch.getSound() != null) return ch.getSound();
                }
            }
        } catch (Throwable ignored) {
            // Kanal nicht lesbar (z.B. Berechtigung entzogen) - Aufrufer
            // faellt auf einen Systemstandard zurueck.
        }
        return null;
    }
}
