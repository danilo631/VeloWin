# Licensing of VeloWin Native (v14)

**The VeloWin Native APK is distributed under the GNU General Public License, version 3.**

Its complete GPLv3 text is available in the pinned source at:
[third_party/winnative/LICENSE](third_party/winnative/LICENSE).
The full relevant credits and component licenses are in:
[WinNative CREDITS.md](third_party/winnative/CREDITS.md) and
[WinNative EMULATOR_CREDITS.md](third_party/winnative/EMULATOR_CREDITS.md).

The original standalone VeloWin Bionic console code from before the full
engine migration remains under the repository's original MIT `LICENSE`
for the original portions. **That MIT license does not relicense the
WinNative/Winlator engine, GPL libraries, native driver code or combined APK.**

A built APK includes:
- `assets/velowin/legal/WinNative-LICENSE` — complete GPL-3.0 text;
- `assets/velowin/legal/WinNative-CREDITS.md` — upstream authors and origins;
- `assets/velowin/legal/WinNative-EMULATOR_CREDITS.md` — emulator authors;
- `assets/velowin/legal/VeloWin-original-MIT-LICENSE.txt`;
- `assets/velowin/legal/VELO-WIN-NOTICE.txt` — exact engine source pin.

To meet GPL corresponding-source obligations when distributing an APK, provide
the **exact source revision, build scripts, pinned Git submodules and any
patches/necessary build-time materials**. Build instructions are in
[docs/NATIVE_ENGINE_V14.md](docs/NATIVE_ENGINE_V14.md) and
[WinNative docs/BUILDING.md](third_party/winnative/docs/BUILDING.md).

Termux:X11 is an upstream GPLv3 reference from prior experimental branches;
the v14 APK uses WinNative's own integrated XServer and does not bundle
Termux:Lorie. No rights to third-party marks are asserted.
