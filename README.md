# MindfulScreen 🧠

*A native Android screen-time helper that turns real, measurable phone usage into gentle,
psychology-backed interventions.*

**Course project — Digital Psychology (Attention Fragmentation / Distraction)**

MindfulScreen reads your **real** per-app usage (via Android's `UsageStatsManager`), shows a
daily **Attention Score** with an emotional avatar, lets you set **goals & per-app limits**,
sends **gentle mindful nudges** when you go over, and charts your **7-day trend** so behaviour
change is measurable — not self-reported.

---

## ✨ Features

| Feature | What it does |
|---------|--------------|
| 📊 **Real Usage Dashboard** | Actual per-app + total screen time, read from the OS |
| 🧠 **Attention Score + Avatar** | Start each day at 100; the avatar reacts as you overuse |
| 🎯 **Goals & Limits** | Daily total goal + per-app limits (e.g. TikTok 30m) |
| 🫠 **Gentle Interventions** | Full-screen mindful nudge when a limit is exceeded |
| 📈 **Insights & Trends** | 7-day chart, "vs. last week" delta, most-used app, late-night usage |

All data stays **on device** (Room + DataStore). No accounts, no cloud, no network.

---

## 🛠 Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **UsageStatsManager** for real usage data
- **Room** (limits, daily scores) + **DataStore** (settings)
- **WorkManager** for periodic limit checks → nudges
- **Navigation-Compose**, MVVM (ViewModel + StateFlow)
- Min SDK 26 (Android 8.0), Target SDK 34

---

## 🚀 Build & Run (Instruction Manual)

> ⚠️ **This app must be built in Android Studio and run on a *real* Android phone.**
> Emulators have no real usage data, and the browser sandbox where the code was authored
> cannot compile Android apps.

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Koala 2024.1.1 or newer)
- A physical Android phone (Android 8.0+), with **USB debugging** enabled
  (Settings → About phone → tap *Build number* 7×, then Settings → Developer options → USB debugging)

### Steps
1. **Clone the repo**
   ```bash
   git clone <your-repo-url>
   cd MindfulScreen
   ```
2. **Open in Android Studio** → *Open* → select the project folder.
   - On first sync, Android Studio downloads the Gradle wrapper JAR, the Android SDK, and
     all dependencies automatically. Let it finish (a few minutes).
3. **Connect your phone** via USB and accept the debugging prompt.
4. **Run** ▶ (select your device). The app installs and launches.
5. **Onboarding:**
   - Tap **Open Usage Access settings** → find *MindfulScreen* → toggle **Allow usage access**.
   - Return to the app, set your daily goal, tap **Get Started**.
6. **Allow notifications** when prompted (needed for nudges on Android 13+).

### Trying the intervention quickly
- In **Limits**, set a small limit (e.g. 1 min via the code default is 30m — lower it in
  `LimitsViewModel.setLimit` if you want a fast demo), use that app past the limit, and the
  periodic worker (every 15 min, WorkManager's minimum) will fire the mindful nudge.
  For an instant demo you can also launch `NudgeActivity` directly from Android Studio.

---

## 📁 Project Structure

```
app/src/main/java/com/mindfulscreen/app/
├─ MindfulScreenApp.kt        # Application + notification channel + scheduler
├─ MainActivity.kt            # Nav host + bottom bar + notif permission
├─ data/
│  ├─ UsageStatsRepository.kt # Reads real usage via event replay
│  ├─ SettingsRepository.kt   # DataStore: onboarding + daily goal
│  ├─ AppContainer.kt         # Manual DI
│  ├─ db/                     # Room: entities, DAOs, database
│  └─ model/                  # UsageModels
├─ domain/
│  └─ AttentionScore.kt       # Score engine + AvatarMood
├─ intervention/
│  ├─ NudgeActivity.kt        # Full-screen mindful nudge
│  ├─ LimitCheckWorker.kt     # Periodic usage-vs-limit check
│  └─ InterventionScheduler.kt
└─ ui/
   ├─ theme/ · components/ · navigation/
   └─ screens/ onboarding · dashboard · limits · insights
```

---

## 📚 Documentation

- **[DESIGN.md](DESIGN.md)** — full design document (problem, psychology, architecture, wireframes)
- **[docs/REPORT.md](docs/REPORT.md)** — project report in PKM-KC proposal format (with citations)
- **[docs/CONTRIBUTORSHIP.md](docs/CONTRIBUTORSHIP.md)** — contributorship form template

---

## ⚖️ Scope & Honest Limitations (MVP)

- Cannot **block** other apps or intercept purchases inside them — Android sandboxes apps
  from each other. MindfulScreen nudges; it doesn't forcibly lock (a deliberate, ethical
  design choice + a platform constraint). Blocking is listed as *future work*.
- iOS is out of scope (Apple restricts Screen Time data). Android only.
- Usage history depends on what the OS retains; very old days may be unavailable.
