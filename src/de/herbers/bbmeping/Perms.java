package de.herbers.bbmeping;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import de.herbers.common.PermReminder;

import java.util.ArrayList;
import java.util.List;

/** Eine Quelle fuer BBMe Pings Berechtigungen - fuer die Erinnerung bei Verlust
 *  (PermReminder) UND den aufklappbaren, erklaerten Berechtigungs-Abschnitt. */
final class Perms {
    private Perms() {}

    static boolean notifAccess(Context c) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm != null && nm.isNotificationListenerAccessGranted(
                    new ComponentName(c, NotificationCapture.class));
        }
        String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName());
    }

    private static Intent appDetails(Context c) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + c.getPackageName()));
    }

    static List<PermReminder.Perm> list(Context ctx) {
        boolean con = ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
        List<PermReminder.Perm> l = new ArrayList<>();
        l.add(new PermReminder.Perm("notif", "Benachrichtigungszugriff", notifAccess(ctx),
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                "Der Kern der App: erkennt eingehende BBM-Enterprise-Nachrichten an ihren "
                + "System-Benachrichtigungen und löst den eingestellten Ton/Vibration aus."));
        l.add(new PermReminder.Perm("contacts", "Kontakte", con, appDetails(ctx),
                "Um Ton-Regeln pro Person zu treffen (Absender an den Kontakten erkennen)."));
        return l;
    }

    static void checkReminders(Context ctx) {
        PermReminder.check(ctx, "BBMe Ping", list(ctx));
    }
}
