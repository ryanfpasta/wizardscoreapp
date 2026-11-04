# Wizard Score

A scorekeeper for the Wizard card game, as a plain Java Android app (no AndroidX).

- 2 to 6 players. Rounds = 60 / players (2 players plays 30 rounds).
- Each round you enter every player's BID and TOOK side by side.
- Exact bid scores 20 + 10 per trick; a miss scores -10 per trick off.
- Optional "hook" rule (the dealer can't make the total bids equal the cards dealt), off by default.

## Building

`build.sh` builds the APK without the Android SDK, using aapt2 (from apktool), javac against the
android.jar stub, dx, and apksig. It expects those tools under `/root/tools`; see the script for paths.
It signs with `debug.keystore`, which is not committed. Use the same keystore for every build so updates
install over the previous version.
