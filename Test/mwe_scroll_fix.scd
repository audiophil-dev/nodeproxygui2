// mwe_scroll_fix.scd
// Minimal Working Example: ScrollView fix verification for NodeProxyGui2
// embedded inside a parent view with a fixed height (the real use case).
//
// Run this file in the SuperCollider IDE (Cmd+Enter on the whole block).
//
// Expected post-window output (fix working):
//   NodeProxyGui2 loaded from: .../nodeproxygui2/Classes/NodeProxyGui2.sc
//   parameterSection class: ScrollView
//   canvas height (innerH): NNN px   <-- larger than viewport
//   viewport height:        MMM px
//   canvas > viewport:      PASS
(
var nParams = 20;
var proxyName = \mweScrollTest;
var parentHeight = 400;   // fixed height imposed by the parent view
var parentWindow, parentView, gui, ps, canvas, canvasH, viewportH;

// --- 1. Confirm which source file SC loaded ---
Post << "NodeProxyGui2 loaded from: " << NodeProxyGui2.filenameSymbol << "\n";

s.waitForBoot({
	// Build an Ndef with nParams float controls.
	Ndef(proxyName, {
		var snd = SinOsc.ar(440);
		nParams.do({ |i|
			snd = snd * NamedControl.kr(("p" ++ i).asSymbol, 1.0);
		});
		snd ! 2
	});

	0.3.wait;

	// --- 2. Parent window with a fixed-height container view ---
	parentWindow = Window("MWE: embedded NPG2", Rect(100, 100, 400, parentHeight + 30));
	parentView   = View(parentWindow, Rect(0, 0, 400, parentHeight));
	parentView.fixedHeight_(parentHeight);
	parentView.layout = VLayout();

	// --- 3. Create NPG2 with show:false, embed contentView into parent ---
	gui = NodeProxyGui2.new(Ndef(proxyName), show: false);
	parentView.layout.add(gui.asView);

	parentWindow.front;

	// Wait for deferred layout pass (makeParameterSection defers 0.07 s).
	0.2.wait;

	// --- 4. Inspect ---
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
			Post << "FAIL  <-- canvas collapsed to viewport size; fix not active\n";
		});
	}, {
		Post << "parameterSection is NOT a ScrollView\n";
		Post << "Half screen height threshold: " << (Window.availableBounds.height * 0.5) << " px\n";
		Post << "Increase nParams or check makeParameterSection condition.\n";
	});
});
)
