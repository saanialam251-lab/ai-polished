# Phases (from the project PDF)
- [x] Phase 0 - Skeleton that builds
- [x] Phase 1 - Home and splash (theme, animations)
- [x] Phase 2 - Board core: infinite grid, pan and zoom, tool bar, undo, minimise-able panel
- [x] Phase 3 - 2D shapes: labels, colour-coded angles, overlap decluttering, edit handles, double-tap menu with Details
- [x] Phase 4 - Graphs and derivatives: all functions, tangent lines, typing shortcuts
- [x] Phase 5 - Solve: pen, highlighter, eraser, sketch recognition
- [x] Phase 6 - Trigonometry: unit circle, triangles, parallel lines
- [x] Phase 7 - 3D solids: all solids plus prism and pyramid from your own shapes
- [x] Phase 8 - Polish: layers, long-press tooltips, tablet sizes, saving boards

Reference for every port: web-reference/www/index.html (v0.11). Nothing in the acceptance checklist is dropped.

## Phase 3 - still to come inside later phases
- Triangle extras not yet ported: altitudes, incircle, circumcircle, orthocentre, bisectors (Phase 6, with the trig tools)
- Bisect a line (5 taps) and the parallel-lines angle tool: Phase 6
- 3D prism / pyramid buttons are in the edit panel but switch on in Phase 7
- Sketch (rough shape to neat figure): Phase 5

## Phase 4 - notes
- Graphs (Graph and Derivative tools), implicit equations (x^2+y^2=25), f' / f'', tap tangent, typing shortcuts: done
- Equations and Details use a built-in maths layout (stacked fractions, powers, roots), so no WebView/KaTeX is needed
- Still on the list: the Derivatives topic's own extras are all in; trig graphs of the unit circle come in Phase 6

## Phase 5 - notes
- Pen, highlighter (colours + width), eraser, sketch recognition (line, triangle, rectangle, quadrilateral, circle) in every topic that has the tools
- Fix in this version: two compile errors from earlier phases (an unsupported `catch (_: ...)` and a missing import)
- The build workflow now prints a "Build failed" box on the Actions run page and saves build.log

## Phase 6a - this version
- Partial eraser (only the part you touch) with size and whole-stroke mode
- Thin, crisp angle marks (blur removed)
- Quadrilateral measurements editable (4 sides + diagonal AC); pentagon and hexagon tools; Regular button
- Parallel lines: automatic detection, numbered angles 1-8, family chips, tap an angle, bisectors, facts in Details
- Lines that close up become a shape (auto-join, on/off with the link button); points/corners snap together
- Triangle: medians, altitudes, bisectors, centroid, orthocentre, incircle, circumcircle, angle at centre; cyclic quadrilaterals
- Undo / redo with named steps for every kind of change
- Still to do (Phase 6b): unit circle and the trig triangle

## Phase 6b - this version
- Labels (side lengths, angles, vertex letters) now sit on a small dark rounded backing with lighter text, so they stay sharp; very small shapes hide the numbers
- Unit circle (drag to set the angle; sin and cos sides, angle arc) and the trig triangle (angle and hypotenuse sliders, adjacent / opposite / hypotenuse) with sin, cos, tan as fractions
- Floating maths symbols only on the home screen; toast no longer covers the zoom buttons; compact header buttons so the title is readable

## Phase 7 - 3D solids
- Cube, cuboid, square pyramid, triangular prism, cylinder, cone, sphere: tap to place, drag to rotate, resize, move
- Prism and pyramid from your own flat shape (edit panel: 3D prism / Pyramid)
- Switches: Spin, Wire, Labels, Diagonal, Height, Axes; volume and surface area with fractions in Details
- Not built yet: joining several rotated pieces into one combined solid
- Next: Phase 8 - polish (layers, long-press tooltips, tablet sizes, saving boards)

