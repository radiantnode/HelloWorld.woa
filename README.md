# HelloWorld.woa

![WebObjects Box](images/webobjects-box.png)

A real, honest-to-goodness **Apple WebObjects 5.4.3** application running in a Docker container. Not a simulation. Not a tribute band. The actual JAR files, the actual NSBundle lookup, the actual `.wo` template files — all of it, running on your laptop in the year of our lord 2026.

## What even is WebObjects?

[WebObjects](https://en.wikipedia.org/wiki/WebObjects) was Apple's (and originally NeXT's) enterprise Java web framework from the late 1990s. Before Rails, before Django, before Spring MVC, there was WebObjects — and it was genuinely ahead of its time. It had object-relational mapping, component-based HTML templates with two-way KVC bindings, and a session management system that would look familiar to anyone who's used modern frameworks.

Apple used it to power the Apple Store and iTunes Store for years. Then they quietly discontinued it around 2008. The JARs live on, preserved by the [WOCommunity](https://wocommunity.org/) folks, and apparently they still run just fine on Java 11.

This project proves it.

## What's in here

- A **HelloWorld.woa** that serves a real WebObjects response with live session IDs and WO headers
- A **classic guestbook** (yes, *that* kind of guestbook — dark background, Comic Sans, gold text) backed by a real database
- A **Docker Compose** setup so the whole thing spins up with one command

## Running it

You need Docker. That's it.

```bash
docker compose up --build -d
```

Then open [http://localhost](http://localhost). Tomcat redirects you straight to `HelloWorld.woa`.

Want to watch the WO startup logs? They're glorious:

```bash
docker compose logs -f
```

To stop:

```bash
docker compose down
```

## The stack

| Layer | Technology | Vibe |
|-------|-----------|------|
| Web framework | Apple WebObjects 5.4.3 | 1999 |
| App server | Tomcat 8.5 | 2016 |
| Database | HSQLDB 2.7 | Early 2000s Java apps |
| Build | Maven 3.9 | Still going strong |
| Runtime | Java 11 | The LTS era |
| Container | Docker | Modern enough |

The database is **HSQLDB** — a pure-Java embedded database that was everywhere in early-2000s Java enterprise apps. No separate container, no service to manage; it just writes files to a Docker volume and persists across restarts.

(We tried FrontBase for maximum retro points. FrontBase's website is running a broken WebObjects app. The JDBC driver appears to have been erased from the internet. The x86 Docker image crashes during initialization on Apple Silicon. FrontBase wins the award for most retro by actually being inaccessible.)

## The guestbook

The real highlight. Sign it. It looks exactly like it should.

[http://localhost/WebObjects/HelloWorld.woa](http://localhost/WebObjects/HelloWorld.woa) → click the guestbook link.

Entries persist in a Docker named volume (`guestbook_data`), so your messages survive container restarts. Just like a real 1999 guestbook server that somehow never gets rebooted.

## How it actually works

WebObjects in servlet mode is not simple to set up. The framework has specific expectations:

- It runs in **WOROOT mode**, where it looks for a `.woa` filesystem bundle at `/opt/woapps/HelloWorld.woa/`. The bundle has to exist with `Contents/Resources/<ComponentName>.wo/` directories for template lookup to work.
- The `WOClasspath` in `web.xml` **must** be newline-separated. The tokenizer uses `\r\n` as its delimiter, not `:`. A colon-separated path on Linux is treated as one giant token and the whole thing breaks.
- Application classes have to be in `WEB-INF/lib/HelloWorld.jar` (for the servlet classloader) AND in the `.woa` bundle path (for bundle detection). Two copies, two purposes.

It took a while to figure all this out. The CLAUDE.md has the gory details if you want them.

## Project structure

```
src/main/java/
  Application.java        WO application entry point
  Main.java               Default page component
  GuestbookPage.java      Guestbook component (form + entry list)
  GuestbookDB.java        HSQLDB data access
  GuestbookEntry.java     Data bean

src/main/resources/
  Main.wo/                Hello World template + bindings
  GuestbookPage.wo/       Guestbook template + bindings
  Properties              WO app config
  Info.plist              Bundle metadata

src/main/webapp/
  index.jsp               Redirects / → HelloWorld.woa
  WEB-INF/web.xml         Servlet config + WOClasspath

Dockerfile                Multi-stage: Maven build → Tomcat
docker-compose.yml        Single service + guestbook volume
```
