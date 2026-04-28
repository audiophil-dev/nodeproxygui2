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