## Phase 8 - polish
- Boards save by themselves (JSON files in app storage); 📂 My boards on the home screen: open, rename, duplicate, delete; tap the board title to rename; 💾 saves now
- Layers: show/hide, lock, reorder, see-through slider, move a figure to another layer
- Long-press tooltips on the toolbar and buttons; light haptic taps
- Interface size setting (Aa on the home screen) for phone, tablet and large panel; four board backgrounds (🎨)
- Still on the long-term list: combine several solids into one, handwriting-to-text, geometry instruments (ruler, protractor, compass), classroom tools (timer, spotlight), export to PDF/PNG, split screen

## Phase 8b - look and feel
- Tool bar: icons only, never scrolls; extra tools open in a "more" grid; the held tool shows as a small tag on the board
- Edit panel: icon tabs (measure / style / info) and icon buttons instead of a long scrolling list; long-press any icon for its name
- Chips wrap instead of scrolling (graph presets, pen colours, parallel-line angles); graph sliders share one row
- Glass panels everywhere (gradient, bright top edge, coloured shadow), soft drifting glow behind the board (blurred on Android 12+)
- Sparkle burst and ring on every quick tap (switch with the sparkle button on the home screen)

## Phase 8c - brown theme, eye box, single angle
- Brown/copper theme, shrunk buttons and panels, shadows everywhere, serif + letter-spaced text.
- Eye button in the edit panel of every polygon opens a small scrolling "Labels" box: show/hide each side, angle and the corner letters; edit side lengths and angles.
- Tap a corner twice (or three times) to select the whole figure and show only that angle; do it again to bring everything back. Move / resize by tap now come from the edit panel buttons or by tapping the sides.

## Phase 8d - brown everywhere, select box, pictures, hidden details
- Every panel, button, glow and menu is now brown/copper (board colours and 2D figure colours are unchanged). Topic cards and sparkles are warm too.
- Everything is smaller: default interface size 85% on phones (Aa on the home screen goes 75% to 150%), smaller tool buttons, chips, text.
- New figures start with their side lengths, angles and line/circle lengths hidden. Select a figure and tap the eye: polygons open a small scrolling "Details" box (Show / Hide / per-label eyes); lines and circles toggle their length. "Hide" also switches off extras (diagonals, midpoints, circles...) without touching the lines; "Show" brings them back.
- Sketch tool has two faces: Shape (rough shape to neat figure) and Drag (drag a box; whatever is inside is selected; drag inside to move, corner square to resize; copy / cut / delete / paste buttons).
- Paste: copy text or a picture anywhere on the phone, then double-tap empty board (Move, Select or Drag tool) for a Paste / Picture menu, or hold on the board, or use the 📥 / 🖼️ buttons (☰ menu and Sketch Drag). Triple-tap empty board opens the picture chooser.
- Pictures: drag the corner handles to resize, ▣ to crop (drag the four corners), ↺ brings the cropped part back, move / resize / copy / cut / delete like any figure. Saved with the board. Text can be edited, sized and resized too.
- Pen: tap the pen to open its colour / width box, tap again to close (✕ too). Highlighter: double-tap its icon. Eraser: tap to open its box.
- Not compiled in the build environment: please run the GitHub Actions build and send any error text back.

## Phase 8e - Content selection, Answer tab, grouped toolbar, text, neat maths
- Content (the old Drag): drag a box round writing, pen drawings, text, pictures or figures. A tick / cross asks if you want them. After the tick you get the bar under the board: copy, cut, delete, paste, crop (pictures), K (neat maths), pen thickness, colour (applies to everything selected), size slider, and arrows to nudge. Drag inside the box to move, the corner square to resize.
- Select tool has two faces: 👆 Pick (default) and ⬚ Content (box round several figures). After the tick, the figures are checked for parallel lines (several pairs, with a "Set 1/2" button); the ∥ button is on every board.
- Answer tab (🧮): pick triangle, right triangle, rectangle, square, quadrilateral (4 sides + diagonal), pentagon, hexagon, circle, any 3D solid or a graph, type the sizes, and it is drawn at those sizes with its labels. Pen, highlighter, text and everything else work on top.
- Toolbar is now a few icons: Write (pen / highlighter / eraser, colours, width, solid / dashed / dotted), Lines (line, curve, point), Shapes, 3D, Graph, then Select, Move, Content, Answer. Every tool is available in every topic.
- Circles no longer show a radius until you ask (👁 or the r chip).
- Aa adds your own text (a question). K turns maths such as x^2, \frac{a}{b}, \sqrt{x}, \alpha into neat symbols (x², a/b, √x, α). This is a built-in converter, not the full KaTeX renderer.
- Fixed: missing import of `height` in EraserPanel.

