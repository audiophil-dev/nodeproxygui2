// Minimal stub model classes for SE GUI reproduction.
// These satisfy the interface expected by KlongSoundEnvironmentGui and KlongGenGui
// without requiring a booted server or full KlongGen initialization.
//
// After adding this file, recompile the class library (Cmd+Shift+L) before
// running minimal_se_repro.scd.

MinimalEnvStub {
	var <name, <hasModulation;
	var <presetFrom, <presetTo, <presetMorphValue;

	*new { |name, hasModulation = false|
		^super.new.init(name, hasModulation)
	}

	init { |aName, aHasMod|
		name = aName.asString;
		hasModulation = aHasMod;
		presetFrom = "none";
		presetTo = "none";
		presetMorphValue = 0;
	}

	// Directs Object.gui to use the real KlongSoundEnvironmentGui
	guiClass { ^KlongSoundEnvironmentGui }

	// Suppress model.changed so KlongSoundEnvironmentGui.update is never triggered.
	// Without this, update fires immediately via AppClock.sched(0) and calls
	// presetFromText.string_ on a nil variable (prMakePresetStatusRow is commented out),
	// flooding AppClock with DoesNotUnderstandError.
	changed { }

	clear {}
}

MinimalGenStub {
	var <environments;

	*new { |envs|
		^super.new.init(envs)
	}

	init { |envs|
		environments = envs;
	}

	// Directs Object.gui to use the real KlongGenGui
	guiClass { ^KlongGenGui }

	clear {}
}
