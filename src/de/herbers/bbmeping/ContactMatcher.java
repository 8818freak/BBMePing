package de.herbers.bbmeping;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

/**
 * Ordnet den Anzeigenamen einer Benachrichtigung (BBM Enterprise setzt den
 * Absendernamen als Titel) einem Kontakt im Adressbuch zu - moeglich, weil
 * BBM-Kontakte bei Mathias als normale Adressbuch-Kontakte gefuehrt werden.
 * Ohne READ_CONTACTS (optional) liefert das immer null - die Standard-Regel
 * greift dann trotzdem.
 */
final class ContactMatcher {

    private ContactMatcher() {}

    static boolean hasPermission(Context ctx) {
        return ctx.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** LOOKUP_KEY des am besten passenden Kontakts fuer diesen Anzeigenamen,
     *  oder null (kein Treffer / keine Berechtigung). Exakter Namensabgleich -
     *  bewusst kein Fuzzy-Matching, um niemals die falsche Person zu
     *  treffen. */
    static String lookupKeyForName(Context ctx, String displayName) {
        if (displayName == null || displayName.trim().isEmpty() || !hasPermission(ctx)) return null;
        try (Cursor c = ctx.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY},
                ContactsContract.Contacts.DISPLAY_NAME + "=?",
                new String[]{displayName.trim()},
                null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Throwable ignored) {}
        return null;
    }

    static String displayNameForLookupKey(Context ctx, String lookupKey) {
        if (lookupKey == null || !hasPermission(ctx)) return lookupKey;
        android.net.Uri uri = android.net.Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
        try (Cursor c = ctx.getContentResolver().query(uri,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Throwable ignored) {}
        return lookupKey;
    }
}
