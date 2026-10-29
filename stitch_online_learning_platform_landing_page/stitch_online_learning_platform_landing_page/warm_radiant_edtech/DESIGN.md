---
name: Warm Radiant EdTech
colors:
  surface: '#fff8f5'
  surface-dim: '#e0d8d5'
  surface-bright: '#fff8f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#faf2ee'
  surface-container: '#f4ece8'
  surface-container-high: '#eee7e3'
  surface-container-highest: '#e9e1dd'
  on-surface: '#1e1b19'
  on-surface-variant: '#5b403a'
  inverse-surface: '#33302d'
  inverse-on-surface: '#f7efeb'
  outline: '#8f7069'
  outline-variant: '#e3beb6'
  surface-tint: '#b52603'
  primary: '#b52603'
  on-primary: '#ffffff'
  primary-container: '#ff5a36'
  on-primary-container: '#5a0c00'
  inverse-primary: '#ffb4a3'
  secondary: '#a73a05'
  on-secondary: '#ffffff'
  secondary-container: '#fe7944'
  on-secondary-container: '#661f00'
  tertiary: '#954919'
  on-tertiary: '#ffffff'
  tertiary-container: '#d57b47'
  on-tertiary-container: '#4c1d00'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad2'
  primary-fixed-dim: '#ffb4a3'
  on-primary-fixed: '#3d0600'
  on-primary-fixed-variant: '#8c1900'
  secondary-fixed: '#ffdbcf'
  secondary-fixed-dim: '#ffb59a'
  on-secondary-fixed: '#380d00'
  on-secondary-fixed-variant: '#802900'
  tertiary-fixed: '#ffdbca'
  tertiary-fixed-dim: '#ffb68f'
  on-tertiary-fixed: '#331100'
  on-tertiary-fixed-variant: '#773202'
  background: '#fff8f5'
  on-background: '#1e1b19'
  surface-variant: '#e9e1dd'
typography:
  display:
    fontFamily: Plus Jakarta Sans
    fontSize: 3rem
    fontWeight: '800'
    lineHeight: 3.5rem
    letterSpacing: -0.03em
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 2.25rem
    fontWeight: '700'
    lineHeight: 2.75rem
    letterSpacing: -0.025em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 1.75rem
    fontWeight: '700'
    lineHeight: 2.25rem
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 1.5rem
    fontWeight: '700'
    lineHeight: 2rem
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 1.25rem
    fontWeight: '600'
    lineHeight: 1.75rem
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 1.125rem
    fontWeight: '600'
    lineHeight: 1.5rem
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 1.125rem
    fontWeight: '400'
    lineHeight: 1.75rem
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 0.9375rem
    fontWeight: '400'
    lineHeight: 1.5rem
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 0.8125rem
    fontWeight: '400'
    lineHeight: 1.25rem
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 0.875rem
    fontWeight: '600'
    lineHeight: 1.25rem
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 0.75rem
    fontWeight: '600'
    lineHeight: 1rem
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 0.6875rem
    fontWeight: '700'
    lineHeight: 0.875rem
    letterSpacing: 0.06em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-sm: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is tailored for an empowering, focused, and energizing modern online learning environment. The visual identity bridges academic rigor with consumer-grade delight, cultivating continuous momentum, curiosity, and pride in daily progress.

The aesthetic blends **Modern Tactile Minimalism** with **Warm Soft-Surface Architecture**:
- **Atmospheric Warmth:** Surfaces reject clinical cold grays in favor of parchment, eggshell, and creamy tinted whites (`#FDFBF9` and `#FBF8F5`), lowering cognitive fatigue during multi-hour study sessions.
- **Dynamic Energy:** Punchy coral-orange gradients (`#FF5A36` through `#FF7A45`) act as high-chroma kinetic focal points for forward motion: progress indicators, completion streaks, call-to-action triggers, and gamified badges.
- **Tactile Clarity:** Deeply considered rounded corners, hairline peach borders, and subtle diffuse lift create a modular, card-based interface that feels physical, intuitive, and welcoming.

## Colors

The palette balances energetic citrus/coral highlights against soothing warm neutrals to keep learners motivated without feeling overwhelmed.

### Functional Roles
- **Primary (`#FF5A36`) & Secondary (`#FF7A45`):** Reserved for primary interactive elements, active navigation states, filled progress bars, and critical achievements. Often deployed in a 135° directional linear gradient (`linear-gradient(135deg, #FF5A36 0%, #FF7A45 100%)`).
- **Warm Neutral Backgrounds:**
  - Base Canvas: `#FBF8F5`
  - Elevated Container / Surface Default: `#FFFFFF`
  - Subtle Inset / Nested Surface: `#FDFBF9`
  - Accent Tint Surface: `#FFF4EE` (used for active pill containers, icon tiles, and subtle badges)
- **Neutral Typography & Outlines:**
  - Ink Primary: `#1C1917` (rich warm off-black)
  - Ink Secondary / Supporting: `#78716C`
  - Ink Muted / Disabled: `#A8A29E`
  - Subtle Borders & Dividers: `#F2ECE4`
- **Feedback & Semantics:**
  - Success: `#10B981` (emerald green for completed tasks)
  - Warning: `#F59E0B` (amber for approaching deadlines)
  - Destructive: `#EF4444`

## Typography

The typography scale utilizes **Plus Jakarta Sans** across all roles to ensure geometric clarity, contemporary warmth, and crisp legibility on high-density displays.

