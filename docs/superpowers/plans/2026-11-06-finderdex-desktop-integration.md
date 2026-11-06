# FinderDex Desktop Integration Implementation Plan

> **For agentic workers:** use superpowers:executing-plans task-by-task.

**Goal:** deliver motion and Android file integration while closing Home and desktop-mode gaps.
**Architecture:** reuse the existing Compose shell and operation controller; add focused policies and Android adapters.
**Tech Stack:** Kotlin, Compose, DataStore, DocumentsProvider.
**Spec:** docs/superpowers/specs/2026-11-06-finderdex-desktop-integration.md

## Global Constraints
Preserve R2.2. No install or merge; independent production publication subsequently authorized by the user. Content URI grants only. Shared storage only. Do not claim RAR extraction.

## Review Focus
- Revoked permissions or removed volumes must deny document access.
- Symlinks and path traversal must never expose other locations.
- Reduced/system-disabled motion must not leave a closing dialog stuck.
- Peripheral changes must not reopen folders or cancel transfers.
- Shared URIs must remain valid until copy completion.

### Task 1: Motion and Quick Look
Files: ui/theme/FinderMotion.kt, FinderAppearance.kt, PreferencesManager.kt, FinderStyleSelector.kt, Navigation.kt, desktop/FinderGlassMaterials.kt, FinderQuickLook.kt, FinderDesktopScreen.kt.
- [x] Test full/reduced/off policy and system override.
- [x] Persist motion mode; observe system duration scale.
- [x] Integrate navigation, desktop feedback and animated themed Quick Look; preserve visible order.
- [ ] Run unit tests and compilation.

### Task 2: Peripheral mode and Home
Files: desktop/DesktopModeController.kt, desktop/rememberDesktopInput.kt, Navigation.kt, PreferencesManager.kt, SettingsScreen.kt, FileBrowserViewModel.kt, FinderDesktopScreen.kt.
- [x] Test explicit modes, mouse auto mode and keyboard-only compact mode.
- [x] Observe input devices and persist mode; show Home at launcher startup and offer desktop Home return.
- [ ] Verify session continuity and compile.

### Task 3: External save and document picker
Files: app/ReceiveFilesActivity.kt, app/FinderDocumentsProvider.kt, util/DocumentPathPolicy.kt, AndroidManifest.xml, resources.
- [ ] Test containment, malformed identifiers, names and URI limits.
- [x] Receive one/multiple shared content files into an explicit SAF destination with explicit conflict decisions.
- [x] Expose accessible shared-storage roots through DocumentsProvider; support read/create/write and selection.
- [ ] Compile, run unit tests/lint and record remaining physical checks.

### Completion
- [x] Review diff and perform independent whole-branch review.
- [ ] Record actual checks, remaining broader provider parity and build blockers.
