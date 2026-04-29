// mwe_scroll_fix.scd
// Minimal Working Example: ScrollView fix verification for NodeProxyGui2
//
// Run this file in the SuperCollider IDE (Cmd+Enter on the whole block).
// It confirms which NodeProxyGui2.sc file is loaded, then opens a GUI with
// 20 parameters and prints a PASS/FAIL verdict on whether the ScrollView
// canvas height exceeds the viewport height.
//
// Expected output (fix working):
//   NodeProxyGui2 loaded from: .../nodeproxygui2/Classes/NodeProxyGui2.sc
//   parameterSection class: ScrollView
//   canvas height (innerH): NNN px
//   viewport height: MMM px
//   canvas > viewport: PASS
(
var nParams = 20;
var proxyName = \mweScrollTest;
var gui, ps, canvas, canvasH, viewportH;

// --- 1. Confirm which source file SC loaded ---
Post << "NodeProxyGui2 loaded from: " << NodeProxyGui2.filenameSymbol << "\n";

// --- 2. Boot server, then build GUI ---
s.waitForBoot({
	// Create an Ndef with nParams float parameters.
	Ndef(proxyName, {
		var snd = SinOsc.ar(440);
		// 20 NamedControl inputs force 20 slider rows.
		nParams.do({ |i|
			snd = snd * NamedControl.kr(("p" ++ i).asSymbol, 1.0);
		});
		snd ! 2
	});

	// Brief wait for the Ndef to register its controls.
	0.3.wait;

	// Open the GUI.
	gui = NodeProxyGui2.new(Ndef(proxyName));

	// Wait for the deferred layout pass (see makeParameterSection: 0.07 s defer).
	0.2.wait;

	// --- 3. Inspect the parameterSection ---
	ps = gui.parameterSection;
	Post << "parameterSection class: " << ps.class << "\n";

	if(ps.isKindOf(ScrollView), {
		canvas    = ps.canvas;
		canvasH   = canvas.bounds.height;
		viewportH = ps.bounds.height;

		Post << "canvas height (innerH): " << canvasH << " px\n";
		Post << "viewport height:        " << viewportH << " px\n";
		Post << "canvas > viewport:      ";

		if(canvasH > viewportH, {
			Post << "PASS\n";
		}, {
			Post << "FAIL  <-- fix not applied or canvas collapsed to viewport size\n";
		});
	}, {
		Post << "parameterSection is NOT a ScrollView -- too few params triggered wrapping, or fix regressed\n";
		Post << "Available screen height half: " << (Window.availableBounds.height * 0.5) << " px\n";
		Post << "Check that nParams=" << nParams << " produces enough rows to exceed that threshold.\n";
	});
});
)
