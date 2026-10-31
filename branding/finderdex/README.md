# FinderDex R2 — launcher artwork

The blue glossy split-face/folder icon approved for FinderDex replaces the Voyager placeholder.

- Original transparent WebP art: `app/src/main/res/drawable-nodpi/finderdex_icon_art.webp`
- Adaptive launcher foreground: `res/drawable/ic_launcher_foreground.xml` (inset to protect the motif on Android launcher masks)
- Gradient background: `res/drawable/ic_launcher_background.xml`
- Android 13+ themed icon: `res/drawable/ic_launcher_monochrome.xml`
- Legacy drawable reference: `res/drawable/ic_finderdex.xml`
- Manifest uses `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`, whose adaptive XML definitions already exist.

All artwork is original; it is inspired by the general split-face/folder concept but is not an Apple asset. The raster image is a PNG-derived transparent WebP; it is not vector artwork. No build or device validation performed in this commit.

This commit deliberately includes [skip ci] to reserve compilation for the next requested build.
