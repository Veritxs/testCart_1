# MindfulScreen — Design Document

*A native Android screen-time helper that turns real, measurable phone usage into gentle, psychology-backed interventions.*

**Course project — Digital Psychology (Attention Fragmentation / Distraction)**
**Status:** Design draft v1 — for group review

---

## 1. Problem Definition

**Problem chosen:** Attention Fragmentation & compulsive screen use in the digital era.

Indonesians are among the heaviest mobile users in the world, and short-form feeds
(TikTok, Reels, Shorts) are engineered to maximize time-on-app. This fragments
attention, displaces sleep and study time, and users consistently *underestimate*
how much they actually use their phones.

**Specific problem:** People lack an honest, real-time mirror of their own usage and
have no low-friction way to set and keep limits — awareness and self-regulation break down.

**Why this is the right scope:** Unlike self-reported surveys, Android exposes **objective,
measurable usage data**. That makes our "results of solution" section evidence-based,
not anecdotal.

---

## 2. Design Goals

| # | Goal | Success looks like |
|---|------|--------------------|
| G1 | Show *real*, accurate usage data | Per-app + daily totals from the OS, not estimates |
| G2 | Make awareness effortless | Open app → instantly see today vs. goal |
| G3 | Gentle, not punitive | Nudges & reflection, never hard lockouts (MVP) |
| G4 | Show measurable change | Weekly trend, "vs. last week" delta |
| G5 | Grounded in psychology | Every feature maps to a cited principle |

**Non-goals (MVP):** blocking other apps, iOS support, cloud sync, accounts.
(These become "Future Work" in the report.)

---

## 3. The Psychology (citation hooks for the report)

| Feature | Principle | Why it works |
|---------|-----------|--------------|
| Real usage dashboard | **Self-monitoring** (behavior-change technique) | Simply observing a behavior tends to change it |
| Daily goals & limits | **Goal-setting theory** (Locke & Latham) | Specific, measurable goals drive behavior |
| Nudge on limit exceeded | **Nudge theory / choice architecture** (Thaler & Sunstein) | Small friction interrupts autopilot |
| Wellness score / avatar | **Loss aversion + Tamagotchi effect** | Emotional stake motivates more than numbers |
| "Attention Score" trend | **Feedback loops** | Immediate feedback reinforces self-regulation |

> Target ≥5 citations — we have 5 solid principles above, each with well-known sources.

---

## 4. Core Features (MVP scope)

### F1 — Real Usage Dashboard (must-have)
- Today's total screen time + top apps, read via Android `UsageStatsManager`.
- Per-app breakdown (time + open count).
- Requires one-time "Usage Access" permission (guided onboarding).

### F2 — Goals & Limits (must-have)
- Set a daily total goal (e.g. 3h) and per-app limits (e.g. TikTok 30m).
- Progress rings/bars showing used vs. remaining.

### F3 — Gentle Interventions (must-have)
- When a limit is exceeded: a **full-screen mindful nudge** ("You've hit 30m on TikTok — take a breath?").
- A background service checks usage periodically and triggers the nudge.

### F4 — Attention Score + Avatar (signature feature)
- Start each day at **100**. Score drops as you exceed goals, recovers on good days.
- A simple avatar/emoji state reflects the score (thriving → tired → melting).
- This is our "Brainrot-inspired but original" emotional hook.

### F5 — Insights & Trends (must-have for "results")
- 7-day chart of screen time.
- "vs. last week" delta, most-used app, late-night usage flag.

### Stretch (post-MVP)
- Focus sessions (timed deep-work with a distraction counter).
- Scheduled "digest" instead of live notifications.
- App blocking via Accessibility Service.

---

## 5. Screen-by-Screen Design (wireframe outline)

