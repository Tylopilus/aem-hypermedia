# PRD: AEM Hypermedia Component Framework

## 1. Summary

We want to define a modern, AEM-native frontend architecture for AEM as a Cloud Service that keeps the existing **AEM Sites Editor**, preserves authorability, remains compatible with Cloud Manager deployment, supports SEO and caching, and improves developer ergonomics for interactive components.

The proposed solution is an **AEM Hypermedia Component Framework** inspired by HTMX, but adapted to AEM-specific needs:

```text
AEM renders canonical HTML.
Sling Models expose typed hypermedia actions.
HTL applies generated attributes.
HTMX or an HTMX-like runtime swaps server-rendered fragments.
Custom elements handle browser-only behavior.
Service workers and realtime transports remain optional.
```

The framework should not replace AEM, HTL, Sling Models, Dispatcher caching, or the Sites Editor. It should make the modern AEM-native path easier and more consistent.

---

# 2. Context

The current AEM implementation uses:

```text
AEM as a Cloud Service
Cloud Manager deployments
AEM Sites Editor
HTL templating
Sling Models
Core Component proxy pattern
multi-tenant AEM archetype structure
clientlibs / ui.frontend builds
```

HTL works well for authorable server-rendered components, but it is limited for modern interactivity. Existing solutions such as SPA Editor are not desired. Fully headless rendering is also not the target.

The desired direction is:

```text
Keep the existing AEM Sites Editor.
Keep authorable components.
Use server-rendered HTML.
Use progressive enhancement.
Support SEO and no-JS fallbacks.
Support Dispatcher/CDN caching.
Avoid large SPA/runtime complexity.
Improve frontend and backend developer ergonomics.
```

---

# 3. Current Multi-Tenant Repository Reality

The repository is not a simple frontend monorepo. Each tenant is a **full AEM archetype-style tenant**, not just a frontend package.

Current tenant structure is conceptually:

```text
platform-tenant
  core
  ui.apps
  ui.config
  ui.frontend maybe optional
  OSGi services
  platform-level AEM configs

components-tenant
  core
  ui.apps
  ui.frontend
  shared base components
  reusable component logic
  shared frontend behavior
  e.g. pagination, accordion, table, swiper

website-tenant-a
  core
  ui.apps
  ui.frontend
  website-specific proxy components
  website-specific components
  e.g. events overview

website-tenant-b
  core
  ui.apps
  ui.frontend
  website-specific proxy components
  website-specific components
```

Some components live in the shared `components-tenant`.

Other components are site-specific and only exist in one website tenant.

Problem:

```ts
import { Pagination } from '../../../../../../../components-tenant/ui.frontend/src/...';
```

This is not acceptable long-term.

The PRD should therefore **not assume a single npm workspace across all tenants as the only answer**. A workspace may still be useful locally or in CI, but the architectural unit is a full AEM tenant.

---

# 4. Goals

## 4.1 Functional Goals

The framework should support:

```text
server-rendered fragments
HTMX-style interactions
typed action generation in Java
clean HTL integration
AEM edit-mode safety
SEO-friendly no-JS fallback
Dispatcher/CDN cache-friendly URLs
fragment caching
resourceResolver.map() for public URLs
custom element lifecycle
browser-only behavior components
multi-tenant frontend reuse
shared TypeScript types
```

## 4.2 Developer Experience Goals

The framework should make common component interactions easy:

```java
return hx()
    .get(fragment("events"))
    .targetSelf()
    .swapOuter()
    .pushUrl(true)
    .fallbackUrl(pageUrl())
    .trigger("change from:select")
    .build();
```

HTL should remain simple:

```html
<a
  href="${action.fallbackUrl}"
  data-sly-attribute="${action.attributes}">
  ${label}
</a>
```

## 4.3 Architectural Goals

The framework should preserve:

```text
AEM Sites Editor authorability
HTL as canonical markup layer
Sling Models for component state and logic
Core Component proxy pattern
Cloud Manager compatibility
Dispatcher/CDN caching
SEO and no-JS behavior
tenant boundaries
```

---

# 5. Non-Goals

This framework will not:

```text
replace HTL
replace Sling Models
replace AEM Sites Editor
use SPA Editor
require React/Vue/Svelte
make every component a Web Component
make every interaction server-driven
make WebSockets the default transport
render all HTML in Java
bypass Dispatcher/CDN caching
break no-JS behavior
force all tenants into a single npm workspace layout
```

