# North Star — Android Companion App (v1)

Hands-free emergency SOS: a trigger grabs your GPS and **texts it to your North Star panic line** — no data
connection and no link to tap. The backend (North Star Phase B) parses the `lat,lon` straight from the SMS and
alerts your contacts with a map pin.

> ⚠️ **Not a replacement for 911.** North Star alerts *your own* emergency contacts. If you can call 911, do.

## Triggers in v1
- **Big SOS button** in the app (with a confirm tap).
- **Quick Settings tile** ("North Star SOS") — swipe down, one tap, works from the lock screen.
- **Notification action** — the ongoing "North Star is ready" notification has an SOS button.
- **Voice (beta, off by default)** — say "North Star, help me". Uses Android's on-device speech recognizer;
  it has gaps and costs battery. The **production wake-word is Picovoice Porcupine** (see below).

## Get the APK (no Android Studio needed)
This repo builds the APK in the cloud via **GitHub Actions**:
1. Open the **Actions** tab → latest **"Build North Star APK"** run → **Artifacts** → download `northstar-debug-apk`.
2. Unzip → you get `app-debug.apk`.
3. On your phone: **Settings → allow install from this source**, then open the APK to install.

To rebuild after a change: push to `main`, or Actions tab → Run workflow.

## First run
1. Grant **SMS**, **Location (Allow all the time** for the lock-screen/voice case**)**, and **Notifications**.
2. Set your **name** and confirm the **panic line number** (defaults to the North Star line `+13148664423`).
3. Tap **Save & Arm**. Add the **North Star SOS** Quick Settings tile (edit your quick-settings panel).
4. **Test it:** tap SOS → you should get the alert flow from North Star. (Keep North Star in **test mode**
   server-side while testing so contacts aren't alarmed.)

## Battery / reliability
- Exempt North Star from battery optimization (Settings → Battery → unrestricted) so the guardian stays alive.
- The guardian restarts after reboot.

## Production wake-word (next drop-in)
`VoiceTrigger.kt` is written as a replaceable module. To ship a true always-on, low-power, offline custom
wake-word ("North Star, help me"):
1. Create a free **Picovoice** account → get an **AccessKey**.
2. Train the keyword in the Picovoice console → download the `.ppn` file → put it in `app/src/main/assets/`.
3. Add the Porcupine Android dependency and swap `VoiceTrigger`'s internals for the Porcupine manager.
(Keep the AccessKey out of git — inject it as a Gradle/CI secret.)

## Roadmap (see NorthStar/NORTHSTAR_TIER2_SCOPE.md)
- **C1 (this):** triggers → GPS → SOS SMS.
- **C2:** background-while-locked location (dynamic location FGS type + background-location permission),
  send retries, weak-signal queue, Porcupine wake-word.
- **D:** siren/strobe, audio clip + transcription, PIN/stealth mode.
- **E (SaaS):** Play Store (SMS-permission declaration + disclaimers), own A2P campaign, accounts.

## Project layout
- `app/src/main/java/com/macvision/northstar/` — `SosSender` (GPS+SMS), `SosService` (guardian),
  `SosTileService` (tile), `VoiceTrigger` (voice beta), `MainActivity` (UI), `Prefs`, `BootReceiver`.
- `.github/workflows/android-build.yml` — cloud build → APK artifact.
