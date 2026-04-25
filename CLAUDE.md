# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Always use `docker compose` for building, running, or testing. Never run services directly on the host.

```bash
GIT_REVISION=$(git rev-parse HEAD) docker compose up --build -d   # build and start (embeds git hash)
docker compose up --build -d   # build and start (revision = "unknown")
docker compose logs -f         # tail logs
docker compose down            # stop
curl http://localhost:1085/WebObjects/HelloWorld.woa   # test
```

## Architecture

Real **Apple WebObjects 5.4.3** application running in Tomcat 8.5 via Docker Compose. WO JARs come from the WOCommunity Maven repository (`https://maven.wocommunity.org/content/groups/public`).

### Build pipeline

1. Maven builds `HelloWorld.jar` (contains `Application.class`, `Main.class`, and `.wo` templates) into `target/bundles/`.
2. The WAR is assembled with:
   - `HelloWorld.jar` in `WEB-INF/lib` — needed so the servlet context classloader can resolve component classes when NSBundle delegates class lookup to it.
   - `.class` files **excluded** from `WEB-INF/classes` (via `packagingExcludes`) to avoid the web app classloader loading app classes there first.
3. The Dockerfile copies the `.woa` filesystem bundle structure into `/opt/woapps/HelloWorld.woa/` and also copies `HelloWorld.jar` into `Contents/Resources/Java/`.
4. The `GIT_REVISION` build arg (passed via `docker-compose.yml` from the env var of the same name) is written to `/opt/woapps/HelloWorld.woa/REVISION` at image build time. `StatsPage.java` reads this file at request time and renders it as a GitHub commit link.

### WOROOT mode — critical details

WO operates in **WOROOT mode** (not WOJarBundle mode). Several non-obvious constraints apply:

- **WOClasspath delimiter:** `WOServletAdaptor.tokenizeClasspath()` uses `\r\n` as its delimiter, not `:`. WOClasspath entries in `web.xml` **must be newline-separated** or they are never split on Linux.
- **Variable substitution:** `getRealPath()` substitutes `WOAINSTALLROOT` → `/opt/woapps` and `WEBINFROOT` → the expanded WAR's WEB-INF path before scanning.
- **Bundle detection:** `tokenizeClasspath()` scans each (file) entry for `.woa/`. The entry `WOAINSTALLROOT/HelloWorld.woa/Contents/Resources/Java/HelloWorld.jar` sets `mainBundlePath = /opt/woapps/HelloWorld.woa`. Without this, WO throws "Can't find application bundle".
- **Template lookup:** resolves to `mainBundlePath/Contents/Resources/Main.wo/Main.html`.
- **Component class lookup:** `NSBundle.classNamed("Main")` uses the servlet context classloader, so `HelloWorld.jar` must be in `WEB-INF/lib` even though it's also referenced in WOClasspath via the `.woa` path.

### WO component gotchas

- **No `WOTextArea`** — multi-line text inputs use `WOText` in WO 5.4.3.
- **Field name conflicts** — `WOComponent` has a `name()` method. A component field also named `name` will be shadowed by the inherited method; KVC `value = name` will return the component class name. Rename to e.g. `authorName`.
- **Submit button needs explicit action** — `WOForm { action = x; }` alone doesn't always fire in 5.4.3; add `action = x` to the `WOSubmitButton` as well.
- **WO form field names** are generated as element IDs (`5.1`, `5.3`, …), not the HTML `name` attribute from the WOD.
- **WOGenericElement vs WOGenericContainer** — `WOGenericElement` renders self-closing (`<div/>`), which is invalid for block elements. Use `WOGenericContainer` when the tag needs a proper open/close pair (e.g. a `div` used as a progress bar fill).
- **Validation pattern** — return `null` from an action method to stay on the current component with field values preserved. Use public String fields (e.g. `nameError`) as error messages; bind them with `WOConditional` + `WOString` pairs in the WOD.
- **Session lifecycle** — `WOApplication` in 5.4.3 does not have `sessionDidCreate` / `sessionDidTimeOut`. Override `createSessionForRequest(WORequest)` to track session creation. Session termination notifications are not reliably exposed; avoid trying to track them. `application().activeSessionsCount()` is a real public method and returns a live count.
- **Session error handlers** — WO 5.4.3 has three distinct methods to override for graceful session expiry handling (all return `WOResponse`):
  - `handleSessionRestorationErrorInContext(WOContext)` — stale session ID in URL (most common; what fires when a user refreshes after a container restart)
  - `handleSessionCreationErrorInContext(WOContext)` — WO cannot create a new session
  - `handlePageRestorationErrorInContext(WOContext)` — session exists but page backtrack limit exceeded
  
  Without overrides, WO shows its own "Your session has timed out." error page. With overrides, redirect to `pageWithName("Main", ctx)` and set a `warningMessage` field before calling `generateResponse()`.
- **WOSubmitButton class binding** — `WOSubmitButton` supports a `class` binding in WOD to apply CSS classes; use `-webkit-appearance: none; appearance: none` in CSS to strip the browser's native input styling.

### Guestbook (HSQLDB)

Guestbook entries persist via **HSQLDB 2.7** (embedded Java database, no separate service). Data lives in the bind-mounted `./data/guestbook/` directory at `/data/guestbook/gb.*` inside the container.

`GuestbookDB` is a singleton with a single persistent `Connection`. All public methods are `synchronized`. Tables: `ENTRIES` (id, posted_at, name, email, location, message) and `VISITOR_COUNT` (single-row counter incremented on each new `GuestbookPage` instance).

### Stats page (StatsPage)

`StatsPage.java` surfaces live metrics via `java.lang.management` MXBeans and `Application.app()`:
- **JVM**: uptime, VM name, Java version, CPU count, system load average, heap used/max (with color-coded bar via `WOGenericContainer`), non-heap/metaspace, loaded class count, GC collector name + collection count + total pause ms, live/peak thread count
- **WebObjects**: app name, version (`application().number()`, falls back to `"1.0"` if `-1`), git revision (read from `/opt/woapps/HelloWorld.woa/REVISION`, rendered as a GitHub commit link), sessions created (tracked via `createSessionForRequest` override in `Application.java`)
- **Guestbook**: entry count, total visitor count

### Key files

| File | Purpose |
|------|---------|
| `src/main/java/Application.java` | WO entry point; overrides `createSessionForRequest` to count sessions; overrides all three session error handlers to redirect to Main with a warning |
| `src/main/java/Main.java` | Default page component |
| `src/main/java/GuestbookPage.java` | Guestbook WOComponent (form + validation + entry list) |
| `src/main/java/GuestbookDB.java` | HSQLDB JDBC data access (singleton) |
| `src/main/java/GuestbookEntry.java` | Guestbook entry data bean |
| `src/main/java/StatsPage.java` | Live stats WOComponent (JVM MXBeans + WO + guestbook) |
| `src/main/resources/Main.wo/` | Main page template + bindings |
| `src/main/resources/GuestbookPage.wo/` | Guestbook template + bindings |
| `src/main/resources/StatsPage.wo/` | Stats page template + bindings |
| `src/main/resources/Properties` | WO app properties (`WOApplicationName`, etc.) |
| `src/main/webapp/WEB-INF/web.xml` | WO context-params (WOROOT, WOClasspath, etc.) |
| `Dockerfile` | Multi-stage: Maven build → Tomcat + .woa bundle |
