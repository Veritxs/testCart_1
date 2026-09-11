# MindfulScreen: A Computational Screen-Time Intervention for Digital Attention Fragmentation

*Project Report — PKM-KC (Karsa Cipta) Proposal Format*

**Field / SDG relevance:** Digital Psychology — Attention Fragmentation & Compulsive Screen Use
**Team:** _[Member 1], [Member 2], [Member 3], [Member 4], [Member 5]_
**Institution / Course:** _[Fill in]_
**Date:** _[Fill in]_

> This report follows the PKM-KC proposal structure. Sections map to the assessment
> criteria: background, specific problem definition, proposed solution, results,
> discussion, and conclusion. Replace all _[bracketed]_ placeholders before submission.

---

## Abstract

MindfulScreen is a native Android application that addresses attention fragmentation in
the digital era by turning users' **real, objectively measured** phone-usage data into a
gentle, psychology-grounded feedback and intervention system. Unlike self-report tools, the
app reads actual per-app usage through Android's `UsageStatsManager`, computes a daily
Attention Score with an emotional avatar, allows users to set goals and per-app limits, and
delivers non-coercive "mindful nudges" when limits are exceeded. This document describes the
problem, the computational design, the implemented MVP, and preliminary evaluation.

---

## 1. Background of the Problem *(Latar Belakang)*

Smartphone use in Indonesia is among the highest in the world, and short-form, algorithmically
optimized feeds (e.g., TikTok, Reels, Shorts) are explicitly engineered to maximize
time-on-app. A well-documented consequence is **attention fragmentation**: frequent
context-switching that degrades focus, displaces sleep and study time, and accrues a measurable
cognitive cost. Field research on interruptions found that returning to a task after an
interruption takes roughly **23 minutes** on average, and that people compensate by working
faster at the cost of higher stress and effort (Mark, Gudith, & Klocke, 2008).

Critically, users tend to **underestimate** their own usage, so awareness — the first step of
behaviour change — breaks down. Simply making a behaviour visible to the person performing it
(self-monitoring) is itself one of the most reliably effective behaviour-change techniques
(Michie et al., 2013). The opportunity, therefore, is a tool that provides an honest mirror of
real usage and low-friction self-regulation supports.

---

## 2. Specific Problem Definition *(Rumusan Masalah)*

1. Users lack an **accurate, real-time** picture of how much time they spend in each app.
2. Existing awareness is **self-reported and biased**, undermining self-regulation.
3. Most "digital wellbeing" responses are either **purely informational** (a dashboard that
   is easy to ignore) or **coercive** (hard app blocks that users resent and disable).

**Research/design questions:**
- Can a computational tool present objective usage data in a form that motivates change?
- Can *gentle* interventions (nudges) meaningfully interrupt autopilot use without coercion?
- Can behaviour change be made **measurable** (week-over-week) rather than anecdotal?

---

## 3. Proposed Solution *(Solusi yang Ditawarkan)*

### 3.1 Computational approach
MindfulScreen ingests real usage events and reconstructs per-app foreground time by replaying
`MOVE_TO_FOREGROUND` / `MOVE_TO_BACKGROUND` events (more accurate than coarse aggregate
queries). From this it computes:

- **Attention Score (0–100):** starts at 100 daily and decreases as a function of (a) minutes
  over the daily goal, (b) number of per-app limits broken, and (c) late-night (00:00–06:00)
  usage. A single legible number that only drops with overuse leverages **feedback-loop** and
  **loss-aversion** dynamics.
- **Goal & limit evaluation:** compares measured usage against user-set targets.
- **Intervention trigger:** a background `WorkManager` job periodically checks usage against
  limits and issues a full-screen **mindful nudge** (once per limit per day, to avoid nagging).

### 3.2 Psychological grounding (design → theory mapping)

