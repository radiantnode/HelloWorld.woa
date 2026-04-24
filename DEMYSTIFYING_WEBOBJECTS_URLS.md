# Demystifying WebObjects URLs

If you've ever poked at a WebObjects app, you've seen URLs like this:

```
/WebObjects/HelloWorld.woa/wo/qatqoDY6ea29X0I4B1Jodw/0.7;jsessionid=5B564DFB9A73896E408249466A01E842
```

Let's break every piece of that down.

---

## The anatomy

```
/WebObjects / HelloWorld.woa / wo / qatqoDY6ea29X0I4B1Jodw / 0.7 ; jsessionid=5B564...
     1               2         3            4                  5          6
```

### 1. `/WebObjects/`

The servlet prefix. In `web.xml` the WO adaptor servlet is mapped to `/WebObjects/*`, so every WO request starts here. This was the standard in the late 90s/early 2000s and it just stuck.

### 2. `HelloWorld.woa`

The application name with a `.woa` extension — **Web Objects Application** bundle. On a classic WO deployment you might have multiple `.woa` apps running on the same server, and WO uses this segment to route the request to the right one. Here there's only one app, but the name is still in the URL.

### 3. `/wo/`

The **request handler type**. This tells WO which handler should process the request. There are three you'll encounter:

| Handler | Path | What it does |
|---------|------|-------------|
| Component | `/wo/` | Stateful, session-based page interactions |
| Direct Action | `/wa/` | Stateless, bookmarkable actions |
| Resource | `/wr/` | Static files served by WO (images, CSS, etc.) |

`wo` is the one you see most often — it handles all the normal click-around-the-app stuff.

### 4. `qatqoDY6ea29X0I4B1Jodw`

The **WO session ID**. This is *not* the same as the Java servlet session (that's the `jsessionid` at the end). This is WO's own session identifier — a random base64url string that WO uses to look up your session in its own session store (`WOSessionStore`).

The WO session holds:
- A **page cache** — the last N pages you visited, with their full component state
- Any data you've stored on `WOSession` itself
- Timeout info

When you submit a form, WO uses this ID to find your session, then finds the exact page component that rendered the form. That's how it knows which Java object to call your action method on.

### 5. `/0.7`

This is the **element context path** — the most interesting part.

```
  0  .  7
  │     └── element ID within the page
  └──── page index in the session's page cache
```

**The page index (`0`):**  
WO keeps a cache of recent pages per session (default is 30). Index `0` means the most recently rendered page. Index `1` is the one before that, and so on. When you click a link or submit a form, WO looks up page `0` in your session's cache and uses the full component state that was in memory when the page was rendered. This is how WO maintains state without you storing anything in a database — the component objects themselves survive between requests.

**The element ID (`7`):**  
When WO renders a page, it walks the entire component tree and assigns each dynamic element a sequential ID. `WOString`, `WOHyperlink`, `WOForm`, `WORepetition` — they each get a number in the order they appear in the template.

So `0.7` means: *"in the most recent page, invoke element number 7."* Element 7 is whatever `WOHyperlink` or `WOSubmitButton` you clicked. WO re-walks the component tree, finds element 7, and calls its action method.

**Nested components** get dotted paths:

```
0.3.1   → page 0, child component at position 3, element 1 within that component
0.7.2.5 → page 0, element 7 (a component), inside that: element 2 (another component), element 5
```

### 6. `;jsessionid=5B564...`

The **Java servlet session ID**, appended to the URL by Tomcat when cookies aren't available. This is the standard Java EE fallback for cookie-less session tracking. It's separate from WO's own session ID — these are two different session systems running in parallel. Usually you see both because WO has its own session layer on top of the servlet session.

---

## Form fields work the same way

When WO renders a form, the `name` attribute on every input is also an element context ID:

```html
<input type="text" name="5.1" />
<input type="text" name="5.3" />
<textarea name="5.7"></textarea>
<input type="submit" name="5.9" />
```

These `5.1`, `5.3`, `5.7` names are *not* your binding names from the WOD file — they're context paths just like in the URL. When the form is submitted, WO uses them to find the right `WOTextField` or `WOText` element in the component tree and call `takeValue()` with the submitted data, which sets the bound Java field on the component via KVC.

---

## Direct action URLs

Not every WO URL has a session ID. **Direct actions** look like this:

```
/WebObjects/HelloWorld.woa/wa/guestbook
/WebObjects/HelloWorld.woa/wa/DirectAction/default
```

The `/wa/` handler is stateless. No session is looked up, no page cache is consulted. WO instantiates a fresh `DirectAction` object, calls the method corresponding to the URL segment, and returns the result. Great for bookmarkable pages, RSS feeds, API endpoints, or anything that shouldn't require a live session.

---

## Why this matters

The component URL system is the core of what made WebObjects feel like magic in 1997. The fact that clicking a link could reach directly into the Java object that rendered it — with the full component state still alive — was a genuinely powerful idea. Modern frameworks achieve similar results in different ways, but WO did it at the URL level, which is why the URLs look like this.

The tradeoff is obvious: these URLs are not bookmarkable, they expire with the session, and they expose the session ID in the URL. For anything that needs to be shareable or crawlable, direct actions (`/wa/`) are the right tool.
