// Tests that the conditional ScrollView wrapping works correctly.
//
// Goal: when the parameter section's natural height exceeds half the screen,
// it must be wrapped in a ScrollView whose canvas height exceeds the viewport —
// making the scrollbar active. Without the innerView.fixedHeight_ pin applied
// BEFORE canvas_(), the canvas collapses to viewport size and no scrolling
// is possible.
//
// Run via: Test/run_tests.scd

TestNodeProxyGui2ScrollView : UnitTest {

	// Number of parameters that reliably triggers the scroll condition.
	// Each row is ~26-30 px. Half of a 900 px screen = ~450 px → need 16+ rows.
	// 20 params gives a content height of ~600 px, safely above the threshold
	// on any common macOS display.
	classvar numManyParams = 20;

	setUp {
		Window.allWindows.do(_.close);
		0.1.wait;
	}

	tearDown {
		Window.allWindows.do(_.close);
		0.1.wait;
	}

	// --- Helpers ---

	// Build an Ndef with numManyParams scalar parameters.
	prManyParamNdef { |key|
		^Ndef(key, {
			|p01=0.1, p02=0.2, p03=0.3, p04=0.4, p05=0.5,
			 p06=0.1, p07=0.2, p08=0.3, p09=0.4, p10=0.5,
			 p11=0.1, p12=0.2, p13=0.3, p14=0.4, p15=0.5,
			 p16=0.1, p17=0.2, p18=0.3, p19=0.4, p20=0.5|
			Silent.ar
		})
	}

	// --- Tests ---

	// === 1. Many params → parameterSection is a ScrollView ===
	// Structural check: the conditional branch that wraps innerView in a
	// ScrollView must be taken when content height exceeds half screen height.

	test_manyParams_usesScrollView {
		var ndef = this.prManyParamNdef(\tSV1);
		var gui = NodeProxyGui2.new(ndef, 0, false);
		0.3.wait;

		this.assert(
			gui.parameterSection.isKindOf(ScrollView),
			"Many params: parameterSection should be a ScrollView. Got: %".format(
				gui.parameterSection.class
			)
		);

		gui.close;
		ndef.clear;
		0.1.wait;
	}

	// === 2. Few params → parameterSection is NOT a ScrollView ===
	// Non-regression guard: the fallback (plain View) path must still be taken
	// when content fits within half screen height.

	test_fewParams_noScrollView {
		var ndef = Ndef(\tSV2, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		var gui = NodeProxyGui2.new(ndef, 0, false);
		0.3.wait;

		this.assert(
			gui.parameterSection.isKindOf(ScrollView).not,
			"Few params: parameterSection should NOT be a ScrollView. Got: %".format(
				gui.parameterSection.class
			)
		);

		gui.close;
		ndef.clear;
		0.1.wait;
	}

	// === 3. ScrollView is capped at half screen height ===
	// maxHeight_ must be applied so the window does not grow to the full
	// content height.

	test_scrollView_isCappedAtHalfScreen {
		var ndef = this.prManyParamNdef(\tSV3);
		var gui = NodeProxyGui2.new(ndef, 0, true);
		var halfScreen = (Window.availableBounds.height * 0.5).asInteger;
		var svHeight;
		0.3.wait;

		svHeight = gui.parameterSection.bounds.height;
		"[cap test] scrollView height=%, halfScreen=%".format(svHeight, halfScreen).postln;

		this.assert(
			svHeight <= (halfScreen + 4),
			"ScrollView height should be <= half screen (% px). Got: % px".format(
				halfScreen, svHeight
			)
		);

		gui.close;
		ndef.clear;
		0.1.wait;
	}

	// === 4. ScrollView canvas height exceeds viewport → scrollbar is active ===
	// This is the core regression test.
	//
	// Before the fix: ScrollView.canvas_() was called before fixedHeight_ was
	// pinned on innerView. Qt resized the canvas to the (zero-sized) viewport,
	// so canvas.bounds.height == svHeight and no scrollbar appeared.
	//
	// After the fix: innerView.fixedHeight_(innerH) is called first, preserving
	// the full content height through the canvas_() reparenting. The canvas
	// must be taller than the viewport for the scrollbar to appear.

	test_scrollView_contentExceedsViewport {
		var ndef = this.prManyParamNdef(\tSV4);
		var gui = NodeProxyGui2.new(ndef, 0, true);
		var sv, contentH, viewportH;
		0.3.wait;

		sv = gui.parameterSection;
		this.assert(
			sv.isKindOf(ScrollView),
			"Prerequisite: parameterSection must be a ScrollView for this test to be valid"
		);

		contentH = sv.canvas.bounds.height;
		viewportH = sv.bounds.height;
		"[scroll content test] canvas height=%, viewport height=%".format(contentH, viewportH).postln;

		this.assert(
			contentH > viewportH,
			"ScrollView canvas (% px) must exceed viewport (% px) for scrollbar to appear".format(
				contentH, viewportH
			)
		);

		gui.close;
		ndef.clear;
		0.1.wait;
	}

	// === 5. After rebuild, scroll condition and canvas height are preserved ===
	// makeParameterSection is called on every source change. The fix must
	// survive repeated rebuilds without the canvas collapsing.

	test_scrollView_persistsAfterRebuild {
		var ndef = this.prManyParamNdef(\tSV5);
		var gui = NodeProxyGui2.new(ndef, 0, true);
		var sv, contentH, viewportH;
		0.3.wait;

		{ gui.makeParameterSection }.defer;
		0.5.wait;

		sv = gui.parameterSection;
		this.assert(
			sv.isKindOf(ScrollView),
			"After rebuild: parameterSection should still be a ScrollView"
		);

		contentH = sv.canvas.bounds.height;
		viewportH = sv.bounds.height;
		"[rebuild test] canvas height=%, viewport height=%".format(contentH, viewportH).postln;

		this.assert(
			contentH > viewportH,
			"After rebuild: canvas (% px) must still exceed viewport (% px)".format(
				contentH, viewportH
			)
		);

		gui.close;
		ndef.clear;
		0.1.wait;
	}
}