| Feature | Principle | Source |
|---------|-----------|--------|
| Real usage dashboard | Self-monitoring (behaviour-change technique) | Michie et al., 2013 |
| Daily goal & per-app limits | Goal-setting theory (specific, hard goals improve performance) | Locke & Latham, 2002 |
| Mindful nudge on limit exceeded | Nudge theory / choice architecture | Thaler & Sunstein, 2008 |
| Cost framing of interruptions | Cost of interrupted work (~23 min recovery) | Mark, Gudith, & Klocke, 2008 |
| Attention Score + avatar | Dual-process theory (System 1 vs System 2) | Kahneman, 2011 |

### 3.3 Ethical design
Interventions are **gentle by design** (nudges, not forced lockouts), all data stays
**on-device** (no accounts, no cloud, no network), and the app never reads message or media
content — only aggregate usage times exposed by the OS.

---

## 4. Results of the Solution *(Hasil)*

### 4.1 Implemented MVP
A working native Android app (Kotlin + Jetpack Compose) with:
- Onboarding + Usage Access permission flow
- Dashboard: avatar, Attention Score ring, today-vs-goal, top apps
- Goals & Limits screen (daily goal + per-app limits, persisted with Room)
- Full-screen mindful nudge via WorkManager
- Insights: 7-day trend chart, week-over-week delta, most-used app, late-night usage

### 4.2 Evaluation plan *(to be completed by the team)*
- **Method:** small within-subjects pilot (N = _[e.g. 5–10]_), one baseline week vs. one
  intervention week.
- **Primary metric:** change in average daily screen time (from the app's own measurements).
- **Secondary metrics:** number of limits kept, late-night usage, self-reported awareness.
- **Preliminary result:** _[insert your measured week-over-week % change and a screenshot of
  the Insights screen]._

> Because the app records objective usage, the "results" section can report **measured**
> behaviour change rather than survey estimates — a key strength of this design.

---

## 5. Discussion *(Pembahasan)*

- **Strengths:** objective data; ethical, non-coercive design; measurable outcomes; fully
  on-device privacy.
- **Limitations:** Android cannot let one app *block* or intercept actions inside another app,
  so MindfulScreen nudges rather than enforces; iOS restricts Screen Time data and is out of
  scope; the pilot is small and short.
- **Threats to validity:** novelty effects, self-selection, and the Hawthorne effect (being
  observed changes behaviour) — which, notably, is also part of *why* the intervention works.
- **Future work:** optional accessibility-based soft-blocking, focus-session mode, scheduled
  notification digests, longer and larger evaluation, Bahasa Indonesia localization.

---

## 6. Conclusion *(Kesimpulan)*

MindfulScreen demonstrates a computational, psychology-grounded approach to attention
fragmentation: it converts objective usage data into an emotionally salient score and gentle
interventions, making both the problem and any improvement **measurable**. The MVP validates
the core loop (measure → reflect → nudge → re-measure) and provides a foundation for a larger
evaluation.

---

## References

1. Kahneman, D. (2011). *Thinking, Fast and Slow*. Farrar, Straus and Giroux.
2. Locke, E. A., & Latham, G. P. (2002). Building a practically useful theory of goal setting
   and task motivation: A 35-year odyssey. *American Psychologist, 57*(9), 705–717.
3. Mark, G., Gudith, D., & Klocke, U. (2008). The cost of interrupted work: More speed and
   stress. *Proceedings of the SIGCHI Conference on Human Factors in Computing Systems (CHI '08)*,
   107–110.
4. Michie, S., et al. (2013). The behavior change technique taxonomy (v1) of 93 hierarchically
   clustered techniques. *Annals of Behavioral Medicine, 46*(1), 81–95.
5. Thaler, R. H., & Sunstein, C. R. (2008). *Nudge: Improving Decisions About Health, Wealth,
   and Happiness*. Yale University Press.

> ✅ Five peer-reviewed / authoritative sources included. Add more as needed and format to your
> institution's required citation style (APA shown here). Verify each reference against the
> original before submission.
