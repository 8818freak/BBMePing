package de.herbers.bbmeping;

import android.app.Notification;
import android.net.Uri;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import de.herbers.common.Notifications;

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
        if (Notifications.isGroupSummary(n)) return;

        Mode mode = Mode.current(this);

        // Absender = Titel der Benachrichtigung (gemeinsame Extraktion).
        String senderName = Notifications.titleAndText(n)[0];
        String lookupKey = ContactMatcher.lookupKeyForName(this, senderName);

        AlertPattern pattern = Rules.resolve(this, lookupKey, mode);
        if (pattern == null) return; // in diesem Modus fuer diesen Absender bewusst nichts

        Uri sound = SoundLocator.resolve(this, BBM_PACKAGE, n.getChannelId(), pattern);
        if (sound == null) return; // kein Ton ermittelbar - lieber nichts als das Falsche abspielen

        // Merken, welcher Ton bei einer echten Nachricht tatsaechlich
        // ermittelt wurde - damit der "Testen"-Knopf in den Einstellungen
        // (der selbst keinen Kanal einer laufenden Benachrichtigung hat)
        // spaeter etwas Sinnvolleres abspielen kann als einen Systemton.
        Rules.setLastKnownAutoSound(this, sound.toString());

        AlertPlayer.play(this, sbn.getKey(), sound, pattern);
    }
}