```
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│  ONBOARDING          │   │  HOME / DASHBOARD    │   │  INSIGHTS            │
│                      │   │                      │   │                      │
│  1. Welcome + why    │   │  [Avatar 🙂]         │   │  ▁▂▄█▅▃▂  7-day bars │
│  2. Grant Usage      │   │  Attention Score 82  │   │                      │
│     Access (deep     │   │                      │   │  ▼ 18% vs last week  │
│     link to settings)│   │  Today: 2h 14m / 3h  │   │  Most used: TikTok   │
│  3. Set daily goal   │   │  ●●●●●○○  progress    │   │  Late-night: 47m     │
│                      │   │                      │   │                      │
│  [Get Started]       │   │  Top apps today:     │   │                      │
│                      │   │   TikTok   58m ▓▓▓   │   │                      │
│                      │   │   IG       31m ▓▓    │   │                      │
│                      │   │   WA       22m ▓     │   │                      │
│                      │   │                      │   │                      │
│                      │   │  [Set Limits]        │   │                      │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘

┌─────────────────────┐   ┌─────────────────────┐
│  LIMITS              │   │  NUDGE (full-screen) │
│                      │   │                      │
│  Daily goal: 3h   ✎  │   │      [Avatar 😵]      │
│                      │   │                      │
│  Per-app limits:     │   │  You've used TikTok  │
│   TikTok  [30m] ✎    │   │  for 30m today.      │
│   IG      [45m] ✎    │   │                      │
│   + Add app          │   │  Take a breath?      │
│                      │   │  [Keep going] [Close]│
└─────────────────────┘   └─────────────────────┘
```

*(These are text wireframes for alignment — we can turn them into real UI mockups next.)*

---

## 6. Technical Architecture

**Stack:** Native **Android / Kotlin**, **Jetpack Compose** UI, **Room** DB for local history,
`UsageStatsManager` for real usage data, `WorkManager`/foreground service for limit checks.

```
┌───────────────────────────────────────────────┐
│                   UI (Compose)                 │
│   Onboarding · Dashboard · Insights · Limits   │
└───────────────────────┬───────────────────────┘
                        │ (ViewModels / StateFlow)
┌───────────────────────┴───────────────────────┐
│                  Domain / Logic                │
│   AttentionScore engine · Goal evaluation      │
└───────────────────────┬───────────────────────┘
              ┌─────────┴──────────┐
┌─────────────┴──────┐   ┌─────────┴────────────┐
│  UsageStatsRepo    │   │  Room DB (history,   │
│  (UsageStatsMgr)   │   │  goals, daily scores)│
└────────────────────┘   └──────────────────────┘
              │
┌─────────────┴──────────────────────┐
│  Background: WorkManager checks     │
│  usage → triggers Nudge notification│
└─────────────────────────────────────┘
```

**Key permissions:** `PACKAGE_USAGE_STATS` (Usage Access — user grants in Settings),
`POST_NOTIFICATIONS` (Android 13+), `FOREGROUND_SERVICE` (for limit monitoring).

---

## 7. Data Model (draft)

- **DailyUsage**: date, totalMs, perApp[{package, label, timeMs, opens}]
- **Goal**: dailyGoalMs, perAppLimits[{package, limitMs}]
- **DailyScore**: date, score (0–100), goalMet(bool)
- **AppInfo** (cached): package, displayName, icon

---

## 8. Build Environment (important for the group)

- Written in **Kotlin**, built with **Android Studio** (Gradle).
- **This cannot be compiled/run in the browser sandbox** — I will write all the code and
  push it to GitHub; your group opens it in Android Studio and runs it on a **real Android
  phone** (the emulator has no real usage data, so a physical device is best).
- Instruction manual will cover: install Android Studio → open project → run on phone →
  grant Usage Access.

---

## 9. Proposed Build Phases

1. **Phase 0 — Project scaffold** (Gradle, Compose, navigation, theme)
2. **Phase 1 — Usage data** (permission flow + read real usage + Dashboard)
3. **Phase 2 — Goals & Limits** (set goals, progress UI, Room persistence)
4. **Phase 3 — Interventions** (background check + full-screen nudge)
5. **Phase 4 — Score + Avatar + Insights** (score engine, trends chart)
6. **Phase 5 — Polish + instruction manual + report/contributorship scaffolding**

---

## 10. Open Questions for the Group

1. **App name** — MindfulScreen? LayarSehat? SadarLayar? (Bahasa name = stronger local angle)
2. **UI language** — English, Bahasa Indonesia, or bilingual?
3. **Avatar style** — cute character (Brainrot-style) vs. minimalist score ring?
4. **How hard should interventions be?** — gentle nudge only (MVP) vs. optional blocking (stretch)?
5. Any features to **add or cut** from the MVP list above?
```

---
*Next step after group review: turn wireframes into visual mockups, then begin Phase 0 scaffold.*