---

# 6. Core Architectural Decision

Use the following interaction split:

```text
AEM + HTMX-style fragments:
  content state
  query state
  sorting
  filtering
  pagination where server-side pagination is acceptable
  lazy-loaded panels
  form validation
  search results

Client-side JS / custom elements:
  viewport state
  scroll state
  gestures
  swipers
  sticky/collision logic
  powerline/back-to-top positioning
  client-side SEO-preserving pagination
  animation state

Service worker:
  optional HTTP/fragment caching
  offline fallback
  asset caching

WebSocket/SSE:
  optional live updates only
```

---

# 7. Recommended Runtime Model

## 7.1 Canonical Rendering

AEM renders the canonical HTML.

```text
GET /events
→ AEM renders full page
→ HTL renders components
→ Sling Models provide state
→ browser receives usable HTML
```

## 7.2 Enhanced Interaction

HTMX-style actions request HTML fragments.

```text
User changes filter
→ GET /events/_jcr_content/root/eventslist.events.html?month=2026-06
→ AEM renders component fragment
→ browser swaps component
```

## 7.3 Browser-Only Behavior

Custom elements or lightweight JS handle local behavior.

```text
powerline visibility
back-to-top offset
swiper gestures
client-side pagination
sticky interactions
focus restoration
```

---

# 8. Component Categories

## 8.1 Server-Driven Hypermedia Components

Use HTMX-style fragment swaps.

Examples:

```text
sortable editorial table
event filter
event list fragment
accordion lazy panel
load more
search results
form validation
```

## 8.2 Client-Side Behavior Components

Use custom elements or lightweight JS.

Examples:

```text
powerline
back-to-top
swiper
sticky table header
modal behavior
client-side pagination for SEO baseline
```

## 8.3 Hybrid Components

Use both.

Examples:

```text
events list:
  AEM renders all matching events
  HTMX swaps filtered full result sets
  client-side pagination hides/shows items for JS users
  no-JS users see all events

swiper:
  AEM renders all slides
  JS turns slides into swiper
  HTMX optionally loads more slides
```

---

# 9. Hypermedia Action API

## 9.1 Java API

Introduce an immutable `HxAction` object.

```java
public final class HxAction {
    private final String fallbackUrl;
    private final Map<String, String> attributes;

    public String getFallbackUrl();
    public Map<String, String> getAttributes();
}
```

## 9.2 Builder Pattern

`HxAction` must be built through a builder.

The builder should store the method explicitly:

```java
method = HxMethod.GET
requestUrl = fragmentUrl
```

and render exactly one of:

```text
hx-get
hx-post
hx-put
hx-patch
hx-delete
```

Example:

```java
return HxAction.builder()
    .fallbackUrl(publicUrl)
    .request(HxMethod.GET, fragmentUrl)
    .target("#" + getId())
    .swap("outerHTML")
    .pushUrl(publicUrl)
    .build();
```

Convenience methods are allowed:

```java
.get(fragmentUrl)
.post(fragmentUrl)
```

but internally they must set `method + requestUrl`, not directly mutate arbitrary attributes.

---

# 10. AEM-Aware URL Handling

## 10.1 Public URLs

All browser-visible URLs must be mapped.

This includes:

```text
href
form action
hx-push-url
hx-replace-url
```

Use:

```java
resourceResolver.map(request, url)
``
`

## 10.2 HTMX Request URLs

HTMX request URLs should also be mapped where possible.

For first implementation, internal component URLs may be acceptable:

```html
hx-get="/events/_jcr_content/root/eventslist.events.html"
```

But the framework should support an abstraction for future public fragment routes:

```html
hx-get="/events.fragments.eventslist.events.html"
```

## 10.3 URL Strategy Modes

The framework should support configurable fragment URL strategies:

```text
RESOURCE_PATH:
  /events/_jcr_content/root/list.events.html

PUBLIC_ROUTE:
  /events.fragments.list.events.html

QUERY:
  /events/_jcr_content/root/list.events.html?month=2026-06

SELECTOR:
  /events/_jcr_content/root/list.events.month-2026-06.html
```

---

# 11. Fragment URL Service

Introduce a backend service responsible for fragment URLs.

