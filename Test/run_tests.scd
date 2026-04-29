// Test runner for NodeProxyGui2
// Run with: /Applications/SuperCollider.app/Contents/MacOS/sclang Test/run_tests.scd
(
"=== NodeProxyGui2 Test Runner ===".postln;

AppClock.sched(0.5, {

	"--- TestNodeProxyGui2ScrollView ---".postln;
	TestNodeProxyGui2ScrollView.run;

	AppClock.sched(15.0, {
		"--- TestNodeProxyGui2SliderHeight ---".postln;
		TestNodeProxyGui2SliderHeight.run;

		AppClock.sched(15.0, {
			"--- TestNodeProxyGui2Embedding ---".postln;
			TestNodeProxyGui2Embedding.run;

			AppClock.sched(30.0, {
				"=== All tests completed ===".postln;
			});
		});
	});

	nil
});
)
