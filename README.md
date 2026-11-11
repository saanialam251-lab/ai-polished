# Smart Math Board - native Kotlin app (Phase 8b)

Built from `smart-math-board-v0_11.zip` (kept in `web-reference/` as the reference) and the project PDF.

## What is in this zip
- Phase 0: empty Kotlin / Jetpack Compose app that GitHub Actions turns into an APK.
- Phase 8 (new): saved boards, layers, tooltips, interface size and backgrounds - see PHASES.md.
- Phase 7: 3D solids - see PHASES.md.
- Phase 6b: readable labels, unit circle, trig triangle.
- Phase 6a: see PHASES.md - eraser fix, polygons, parallel-line angles, auto-join, triangle circles and centres, undo/redo history.
- Phase 5: Solve tools - pen, highlighter (colours and width with a live preview), eraser with a ring, and Sketch: draw a rough shape and it turns into a neat figure you can edit.
- Phase 4: graphs - type an equation (y = f(x), x^2+y^2=25 ...), every function (cosec, sec, cot, asin, acos, atan, sinh, cosh, tanh, floor, ceil, sign ...), presets, a/b/c sliders, Animate a, f\u2032 and f\u2033 curves, tap a graph for its tangent and slope, intercepts and turning points in the info bar, typing shortcuts (a*x becomes ax, x^2 becomes x\u00b2).
- Phase 3: 2D shapes (triangle, right triangle, quadrilateral, rectangle, circle) with vertex names, side lengths, colour-coded angles, overlap decluttering, drag handles, Select tool with 1-5 taps, double-tap menu (Copy, Cut, Delete, Details, Paste), hold-to-paste, edit panel with dimensions, size, rotate, colours and Details in fraction form, shaded crossing angles.
- Phase 2: the infinite board - pan, pinch-zoom (1% to 50,000%), self-spacing grid and numbers, axes for graph topics, Point and Line tools with snapping, undo, clear, zoom buttons, reset, full screen, minimise-able bottom panel.
- Phase 1: animated splash (same triangle + circle + title + loading bar as the web version), animated home with the same 11 topic cards, colours and icons, and a placeholder topic screen with each topic's real tool bar.

## How to upload
1. On GitHub, DELETE the old web-app files from the repo root: `package.json`, `capacitor.config.json`, `www/`, `assets/`, and `android/` if it exists. (The old `.github/workflows/build-apk.yml` is replaced by the new one.)
2. Unzip this file and upload EVERYTHING (including the hidden `.github` folder) to the ROOT of the repo.
3. Push / commit to `main`. Open the Actions tab, open the latest run, download the `smart-math-board-apk` artifact, unzip and install the APK.

The first native build may need a small fix - if the run fails, open it, copy the red error text and send it to me.

## If the build fails
Open the failed run in the Actions tab. Scroll to the top of the run page: a "Build failed" box lists the exact error lines. Copy them and send them to Claude. The full log is also saved as the `build-log` artifact.