Example API:

```java
fragment("events")
    .params(params)
    .toUrl();

fragment("table")
    .selector("sort-2")
    .selector("asc")
    .toUrl();
```

Responsibilities:

```text
build selector URLs
build query URLs
map URLs through resourceResolver.map()
encode query parameters
support cache-friendly state encoding
hide implementation details from component models
```

---

# 12. Request State Parsing

Provide a safe request state parser.

Example:

```java
HxRequestState state = hxRequest()
    .yearMonth("month")
    .string("location").pattern("[a-z0-9_-]{1,64}")
    .integer("page").min(1).defaultValue(1)
    .parse();
```

It should parse both:

```text
?month=2026-06&location=berlin&page=2
```

and, where configured:

```text
.events.month-2026-06.location-berlin.page-2.html
```

The parser should:

```text
validate values
normalize values
reject invalid params
avoid cache pollution
avoid unsafe query usage
```

---

# 13. Edit Mode Behavior

HTMX-style behavior should be disabled by default in AEM edit mode.

```text
wcmmode.edit:
  no hx-* attributes
  normal markup
  authoring overlays work
  child components remain editable

preview/publish:
  hx-* attributes enabled
```

The `HxAction` builder should support:

```java
.disabled(!isInteractiveMode())
```

or this should be handled automatically by the base component.

Fallback links/forms should still render.

---

# 14. HTL Integration

HTL should remain the markup layer.

Preferred pattern:

```html
<a
  href="${action.fallbackUrl}"
  data-sly-attribute="${action.attributes}">
  ${label}
</a>
```

For forms:

```html
<form
  method="get"
  action="${action.fallbackUrl}"
  data-sly-attribute="${action.attributes}">
</form>
```

Do not generate full HTML in Java.

Java should generate:

```text
state
URLs
attribute maps
typed actions
```

HTL should own:

```text
semantic markup
resource inclusion
escaping context
wcmmode-sensitive rendering
child resources
authorable structure
```

---

# 15. Base Component Model

Provide a base class for component models.

Example responsibilities:

```text
access request
access resource
access currentPage
stable component ID generation
mapped page URL helpers
fragment URL helpers
HxAction builder
edit-mode detection
target helpers
```

Example:

```java
public abstract class AbstractHypermediaComponent {

    protected HxActionBuilder hx();

    protected HxFragment fragment(String name);

    protected HxUrl pageUrl();

    protected HxTarget componentTarget();

    protected boolean isInteractiveMode();

    public abstract String getId();
}
```

---

# 16. Frontend Layer

## 16.1 Custom Elements Without Shadow DOM

Use custom elements for components with lifecycle/state.

Good candidates:

```text
pagination
accordion
tabs
swiper
events-list
powerline
back-to-top
modal
table
```

Avoid using custom elements for purely static atoms.

No Shadow DOM by default because:

```text
AEM/site CSS can style components normally
existing CSS architecture remains usable
HTMX swaps are simpler
Data Layer and analytics inspection remain easier
authoring markup remains more transparent
```

## 16.2 Lifecycle Base Class

Provide a base class:

```ts
export abstract class AemElement extends HTMLElement {
  connectedCallback(): void;
  disconnectedCallback(): void;

  protected onConnect(): void {}
  protected onDisconnect(): void {}
}
```

Custom elements auto-initialize after HTMX swaps because the browser calls `connectedCallback()` on inserted custom elements.

## 16.3 Runtime Registry

Provide a registry:

```ts
registerAemComponent('cmp-pagination', CmpPagination);
registerAemComponent('cmp-swiper', CmpSwiper);
registerAemComponent('cmp-powerline', CmpPowerline);
```

Support both custom-element and data-component modes if needed.

---

# 17. HTMX Lifecycle Bridge

The frontend package should normalize HTMX lifecycle events.

Examples:

```ts
document.body.addEventListener('htmx:afterSwap', ...);
```

should be bridged into framework events:

```text
aem:request:start
aem:fragment:swap
aem:request:error
```

This prevents all site components from depending directly on HTMX event names.

---

# 18. DOM Events for Composition

Use DOM events for cross-component communication where possible.

Example:

```ts
this.dispatchEvent(new CustomEvent('aem:pagination:change', {
  bubbles: true,
  detail: {
    page,
    pageSize
  }
}));
```

