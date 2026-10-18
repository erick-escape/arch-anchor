# ArchAnchor: design brief for the UI redesign

The input for a new design of the web UI (`web/`). It has three parts:
- what the tool is and the brand it already has;
- what the current UI does, down to its strings, so nothing is lost;
- where the new design should go.

It is written for the designer (Claude Design) and for whoever implements the result. Issue #56.

The current UI was built before the tool had a visual identity. The brand came later, from the
ECSA 2026 talk and poster (`deck-kit/appsscript/00_theme.gs`, `20_vocab.gs` in the UFLA workspace).
The new design replaces the current look entirely. It must keep every feature in
[section 5](#5-feature-checklist).

Files next to this brief:

| Path | What it is |
|---|---|
| `brand/archanchor-mark.svg` | The mark for light backgrounds |
| `brand/archanchor-mark-dark.svg` | The mark for dark backgrounds |
| `brand/archanchor-mark-mono.svg` | The chevron alone, single ink, for favicons and the reference-class tile |
| `current-ui/overview-before.png` | The Analyze page today, bookstore example |
| `current-ui/sidebars-violation.png` | Module detail + a violation card today |
| `current-ui/recommendations-panel.png` | The recommendations panel today |

The screenshots were taken at `c35fbdf` (13 Oct 2026), before the Refactorings / Violations tabs
were added (`ae6d6e0`). Section 4 describes `main` as of `f804c45`.

Built from this brief in Claude Design. Both are private to their owner until shared:
- the **ArchAnchor design system**: the tokens of section 3 and the vocabulary of 2.5 as live
  components. https://claude.ai/artifact/VSYe8N7r7uV7iktGn6nFRR
- the **ArchAnchor redesign** canvas: the artboards of section 7.
  https://claude.ai/artifact/1Y6dWU4pnRbY77RTm9yD9z

---

## 1. The tool

ArchAnchor reads a Java codebase and recovers its architectural constraints: which module may
depend on which package. It needs no architectural documentation to start. In each module it
elects a **reference class**, the class whose dependencies best represent the module, rated on
similarity to its peers and on the violations it would leave. The reference class's dependencies
become the module's contract (`CAN-DEPEND`, or `MUST-DEPEND` in MUST mode). A dependency outside
the contract is a **violation**. From those measures the tool recommends **split**, **merge** and
**move** refactorings, but only those with a net architectural benefit. The architect can override
the election, change the rating weights, apply recommendations and export the constraints as a PDF.

The users are software architects and researchers. The tool is the subject of an ECSA 2026 paper
and of an ICSE 2027 tool-demo submission, whose reviewers run it on `examples/bookstore`.

**Tone.** Every sentence in the UI must be one the tool can back. The homepage may be persuasive,
but only through claims the research defends ("The architecture is already in your code."). No
"AI-powered", no invented numbers, no "10×". Introduce the idea first; the tool is the evidence
that the idea works.

---

## 2. Brand

### 2.1 Mark and lockup

- **Mark.** Chevron (an "A" that is also an anchor's arms, with anchor-stock nubs), a mountain
  inside, and a diamond base plate.
  - `brand/archanchor-mark.svg` uses a deepNavy chevron, mountainBlue mountain and baseBlue base.
  - `brand/archanchor-mark-dark.svg` uses a wash chevron, anchorBlue mountain and baseBlue base.
    deepNavy on the dark page is 1.22:1 and would disappear.
  - Both carve their white areas out of the shape, so the background shows through. The deck
    version painted them white because Google Slides has no masks.
  - The carved shapes are solved as plain polygons, so there are no masks and no ids. The mark
    can be inlined any number of times on one page. A render at 416 px differs from a masked
    version by zero pixels.
- **Lockup.** Mark + wordmark **Arch**​**Anchor** in Jost Bold, with no space between the two
  words.
  - Light background: "Arch" in deepNavy, "Anchor" in anchorBlue.
  - Dark background: "Arch" in wash, "Anchor" in anchorBlue (5.00:1).
  - Tagline: *Architecture grounded in code.*
  - Proportions from `LOCKUP` in `20_vocab.gs`, all relative to the mark's size:
    - wordmark starts 1.24 × to the right;
    - wordmark cap size 0.60 ×;
    - tagline size 0.19 ×, set 0.68 × below the top.
- **Monochrome mark.** The chevron alone in one colour. Use it at small sizes, where the mountain
  turns to mush:
  - the favicon (16-32 px);
  - the glyph on a reference-class tile;
  - badges.

### 2.2 Palette

Sampled from the logo with a weighted colour histogram (`00_theme.gs`). Do not invent new brand
hues; derive tints and shades from these.

| Name | Hex | Meaning in the brand |
|---|---|---|
| anchorBlue | `#2287F0` | Primary. The word "Anchor", the reference class, allowed dependencies, meters, focus |
| deepNavy | `#112749` | The word "Arch", the chevron; strong text; the dark theme's surface |
| mountainBlue | `#0E5FB6` | The mountain; light theme's links and primary buttons (see 3) |
| baseBlue | `#9DCAEE` | The base plate; brand borders, meter tracks |
| wash | `#EAF3FE` | Tint of anchorBlue; panels, module containers; dark theme's text |
| ink | `#22282E` | Body text |
| muted | `#9AA7B2` | Ordinary (non-reference) classes, secondary strokes |
| shadow | `#D7DEE7` | Dividers, the offset shadow of deck panels |
| white | `#FFFFFF` | Surfaces |

### 2.3 Three semantic colours, one meaning each

| Name | Hex | Means | Deck drawing |
|---|---|---|---|
| conform | `#2E9E5B` | A dependency that conforms to the reference class | green outline, "conforms" panel |
| divergence | `#D64545` | A forbidden dependency: the violation | red dashed arrow |
| absence | `#E9A13B` | A missing mandatory dependency (MUST mode) | amber dotted arrow |

On the graph and in every piece of architectural data, these three colours mean dependency status
and nothing else. An allowed dependency edge is anchorBlue, not green; green is reserved for a
check that passed.

Outside the architecture data, system feedback may borrow conform and divergence for success and
error, because the meanings agree. Absence amber is never a generic "warning". **Priority never
uses the trio.** Today High/Medium/Low are red/amber/green borders; that reads as "violation /
absence / conforms" and must go (see 6.5).

### 2.4 Typography

| Role | Font | Use |
|---|---|---|
| Display | **Jost** (Google Fonts) | The wordmark, headings, the homepage claims, big numbers on stat tiles |
| UI | **Inter** (Google Fonts) | Everything dense: panels, cards, forms, tables, 12-16 px |
| Code | **Roboto Mono** (Google Fonts) | Package names, fully qualified names, constraints (`AC1 - … CAN-DEPEND …`), rates |

- Jost is the deck's choice: the closest Google Font to the URW Gothic of the original talk. It is
  geometric and beautiful large, but too loose at 12 px, hence Inter for the UI.
- Numbers that line up (percentages, rates, counts) use tabular figures.
- Suggested scale: 12 / 14 / 16 / 20 / 24 / 32 / 48 / 64.

### 2.5 The visual vocabulary

One drawing per concept, used identically everywhere: graph, panels, homepage, empty states. This
is what made the deck and the poster read as one system. Constructions are in `20_vocab.gs`.

| Concept | Deck construction | Web notes |
|---|---|---|
| **Class** | Rounded square; white fill; 1 pt muted stroke; `</>` in Roboto Mono, muted, 0.26 × the side; name below in micro text | Stroke in `line-strong` (muted on white is 2.46:1, below the 3:1 a graphic needs) |
| **Reference class** | The same square filled anchorBlue, wearing the monochrome mark in white (0.56 × the side); name below in bold anchorBlue | The reference class is always this tile: on the graph, in lists, in cards |
| **Module** | Rounded container; wash fill; 1.25 pt baseBlue stroke; a name tab riding the top edge (anchorBlue, white bold text, 14 u in from the left, half above the edge) | Light theme: tab fill mountainBlue (white text 6.30:1; on anchorBlue it would be 3.63:1). Dark theme: tab anchorBlue with page-coloured text (5.00:1) |
| **Package** | Pill chip; white fill; 0.75 pt baseBlue stroke; Roboto Mono deepNavy text | Every package name in the UI is this chip |
| **Allowed dependency** | anchorBlue solid arrow, filled head | |
| **Divergence** | divergence dashed arrow | The violation |
| **Absence** | absence dotted arrow | A MUST dependency a class lacks |
| **Meter** | 0-1 bar; baseBlue track; anchorBlue fill; rounded | Similarity and rates are shown as meters plus a number, never as formulas |
| **Stat tile** | wash tile; baseBlue stroke; big anchorBlue number (Jost Bold); ink caption | Headline numbers: project similarity, violations, modules |
| **Panel** | Rounded white panel with a faked offset shadow | On the web, a real shadow in light and a border with a faint glow in dark |
| **Title states the claim** | Every frame title is a claim, not a topic | Page headings say what the user is looking at ("4 modules · 1 violation"), not "Analyzed Modules for Project:" |

The ECSA poster (`graduation/ECSA/talk/preview/poster.png`) shows the vocabulary in use. Its
"How the anchor is chosen" panel is a good model for explaining the election with meters instead
of formulas.

---

## 3. Theme tokens

Two themes. **The default follows the system** (`prefers-color-scheme`).
- The header has a three-state switch: **System / Light / Dark**.
- An explicit choice is remembered per browser; choosing System forgets it.
- Set the theme before first paint, so there is no flash.

Every pair below was checked with WCAG 2.x contrast:
- text needs ≥ 4.5:1;
- graphics, borders that identify a control, and focus rings need ≥ 3:1.

The ratio given is the **worst case** over every surface it can sit on:
- light: page, surface, sunken;
- dark: page, surface, sunken, raised.

How the derived colours were made: each is the brand colour mixed toward black (light) or white
(dark) by the smallest step that reaches its ratio, so it stays as close to the brand as possible.

| Token | Light | worst | Dark | worst | Role |
|---|---|---|---|---|---|
| `page` | `#F7FAFE` | | `#0A1628` | | Page background |
| `surface` | `#FFFFFF` | | `#112749` (deepNavy) | | Panels, cards, nodes |
| `sunken` | `#EAF3FE` (wash) | | `#0E2142` | | Wells, module containers, inputs |
| `raised` | `#FFFFFF` + shadow | | `#17325C` | | Popovers, menus, the command palette |
| `accent-soft` | `#E2EEFD` | | `#15305A` | | Selected rows and tiles. `text` and `link` pass on it; `text-muted` does not in light (4.30) |
| `text` | `#22282E` (ink) | 13.29 | `#EAF3FE` (wash) | 11.38 | Body text |
| `text-strong` | `#112749` (deepNavy) | 13.30 | `#FFFFFF` | 12.75 | Headings |
| `text-muted` | `#677077` | 4.51 | `#9AA7B2` (muted) | 5.19 | Secondary text |
| `link` | `#0E5FB6` (mountainBlue) | 5.63 | `#4C9EF3` | 4.55 | Links, text buttons |
| `primary` | `#0E5FB6` (mountainBlue) | 6.02 | `#2287F0` (anchorBlue) | 5.00 | Primary button fill |
| `on-primary` | `#FFFFFF` | 6.30 | `#0A1628` | 5.00 | Text on a primary button |
| `accent` | `#2287F0` (anchorBlue) | 3.24 | `#2287F0` | 3.52 | Reference class, allowed edges, meters, focus ring, large display type |
| `line` | `#D7DEE7` (shadow) | | `#22406E` | | Dividers only, never the sole edge of a control |
| `line-strong` | `#818C96` | 3.06 | `#677DA0` | 3.05 | Input borders, class-tile strokes |
| `line-brand` | `#9DCAEE` (baseBlue) | | `#0E5FB6` | | Module container stroke, chips, meter track |
| `conform` | `#2E9E5B` | 3.04 | `#2E9E5B` | 3.74 | Graphic |
| `conform-text` | `#257E49` | 4.51 | `#4BAC72` | 4.52 | Text |
| `divergence` | `#D64545` | 3.91 | `#D74B4B` | 3.03 | Graphic |
| `divergence-text` | `#C53F3F` | 4.51 | `#E27D7D` | 4.53 | Text |
| `absence` | `#B87F2F` | 3.07 | `#E9A13B` | 5.84 | Graphic |
| `absence-text` | `#936525` | 4.54 | `#E9A13B` | 5.84 | Text |

Facts behind the table, which the design must respect:

- **anchorBlue is 3.63:1 on white.** That is enough for graphics, large type (≥ 24 px, or
  ≥ 18.66 px bold) and focus rings, but not for body text or a button label. Light-theme links and
  primary buttons therefore use mountainBlue (6.30:1). White text on an anchorBlue button would be
  3.63:1.
- **muted `#9AA7B2` is 2.46:1 on white.** It cannot carry text or a control's outline in the light
  theme, hence `text-muted` `#677077` and `line-strong` `#818C96`. On the dark page it passes as
  text (7.38:1).
- **The deck's absence amber is 2.18:1 on white.** A dotted amber line would vanish, so light uses
  `#B87F2F`.
- In the dark theme deepNavy becomes the surface, and the page goes one step darker. The brand
  stays recognisable without a black UI.
- Shadows: real shadows in light (e.g. `0 1px 2px` and `0 8px 24px`, deepNavy at 6-10 %). In dark,
  depth comes from the `raised` surface plus a 1 px `line` border; an optional anchorBlue glow at
  ~25 % marks focus or "live" states.

---

## 4. The current UI/UX

Stack: React 18.3, react-router-dom 7, antd 5.24 (no `ConfigProvider`, so the default light
theme), @xyflow/react 12.4, TanStack Query 5, axios, jsPDF, FontAwesome 6 (solid).
- Query defaults: no refetch on focus, one retry, 5-minute stale time.
- Paths below are relative to `web/src/`.

### 4.1 Global

- `index.html`: title **"Arch Anchor"** on every page, Vite's default favicon. There is no logo
  anywhere in the app.
- `index.css` is the Vite template:
  - `#242424` background with `rgba(255,255,255,.87)` text, flipping to `#fff` / `#213547` under a
    light OS scheme;
  - links `#646cff`;
  - a global `button` rule (`#1a1a1a`, radius 8) that leaks into every raw `<button>`;
  - `h1` at 3.2em.
- `App.css`: `#root { text-align: center }` (everything inherits centred text), unused template
  rules, and `.highlight` (merge target: `2px solid #ff0072`, pink glow).
- `xy-theme.css`: nodes `background: black; color: white`, border `#EDEDED`, radius 8, selected
  border `#F57DBD`, hover `#C5C5C5`. A dark variant is defined but never active.

### 4.2 Routes and navigation

- `/` is the Projects list. `/analyze/:projectName` is the Analyze page.
- There is no 404 route, no layout shell and no homepage.
- The header exists only on Analyze and has **no way back** to Projects except the browser's back
  button.

### 4.3 Projects (`screens/Projects`)

**Content**
- `<h1>`**"Projects"** and a primary button **"Add Project"**.
- An antd table with two columns:
  - **"Project Name"**;
  - **"Actions"**: primary **"Analyze"** and danger **"Delete"**.
- A project is only `{ name }`: no source, date or commit.

**Feedback**
- While loading, a large spinner replaces the table. Load error toast: **"Failed to fetch
  projects"**.
- Delete opens a Popconfirm **"Are you sure you want to delete {name}?"** with **"Yes"** / **"No"**.
  - Success: **`Project "{name}" deleted successfully!`**. Error: **"Failed to delete project"**.
  - The whole table becomes a spinner while it deletes.
- Empty state: antd's default "No data".

**Look:** a white antd table on the dark `#242424` page.

### 4.4 Add Project (`components/AddProjectModal`)

Modal titled **"Add Project"** with two tabs. The default is the first.

**"GitHub repository"**
- **"Repository URL"**: required, message **"Enter the repository URL"**, placeholder
  **"https://github.com/owner/repo"**.
- An **"Options"** collapse:
  - **"Branch or tag"**: placeholder *"The repository's default branch"*.
  - **"Subdirectory"**: placeholder *"backend"*; help *"The folder that holds src/, when it is
    not the repository root."*
  - **"Project name"**: placeholder *"The repository name"*.
  - **"Access token"**: password field, placeholder *"github_pat_…"*; help *"Only for private
    repositories. Used once to clone, never stored."*
- **"Import"** button.
- Success: **`Imported {name} at {sha7}`**.
- Error: the API's problem `detail`, or **"Could not import the repository. Check that the API is
  running and try again."**
- The import runs inside the request, with no progress events, so a large repository simply keeps
  the button spinning.

**"Local directory"**
- A raw, unstyled native `<input type="file" webkitdirectory>` and an **"Upload"** button.
- Messages:
  - nothing chosen: **"Please select a directory to upload"**;
  - success: **"Directory uploaded successfully!"**;
  - errors: **"Failed to upload directory"** or **"Error during upload"**.

### 4.5 Analyze (`screens/Analyze`)

**Layout**
- Header on top.
- Canvas below it, `calc(100vh - 60px)`, with a 300 px left margin when the module panel is open
  and an **always-on** 350 px right margin.
- Left module panel, fixed 300 px. Right recommendations panel, fixed 350 px.
- The header is about 51 px tall, but every calculation assumes 60.
- Collapsing the right panel never gives its space back.

**Load**
- Opening the page runs a fresh analysis (`POST /api/analyze`) every time.
- While it runs, the canvas is replaced by a bare **"Loading..."** paragraph.
- On failure the user sees nothing; the error only reaches the console.

**Canvas**
- ReactFlow with dot background (gap 100, size 3), the default Controls at the bottom left, and
  fit-view on load.
- No minimap, legend or search.
- The backend sends no edges. Users can drag edges between handles, but they are kept only in the
  browser and mean nothing.

**Module node**
- Content: module name (bold), **"Ref Class:"** + the *first* reference class only, and
  **"Similarity:"** `xx.xx%`.
- Look: every node is an identical black box, whatever its similarity, violations or mode.
- Resizable from a bottom-right handle (minimum 100 × 50).

**Layout algorithm**
- The first module goes at the window centre.
- Then concentric rings, radius 200·k, holding ⌊2πr / 150⌋ nodes each.
- After a reload, nodes keep their positions; new ones go on a radius-300 circle and may overlap.

**Merge by dragging**
- Dropping a node onto another highlights the target pink.
- A floating popup then asks **"Merge {source} into {target}?"** with **"Cancel"** / **"Confirm"**.
  It is light grey (`#f5f5f5`, 180 px wide), at the top right of the canvas.
- Dragging is the only way to merge by hand.

### 4.6 Header (`components/Header`, Analyze only)

- A black bar.
- Left: `faBars` toggles the module panel and rotates 90° when it is open.
- Centre: **"Analyzed Modules for Project: {projectName}"** (18 px bold, not truncated).
- Right: `faEllipsisVertical` opens a black menu with **"Export ACs"** and **"Settings"**.
- The icons are clickable SVGs: not buttons, no labels, no keyboard access.

### 4.7 Module panel (`components/Sidebar`, left)

**List view**
- **"Modules"** heading, then one card per module (name, ellipsised).
- No count, search, sort or empty state.

**Card menu (⋮)**
- **"Rename"**: an inline input. Enter or ✓ saves; Escape or ✕ cancels. There is no success
  feedback.
- **"Delete"**: a custom dark modal **"Delete Module"**, *"Are you sure you want to delete this
  module? This action cannot be undone."*, with **"Cancel"** / **"Delete"**. It has no Escape key,
  focus trap or dialog role.

**Detail view** (click a card)
- Back chevron + module name, then three collapsible sections.
- **"Reference Classes"** (open by default)
  - Each row: the name in green, its similarity, and a **MUST** (orange) / **ALLOW** (green)
    pill that toggles the mode.
  - Tooltips: *"MUST mode: peer classes must include all these dependencies. Click to switch to
    ALLOW."* / *"ALLOW mode: peer classes may use any subset of these dependencies. Click to switch
    to MUST."*
- **"Clazzes"** (open)
  - Each row: name + similarity.
  - **Click toggles multi-selection**, with no checkbox, count or way to clear it.
  - **Right-click** opens a menu with **"Set as Ref Classes"** and **"Split"**. These two actions
    exist *only* behind a right-click.
- **"Dependencies"** (closed)
  - Package names, or **"No dependencies"**.
- Footer: **"Module Similarity:"** `xx.xx%`.

Rename, delete, split, set reference classes and toggling the mode all report errors only to the
console.

### 4.8 Recommendations panel (`components/RecommendationsSidebar`, right)

**Structure**
- Black, 350 px, collapsible through a 40 × 80 px tab on its left edge.
- Header: **"Recommendations"** with a count badge (the backend's total).
- Tabs:
  - **"Refactorings {n}"** holds split, merge and move.
  - **"Violations {n}"**.
- The Refactorings tab opens with three boxes, **High / Medium / Low**: counts in the priority
  colour, computed in the browser.
- Cards are sorted by rate, highest first, 50 at a time behind **"Show {k} more of {left}"**
  (added in #49 after 1,593 cards froze the page for 40 s).
- Empty states: **"No refactorings recommended"**, **"No violations"**.
- Load error: **"Error"** / **"Failed to load recommendations"**.

**Priority**
- rate > 0.5 is **High** (`#ff4d4f`); > 0.2 is **Medium** (`#faad14`); otherwise **Low**
  (`#52c41a`).
- Violations have no rate and get a **"Violation"** tag (`#fa8c16`).

**Card**
- Icon in the priority colour, title, description and priority tag.
- A **"View Details"** collapse and an **"Apply Recommendation"** button.
- Apply has no confirmation, no undo and no preview.

| Type | Title | Description | Details |
|---|---|---|---|
| Split | `Split "{module}"` | `Split into 2 modules with {rate} improvement` | Original Similarity · After Split (Avg) · Similarity Improvement · Violations a → b · Module 1 Classes · Module 2 Classes |
| Merge | `Merge "{m1}" and "{m2}"` | `Combine modules with {rate} improvement` | Module 1 Similarity · Module 2 Similarity · Merged Similarity · Similarity Improvement · Violations (a + b) → c |
| Move | `Move "{class}"` | `Move class with {rate} improvement` | From Module · To Module · Similarity Improvement · Source Violations a → b · Target Violations a → b |
| Violation | `Fix violation in "{class}"` | `Resolve {package} dependency violation` | Source Module · Violation · Violating Dependencies (count) · Suggested Target · Similarity Improvement · New Violations |

Formatting
- Descriptions show the rate as a decimal (`0.56`).
- Details show signed percentages with one decimal, a typographic minus (−), and no sign when the
  value rounds to 0.

**Apply** reloads the saved modules (node positions are kept) and refreshes the list.
- Success toasts: **"Split module recommendation applied successfully"** (likewise merge and move).
- Error toasts: **`Failed to apply split: {message}`**.
- A violation is applied as a move to its suggested target. Without a suggestion, Apply does
  nothing and says nothing.

### 4.9 Settings (`components/SettingsModal`)

Modal **"Settings"** with two tabs, each with its own **"Cancel"** / **"Save"**.
- Saving reloads the modules and closes the modal.
- The second tab is labelled **"Recommendations"** but holds the analysis settings.

**"Rating weights"**: two sliders, 0-1 in steps of 0.05.
- Marks: **"Violations"** at 0, **"Equal"** at 0.5, **"Similarity"** at 1.
- The handle sets the similarity weight; violations get the rest, so the pair always sums to 1.
- Readout: **"Similarity 0.60 · Violations 0.40"**.
- **"Election"**: *"Which class represents each module. Saving a change re-elects every module
  except those whose reference classes you chose by hand."*
- **"Recommendation rate"**: *"Which refactoring is worth making. Saving a change re-rates the
  split, merge and move recommendations."*
- Saved: **"Weights saved: modules re-elected and recommendations re-rated"**.

**Analysis settings** (the defaults filter nothing). Saved: **"Settings saved: recommendations
refreshed"**.

| Label | Input | Explanation |
|---|---|---|
| Fewest classes in each half of a split | integer ≥ 1 | A split with a smaller half is not recommended. A one-class half scores similarity 1 by definition. |
| Fewest classes a move leaves in its module | integer ≥ 1 | A move that would leave its module with fewer classes is not recommended. |
| Move targets per class | integer ≥ 1, or **No limit** | Only each class's best-rated targets are recommended. |
| Fewest package dependencies of a judged class | integer ≥ 0 | A class with fewer gets no move and no violation entries. Its module still counts its violations. |
| Lowest rate of a shown split, merge or move | 0-1, step 0.05 | Violations have no rate and are always shown. |
| Lowest similarity change of a recommended merge | −1-1, step 0.05, or **No limit** | 0 keeps out merges that lower similarity, recommended only for the violations they remove. |

### 4.10 Export ACs (`utils/exportACs.ts`)

- Header ⋮ → **"Export ACs"** downloads `architectural-constraints-{project}.pdf`.
- It is built from the modules currently on screen, so it reflects applied changes.
- Content:
  - title **`Architectural Constraints for '{project}'`**;
  - one line per reference-class dependency: **`AC{n} - {module} {CAN-DEPEND|MUST-DEPEND}
    {package}`**.
- jsPDF defaults: A4, Helvetica, no grouping, long lines can run off the page.
- With no modules it silently does nothing. There is no success message.

### 4.11 Feedback, theme, responsiveness, accessibility

**Feedback**
- Three confirm styles: antd Popconfirm, a custom dark modal, a custom light popup.
- Four loading styles: antd Spin, bare "Loading...", button spinners, nothing.
- Many operations fail silently (listed above).

**Theme**
- No theme system.
- Custom panels are hard-coded black, antd components are default white, and the canvas follows
  the OS. The result is three visual languages on one screen.

**Responsiveness:** none. Fixed 300 / 350 / 180 px widths, no layout below desktop.

**Accessibility**
- Some is in place: `aria-label` on the module panel, `aria-expanded` section buttons, labelled
  sliders and inputs, priority boxes with `aria-label`.
- Header icons, menus, cards and the panel tab are clickable `div`s or `svg`s.
- Split and set-reference need a right-click; merge needs a mouse drag.
- Priority is shown mainly by colour.

### 4.12 What each screen can get from the API

| Screen | Call | Gives |
|---|---|---|
| Projects | `GET /api/projects`, `DELETE /api/projects/{name}` | `{ projects: [{ name }] }` |
| Import | `POST /api/projects/import` | body `{ repositoryUrl, ref?, subdirectory?, projectName?, accessToken? }` → `{ name, repositoryUrl, commitSha }` |
| Upload | `POST /api/upload` | multipart folder |
| Analyze | `POST /api/analyze?projectName=` | `{ projectName, modulesList, projectSimilarity, architecturalConstraints }` |
| Analyze (after a change) | `GET /api/module` | the saved modules |
| Module panel | `POST /api/module/merge`, `/rename`, `/split`, `/ref-clazzes`, `/ref-clazz-mode`, `DELETE /api/module/delete` | the changed module(s) |
| Recommendations | `GET /api/recommendations`, `POST /api/recommendations/apply/{split,merge,move}` | `{ splits, merges, moves, violations, summary }` |
| Settings | `GET`/`PUT /api/projects/{name}/rating-weights`, `…/analysis-settings` | the two weight pairs; the six settings |

**Data the API already returns that the UI never shows.** The redesign may surface any of it
without backend work:
- **Project:** `projectSimilarity` (93.75 % for the bookstore) and the project's constraints.
- **Module:**
  - every reference class, not just the first;
  - `refClazzesDependencies` (the contract itself), and `allDependencies`. A module's violations
    are the packages in the second that are not in the first;
  - `refClazzesChosenByHand`;
  - `avgRefClazzesSimilarity`.
- **Class:** its dependencies with their types, `firstModule` vs `currentModule` (where a class
  started), and `avgSimilarityWithRefClazzes`.
- **Recommendations:**
  - split: `superRefClassName` / `deRefClassName` (the reference class of each half);
  - `violationsImprovement` on split, merge and move;
  - move: `classCurrentSimilarity` / `classTargetSimilarity`;
  - violation: the `violatingDependencies` list itself (five entries for `SalesReport`; only the
    count is shown), `bestSuggestion.reason`
    (e.g. *"Perfect architectural fit, improved similarity and no new violations"*),
    `requiresNewModule`, and every alternative suggestion.
- **An unused endpoint:** `POST /api/module/move-class` (manual move) exists but no UI calls it.

### 4.13 Pain points, in one place

1. Three visual languages and duplicated colours. For example, "danger" is `#ff4d4d` / `#ff4d4f`
   and "green" is three different greens.
2. Inline style objects everywhere, no tokens, no spacing scale.
3. The graph encodes nothing: identical black boxes, meaningless edges.
4. Actions hidden behind a right-click, a drag or a ⋮ menu; invisible selection state.
5. Silent failures, no designed loading or error screens, and Apply with no confirmation or
   preview.
6. No branding, no homepage, no way back from Analyze, a static page title.
7. Fragile layout arithmetic (60 / 300 / 350 px) and nothing below desktop width.

---

## 5. Feature checklist

Everything here exists today and must survive. Artboard ids refer to [section 7](#7-artboards).

| Feature | Today | Artboard |
|---|---|---|
| List projects | Projects table | P1 |
| Analyze a project | "Analyze" button | P1, H1 (recent projects) |
| Delete a project, with confirmation | Popconfirm | P3 |
| Import from GitHub: URL, branch/tag, subdirectory, project name, access token | Add Project modal | H1, H2 |
| Upload a local folder | Add Project modal | H1 (drop zone), H2 |
| Import / upload success and error messages | toasts | H3, H4, X1 |
| Run the analysis on opening a project | `POST /api/analyze` | A1, A2 |
| Module graph: nodes with name, reference class(es), similarity | canvas | A3 |
| Drag, resize, zoom, pan, fit view, lock | canvas controls | A3 |
| Merge two modules by dragging, with a highlight and a confirmation | canvas | A5 |
| Toggle the module panel | header ☰ | A3 |
| Module list | left panel | A3 |
| Rename a module (Enter / Escape) | card ⋮ | M2 |
| Delete a module, with confirmation | card ⋮ | M2, X2 |
| Module detail: reference classes, similarity, MUST / ALLOW toggle with explanation | detail view | M1 |
| Classes with similarity; multi-select; Set as reference classes; Split | detail view | M1, A6 |
| Module dependencies, with an empty state | detail view | M1 |
| Module similarity | detail view | M1 |
| Recommendations: total, Refactorings / Violations tabs with counts | right panel | R1 |
| High / Medium / Low summary | right panel | R1 |
| Cards sorted by rate, 50 per page with "Show more" | right panel | R1 |
| Card: type, priority, title, description, details, Apply with loading | card | R2 |
| Recommendations loading, error and empty states | right panel | R3 |
| Collapse the recommendations panel | panel tab | A3 |
| Export ACs as PDF | header ⋮ | E1 |
| Settings: election and recommendation-rate sliders with live readout | Settings modal | S1 |
| Settings: the six analysis settings, two with "No limit" | Settings modal | S2 |
| Saving settings re-elects / re-rates and refreshes | Settings modal | S1, S2, X1 |

---

## 6. Direction

The brief: modern, technological, alive, interactive. Concretely:

### 6.1 Principles

1. **The graph is the product.**
   - On Analyze the canvas is full-bleed, and the panels float over it as collapsible surfaces
     instead of shoving it aside.
   - A module node is drawn as the vocabulary's module container: name tab, class tiles inside,
     the reference class as the anchorBlue tile wearing the mark, a similarity meter, a violation
     count in divergence.
   - Large modules collapse their tiles into a count.
2. **Module dependencies become edges.**
   - A module's dependency on a package of another module is drawn as an arrow between them:
     allowed (blue, solid) or divergence (red, dashed).
   - The data is there: each module's `allDependencies` against its `refClazzesDependencies`, and
     package names map to modules (`bookstore.service` is `bookstore/service`).
   - The user-drawn edges of today go away.
3. **Motion carries meaning.** Animate what the tool actually does:
   - an election (tiles rate, one becomes the anchor);
   - a move (the class tile travels to its target);
   - a split (the container divides);
   - a merge (two containers fuse).

   Everything stills under `prefers-reduced-motion`. No decorative motion inside the working
   screens.
4. **Every action is discoverable.**
   - Selecting classes opens a visible toolbar: *"2 selected · Set as reference · Split · Clear"*.
   - Merge has a keyboard and menu path ("Merge into…") besides dragging.
   - Hovering or focusing a recommendation card **highlights the modules and classes it touches**
     on the graph.
5. **One pattern per job:** one confirm dialog, one toast, one inline error, one empty state, one
   loading treatment drawn in the vocabulary. Nothing fails silently.
6. **Keyboard and command palette.** ⌘K / Ctrl K jumps to a module or class and runs any action
   (Analyze, Export ACs, Settings, theme). This is the "technological" feel, earned rather than
   painted on.

### 6.2 Homepage (new)

**Header** (every page)
- The lockup on the left, linking to `/`.
- Navigation: **Projects**, **How it works**, **Research** (the ECSA 2026 paper and the Zenodo
  artifact), and a **GitHub** link.
- On the right: the System / Light / Dark switch and the ⌘K trigger.
- Below 768 px the navigation folds into a menu.

**Hero:** a centred **import bar** that does the job of today's Add Project modal.
- It recognises a GitHub URL as it is typed or pasted, and shows the repository as a chip
  (`owner/repo`).
- It has an **Options** disclosure with the four real fields: branch or tag, subdirectory, project
  name, access token. Keep the help texts from 4.4.
- It has an **Upload folder** action. A folder can also be **dropped anywhere on the page**, which
  becomes a full-page drop zone while dragging.
- Its primary action is **Analyze**: import, then open the Analyze page.
- Under the bar, a chip **"Try it on the bookstore example"** prefills
  `https://github.com/erick-escape/arch-anchor`, subdirectory `examples/bookstore`, name
  `bookstore`. One click gives reviewers and first-time users a real result.
- Progress is honest. The import has no progress events, so show an indeterminate animation in the
  vocabulary with the two real stages, *Importing* then *Analyzing*. Do not show a fake percentage.

**Label above the bar.** It must say *Java*, because the parser reads nothing else. Candidates, to
compare on the canvas:
- "Find the architecture already in your Java code."
- "Which class anchors each of your modules? Paste a Java repository to find out."
- "Point ArchAnchor at a Java project to recover the rules its architecture follows."
- "Recover the architecture your Java code already has." (from the README's opening line)

**Claims carousel.** Each slide is one claim plus a 3-second looping vignette drawn in the
vocabulary. The claims come from the ECSA talk, so each is defended by the paper:

| Claim | Vignette |
|---|---|
| The architecture is already in your code. | Scattered class tiles settle into module containers |
| One class per module already carries its rules. | Tiles rate on meters; one turns into the reference-class tile |
| No architectural model required. | A repository chip turns into a module graph with no diagram in between |
| The rules live in the code. | The reference class's package chips become `CAN-DEPEND` lines |
| A start for the plan you never wrote. | The constraints list assembles into an exported page |

- It advances on its own, pauses on hover and focus, has dots and arrow keys, and shows the first
  slide statically under reduced motion.
- Do not claim "regenerated on every commit": the tool has no CI integration.

**Further down the page**
- **How it works** in three steps, drawn in the vocabulary: *Import* → *Elect the anchors* →
  *Act on violations and refactorings*.
- **Recent projects** (when there are any) as cards that open Analyze.
- A **footer** with the paper citation, UFLA / unibz, the licence and the repository.

**Background:** a quiet, slowly drifting field of class tiles and dependency lines behind the
hero. A few tiles glow as anchors. It is the one place decoration is allowed, and it stops under
reduced motion.

### 6.3 Projects

- Cards or a table, whichever reads better at a glance. Each shows the name and, when known, the
  repository chip.
- **Analyze** is the primary action. **Delete** sits behind a confirmation that names the project.
- Empty state: the vocabulary's empty module container, with a link to the import bar.
- The import bar (or the same component in a dialog) replaces "Add Project".

### 6.4 Analyze

**Header**
- A breadcrumb *Projects › bookstore* (the way back) and a heading that states the claim, e.g.
  **"bookstore · 4 modules · 1 violation"**.
- A stat strip: project similarity, modules, violations.
- **Export ACs**, **Settings**, the theme switch, ⌘K.

**Left panel: modules**
- A searchable list with counts.
- Detail view:
  - reference classes as tiles with a **MUST / ALLOW segmented control**. The explanation sits
    visibly under the control, not in a tooltip.
  - classes with similarity meters and checkboxes, plus the selection toolbar;
  - dependencies as package chips, each marked allowed or divergence against the contract;
  - module similarity as a stat.

**Canvas**
- Module containers (6.1), edges (6.1), a minimap, a legend of the vocabulary, and zoom controls.
- A visible merge-target state while dragging, and an anchored merge confirmation with Confirm,
  Cancel, loading and error states.

**Loading and failure**
- Loading: the election vignette on an empty canvas.
- Failed: a designed error with Retry.

### 6.5 Recommendations

- Tabs for Refactorings and Violations, with counts. **The badge and the tab counts must come from
  one source.**
- **Priority without the semantic trio.** Use brand intensity plus a word:
  - High: a solid anchorBlue badge;
  - Medium: an outlined badge;
  - Low: a muted badge.

  Pair it with the rate as a meter and a number (`0.63`). Show the rate the same way in the
  description and the details, not as a decimal in one and percentages in the other.
- Each type gets its own small vocabulary drawing:
  - split: a container dividing;
  - merge: two containers joining;
  - move: a tile and an arrow;
  - violation: a red dashed arrow to a package chip.
- Details show before → after pairs as meters, not label soup.
- **Apply** behaviour:
  - hover or focus previews the change on the graph;
  - Apply asks for a short confirmation that names the change;
  - it shows a loading state, then the change animates in;
  - a toast offers to show the result.
- A violation without a usable suggestion shows *why* Apply is unavailable (`requiresNewModule`,
  no suggestion) instead of a button that does nothing.

### 6.6 Settings

- The same two tabs. Rename the second to **"Analysis filters"** (or similar), so it no longer
  says "Recommendations".
- **Weights:** keep the slider semantics (0-1, step 0.05, the pair sums to 1, the readout).
  Visualise the split as two meters.
- **Filters:** keep the labels and explanations from 4.9, and the **No limit** checkboxes.

### 6.7 Optional opportunities (no backend change)

Each of these may appear in the design, clearly marked optional:
- Show every reference class of a module, and the module's contract, as package chips.
- Show where a class started (`firstModule`) after moves.
- List a violation's offending types and its alternative suggestions, with `bestSuggestion.reason`.
- Manual **Move class to…** through the existing `POST /api/module/move-class`.
- Export ACs with a preview of the constraints before downloading, grouped by module.

---

## 7. Artboards

Make every artboard in both **light and dark**, at **1440 px** wide. Make the homepage also at
**390 px**. Use the bookstore's real data (section 7.1) so the design matches what the tool shows.

| Id | Screen | States |
|---|---|---|
| H1 | Home | idle, carousel visible, recent projects |
| H2 | Home | URL recognised (repo chip), Options open |
| H3 | Home | importing → analyzing |
| H4 | Home | import error (API `detail` text), folder dragged over the page |
| P1 | Projects | list |
| P2 | Projects | empty |
| P3 | Projects | delete confirmation |
| A1 | Analyze | loading (election vignette) |
| A2 | Analyze | analysis failed, Retry |
| A3 | Analyze | overview: both panels, graph with edges, minimap, legend |
| A4 | Analyze | hovering "Move SalesReport": source and target highlighted |
| A5 | Analyze | merge drag: target highlighted, then the confirmation |
| A6 | Analyze | two classes selected: selection toolbar |
| M1 | Module detail | `bookstore/web`: reference class with MUST / ALLOW, classes with meters, dependencies as chips with `java.sql` marked divergence |
| M2 | Module list | rename in place, card menu |
| R1 | Recommendations | Refactorings tab with summary and three cards; Violations tab |
| R2 | Recommendation cards | each of the four types, collapsed and expanded, Apply loading |
| R3 | Recommendations | loading, error, both empty states, a violation with no usable suggestion |
| S1 | Settings | weights |
| S2 | Settings | analysis filters, one "No limit" checked |
| E1 | Export ACs | menu action and its confirmation (optionally the preview from 6.7) |
| X1 | Feedback | success and error toasts, inline error |
| X2 | Confirm dialog | the one shared pattern, destructive variant |
| T | Theme switch | System / Light / Dark |

Show the homepage label candidates (6.2) side by side on H1.

### 7.1 Sample data: the bookstore

From `examples/bookstore/README.md`, recorded from a real run.

**Modules**

| Module | Reference class | Classes (similarity) | Similarity | Violations |
|---|---|---|---|---|
| `bookstore/web` | `CatalogController` | CatalogController, CustomerController, OrderController (83.33 %), SalesReport (50.00 %) | 75.00 % | 1: `java.sql` |
| `bookstore/service` | `CatalogService` | CatalogService, CustomerService, OrderService | 100.00 % | 0 |
| `bookstore/persistence` | `BookRepository` | BookRepository, CustomerRepository, OrderRepository | 100.00 % | 0 |
| `bookstore/model` | `Book` | Book, Customer, Order | 100.00 % | 0 |

**Project and module dependencies**
- Project similarity: 93.75 %.
- `web` → `bookstore.service`, `bookstore.model`, `java.sql` (the divergence).
- `service` → `bookstore.persistence`, `bookstore.model`.
- `persistence` → `java.sql`, `bookstore.model`.
- `model` → `java.time`.

**Recommendations**

| Card | Rate | Priority | Details |
|---|---|---|---|
| Split "bookstore/web" | 0.625 | High | 75.00 % → 100.00 %, violations 1 → 0, halves of 3 and 1 classes |
| Move "SalesReport" | 0.5625 | High | web → persistence, +12.5 %, source violations 1 → 0, target 0 → 0 |
| Merge "bookstore/persistence" and "bookstore/web" | 0.4196 | Medium | −16.1 %, violations 1 → 0 |

**Violation**
- Fix violation in "SalesReport": `java.sql` in `bookstore/web`.
- Types: Connection, DriverManager, ResultSet, SQLException, Statement.
- Suggested target `bookstore/persistence`, +25.0 %, 0 new violations.

**Constraints** (Export ACs)

```
AC1 - bookstore/model CAN-DEPEND java.time
AC2 - bookstore/persistence CAN-DEPEND bookstore.model
AC3 - bookstore/persistence CAN-DEPEND java.sql
AC4 - bookstore/service CAN-DEPEND bookstore.model
AC5 - bookstore/service CAN-DEPEND bookstore.persistence
AC6 - bookstore/web CAN-DEPEND bookstore.model
AC7 - bookstore/web CAN-DEPEND bookstore.service
```

**After applying the move:** every module is at 100 %, `persistence` has 4 classes, and the panel
is empty.

---

## 8. Constraints for the implementation

- **Stack stays.** React 18 + antd 5 + @xyflow/react 12.
  - The tokens of section 3 map onto antd's `ConfigProvider` theme tokens, with
    `theme.darkAlgorithm` for dark.
  - ReactFlow takes `colorMode` from the same switch.
  - Custom components (nodes, tiles, chips, meters) read the same CSS custom properties.
- Fonts: Jost, Inter and Roboto Mono, self-hosted from the Fontsource packages so an offline run
  (a reviewer's, say) looks the same.
- WCAG 2.2 AA:
  - the contrast of section 3;
  - visible focus;
  - every action reachable by keyboard;
  - dialogs with focus trap, Escape and roles;
  - status never by colour alone.
- `prefers-reduced-motion` respected everywhere.
- The homepage is responsive down to 390 px. Analyze is desktop-first; below 1024 px its panels
  become drawers.
- Timing:
  - The redesign ships before the ICSE 2027 demo submission (23 Oct 2026), and the paper's
    figures, walkthrough and video are retaken from it.
  - Tag the commit the figures were captured at (`icse27-demo`), so the reviewers' artifact
    matches the paper.
  - Keep the Refactorings order (splits, merges and moves, stable-sorted by rate, highest
    first) and the 50-card pages: the paper's evaluation harness copies both.

## 9. Decisions

Settled on the canvas, 18 Oct 2026:

- [x] The label above the import bar: "Find the architecture already in your Java code."
- [x] The claims in the carousel: the five of 6.2.
- [x] The header: Projects · How it works · Research · GitHub, then the theme switch and ⌘K.
- [x] Projects as cards.
- [x] The second Settings tab is "Analysis filters".
- [x] Optional opportunities in: a module's contract as status chips, a violation's offending types
      and alternative targets, and the Export preview grouped by module. Out for now: manual
      "Move class to…" and a class's first module.
