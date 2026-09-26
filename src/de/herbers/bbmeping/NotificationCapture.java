package de.herbers.bbmeping;

import android.app.Notification;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Beobachtet Benachrichtigungen von BBM Enterprise (com.bbm.enterprise) und
 * loest bei der Prioritaets-Kategorie ("PING"-Nachfolger) das konfigurierte
 * Ton-/Wiederholungsmuster aus - abhaengig vom aktuellen Telefon-Modus und,
 * falls zuordenbar, vom Absender (siehe Rules.resolve).
 *
 * Die Erkennungsbedingung (Kanal-ID enthaelt "priority") wurde durch
 * Dekompilieren der aktuellen BBM-Enterprise-APK bestaetigt: der Kanal heisst
 * dort "com.bbm.enterprise.notification_priority_v3".
 */
public class NotificationCapture extends NotificationListenerService {

    private static final String BBM_PACKAGE = "com.bbm.enterprise";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            handle(sbn);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) AlertPlayer.stopIfMatches(sbn.getKey());
    }

    private void handle(StatusBarNotification sbn) {
        if (sbn == null || !BBM_PACKAGE.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null || n.getChannelId() == null || !n.getChannelId().contains("priority")) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        Mode mode = Mode.current(this);

        Bundle extras = n.extras;
        String senderName = extras == null ? null
                : String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, ""));
        String lookupKey = ContactMatcher.lookupKeyForName(this, senderName);

        AlertPattern pattern = Rules.resolve(this, lookupKey, mode);
        if (pattern == null) return; // in diesem Modus fuer diesen Absender bewusst nichts

        Uri sound = SoundLocator.resolve(this, BBM_PACKAGE, n.getChannelId(), pattern);
        if (sound == null) return; // kein Ton ermittelbar - lieber nichts als das Falsche abspielen

        AlertPlayer.play(this, sbn.getKey(), sound, pattern);
    }
}
