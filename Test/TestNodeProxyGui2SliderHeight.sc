// Tests that slider heights remain compact (~20-26px) under all gui2 call patterns.
//
// Regression coverage for the showInfo=true bug where sliders grew to ~150px
// because the outer VLayout distributed surplus height into the parameter section
// and individual sliders had no vertical cap.
//
// Run via: Test/run_all_tests.scd

TestNodeProxyGui2SliderHeight : UnitTest {

	// Maximum acceptable slider height in pixels.
	// Sliders capped by maxHeight_(26); allow 2px platform rounding margin.
	classvar <>maxSliderH = 28;

	setUp {
		Window.allWindows.do(_.close);
		0.1.wait;
	}

	tearDown {
		Window.allWindows.do(_.close);
		0.1.wait;
	}

	// --- Helpers ---

	prCollectSliders { |view|
		var found = List.new;
		if(view.isKindOf(Slider)) { found.add(view) };
		if(view.respondsTo(\children)) {
			view.children.do { |c| found.addAll(this.prCollectSliders(c)) }
		};
		^found
	}

	prAssertSliders { |view, label|
		var sliders = this.prCollectSliders(view);
		this.assert(
			sliders.size > 0,
			"[%] expected at least one slider".format(label)
		);
		sliders.do { |sl, i|
			var h = sl.bounds.height;
			this.assert(
				h <= maxSliderH,
				"[%] slider % height should be <= %, got %".format(label, i, maxSliderH, h)
			)
		}
	}

	// --- Tests ---

	// showInfo=true, showTransport=true: the MacroMediator call pattern.
	// This was the broken case: outer VLayout had info+transport+params sections,
	// surplus height leaked into the parameter section, sliders grew to ~150px.

	test_sliderHeight_showInfoTrue {
		var ndef = Ndef(\tSlH1, { |freq = 440, amp = 0.5| Silent.ar });

		var gui = NodeProxyGui2.new(ndef, 0, false, false, true, true);
		0.5.wait;

		this.prAssertSliders(gui.window.view, "showInfo=true");

		gui.window.close;
		ndef.clear;
		0.1.wait;
	}

	// showInfo=false, showTransport=false: the Macro call pattern.
	// Sliders were already correct here; this is a non-regression guard.

	test_sliderHeight_showInfoFalse {
		var ndef = Ndef(\tSlH2, { |freq = 440, amp = 0.5, pan = 0| Silent.ar });

		var gui = NodeProxyGui2.new(ndef, 0, false, false, false, false);
		0.5.wait;

		this.prAssertSliders(gui.window.view, "showInfo=false");

		gui.window.close;
		ndef.clear;
		0.1.wait;
	}

	// showInfo=true with more parameters. Ensures the VLayout row count does not
	// produce additional surplus that leaks into slider heights.

	test_sliderHeight_showInfoTrue_manyParams {
		var ndef = Ndef(\tSlH3, {
			|freq = 440, amp = 0.5, pan = 0, rate = 1.0, depth = 0.3, mix = 0.5|
			Silent.ar
		});

		var gui = NodeProxyGui2.new(ndef, 0, false, false, true, true);
		0.5.wait;

		this.prAssertSliders(gui.window.view, "showInfo=true manyParams");

		gui.window.close;
		ndef.clear;
		0.1.wait;
	}

	// Replicates the SE/MacroMediator embedding pattern: show=false, then
	// asView inserted into a taller parent window layout. The parent can
	// override fixedHeight_ on the TopView; sliders must still stay compact
	// because maxHeight_ is set at the widget level.

	test_sliderHeight_asView_embedded {
		var ndef = Ndef(\tSlH4, { |freq = 440, amp = 0.5| Silent.ar });
		var gui = NodeProxyGui2.new(ndef, 0, false, false, true, true);
		var parent = Window("embed-test", Rect(0, 0, 400, 600));

		parent.layout = VLayout(
			StaticText().string_("Header"),
			gui.asView,
			nil
		);
		parent.front;
		0.5.wait;

		this.prAssertSliders(gui.asView, "asView embedded");

		parent.close;
		gui.window.close;
		ndef.clear;
		0.1.wait;
	}

	// Rebuild via makeParameterSection must not re-inflate slider heights.

	test_sliderHeight_afterRebuild {
		var ndef = Ndef(\tSlH5, { |freq = 440, amp = 0.5| Silent.ar });
		var gui = NodeProxyGui2.new(ndef, 0, false, false, true, true);
		0.3.wait;

		// Trigger a rebuild — same path taken on Ndef source change.
		{ gui.makeParameterSection }.defer;
		0.5.wait;

		this.prAssertSliders(gui.window.view, "showInfo=true after rebuild");

		gui.window.close;
		ndef.clear;
		0.1.wait;
	}
}
