TestNodeProxyGui2Embedding : UnitTest {

	var server;

	setUp {
		Window.allWindows.do(_.close);
		0.1.wait;
		server = Server.default;
	}

	tearDown {
		Window.allWindows.do(_.close);
		0.1.wait;
	}

	// Build an SE-like environment column containing a gui2
	prBuildEnvColumn { |gui2|
		var envGui = View(nil);
		var firstRow, innerLayout;
		envGui.layout = VLayout();
		firstRow = View(nil, Rect(0, 0, 100, 400));
		firstRow.layout = HLayout();
		firstRow.layout.margins_([1, 1, 1, 1]);
		envGui.layout.add(firstRow);
		innerLayout = VLayout();
		firstRow.layout.add(innerLayout);
		innerLayout.add(gui2);
		firstRow.layout.add(nil);
		^envGui
	}

	prPrintViewHierarchy { |view, indent = 0|
		var prefix = String.fill(indent, $ );
		var bounds = view.bounds;
		"%%: h=% sizeHint.h=%".format(
			prefix, view.class, bounds.height.round(1), view.sizeHint.height.round(1)
		).postln;
		if(view.respondsTo(\children)) {
			view.children.do { |child|
				this.prPrintViewHierarchy(child, indent + 2)
			}
		};
	}

	// Collect all Slider heights from a view tree
	prCollectSliderHeights { |view|
		var heights = List.new;
		if(view.isKindOf(Slider)) {
			heights.add(view.bounds.height);
		};
		if(view.respondsTo(\children)) {
			view.children.do { |child|
				heights.addAll(this.prCollectSliderHeights(child))
			}
		};
		^heights
	}

	// === 1. Simple embedding with stretch ===

	test_simpleEmbedWithStretch {
		var parentWindow, gui2, ndef;
		var gui2View, hBefore, hAfter;
		var tolerance = 50;

		ndef = Ndef(\testSimple, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		gui2 = ndef.gui2(0, false);
		gui2View = gui2.asView;
		hBefore = gui2View.sizeHint.height;

		parentWindow = Window("Simple Embed", Rect(100, 100, 400, 800));
		parentWindow.layout = VLayout(
			StaticText().string_("Header"),
			gui2,
			nil
		);
		parentWindow.front;
		0.3.wait;

		hAfter = gui2View.bounds.height;
		"[simple] before=%, after=%".format(hBefore, hAfter).postln;

		this.assert(
			hAfter <= (hBefore + tolerance),
			"Simple embed: gui2 should not grow. Got: % (sizeHint: %)".format(hAfter, hBefore)
		);

		parentWindow.close;
		0.1.wait;
		ndef.clear;
	}

	// === 2. SE hierarchy ===

	test_exactSEHierarchy {
		var parentWindow, scrollView, container;
		var envGui;
		var gui2, ndef, gui2View;
		var hBefore, hAfter;
		var scrollHeight = 300;
		var tolerance = 50;

		ndef = Ndef(\testSE, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		gui2 = ndef.gui2(0, false, false, false, false);
		gui2View = gui2.asView;
		hBefore = gui2View.sizeHint.height;

		envGui = this.prBuildEnvColumn(gui2);

		container = View(nil);
		container.layout = HLayout();
		container.layout.add(envGui);

		scrollView = ScrollView(nil, Rect(0, 0, 100, 100));
		scrollView.canvas = container;
		scrollView.hasVerticalScroller_(false);
		scrollView.fixedHeight_(scrollHeight);

		parentWindow = Window("SE Hierarchy", Rect(100, 100, 800, 600));
		parentWindow.layout = VLayout();
		parentWindow.layout.add(scrollView);
		parentWindow.front;

		0.5.wait;

		hAfter = gui2View.bounds.height;
		"[SE] before=%, after=%".format(hBefore, hAfter).postln;

		this.assert(
			hAfter <= (hBefore + tolerance),
			"SE hierarchy: gui2 should not grow. Got: % (sizeHint: %)".format(hAfter, hBefore)
		);

		parentWindow.close;
		0.1.wait;
		ndef.clear;
	}

	// === 3. Source change after embedding ===

	test_sourceChangeAfterEmbedding {
		var parentWindow, scrollView, container;
		var envGui;
		var gui2, ndef, gui2View;
		var hAfterEmbed, hAfterSource;
		var slidersBefore, slidersAfter;
		var scrollHeight = 300;
		var tolerance = 50;

		ndef = Ndef(\testSourceChange, { |freq = 440, amp = 0.5| Silent.ar });
		gui2 = ndef.gui2(0, false, false, false, false);
		gui2View = gui2.asView;

		envGui = this.prBuildEnvColumn(gui2);

		container = View(nil);
		container.layout = HLayout();
		container.layout.add(envGui);

		scrollView = ScrollView(nil, Rect(0, 0, 100, 100));
		scrollView.canvas = container;
		scrollView.hasVerticalScroller_(false);
		scrollView.fixedHeight_(scrollHeight);

		parentWindow = Window("Source Change", Rect(100, 100, 800, 600));
		parentWindow.layout = VLayout();
		parentWindow.layout.add(scrollView);
		parentWindow.front;

		0.5.wait;
		hAfterEmbed = gui2View.bounds.height;
		slidersBefore = this.prCollectSliderHeights(gui2View);
		"[source-change] embed h=%, sliders=%".format(hAfterEmbed, slidersBefore).postln;

		// Change source with more params
		ndef.source = { |freq = 440, amp = 0.5, pan = 0, rate = 1, depth = 0.3| Silent.ar };
		0.5.wait;

		hAfterSource = gui2View.bounds.height;
		slidersAfter = this.prCollectSliderHeights(gui2View);
		"[source-change] source h=%, sliders=%".format(hAfterSource, slidersAfter).postln;

		// Check individual slider heights — should be ~20-26px, not stretched
		slidersAfter.do { |sh, i|
			this.assert(
				sh <= 40,
				"Slider % height should be compact (<= 40). Got: %".format(i, sh)
			)
		};

		parentWindow.close;
		0.1.wait;
		ndef.clear;
	}

	// === 4. Deferred makeParameterSection ===

	test_deferredMakeParameterSection {
		var parentWindow, scrollView, container;
		var envGui;
		var gui2, ndef, gui2View;
		var hBefore, hAfterDefer;
		var scrollHeight = 300;
		var tolerance = 50;

		ndef = Ndef(\testDefer, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		gui2 = ndef.gui2(0, false, false, false, false);
		gui2View = gui2.asView;

		envGui = this.prBuildEnvColumn(gui2);

		container = View(nil);
		container.layout = HLayout();
		container.layout.add(envGui);

		scrollView = ScrollView(nil, Rect(0, 0, 100, 100));
		scrollView.canvas = container;
		scrollView.hasVerticalScroller_(false);
		scrollView.fixedHeight_(scrollHeight);

		parentWindow = Window("Deferred Rebuild", Rect(100, 100, 800, 600));
		parentWindow.layout = VLayout();
		parentWindow.layout.add(scrollView);
		parentWindow.front;

		0.5.wait;
		hBefore = gui2View.bounds.height;

		{ gui2.makeParameterSection }.defer;
		0.5.wait;

		hAfterDefer = gui2View.bounds.height;
		"[deferred] before=%, after=%".format(hBefore, hAfterDefer).postln;

		this.assert(
			hAfterDefer <= (hBefore + tolerance),
			"After deferred rebuild: should not grow. Got: % (was: %)".format(hAfterDefer, hBefore)
		);

		parentWindow.close;
		0.1.wait;
		ndef.clear;
	}

	// === 5. SE hierarchy WITHOUT ScrollView (non-scroll rows mode) ===
	// The ScrollView constrains height, so growth may only show without it.

	test_SENoScrollGrowth {
		var parentWindow, row;
		var envGui;
		var gui2, ndef, gui2View;
		var hBefore, hAfter;
		var sliders;
		var tolerance = 50;

		ndef = Ndef(\testNoScroll, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		gui2 = ndef.gui2(0, false, false, false, false);
		gui2View = gui2.asView;

		envGui = this.prBuildEnvColumn(gui2);

		// Row wrapping like KlongGenGui non-scroll mode
		row = View(nil, Rect(0, 0, 100, 500));
		row.layout = HLayout();
		row.layout.margins_([1, 1, 1, 1]);
		row.layout.add(envGui);
		row.layout.add(nil);

		// Large parent window — surplus space available
		parentWindow = Window("No Scroll Growth", Rect(100, 100, 800, 800));
		parentWindow.layout = VLayout();
		parentWindow.layout.add(row);
		parentWindow.layout.add(nil); // stretch at bottom
		parentWindow.front;

		0.5.wait;
		hBefore = gui2View.bounds.height;
		sliders = this.prCollectSliderHeights(gui2View);
		"[no-scroll] h=%, sliders=%".format(hBefore, sliders).postln;

		// Now trigger a rebuild
		{ gui2.makeParameterSection }.defer;
		0.5.wait;

		hAfter = gui2View.bounds.height;
		sliders = this.prCollectSliderHeights(gui2View);
		"[no-scroll after rebuild] h=%, sliders=%".format(hAfter, sliders).postln;

		"=== No Scroll Hierarchy ===".postln;
		this.prPrintViewHierarchy(parentWindow.view);

		this.assert(
			hAfter <= (hBefore + tolerance),
			"No-scroll: gui2 should not grow after rebuild. Got: % (was: %)".format(hAfter, hBefore)
		);

		// Check sliders
		sliders.do { |sh, i|
			this.assert(sh <= 40, "Slider % too tall: %".format(i, sh))
		};

		parentWindow.close;
		0.1.wait;
		ndef.clear;
	}

	// === 6. Multiple environments WITHOUT ScrollView ===
	// This is the closest to what user sees if scrollMode were false

	test_multipleEnvsNoScroll {
		var parentWindow, row;
		var gui2a, gui2b, ndefA, ndefB;
		var viewA, viewB, hA, hB;
		var slidersA, slidersB;
		var tolerance = 50;

		ndefA = Ndef(\testMultiA, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });
		ndefB = Ndef(\testMultiB, { |rate = 1, depth = 0.5, mix = 0.3| Silent.ar });

		gui2a = ndefA.gui2(0, false, false, false, false);
		gui2b = ndefB.gui2(0, false, false, false, false);
		viewA = gui2a.asView;
		viewB = gui2b.asView;

		row = View(nil, Rect(0, 0, 100, 500));
		row.layout = HLayout();
		row.layout.margins_([1, 1, 1, 1]);
		row.layout.add(this.prBuildEnvColumn(gui2a));
		row.layout.add(this.prBuildEnvColumn(gui2b));
		row.layout.add(nil);

		parentWindow = Window("Multi No Scroll", Rect(100, 100, 800, 800));
		parentWindow.layout = VLayout();
		parentWindow.layout.add(row);
		parentWindow.layout.add(nil);
		parentWindow.front;

		0.5.wait;

		hA = viewA.bounds.height;
		hB = viewB.bounds.height;
		slidersA = this.prCollectSliderHeights(viewA);
		slidersB = this.prCollectSliderHeights(viewB);
		"[multi-no-scroll] A h=% sliders=%, B h=% sliders=%".format(hA, slidersA, hB, slidersB).postln;

		"=== Multi No-Scroll Hierarchy ===".postln;
		this.prPrintViewHierarchy(parentWindow.view);

		// Check slider heights are compact
		slidersA.do { |sh, i|
			this.assert(sh <= 40, "EnvA slider % too tall: %".format(i, sh))
		};
		slidersB.do { |sh, i|
			this.assert(sh <= 40, "EnvB slider % too tall: %".format(i, sh))
		};

		parentWindow.close;
		0.1.wait;
		ndefA.clear;
		ndefB.clear;
	}
}