- **Headlines:** Use tighter negative letter tracking (`-0.015em` to `-0.03em`) and heavy weights (`700`, `800`) to anchor section headers and course module titles with confidence.
- **Labels & Microcopy:** Category indicators (e.g., `DESIGN`, `DEVELOPMENT`) leverage `label-sm` or `label-md` styled in uppercase with intentional letter-spacing (`0.04em` to `0.06em`) to preserve legibility when set against muted or tinted badge backgrounds.
- **Numerical Metrics:** Streak numbers, hours remaining, and point totals employ bold/semibold weights to draw the eye directly to student achievements.

## Layout & Spacing

The layout utilizes a structured 3-tier horizontal modular dashboard structure:
1. **Left Persistent Navigation Rail:** Fixed width (`240px` on desktop) providing clear categorical anchors.
2. **Main Fluid Work area:** Dynamic multi-column responsive canvas holding learning feeds, in-progress courses, and discovery grids.
3. **Right Contextual / Profile Panel:** Dedicated persistent surface (`340px` on desktop) for personal analytics, streaks, calendar tracking, and user profile metrics.

### Breakpoint Scaling
- **Desktop (>= 1280px):** 3-column layout (Sidebar + Main Content + Metric Rail). Margins set to `2rem` (`space-xl`), grid gutters set to `1.5rem`.
- **Tablet (768px - 1279px):** Sidebar collapses into an icon-rail or top bar. Main workspace expands, right analytical panel drops below or transforms into a slide-over drawer.
- **Mobile (< 768px):** Single-column vertical stream. Canvas margins compress to `1rem`. Top navigation collapses to app-bar with drawer navigation; multi-column course grids reflow to single stacked cards or horizontal carousels.

## Elevation & Depth

This design system avoids dark or harsh shadows. Instead, it creates dimensional separation using **ambient warm diffusion** paired with **hairline tinted boundaries**:

- **Level 0 (Canvas Base):** Flat `#FBF8F5` surface with zero elevation.
- **Level 1 (Card & Module Surfaces):** Solid `#FFFFFF` fill bounded by a subtle `1px solid #F2ECE4` outline, supported by an extra-soft tinted shadow:
  - `box-shadow: 0 4px 20px -2px rgba(28, 25, 23, 0.03), 0 2px 6px -1px rgba(28, 25, 23, 0.02);`
- **Level 2 (Interactive Floating Elements / Popovers / Active Cards):** Lifted surface with enhanced warmth:
  - `box-shadow: 0 12px 32px -4px rgba(255, 90, 54, 0.08), 0 4px 12px -2px rgba(28, 25, 23, 0.04);`
- **Tonal Insets:** Inset surfaces (such as unfinished progress tracks or search bars) use `#FDFBF9` with an optional `inset 0 1px 2px rgba(28, 25, 23, 0.04)` for tactile depth.

## Shapes

The interface embraces a friendly, tactile geometry featuring high pill-roundedness for controls paired with smooth, modern curves for containers:

- **Pills (`rounded-full` / `9999px`):** Primary buttons, interactive chips, calendar active indicators, search inputs, and progress pill capsules.
- **Cards & Dashboard Panels (`rounded-xl` / `1.5rem` to `1.75rem`):** Major content modules, video thumbnails, and profile cards feature wide, inviting radii that soften complex data visualizations.
- **Small Badges & Icon Tiles (`rounded-lg` / `0.75rem`):** Subject matter icons (e.g., UI/UX compass, HTML badge) sit inside gentle squircle containers.

## Components

### Buttons
- **Primary Action:** Pill-shaped (`rounded-full`), rendered with the signature gradient `linear-gradient(135deg, #FF5A36 0%, #FF7A45 100%)`, crisp white text, and an ambient coral glow shadow on hover (`0 8px 20px rgba(255, 90, 54, 0.35)`). Includes directional chevron or upward-slanted arrows where relevant.
- **Secondary / Ghost:** Soft warm pill `#FFF4EE` with primary coral text (`#FF5A36`), shifting to a solid light border on hover.
- **Tertiary / Link:** Unframed label with inline directional circle icon (e.g., "Resume Course ->").

### Learner Progress Bars
- Dual-layer pill capsule (`height: 8px` or `10px`).
- Inactive track: `#F2ECE4` or `#F7EFE8`.
- Active fill: Solid coral or coral-to-orange gradient with rounded ends, smoothly animated across state transitions.

### Chips & Course Badges
- **Category Badge:** Compact squircle or pill container with `#FFF4EE` background, displaying uppercase `label-sm` text in `#FF5A36`.
- **Status Indicator (e.g., "New"):** Vibrant coral pill placed over video course thumbnails in top-left position.
- **Streak Pills:** Calendar date containers that transform into solid coral pills (`#FF5A36`) with white bold text when a daily goal is met.

### Cards
- **Course Cards:** White container (`#FFFFFF`) with a `1px solid #F2ECE4` border. Includes a 16:9 media preview card with `rounded-xl` corners, inline duration badges, instructor metadata, star rating icons (`#FF9D66`), and pricing tag.
- **In-Progress Cards:** Modular horizontal layout pairing a soft icon container on the left, module progress metrics, dual-layer progress bar, and immediate resume trigger.

### Inputs & Search Bars
- Pill-shaped (`rounded-full`) background in `#FDFBF9` with a subtle `#F2ECE4` border.
- Leading search magnifying glass in `#A8A29E`, trailing keyboard shortcut chip (e.g., `⌘K`) in elevated white with hairline border.

### Analytics & Metric Widgets
- Streak, goal, and ranking widgets use consistent vertical stacking: expressive emoji or icon tile top/left, bold headline metric in `headline-md`, and muted descriptor in `body-sm`.