## Phase 8f - parallel lines: only real transversals, one at a time
- A line counts as a transversal only if it cuts through the inside of both parallel lines. Sides that just meet them at a corner (triangle / rectangle sides) are ignored.
- Only one set of angles is shown. Double-tap another transversal (or a line of another parallel pair) with Select and that set replaces the one showing. The "Set n/m" button still cycles.
- Tapping inside a selected Content box with Select now counts as a tap (it used to be swallowed by the move gesture).

## Phase 8g - graphs for everything, ? help, stylus
- Graph equations: |x| bars, max / min / mod / atan2 / cbrt, circles, ellipses, squares, rectangles, rhombus, triangle (presets "Shapes by equation"), parametric curves (cos(t), sin(t)) and polar curves r = a sin(b theta). New preset groups: Shapes by equation, Curves, Calculus.
- Derivatives typed the way they are written: "dy/dx of x^3 - 3x", "d/dx(sin(x))", "d2y/dx2 of ...", "derivative of x^2" - plots the function and its slope curve. The graph panel always shows dy/dx and d2y/dx2 buttons.
- The "Aa" interface-size button on the home page is gone. A "?" button (home page, and on every board) opens a picture guide for graphs, derivatives, shapes, curves, Content, Answer, parallel lines, text/K and the pen.
- Stylus: palm touches are ignored while a stylus writes, the stylus eraser end erases, fast strokes use every reported point.

## Phase 8h — typed maths, neat maths everywhere, new "?" page

- **Typed maths (no backslashes).** `math/RichMath.kt` turns what people type into a `MathNode`: `a/b` → stacked fraction (`2x/3y`, `(a+b)/(c+d)`, `x^2/y^2`), `sqrt(x)` / `sqr(x)` / `sqrt x` / `cbrt(x)` → roots, `x^2`, `x_1`, `x^(n+1)`, `a*x` → `ax`, `pi theta alpha …` → Greek, `<= >= != ->`, `|x|`. LaTeX still works (`\frac \sqrt[n] \sum \prod \int \lim \binom`, matrices, `\text \mathbf \mathbb \hat \vec`). Words such as "and/or" stay words.
- **Layout engine.** `math/MathLayout.kt` (android.graphics): stacked fractions, real radicals, tall brackets that grow with the content, sub/superscripts, big operators with limits, matrices, accents, line wrapping. `MathView` (Compose) and board text both use it.
- **Board text** (`drawTextFig`) is now always drawn through the engine; layouts are cached at a fixed size and scaled while zooming (`MathCache`). `fitText` measures with the same engine. K re-fits the box (maths is automatic now).
- **Add text** dialog shows a live "looks like this on the board" preview.
- **Info bars / Details / answers** (`RichLine`): text parts that contain maths are rendered through the engine, fractions (`Part.F`) too, so roots, powers and fractions are never plain text.
- **"?" page** redesigned: grid of topics (Graphs, Derivatives, Pen, Dragging, Content, Sketch, KaTeX, Shapes, Curves, Answer, Parallel) → each opens a page with a large picture, numbered steps, "try typing → how it looks" examples and tips; the KaTeX page holds the full cheat sheet (11 groups, each row rendered live).
- Not yet compiled on a machine: if GitHub Actions reports an error in these files, fix that line first (`math/RichMath.kt`, `math/MathLayout.kt`, `ui/components/HelpDialog.kt`).

