# FinderDex — Desktop, motion and Android integration

Implementation branch: `feat/finderdex-desktop-motion-integration`, based on R3.4 `671d637`.

## Included

- Shared navigation starts at Home. Explicit folder shortcuts still navigate to their requested folder. Desktop sidebar Home returns to Home instead of the storage root.
- Persistent Automatic / Mobile / Desktop setting. Auto responds to mouse/trackpad, desk mode or an external display; narrow windows retain compact desktop layout. A keyboard alone does not force desktop. Existing browser session and transfer controller survive layout changes.
- Full / Reduced / Off motion preferences, with live observation of Android's animator-disable setting. Navigation, existing desktop feedback, drag scaling, list placement and Quick Look follow the policy. Reduced mode removes spatial animation.
- Quick Look retains Space/Escape, uses theme colors and animates opening/closing. Arrow traversal follows the current filter, sort and group order. F2 opens the existing rename dialog.
- Save in FinderDex receives single or multiple shared `content://` files (up to 64), explicitly asks for a SAF destination, and uses the foreground transfer engine with progress, cancellation and conflict confirmation. Original files remain intact. Source grants follow the service; destination grant additions are released on completion. Results and cancellation are bound to the receiver's own operation identity.
- FinderDex storage roots participate in Android's document picker. Currently authorized local shared volumes can be browsed, read and used to create/write documents. Paths are revalidated, protected Android trees excluded, and exclusive creation uses a held parent descriptor. Cloud, root and Shizuku are not re-exported.
- Existing archive Open with integration remains. Android controls default selections and the system picker.

## Deliberately separate work

The full Material 3 Expressive desktop redesign, uniform cloud/network desktop sessions, RAR extraction, durable resumable transfer queue and universal undo remain separate work. The existing RAR handler does not imply extraction support. The user subsequently authorized publication as production version 3.5.0, independent package com.finderdex, with a separate production signing key. No installation or master merge is performed.

## Validation

See the final verification section below for actual command results. Android instrumentation compilation does not imply device execution. Before release, exercise DeX window resizing and keyboard/mouse selection, peripheral disconnect with an active transfer, Space/Escape timing and reduced motion, share-task removal during multi-file copying, document picker create/read/save and revoked volume permissions.

## Recorded automated checks

- JVM suite: **605 cases; 590 passed, 15 skipped, zero failures/errors**. The skipped cases require optional external conditions.
- Standalone motion/input/path policy checks: **9 passed**, including a reproduced-then-fixed case-insensitive protected-tree regression.
- `compileDebugAndroidTestKotlin`: passed after applying the required experimental test API opt-in. Tests were not executed on hardware.
- `lintDebug`: **0 errors**, 210 warnings. Existing warnings are not represented as a clean warning-free report.
- Independent source review found no remaining blockers after fixes for descriptor-relative creation, temporary grant ownership, operation identity and history persistence.
- Production publication requested by the user: package `com.finderdex`, version `3.5.0`, versionCode `26`, separate signing key. The release assets include APK identity, certificate fingerprint and SHA-256 verification.

The ARM64 build host uses native AIDL and a qemu-backed SDK aapt2 wrapper. A local init script retains native debug symbols because the downloaded NDK host strip tool is x86_64. This does not enable Android debugging in the release APK. Standard x86_64 CI does not require these local overrides.

Production key material and the R8 mapping are retained privately outside the repository. Only the public certificate fingerprint is distributed. The legacy Voyager release workflow is not used; FinderDex uses a `finderdex-v` tag from `production/finderdex` without merging into master.
