---
name: UCA CFC Connect operational panels
description: Existing visual system for administration and reception workspaces.
colors:
  action-blue: "#316bff"
  action-blue-hover: "#1652df"
  sidebar-charcoal: "#24282b"
  sidebar-active: "#3a4045"
  workspace-admin: "#fafbfe"
  workspace-reception: "#f8fafe"
  surface: "#ffffff"
  ink-admin: "#29313a"
  ink-reception: "#263340"
  muted-admin: "#687380"
  muted-reception: "#627182"
  line-admin: "#e4e8ee"
  line-reception: "#dfe6ef"
  booking-fill: "#91d6ec"
  booking-border: "#55b5d7"
typography:
  admin-page-title:
    fontFamily: "Poppins, 'Segoe UI', sans-serif"
    fontSize: "clamp(1.9rem, 3vw, 2.45rem)"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "-.035em"
  reception-page-title:
    fontFamily: "Poppins, 'Segoe UI', sans-serif"
    fontSize: "clamp(1.85rem, 3vw, 2.45rem)"
    lineHeight: 1.2
    letterSpacing: "-.035em"
  body:
    fontFamily: "Poppins, 'Segoe UI', sans-serif"
    fontWeight: 400
  field-label:
    fontFamily: "Poppins, 'Segoe UI', sans-serif"
    fontSize: ".72rem"
    fontWeight: 500
rounded:
  control: "7px"
  admin-panel: "9px"
  reception-panel: "12px"
  pill: "999px"
components:
  primary-button:
    backgroundColor: "{colors.action-blue}"
    textColor: "{colors.surface}"
    rounded: "{rounded.control}"
    padding: "8px 16px"
  primary-button-hover:
    backgroundColor: "{colors.action-blue-hover}"
  sidebar-link-active:
    backgroundColor: "{colors.sidebar-active}"
    textColor: "{colors.surface}"
    rounded: "{rounded.control}"
---

# Design System: UCA CFC Connect Operational Panels

## Overview

**Creative North Star: "The Daily Operations Desk"**

This documents the implemented admin and receptionist workspaces. Both use a dark navigation rail, a compact white top bar, pale work area, restrained white panels, and blue for primary actions and focus. The receptionist screen extends that language with an agenda as its first and most prominent tool. The public landing and client pages have separate styles and are outside this document's scope.

**Key Characteristics:**

- Dense, readable operational information in a calm light workspace.
- Persistent dark navigation on wide screens; a dismissible drawer on narrow screens.
- Blue actions and focus, with status colors reserved for meaning.

## Colors

The shared action color is `action-blue`; both panels use `sidebar-charcoal` and white surfaces. Admin and reception keep their slightly different workspace, text, muted, and line values as recorded in the frontmatter. Reception uses `booking-fill` and `booking-border` to distinguish occupied agenda blocks from the pale open slots. Success and error feedback use green and red text in the existing CSS; they are semantic states, not general accents.

**The Operational Blue Rule.** Use blue for primary actions, focus, and selected informational treatments; keep the sidebar charcoal and the content surfaces light.

## Typography

Local Poppins files supply regular, medium, and semibold weights, with Segoe UI and sans-serif fallbacks. Page titles are semibold with tight tracking. Section headings, field labels, table labels, and supporting text step down in size; muted copy separates guidance from data. The panels use small text for information density, so preserve the clear weight and color hierarchy rather than adding decorative type.

## Layout

Both workspaces are centered grid shells with a fixed-width sidebar (admin 220px; reception 230px) and a flexible main column. The top bar is at least 68px tall. Main content uses 34px top padding and fluid horizontal padding near 20–44px. Admin lists use wide, scrollable tables and dialogs; reception uses a scrollable agenda, stacked list cards, and two-column forms. The reception agenda precedes its reservation forms in the implemented page.

Admin navigation becomes a drawer below 950px; reception does so below 1050px. Reception's two-column cards stack below 760px, and its forms become single-column below 540px. Admin forms and list presentation also compress at their documented CSS breakpoints. Keep horizontal scrolling within dense tables and the agenda instead of squeezing their content unreadably.

## Elevation & Depth

Thin borders and small background shifts define ordinary panels and rows. A broad, soft shadow separates each centered shell from the page background. Stronger shadows belong to admin dialogs and open mobile navigation; ordinary cards remain flat.

## Shapes

Controls and navigation links use gently rounded corners (`control`). Admin list panels are slightly rounder (`admin-panel`), while reception cards use the larger `reception-panel` radius. Status and role pills are fully rounded. Inputs retain a visible one-pixel border; the booking blocks use a tighter 5px corner inside the agenda grid.

## Components

### Navigation

The charcoal rail contains the UCA CFC wordmark, task links, and a quiet footer. Current and hovered links receive a lighter charcoal fill; the current link also gains white semibold text. At narrow widths a menu button reveals the rail as an overlay, with a close control. The top bar stays white with a subtle bottom divider.

### Buttons and fields

Primary actions are solid blue with white semibold text, a 7px corner, and a darker blue hover. Secondary actions are white with a fine gray border; hover moves the border toward blue. Inputs and selects are white, bordered, and roughly 40–41px high. The global `:focus-visible` treatment is a 3px blue outline with a 3px offset.

### Cards, lists, and status

Admin lists live in bordered white panels with subtle striped and hover rows. Reception forms, agenda, and lists use bordered white cards. List rows separate with fine rules, while rounded pills identify status. Green and red feedback text conveys success and error. The receptionist agenda uses pale free slots, sticky time and space headers, and cyan occupied blocks with dark text.

## Do's and Don'ts

### Do:

- **Do** continue the dark rail, light workspace, white card, and blue action pattern on new operational screens.
- **Do** place availability and its legend before reservation entry when extending the reception agenda.
- **Do** retain visible focus outlines, readable status text, and local scrolling for dense schedules or tables.

### Don't:

- **Don't** import the public landing page's brighter palette or typography into these operational panels.
- **Don't** use card shadows as the ordinary separation method; the existing panels use borders and surface contrast.
