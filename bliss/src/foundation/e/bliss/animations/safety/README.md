# animations/safety

Two tiny helpers that consolidate the "Bliss animator math threw or produced
NaN — silently fall back to AOSP" pattern introduced piecemeal across
Migrations 02 and 03. Use them; do not reinvent.

## When to use FloatGuard

- A measure-time computation feeds a float into an Android Animator setter
  (`setScaleX`, `setTranslationY`, …) and the inputs include view dimensions
  or layout-derived sizes that can be `0` or `NaN` mid-layout.
- `FloatGuard.safeDivide(name, num, den, fallback)` for divisions where the
  denominator is a measured float that may be zero before first layout.
- `FloatGuard.requireAllFinite(name, vararg floats)` for post-construction
  scans of an animator-data struct — call it once, list every field that
  feeds an Animator, and throw on `false` to trigger `AnimatorFallback`.

## When to use AnimatorFallback

- The Bliss-side animator build can throw or return `null` and a graceful
  AOSP fallback exists. Wrap the build in `AnimatorFallback.tryBuild(name,
  build, fallback)`. The wrapper logs at WARN, never crashes the launcher.

## Debug vs release

- Debug builds: `FloatGuard.fail` throws `IllegalStateException` (caught by
  the surrounding `AnimatorFallback`). Crashes early so real bugs surface.
- Release builds: logs at WARN and returns the fallback. The animator falls
  back; the user sees the AOSP animation, not a crash.

Plan ref: `Plans/Migration04/06-animation-safety.md`.
