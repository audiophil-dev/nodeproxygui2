# Fork Notes

This repository is a fork of [madskjeldgaard/nodeproxygui2](https://github.com/madskjeldgaard/nodeproxygui2) maintained by audiophil-dev. It tracks changes made in the Tender Soul of Ocean sound engine project and isolates those suitable for upstream contribution.

## Change Log

### Not PR'd — `this.document` / `this.asCode` receiver (Change 1)

**Files**: `Classes/NodeProxyGui2.sc` lines 330–331

The sound engine version changed the popup switch from:

```supercollider
3, { this.document },
4, { this.asCode.postln },
```

to:

```supercollider
3, { nodeProxy.document },
4, { nodeProxy.asCode.postln },
```

**Reason this change was made in the sound engine**: When the fork diverged (at upstream version 1.1.0), `NodeProxyGui2` did not define `asCode` or `document` methods. Calling `this.document` on a `NodeProxyGui2` instance would throw `doesNotUnderstand`. Changing the receiver to `nodeProxy` (the ivar) was the correct fix at the time.

**Reason this is not PR'd upstream**: Upstream added dedicated `asCode` and `document` methods to `NodeProxyGui2` in PR #64 (commit `a166c48`, merged 29 Sep 2025, released as version 1.1.5). Those methods internally delegate to `nodeProxy`, so `this.document` and `this.asCode` now work correctly. Both forms produce identical behaviour. The upstream fix supersedes the sound engine workaround.

**Conclusion**: No change needed upstream. The sound engine's version of these lines is a now-redundant workaround that predates the upstream fix.

---

### Change 2 — `paramSectionMaxHeight` ivar and setter

**Branch**: `feature/fix-scrollview-condition`
**Files**: `Classes/NodeProxyGui2.sc`

Added `var <paramSectionMaxHeight` instance variable and `paramSectionMaxHeight_` setter. This overrides the default cap on the internal parameter scroll area (default: 50% of screen height) without modifying any classvar.

**Why**: When NPG2 is embedded inside a height-constrained host layout (e.g. a `fixedHeight_(400)` `ScrollView` in `KlongGenGui`), the default 50%-of-screen cap would make the parameter section taller than the host allows, breaking layout.

**Behaviour of the setter**: Stores the value and calls `{ this.makeParameterSection }.defer` — it rebuilds the parameter section immediately on the next AppClock tick.

**Upstream candidacy**: Yes — this is a clean, non-breaking addition with no classvar side effects. Suitable for PR.

---

### Change 3 — `embedded` mode (suppress `contentView.fixedHeight_`)

**Branch**: `feature/fix-scrollview-condition`  
**Commits**: `219aaa3`, `eb7b460`  
**Files**: `Classes/NodeProxyGui2.sc`

Added `var embedded` instance variable (default `false`) and `embedded_` setter. When `embedded = true`, the two call sites in `makeParameterSection` that call `contentView.fixedHeight_(windowTargetH)` and `contentView.maxHeight_(windowTargetH)` are skipped. The internal `parameterSection.maxHeight_(paramSectionMaxHeight)` call is applied in both modes.

**Why**: In standalone mode, `fixedHeight_` prevents the window from showing empty space below the parameter section. In embedded mode, the host layout (e.g. `firstRow` HLayout in `KlongSoundEnvironmentGui`) determines `contentView`'s height. Applying `fixedHeight_` in embedded mode pins the view at a small value (121px in the KSEG context) regardless of how much space the host offers.

**Upstream candidacy**: Yes — clean opt-in flag, backward compatible. Suitable for PR.

---

### Change 4 — `<contentView` getter

**Branch**: `feature/fix-scrollview-condition`  
**Commits**: `eb7b460`  
**Files**: `Classes/NodeProxyGui2.sc` line 16

Changed `var contentView` to `var <contentView` to expose a read accessor. Required by the host (KSEG) to apply a deferred `fixedHeight_` pin after the layout settles.

**Upstream candidacy**: Yes — non-breaking addition. Suitable for PR.

---

### Change 5 — `parameterSection` fills available height in embedded no-scroll path

**Branch**: `feature/fix-scrollview-condition`  
**Commits**: `f3f7da8`  
**Files**: `Classes/NodeProxyGui2.sc`, `makeParameterSection`

Two changes to the no-scroll branch of `makeParameterSection`:

1. `innerView.fixedHeight_(innerH)` is now inside the scroll branch only. It is required there to prevent `ScrollView.canvas_` from collapsing the canvas to zero. In the no-scroll path, `fixedHeight_` is skipped when `embedded = true` so `innerView` can stretch to fill available space.
2. `contentView.layout.add(parameterSection, 0)` changed to `contentView.layout.add(parameterSection, if(embedded, { 1 }, { 0 }))` — stretch=1 in embedded mode so Qt distributes remaining vertical space to `parameterSection`.

**Why**: Without these changes, `parameterSection` was content-sized (46–166px) even though `contentView` was 303px. The result was visible blank space inside NPG2. After the fix, `ps.h + hdr.h = cv.h` for all environments — no blank space.

**Upstream candidacy**: Yes, in conjunction with Change 3. The `embedded` guard on `fixedHeight_` skipping is clean and backward compatible.

---

## Investigation: NPG2 Not Expanding in KlongSoundEnvironmentGui

Conducted on branch `feature/extract-nodeproxygui2` (Sound Engine) and `feature/fix-scrollview-condition` (NPG2).

### Goal

NPG2 embedded in `KlongSoundEnvironmentGui` (KSEG) should expand vertically to fill the available space in `firstRow`. Previously it was pinned at 121px regardless of available space.

### Real Engine Measurements

All measurements obtained by running `startEngine.scd` with `postln` debug output inserted into `KlongSoundEnvironmentGui.sc`. Port 5005 was cleared before each run. Source: empirical observation, not code inference.

| Component | Height | Notes |
|---|---|---|
| `KlongGenGui` `scrollView` | 400px | `fixedHeight_(400)` in `KlongGenGui.sc` line 33. Earlier assumed to be 200px — incorrect. |
| KSEG total (`VLayout`) | 380px | Measured via `bounds.height` after layout pass |
| `firstRow` | 303–308px | Driven by channel GUI vertical slider, not NPG2 |
| `secondRow` | 26px | Pinned by `secondRow.fixedHeight_(26)` |
| `thirdRow` | 21px | Pinned by `thirdRow.fixedHeight_(21)` |
| NPG2 `contentView` before fix | 121px | `headerHeight(46) + paramSectionMaxHeight(75)` |
| NPG2 `contentView` after fix | 186–306px | Varies by environment param count; expands to fill `firstRow` |
| NPG2 slider heights after fix | 24–28px | Compact; sliders do NOT stretch |

### Root Cause of NPG2 Not Expanding

`makeParameterSection` (lines 410–424 before fix) computed `windowTargetH = headerHeight + paramSectionMaxHeight = 46 + 75 = 121` and called:

```supercollider
contentView.fixedHeight_(windowTargetH);
contentView.maxHeight_(windowTargetH);
// ... deferred re-apply:
{ contentView.fixedHeight_(windowTargetH); contentView.maxHeight_(windowTargetH); }.defer(0.07);
```

`fixedHeight_` is a hard constraint on Qt's layout engine. It overrides the host layout's allocation, pinning `contentView` to 121px even though `firstRow` offers 308px.

Verified by assumption test A2 (`Test/test_assumptions.scd`): `fixedHeight_` is respected when `contentView` is embedded in a parent layout — the parent's surplus space does not override it.

### Why `firstRow` Is 308px and Not Taller

`prMakeSourceChannels` calls `model.channel.gui(firstRow)` which inserts a label, vertical `Slider`, and meter into `firstRow`'s HLayout. The slider has no `fixedHeight_`, so Qt's VLayout allocates it all available vertical space. `firstRow`'s height is therefore determined by how much the outer VLayout assigns to it after reserving `secondRow` (26px) and `thirdRow` (21px) from the 400px scrollView height (minus VLayout margins/slack ≈ 30px → 400 − 26 − 21 − 30 ≈ 323px, empirically observed as 308px). NPG2 sits alongside the slider in `firstRow`'s HLayout and receives the same row height.

### Why `secondRow` and `thirdRow` Were Being Stretched

Before the fix, `VLayout` in KSEG had no height constraints on `secondRow` or `thirdRow`. Qt distributed surplus vertical space proportionally across all rows, stretching them beyond their natural heights. Adding `secondRow.fixedHeight_(26)` and `thirdRow.fixedHeight_(21)` (measured empirically) prevented this.

Verified empirically: after fix, `secondRow = 26px`, `thirdRow = 21px` consistently across all engine runs.

### Fix Applied

**NPG2 (`219aaa3`, `eb7b460`, `f3f7da8`)**: Added `embedded` mode, `<contentView` getter, and parameterSection fill fix (Changes 3–5 above).

**KSEG (`583c0cb`, `b1b5891`)**: Added `macroView.embedded_(true)`, raised `paramSectionMaxHeight_(262)`, removed intermediate VLayout, added deferred `contentView.fixedHeight_(firstRow.bounds.height)` pin.

### Verified Post-Fix Results

Run `startEngine.scd` with debug postln measuring `firstRow.bounds.height`, `cv.bounds.height`, `parameterSection.bounds.height`, and derived `hdr.h = cv.h - ps.h` at `defer(0.6)` after GUI construction.

| Metric | Before fix | After fix |
|---|---|---|
| `contentView` height | 121px (all environments) | 303px (all environments, = firstRow − 2px border) |
| Slider heights | 24–28px | 24–28px (unchanged) |
| `firstRow` height | 308px | 308px (unchanged) |
| `parameterSection` height | 46–166px (content-sized) | `cv.h − hdr.h` (fills all available space) |
| `ps.h + hdr.h = cv.h` | No | Yes — verified for all 8 environments |
| Blank space inside NPG2 | Yes (when few params) | No |

Two distinct header heights observed across the 8 environments:

| `hdr.h` | `ps.h` | Sum = `cv.h` |
|---|---|---|
| 52px | 251px | 303px |
| 123px | 180px | 303px |

Both sums equal `cv.h = 303px`. Blank space eliminated.

---

## Assumptions Verified by Tests

All tests are in `Test/`. Run via `Test/run_tests.scd`.

| ID | Assumption | Test | Result |
|---|---|---|---|
| A1 | `innerView.sizeHint.height` after `resizeToHint` equals actual content height | `test_assumptions.scd` block A1 | PASS |
| A2 | `contentView.fixedHeight_(H)` is respected when embedded in a parent layout — parent surplus does not override it | `test_assumptions.scd` block A2 | PASS |
| A3 | Two `excludeParams_` calls produce two deferred `makeParameterSection` rebuilds | `test_assumptions.scd` block A3 | PASS |
| A4 | Deferred re-apply from call #1 fires after call #2's main body — last re-apply wins, no race condition | `test_assumptions.scd` block A4 | PASS |
| A5 | `contentView.bounds.height` equals `windowTargetH` after 0.5s settle | `test_assumptions.scd` block A5 | PASS |
| A6 | `ScrollView` is created when `innerH > paramSectionMaxHeight` | `test_assumptions.scd` block A6 | PASS |
| A7 | `ScrollView.maxHeight_` is respected when embedded in a parent layout | `test_assumptions.scd` block A7 | PASS |
| — | `secondRow` stays at 26px after `fixedHeight_(26)` across all engine instances | Real engine run (`startEngine.scd` with `postln`) | PASS |
| — | `thirdRow` stays at 21px after `fixedHeight_(21)` across all engine instances | Real engine run | PASS |
| — | `KlongGenGui` scrollView height is 400px (not 200px as previously assumed) | Real engine run | PASS — 400px confirmed |
| — | NPG2 `contentView` expands beyond 121px when `embedded_(true)` | Real engine run post-fix | PASS — 303px observed (all environments) |
| — | Sliders remain compact (≤ 28px) after embedded mode removes `fixedHeight_` | Real engine run post-fix | PASS — 24–28px across all environments |
| — | `paramSectionMaxHeight_` setter rebuilds `makeParameterSection` immediately on next AppClock tick | Code inspection + `TestNodeProxyGui2SliderHeight` | PASS |
| — | Many params (≥ 20) produce a `ScrollView`; few params do not | `TestNodeProxyGui2ScrollView.test_manyParams_usesScrollView`, `test_fewParams_noScrollView` | PASS |
| — | `ScrollView` canvas height exceeds viewport after `innerView.fixedHeight_` is applied before `canvas_()` | `TestNodeProxyGui2ScrollView.test_scrollView_contentExceedsViewport` | PASS |
| — | Slider heights remain ≤ 28px in all `gui2` call patterns incl. `showInfo=true` | `TestNodeProxyGui2SliderHeight` (all 5 tests) | PASS |
| — | `ps.h + hdr.h = cv.h` — parameterSection fills all available space in embedded no-scroll path | Real engine run with header debug (`f3f7da8`) | PASS — verified for all 8 environments |
| — | No blank space inside NPG2 when embedded in KSEG (few params, no scroll) | Real engine run post `f3f7da8` | PASS — ps.h fills cv.h − hdr.h |
