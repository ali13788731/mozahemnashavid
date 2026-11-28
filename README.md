# Khalvat

Android app (Kotlin + Jetpack Compose) that hides calls and messages by rule (people, days, hours, dates).

## How hiding works
- **Calls:** Khalvat is set as the *call screening app* (system role). Matching calls can be silenced, rejected,
  hidden from the screen/notification and removed from the call log.
- **Messages:** Khalvat is set as the *default SMS app* (system role). Hidden messages are not written to the phone
  inbox; a copy is kept in the encrypted private archive. Other messages are written to the inbox normally and
  shown as notifications that open your previous messaging app.
  Limitation: picture messages (MMS) are not supported while Khalvat is the default SMS app.

## Start-up
A setup screen asks for everything at first launch (permissions → call screening → default SMS app).
The app stays blocked until all three are granted. A background check (~15 min) shows a heads-up
notification if any of them is revoked later.

## UI flow
Home (status, focus mode, rules) → **Add** → choose *Calls* or *Messages* → rule settings.
Archive and Settings are icons in the header. English by default, Persian (with Jalali calendar) when the phone language is Persian.
The launch screen is static (no animation). There is no app lock.

## Build
Open in Android Studio (Ladybug+), or `gradle :app:assembleDebug`. Test on a real device (Android 10+).
This version has not been run on a device yet.