This reduces direct coupling between website-specific components and shared base components.

---

# 19. TypeScript Types

Shared frontend code must expose public TypeScript types.

Website tenants must not import from deep relative source paths.

Avoid:

```ts
import type { PaginationOptions } from '../../../../../../../components-tenant/ui.frontend/src/pagination/types';
```

Prefer:

```ts
import type { PaginationOptions } from '@myorg/aem-base-components/pagination';
```

or, if runtime JS is loaded separately:

```ts
import type { PaginationOptions } from '@myorg/aem-component-types';
```

## 19.1 Type Package Strategy

Because tenants are full AEM archetypes, not just frontend packages, use one of these strategies:

### Preferred: Published Internal Packages

Each shared frontend tenant can build and publish an internal npm package artifact.

```text
components-tenant/ui.frontend
  builds:
    @myorg/aem-base-components
    dist/**/*.js
    dist/**/*.d.ts
```

Website tenants consume it through package dependencies.

```json
{
  "dependencies": {
    "@myorg/aem-base-components": "1.4.0"
  }
}
```

### Alternative: Local File/Tarball Dependency in Reactor Build

The components tenant builds a package tarball:

```text
components-tenant/ui.frontend/dist/npm/myorg-aem-base-components-1.4.0.tgz
```

Website tenant consumes:

```json
{
  "dependencies": {
    "@myorg/aem-base-components": "file:../../components-tenant/ui.frontend/dist/npm/myorg-aem-base-components-1.4.0.tgz"
  }
}
```

This is more complex but avoids deep source imports.

### Short-Term: TypeScript Path Aliases

Acceptable only as an interim step.

```json
{
  "compilerOptions": {
    "paths": {
      "@components/*": [
        "../../components-tenant/ui.frontend/src/*"
      ]
    }
  }
}
```

This improves import readability but does not create a true dependency boundary.

---

# 20. Multi-Tenant Packaging Requirement

Because each tenant is a full AEM archetype, the solution must support:

```text
independent tenant builds
shared frontend packages
shared Java packages where needed
versioned dependencies between tenants
Cloud Manager-compatible Maven builds
no fragile relative imports
no hidden dependency on physical repo layout
```

## 20.1 Recommended Packaging Model

Use internal packages:

```text
@myorg/aem-ui-core
  TS lifecycle, event helpers, HTMX bridge

@myorg/aem-base-components
  pagination, accordion, table, swiper, powerline frontend code

@myorg/aem-component-types
  optional pure type contracts

Java bundle:
  com.myorg.aem.hypermedia
  HxAction, FragmentUrlService, AbstractHypermediaComponent
```

## 20.2 Dependency Direction

```text
website-tenant-a
  → components-tenant
  → platform-tenant

website-tenant-b
  → components-tenant
  → platform-tenant

components-tenant
  → platform-tenant

platform-tenant
  → no website tenant
```

Avoid:

```text
components-tenant → website-tenant-a
```

---

# 21. Events Component Requirement

## 21.1 Current Need

Editors create event pages with metadata:

```text
location
type
date
```

The events overview has:

```text
month filter
location filter
event cards
pagination if total result > 6
```

Month filter should only show months that have available events.

Location filter should only show available locations.

## 21.2 SEO Requirement

The current solution cares about SEO/no-JS.

Therefore, the preferred events component mode is:

```text
AEM renders all matching events in HTML.
JS pagination hides/shows items for enhanced users.
No-JS users see all matching events.
HTMX can swap filtered full result sets.
```

Do not force server-side pagination if the SEO requirement is that all items are present without JS.

## 21.3 HTMX Usage

HTMX should enhance filter changes:

```text
User changes month/location
→ HTMX requests filtered component fragment
→ server returns all matching event cards
→ browser swaps the whole events component
→ custom pagination initializes
```

No-JS fallback:

```text
filter form submits normally
full page reloads
all matching events are visible
```

## 21.4 Pagination Mode

Use client-side pagination for SEO-first lists.

Pagination component:

```text
receives pageSize
finds list by selector
shows first 6
hides rest
reinitializes after HTMX swap
does not require server page state
```

For components where SEO does not require all items in HTML, server-driven pagination may still be supported.

---

# 22. Reusable Pagination Component

The pagination component should support two modes.

