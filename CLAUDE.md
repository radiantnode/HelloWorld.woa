# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Always use `docker compose` for building, running, or testing. Never run services directly on the host.

```bash
docker compose up --build -d   # build and start
docker compose logs -f         # tail logs
docker compose down            # stop
curl http://localhost/WebObjects/HelloWorld.woa   # test
```

## Architecture

Real **Apple WebObjects 5.4.3** application running in Tomcat 8.5 via Docker Compose. WO JARs come from the WOCommunity Maven repository (`https://maven.wocommunity.org/content/groups/public`).

### Build pipeline

1. Maven builds `HelloWorld.jar` (contains `Application.class`, `Main.class`, and `.wo` templates) into `target/bundles/`.
2. The WAR is assembled with:
   - `HelloWorld.jar` in `WEB-INF/lib` — needed so the servlet context classloader can resolve component classes when NSBundle delegates class lookup to it.
   - `.class` files **excluded** from `WEB-INF/classes` (via `packagingExcludes`) to avoid the web app classloader loading app classes there first.
3. The Dockerfile copies the `.woa` filesystem bundle structure into `/opt/woapps/HelloWorld.woa/` and also copies `HelloWorld.jar` into `Contents/Resources/Java/`.

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

### Guestbook (HSQLDB)

Guestbook entries persist via **HSQLDB 2.7** (embedded Java database, no separate service). Data lives in the `guestbook_data` Docker volume at `/data/guestbook/gb.*`.

```bash
curl http://localhost/WebObjects/HelloWorld.woa   # main page → guestbook link
```

### Key files

| File | Purpose |
|------|---------|
| `src/main/java/Application.java` | WO application entry point |
| `src/main/java/Main.java` | Default page component |
| `src/main/java/GuestbookPage.java` | Guestbook WOComponent (form + entry list) |
| `src/main/java/GuestbookDB.java` | HSQLDB JDBC data access (singleton) |
| `src/main/java/GuestbookEntry.java` | Guestbook entry data bean |
| `src/main/resources/Main.wo/Main.html` | WO HTML template |
| `src/main/resources/Main.wo/Main.wod` | WO bindings |
| `src/main/resources/GuestbookPage.wo/` | Guestbook template + bindings |
| `src/main/resources/Properties` | WO app properties (`WOApplicationName`, etc.) |
| `src/main/webapp/WEB-INF/web.xml` | WO context-params (WOROOT, WOClasspath, etc.) |
| `Dockerfile` | Multi-stage: Maven build → Tomcat + .woa bundle |
