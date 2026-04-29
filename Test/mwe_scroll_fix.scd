// mwe_scroll_fix.scd
// Demonstrates the paramSectionMaxHeight fix for NPG2 embedded inside the SE's
// 300px fixed-height outer ScrollView (KlongGenGui).
//
// Replicates the exact SE call sequence:
//   gui2(0, false, false, false, false)
//   paramSectionMaxHeight_(175)          -- fix: cap param scroll area
//   embed in SE layout structure
//   excludeParams_([...])               -- triggers deferred makeParameterSection rebuild
//   wait and report
//
// Without the fix: cv height = ~600px, overflows 300px outer scroll,
//   inner scrollbar handle extends below clip boundary → inaccessible.
// With the fix: cv height ≤ 300px, parameterSection scrollbar fully visible.
//
// Measured NPG2 layout overhead (header rows + VLayout spacing) = 123px.
// Max paramSection height = 300 - 123 = 177px (NPG2-only overhead).
// Full KSEG overhead (NPG2 + secondRow + thirdRow + VLayout) = 125px.
// Max paramSection height = 300 - 125 = 175px.
//
// Run in SC IDE with Cmd+Enter.
(
var proxyName = \mweScrollFix;
var gui, ps, window;
var scrollView, container, envGui, firstRow, innerLayout;
var scrollHeight = 300;

Post << "NodeProxyGui2 loaded from: " << NodeProxyGui2.filenameSymbol << "\n";

// 35 params — after excluding intOff/varOff/freq/amp (4), 31 remain.
// 31 * ~24px = ~744px > 527px threshold → scroll triggers.
Ndef(proxyName, {
	|intOff=0, varOff=0, varVal=0, freq=0, amp=0,
	 p5=0, p6=0, p7=0, p8=0, p9=0,
	 p10=0, p11=0, p12=0, p13=0, p14=0,
	 p15=0, p16=0, p17=0, p18=0, p19=0,
	 p20=0, p21=0, p22=0, p23=0, p24=0,
	 p25=0, p26=0, p27=0, p28=0, p29=0,
	 p30=0, p31=0, p32=0, p33=0, p34=0|
	Silent.ar
});

// --- SE outer structure (mirrors KlongGenGui layout) ---
window = Window("MWE: paramSectionMaxHeight fix").front;
window.layout = VLayout();

scrollView = ScrollView(nil, [0, 0, 100, 100]);
container = View(nil);
container.layout = HLayout();

envGui = View(nil, [0, 0, 100, 400]);
envGui.layout = VLayout();

firstRow = View(nil, [0, 0, 100, 400]);
firstRow.layout = HLayout();
firstRow.layout.margins_([1, 1, 1, 1]);
envGui.layout.add(firstRow);

innerLayout = VLayout();
firstRow.layout.add(innerLayout);

// --- Step 1: create gui2 exactly as prMakeMacroControls does ---
gui = Ndef(proxyName).gui2(0, false, false, false, false);

// --- Step 2: apply fix before embedding ---
// NPG2 layout overhead (header rows + VLayout spacing) = 123px empirically.
// 300px (outer scroll) - 123px (overhead) = 177px max for parameterSection.
gui.paramSectionMaxHeight_(175);

// --- Step 3: embed ---
innerLayout.add(gui);
firstRow.layout.add(nil);
container.layout.add(envGui);
scrollView.canvas = container;
scrollView.hasVerticalScroller_(false);
scrollView.fixedHeight_(scrollHeight);
window.layout.add(scrollView);

// --- Step 4: call excludeParams_ AFTER embedding, exactly as the SE does ---
gui.excludeParams_([\intOff, \varOff, \freq, \amp]);

// --- Step 5: report after the deferred makeParameterSection rebuild ---
{
	var cvH, psH;
	ps = gui.parameterSection;
	cvH = gui.asView.bounds.height;
	psH = ps.bounds.height;

	Post << "\n--- After excludeParams_ rebuild ---\n";
	Post << "cv total height:         " << cvH << " px\n";
	Post << "parameterSection class:  " << ps.class << "\n";
	Post << "parameterSection height: " << psH << " px\n";
	Post << "NPG2 overhead (cv - ps): " << (cvH - psH) << " px\n";

	if(ps.isKindOf(ScrollView), {
		Post << "CHECK parameterSection is ScrollView: PASS\n";
	}, {
		Post << "CHECK parameterSection is ScrollView: FAIL\n";
	});

	if(cvH <= scrollHeight, {
		Post << "CHECK cv height " << cvH << " fits in " << scrollHeight << "px: PASS\n";
	}, {
		Post << "CHECK cv height " << cvH << " fits in " << scrollHeight << "px: FAIL\n";
	});

	if(psH <= 175, {
		Post << "CHECK paramSection " << psH << " <= 175px cap: PASS\n";
	}, {
		Post << "CHECK paramSection " << psH << " <= 175px cap: FAIL\n";
	});

	if(ps.isKindOf(ScrollView), {
		var canvasH = ps.canvas.bounds.height;
		var viewportH = ps.bounds.height;
		Post << "paramSection canvas: " << canvasH << "px  viewport: " << viewportH << "px\n";
		if(canvasH > viewportH, {
			Post << "CHECK inner scroll active (canvas > viewport): PASS\n";
		}, {
			Post << "CHECK inner scroll active (canvas > viewport): FAIL — all params visible, no scroll needed\n";
		});
	});
}.defer(0.4);
)