## 22.1 Client Pagination Mode

Used for SEO-first lists.

```html
<cmp-pagination
  data-page-size="6"
  data-list-selector=".cmp-events-list__items">
</cmp-pagination>
```

Responsibilities:

```text
count DOM items
show/hide items
render controls
reset to page 1 when component is swapped
work after HTMX swaps
emit typed DOM events
```

## 22.2 Server Pagination Mode

Used when only current-page results are rendered.

Responsibilities:

```text
render previous/next/page links
use HxAction
swap parent component
push URL
fallback to normal links
```

The parent provides a `PaginationContext`.

---

# 23. Editorial Table Component

A sortable editorial table should support arbitrary authored columns.

## 23.1 Sorting

Do not hardcode fields like:

```text
price
name
availability
```

Use authored column index:

```text
sort=0
sort=1
sort=2
```

or selector format:

```text
.table.sort-2.asc.html
```

## 23.2 Column Metadata

Authors can configure:

```text
column label
sortable yes/no
type: text / number / date
optional hidden sort value
alignment
width
```

## 23.3 HTMX Behavior

Clicking a header:

```text
GET sorted table fragment
AEM sorts rows
AEM returns full table wrapper
HTMX swaps table
```

No-JS fallback:

```text
header is a real link
full page reloads with sorted state
```

---

# 24. Accordion Component

Adapt Core Component Accordion as a proxy component.

Use AEM’s existing authoring/container semantics.

Recommended behavior:

```text
author mode:
  render all panels
  disable lazy HTMX loading

publish:
  render shell/default panels
  lazy-load panel body by HTMX where useful
```

HTMX should load missing panel content, but a small JS controller should own:

```text
aria-expanded
hidden state
single expansion behavior
deep linking
focus
```

---

# 25. Powerline and Back-to-Top

Do not use HTMX for this.

This behavior depends on browser-only state:

```text
scroll position
viewport height
bottom sentinel visibility
powerline height
fixed element collision
```

Use:

```text
IntersectionObserver
CSS custom properties
small custom element
body class or CSS variable
```

The back-to-top button should not contain special powerline logic. It should react to shared CSS state:

```css
body.has-powerline-visible {
  --cmp-powerline-offset: var(--cmp-powerline-height, 64px);
}
```

---

# 26. Swipers

Use client-side JS/custom elements.

HTMX is useful only for optional surrounding behaviors:

```text
load more slides
change slide set by filter
lazy-load heavy slide content
replace carousel after personalization
```

Do not use HTMX for:

```text
next slide
previous slide
dragging
touch gestures
autoplay
looping
slide transforms
```

SEO baseline:

```text
AEM renders all slides as HTML.
JS enhances into swiper.
No-JS users see stacked content.
```

---

# 27. Service Worker

Service workers are optional.

Use for:

```text
static asset caching
offline fallback
safe public HTMX fragment caching
visited page caching
repeat interaction performance
```

Do not use for:

```text
main component rendering
AEM authoring behavior
private/personalized fragment caching
SEO
universal app protocol
```

Service worker must not interfere with author mode.

---

# 28. WebSocket / SSE

WebSocket or SSE should be optional.

Use for:

```text
live event capacity
live status
notifications
real-time updates
```

Do not use as the default transport for sorting/filtering/pagination.

Preferred pattern:

```text
WebSocket/SSE message says something changed.
Client triggers HTMX refresh of affected fragment.
AEM renders updated HTML.
```

Do not center the framework around a Service Worker WebSocket.

---

# 29. Caching Strategy

## 29.1 Full Page Cache

Full pages may be served by Dispatcher/CDN.

If cached, Sling Models do not execute.

## 29.2 Fragment Cache

HTMX fragment requests are separate HTTP requests.

Each can be cached independently:

```text
/events.events.html?month=2026-06
/table.table.sort-2.asc.html
/accordion.panel.item-3.html
```

## 29.3 Cacheable vs Non-Cacheable

Cacheable:

```text
public event lists
public table sorts
accordion panels
load-more article lists
```

Non-cacheable:

```text
cart
account menu
personalized prices
permission-sensitive fragments
CSRF-dependent forms
```

## 29.4 Cache-Friendly URLs

For public cacheable fragments, prefer deterministic URLs.

Selectors may be better than query params:

