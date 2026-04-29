// Test runner for NodeProxyGui2 embedding tests
// Run with: /Applications/SuperCollider.app/Contents/MacOS/sclang Test/run_tests.scd
(
"=== NodeProxyGui2 Embedding Test Runner ===".postln;

// Qt GUI requires AppClock. Schedule everything on AppClock.
AppClock.sched(0.5, {
	"Running TestNodeProxyGui2Embedding on AppClock...".postln;

	TestNodeProxyGui2Embedding.run;

	// Wait for tests, then exit
	AppClock.sched(10.0, {
		"=== Tests completed ===".postln;
		//0.exit;
	});

	nil
});
)