### 8h follow-up
- Parser follows the "100% functions map" (Hh.txt): towers (`a^x^2`, `a^(x^y)`), `root(...)`, `4rt(...)`, `lim_(x->0)`, `sum_(i=1)^n`, `int_(0)^1 ... dx`, `prod_`, `log_(a) b`, `d/dx (a/b)`, `upon`, `binom n k`, `vec/hat/bar a`, `floor()/ceil()`, `30deg`, `angle`, `triangle`, `arc AB`, `perp/parallel/cong`, all Greek names. Sub-scripts never chain; only `^` does.
- Chips and panel buttons draw their label as maths when it contains maths (1/x, √x, dy/dx, d²y/dx²).
- Parallel lines: with nothing selected, the sides of drawn rectangles/triangles/quads are now searched too (before, only loose lines were, so a rectangle's set vanished when the Content box was dismissed). New "All sets" chip shows the angles of every transversal together.

### 8i — letters and the Content eye
- `board/Labels.kt`: labels may carry primes/digits ("A′B′C′"); `Nm` indexes them; `runLabel(k)` gives A…Z, A′…Z′, A″… . Every new line (seg) gets two letters from `nextSegNames()` and draws them at its ends (hide key "v" switches them off).
- 🔤 button in the edit panel (lines and polygons) opens `NamesBar` to type new letters (undoable); A′B′ adds primes.
- Content bar 👁 (`grpEye`, `toggleGroupEye`): shows all details of the selected figures and, through `drawCrossings(... deg)`, the degrees of every angle at crossings between selected figures. Off by default so the board stays clean.

### 8k — the KaTeX catalogue
- `ui/components/KatexExamples.kt`: 21 groups / 228 typed examples (powers, towers, fractions, fraction-upon-fraction, roots, nested roots, brackets, Greek, limits, sums, integrals, logs/trig, derivatives, word-symbols, operators, LaTeX, matrices, text & style, special symbols, stress tests, famous formulas). The "?" page → KaTeX topic shows each as a big live-drawn card (`ExampleCard`) with its typing underneath.
- Parser: `^inf`/`^alpha` take the whole word; a function name (sin, log_a …) keeps a gap before what follows even inside brackets; `\psi \chi …` work.

### 8l — the eye is a mode, joints
- Content eye (`grpEye`) no longer rewrites figures: `drawFig(... all = grpEye && fig in group)` shows everything for selected figures only, so it disappears with the box and returns when selected again while the eye is on.
- `board/Joints.kt`: letters at coincident points are drawn once (`labelPlan().skip`), length labels of lines crossing at their middles are spread (`lenT`), angles between lines/corners that meet at a point are drawn as wedges and, with the eye, as `∠BAD 77.5°` (`drawJoints`).
- Line details panel (`SegDetails`, opened by 👁 on a line): letters, length, and each joint angle with a number box (`setJointAngle` turns the other line); the eye of an angle (`ov` key `jdN`) shows its degrees.
- Parser keeps emoji (surrogate pairs) in one piece; help text is drawn through the maths renderer (lines that show LaTeX code stay plain).

### 8m — big text, any root, graph roots
- Add text: field and preview have fixed heights and scroll (preview was squeezed to nothing by long text). `MathCache` keys by the text itself so long texts are not re-built every frame.
- Any root: `4brt6`, `(4/5)brt(6/8)`, `nbrt(x)`, `infbrt(x)`, `(int_0^1 x dx)brt(6)` in `RichMath` (index before brt). The root draws a big index smaller first and enlarges the radical if it still does not fit (`lay` Root + `sqrtBox`).
- Graph section (`ExprParser`): `cbrt(x)` is drawn as a real cube root; new `nrt(n, v)` evaluator; `sqr()`, `root()`, `4rt()`, `4brt()`, `(1/3)brt()` are rewritten by `nthRoots` in `normalize`, so plotting and the equation bar both understand them.

### 8n — review pass + extras
- Static review (no compiler available): every `com.aismartmath` import resolves to a definition; Compose symbols used in all files are imported.
- Graph: new functions `gamma`, `erf`, `asinh`, `acosh`, `atanh`, `log2`, `round`, `frac`, two-argument `pow(a,b)`, `hypot(a,b)`, `logb(base,x)`; displayed as real maths (Γ, log₂, powers, log with base).
- "?" page: cards and steps cascade in (`FadeIn`: fade + slide + slight scale, 45 ms stagger).

## Phase 11 — Question tab: angle chase (adjacent / vertical / linear pair / parallel lines), x-algebra, implicit constructions, Pythagoras with x, Heron
- `board/AngleSolver.kt` (new). Figures: a triangle with a point on a side (and a line DE ∥ BC across two sides), two crossing lines, rays standing on
  a straight line, parallel lines cut by a transversal. **If an angle needs a line the question never drew (∠ADC needs AD), the line is joined
  automatically and written as "Construction: Join AD (not stated, assumed …)"**; a point that is only named in an angle is assumed to be on a side.
- From the drawn figure every true relation is collected: angle sum of a triangle, angles on a straight line / linear pair, adjacent angles,
  angles around a point, vertically opposite angles, isosceles base angles, exterior angle theorem, corresponding / alternate / co-interior angles for
  lines declared parallel, complementary / supplementary statements, perpendicular and bisector facts. They are solved one at a time, each
  step with its reason in brackets; **x** is found when an equation has only x left ("3x + (2x + 10) = 180° ⇒ 5x = 170 ⇒ x = 34").
- When the facts do not fix an angle (point D anywhere on BC), the answer says so and gives the relation, e.g. ∠ADC = 40° + ∠BAD (exterior angle theorem).
- The figure is built twice: once to read its structure, once at the solved values; the drone measures it and compares with every solved angle.
- `board/PythagorasSolver.kt` (new): right triangle with x in the sides → Pythagoras → expand → quadratic → factorise or formula → reject a root that
  makes a side negative → draw with real lengths and measure.
- Numeric triangle solver: area now uses Heron's formula (or ½ × leg × leg for a right triangle).
- Not yet: midpoint theorem and its converse (proof and lengths), similarity, circles; ∠ADC type questions where D is defined by a length (BD = 2 DC).
- Not compiled in the build environment: build with GitHub Actions and send any error text.

## Phase 10 — Question tab: triangle PROOFS (congruence), Given / To prove / Proof with a reason in brackets on every line
- `board/ProofSolver.kt` (new, pure Kotlin, no AI, no internet). Reads **Given** (AB = CD, ∠B = ∠E, ∠B = 90°, AD ⊥ BC, D is the midpoint of BC,
  AD bisects ∠A, D lies on BC, AD and BC bisect each other at O, points A, B, C, D lie on a line in this order, ABCD is a parallelogram / rhombus /
  rectangle / square / kite, altitude AD, median AD, isosceles, equilateral) and **To prove / To show** (ΔABC ≅ ΔDEF, AC = BD, ∠B = ∠C, AD ⊥ BC).
- Builds a figure that satisfies the Given facts (two separate triangles; one triangle with a point on a side; a quadrilateral with diagonals;
  two lines crossing at a point; points on a line) and puts it on the board with the app's own figure, quadrilateral and segment figures.
- The prover: common side, midpoint, right angles, bisector, vertically opposite angles, **adding / subtracting equal segments**
  ("AB + BC = CD + BC … AC = BD"), the five congruence rules **SSS, SAS, ASA, AAS, RHS**, **CPCT**, isosceles theorem (only after plain proofs fail,
  so it never proves itself). If the goal is not reachable it tries extra lines one at a time: altitude, median, angle bisector, diagonals.
- The proof is written on the board as: Given: • … / To prove: … / Construction: … / Proof: numbered lines, each with its reason in brackets:
  (Given), (D is the midpoint of BC), (common side), (vertically opposite angles), (adding steps 1 and 2), (CPCT: …, step 4),
  (proved above, step 5). The drone then measures the figure and checks every Given fact and the conclusion.
- Numeric triangle questions now use the same layout: Given / To find / Solution with reasons in brackets.
- ✗ Wrong → next construction / next way of drawing; ✓ keeps it.
- Trigonometry code (TrigEngine / TrigSolver) is still in the project but is not the focus; it comes back in a later phase.
- Not yet: parallel lines (alternate / corresponding angles), similarity, circles, longer proofs with several constructions.
- Not compiled in the build environment: build with GitHub Actions and send any error text.

## Phase 9b — Question tab: thinking, board solution, ✓/✗, theorems, trigonometry (no AI, no internet)
- **Panel flow** (`board/QuestionPanel.kt`): what you type is shown as maths while you type (MathTextView). Solve ✓ →
  🧠 *Thinking* (about 2 s: the facts read, the data pattern SSS/SAS/ASA/SSA/RHS, every theorem weighed with ✓ will use / – not needed,
  the trig functions in play, the conditions a triangle must meet, how many figures were tried) → reading / solving / the figure is
  drawn with the app's own figure and extras → the **drone checks** the figure against every fact → the **solution is written on the
  board line by line** (typed maths, so fractions, powers and roots are drawn by your maths view) → answers → **✓ Right / ✗ Wrong — check again**.
- **✗ Wrong**: `QuestionSolver.solve(text, variant + 1)` — changes the assumptions (which corner is the right angle, which sides are equal),
  the example values, the way the figure is placed (triangles); the quadrant (given-ratio questions); the range and the scan (equations).
  The old figure and the old solution text are removed first; undo works.
- `board/QuestionSolver.kt` (triangles): angle sum, isosceles theorem, Pythagoras, sine rule, cosine rule, **exterior angle theorem, Heron's
  formula, circumradius (extended sine rule), inradius, altitude length, Apollonius' median**, `sin A = 3/5` style givens.
- `board/TrigEngine.kt`: own parser for typed trig (sin θ, 2sinθcosθ, sin²x, sin 2θ, tan 30°, √3/2, π/6), exact values (1/2, √3/2, 2−√3 …),
  equation scan over a range, identity test, a table of 21 named identities.
- `board/TrigSolver.kt`: *find the value* (sin 30° + cos 60°), *solve* an equation (every solution in the range + general solution),
  *prove / is it an identity* (checked by numbers, counter-example if false), *if sin θ = 3/5 find cos θ, tan θ* (right triangle drawn, quadrant),
  *find θ if tan θ = 1/√3*.
- `BoardState`: `beginQuestion(plan)`, `writeSolution(line)`, `keepQuestionDrawing()`, `clearQuestionDrawing()`.
- Honest limits: an identity is confirmed by numbers (a written algebra proof needs symbolic steps — later); congruence proofs with extra lines,
  quadrilaterals, parallel lines and polynomials are still to come. Not compiled here — build with GitHub Actions and send any error text.

## Phase 9a — Question tab (triangles), no AI, no internet
- New 📝 **Question** button in the tool bar (every topic). Type or paste a question, tap **Solve ✓**.
- `board/QuestionSolver.kt` (pure Kotlin): reads the text with patterns (triangle letters, ∠A = 40°, AB = AC, AB = 5 cm, right angle at B, ⊥, hypotenuse / legs / "sides 3, 4, 5", isosceles / equilateral / acute / obtuse, "angles are x, 2x, 3x", "∠A = 2∠B", ratios, "two angles are 50° and 60°"), turns them into equations, solves with angle sum, base angles, Pythagoras, sine rule and cosine rule, then **builds the figure and checks it against every fact**. A figure that fails is thrown away and drawn another way; if the question leaves a value free, example values are tried one by one and checked.
- `board/QuestionPanel.kt`: the text box, example chips and the step-by-step status list (reading → equations → solving → drawing → check marks → answer).
- `BoardState.drawSolved(plan)`: puts the checked triangle on the board (a new question replaces the old figure; undo works), switches on only the sides / angles that belong to the question, and switches on extras the question mentions (altitudes, medians, bisectors, incircle, circumcircle, centroid, orthocentre, midpoints).
- Not understood yet (the panel says so instead of guessing): quadrilaterals, parallel lines, circles, polynomials, exterior angles, proofs (congruence with extra lines) — these are the next phases.
- Not compiled in the build environment: run the GitHub Actions build and send any error text back (files: QuestionSolver.kt, QuestionPanel.kt, the small addition at the end of BoardState.kt).