```text
.table.sort-2.asc.html
```

Query params are acceptable when Dispatcher/CDN rules are configured appropriately.

---

# 30. Security Requirements

The framework must:

```text
allowlist HTMX attribute names
escape all HTL output in correct context
validate request parameters
validate selector state
avoid arbitrary resource rendering
avoid arbitrary path rendering
avoid unsafe author-controlled attribute names
avoid private data in shared cached fragments
respect CSRF for unsafe methods
```

HTMX actions must not allow arbitrary method attributes through generic `attr()`.

---

# 31. Analytics and Data Layer

The frontend bridge should emit standardized lifecycle events:

```text
aem:fragment:swap
aem:request:start
aem:request:error
```

Components should reinitialize analytics/data-layer behavior after swaps.

The backend may optionally generate tracking attributes as part of `HxAction`.

---

# 32. Accessibility Requirements

Components must preserve:

```text
real links for navigable states
real forms for filters
aria-current for pagination
aria-sort for sortable tables
aria-expanded for accordions
focus restoration after swaps
keyboard support
screen-reader labels
no-JS usability
```

---

# 33. Deliverables

## 33.1 Java Backend Library

Package:

```text
com.myorg.aem.hypermedia
```

Includes:

```text
HxAction
HxAction.Builder
HxMethod
HxSwap
HxTarget
HxUrl
HxFragment
FragmentUrlService
MappedUrlService
HxRequestState
AbstractHypermediaComponent
PaginationContext contracts
```

## 33.2 Frontend Library

Internal npm package:

```text
@myorg/aem-ui-core
```

Includes:

```text
AemElement
component registry
HTMX lifecycle bridge
DOM event helpers
typed event contracts
safe init helpers
```

## 33.3 Base Components Package

Internal npm package:

```text
@myorg/aem-base-components
```

Includes frontend implementations for:

```text
pagination
accordion behavior
swiper initializer
powerline
back-to-top
table enhancement helpers
```

## 33.4 Type Package

Optional:

```text
@myorg/aem-component-types
```

Includes:

```text
PaginationOptions
PaginationChangeDetail
EventsFilterState
AemComponentEvent types
```

---

# 34. Migration Plan

## Phase 1: Foundation

```text
Create HxAction builder.
Create URL mapping helpers.
Create AbstractHypermediaComponent.
Create frontend AemElement base.
Create package export strategy for TypeScript types.
```

## Phase 2: Pilot Components

Implement:

```text
events overview filter with HTMX-enhanced full-result swaps
client-side pagination custom element
editorial sortable table
powerline/back-to-top client-side component
```

## Phase 3: Shared Component Library

Move reusable behavior into:

```text
components-tenant
```

Expose as internal package artifacts.

Website tenants consume the package rather than relative paths.

## Phase 4: Cache and URL Strategy

Add:

```text
selector-based fragment URLs
public fragment route support
cache-control conventions
service worker optional fragment cache
```

## Phase 5: Wider Adoption

Apply to:

```text
accordion
tabs
search results
forms
load more
swiper dynamic slide sets
```

---

# 35. Success Criteria

The project is successful if:

```text
authors can still use the normal AEM Sites Editor
components remain proxyable and tenant-friendly
HTL remains readable
Java models become more ergonomic
frontend imports no longer use deep relative tenant paths
shared TypeScript types resolve cleanly
interactive components work after HTMX swaps
no-JS fallback remains valid
SEO baseline is preserved where required
Dispatcher/CDN caching is not harmed
fragment requests can be cached when public
edit mode is not broken by HTMX behavior
```

---

# 36. Final Recommended Architecture

```text
AEM Sites Editor
  remains the authoring surface

HTL
  remains the canonical markup layer

Sling Models
  expose state and typed HxAction objects

AEM Hypermedia Java Library
  builds mapped URLs, fragment URLs, and hx attributes

HTMX or HTMX-inspired runtime
  requests and swaps server-rendered fragments

Custom Elements without Shadow DOM
  handle lifecycle and browser-only behavior

Service Worker
  optional cache/offline layer

WebSocket/SSE
  optional live-update layer

Internal frontend packages
  provide shared JS and .d.ts types across full AEM tenants
```

The guiding principle:

```text
Make the AEM-native, authorable, cacheable, SEO-friendly path the easiest path.
```

