# FinderDex desktop integration

Approved conversational scope: dual mobile/desktop, Home on launcher entry, peripheral-aware desktop interaction, themes and responsive layouts; motion with full/reduced/off preferences; existing Space Quick Look; archive open-with, receive/share/save and participation in Android document picker.

Approach: evolve R3.4 in an isolated branch, reuse existing providers, controllers, and transfer engine. Preserve the protected R2.2 branch. Rebuilding the shell would discard working interactions; cosmetic-only changes would not address desktop usability. User authorized execution with “Go”.

Implementation boundaries: local shared-storage document provider only, never root/private storage or credentials. Cloud and network remain existing integrations until adapted and validated. Android owns default choices and the system picker. RAR must remain honestly unsupported until a decoder is implemented. No installation or merge. The user subsequently authorized publication as an independent production app, com.finderdex, with its own signing key and a GitHub Release from a dedicated branch.

Motion: persisted full/reduced/off setting, system-disabled motion wins, reduced motion omits spatial effects. Quick Look uses theme colors, closes on Space/Escape, navigates in visible order. Navigation and drag feedback honor motion settings.

Desktop: persist Auto/Mobile/Desktop choice, observe mouse/trackpad changes; compact desktop fits narrow windows, no permission escalation. Home is launcher start; explicit file/shortcut intents retain their destinations.

External files: receive one/multiple content URIs with explicit destination and copy semantics, keep grants through completion. DocumentsProvider exposes currently accessible shared-storage roots and refuses traversal, symlink escape, unauthorized modes, and overwrites.

Verification: unit tests for policy, path containment and input limits; Android compilation, unit suite and lint; instrumented tests where hardware is available. Report missing checks explicitly.
