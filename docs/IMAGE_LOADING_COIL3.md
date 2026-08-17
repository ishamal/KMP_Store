# Image loading with Coil 3

Why Coil 3 was chosen and what it costs (Section 1), how network images are loaded and cached, the exact
steps to integrate it, the security surface it brings (Section 7), and how the setup handles the demanding
case: **a large scrolling list where every row has an image URL, and scrolling must never refetch an
image that has already been downloaded.**

See also: [`ARCHITECTURE.md`](ARCHITECTURE.md) (module layout) and
[`../.claude/docs/STACK.md`](../.claude/docs/STACK.md) (versions).

---

## 1. Why Coil 3

**Recommendation: Coil 3.** It is the only mainstream image library that is Kotlin-first,
Compose-first and **Kotlin Multiplatform** — and every module in this repo already declares
`iosArm64`/`iosSimulatorArm64` targets. Picking an Android-only library would put a permanent
Android-only layer into a codebase whose purpose is shared code. Everything the large-list
requirement needs (Section 6) it does with no configuration; the total footprint here is two dependencies,
one permission and one composable.

### Alternatives considered

Scoped to platform support — the property that actually decides it here, and the one that can be
checked. Glide and Picasso were **not** otherwise evaluated; their caching and Compose behaviour is
not characterised below because it never became relevant.

| Option | Multiplatform (iOS) | Verdict |
|---|---|---|
| **Coil 3** | Yes | **Chosen.** Compose-first (`AsyncImage` is a composable); brings no HTTP client (Section 3) |
| Glide | No — Android only | Would strand iOS |
| Picasso | No — Android/View era | Would strand iOS |
| Hand-rolled (OkHttp + own cache) | Possible | Reimplements Section 6 by hand |

Hand-rolling deserves one line: it means reimplementing memory/disk tiering, LRU eviction, size-aware
decoding and request cancellation — all of which Section 6 gets for free, and all of which are easy to get
subtly wrong in exactly the ways a long list exposes.

### Pros

- **Multiplatform.** The one property that decides it for this repo. iOS needs only a different
  network artifact (Section 10), not a different image layer.
- **Compose-native.** `AsyncImage` is a composable; loads are tied to composition and cancel when a
  row scrolls away. No View interop, no adapter, no lifecycle plumbing.
- **Meets the caching requirement with zero code.** Memory + disk tiering and a non-revalidating disk
  strategy are the defaults (Section 6). No configuration in this project at all (Section 4.2).
- **No annotation processor or code generation.** Adding Coil required no KSP/KAPT and no change to
  any module's `plugins { }` block — nothing added to build time.
- **Pluggable networking.** OkHttp on Android, Ktor for multiplatform — chosen per source set rather
  than baked in.
- **Small integration surface.** Two catalog entries, two lines in one module, one permission.

### Cons

- **No HTTP client bundled**, and the failure mode is silent — an empty composable, no error (Section 3).
  This is the single biggest cost of choosing Coil 3 over Coil 2 or Glide.
- **No in-flight request de-duplication by default.** `ConcurrentRequestStrategy` is `UNCOORDINATED`;
  the de-duplicating implementation is `@ExperimentalCoilApi` (Section 6.3).
- **Opaque default disk cap** — 2% of free space clamped to `[10 MB, 250 MB]`, so the cache you get
  varies by device (Section 6.1).
- **Default cache location is unverified on Android**, with a security consequence (Section 7).
- **The 2 → 3 rename poisons search results.** Most snippets and answers online are Coil 2
  (`io.coil-kt:coil-compose`, `coil.compose.*`) and will not compile here.
- **Never revalidates.** Excellent for this requirement, wrong if image URLs are reused for changing
  content (Section 7).

> **Coil 2 vs Coil 3.** Different artifacts, different packages. Coil 2 is `io.coil-kt:coil-compose`
> with `coil.compose.*` imports; Coil 3 is `io.coil-kt.coil3:coil-compose` with `coil3.compose.*`.
> Copying a Coil 2 snippet gives an unresolved-import error; copying a Coil 2 *dependency setup*
> gives something worse — see Section 3.

---

## 2. The environment this had to fit

Everything below was read from the repo, not assumed:

| Aspect | Value | Why it matters here |
|---|---|---|
| Structure | Kotlin Multiplatform, `core/<name>/{api,real}` + `features/<name>/{api,real}` | The dependency has to land in a specific module, not "the app" |
| Targets | Android + `iosArm64`, `iosSimulatorArm64` | Rules out Android-only image libraries for the long term |
| UI | Compose Multiplatform 1.11.1, Material3 1.11.0-alpha07 | **There is no XML layout and no `ImageView` anywhere in this repo** |
| Compose UI location | Each module's `androidMain` | Where the image composable goes |
| Kotlin / AGP | 2.4.0 / 9.0.1, JVM target 11 | Coil 3.5.0 is built against a compatible Kotlin |
| SDK | `compileSdk`/`targetSdk` 36, `minSdk` 26 | minSdk 26 means low-RAM devices are in scope |
| DI | Metro 1.2.1 | Not involved — Coil's loader is a library-owned singleton, see Section 4.2 |
| Flavors | `storeA`–`storeD` from `build-logic/.../Stores.kt` | A feature module isn't in every flavor's graph |
| Dependencies | Version catalog `gradle/libs.versions.toml`, type-safe accessors | Where the coordinates are declared |
| Networking | **None.** No HTTP client, no Retrofit/Ktor. Backend is stubbed (`StubLoginData.kt`) | The image library has to bring its own HTTP stack |

Two of these drove the whole design. The repo is **Compose-only**, so the answer is a composable and
not an `ImageView`. And the repo has **no HTTP client at all**, so whatever we picked had to ship one
— which turns out to be the single biggest gotcha in Coil 3 (Section 3).

---

## 3. The one non-obvious trap: Coil 3 has no HTTP client

**Coil 2 bundled OkHttp. Coil 3 does not.** `coil-compose` on its own has no network fetcher, so an
`https://` model resolves to nothing: the composable renders empty, no crash, no error in logcat that
points at the cause. This is the most common Coil 2 → 3 migration failure.

You must add a network artifact explicitly:

- `coil-network-okhttp` — for Android/JVM (what this project uses)
- `coil-network-ktor3` — if the call site ever moves to `commonMain` for iOS

It registers itself through a ServiceLoader entry, so no code is needed — but that entry has to
survive packaging, which Section 9 verifies.

The second, more boring trap: this app had **no `INTERNET` permission**, because nothing had ever
made a network call.

---

## 4. Design decisions

### 4.1 One `ImageLoader` for the process

An `ImageLoader` owns the memory cache, the disk cache, the OkHttp client and the dispatchers. Those
must be shared — one per screen would mean N independent caches that never hit each other's entries,
which directly defeats the requirement.

Coil models this as a process-wide singleton. The `AsyncImage(model, contentDescription, …)` overload
we use has **no** `imageLoader` parameter; it calls `SingletonImageLoader.get(...)` internally
(`SingletonAsyncImage.kt:65`).

### 4.2 The singleton is left at Coil's defaults — no configuration code

**There is deliberately no `ImageLoader` configuration in this project.** No `Application` subclass,
no `SingletonImageLoader.Factory`, no Metro binding. Coil builds its own loader via
`DefaultSingletonImageLoaderFactory`, which already supplies a memory cache, an enabled disk cache and
the non-revalidating `DefaultCacheStrategy` — i.e. everything Section 6's guarantee rests on. Configuration
code would have added no behaviour, only knobs.

The one consequence, and the reason this is a *decision* rather than an omission: **the disk cache cap
is therefore device-dependent, somewhere in `[10 MB, 250 MB]`** (Section 6.1). Revisit if the measurement in
Section 6.1 shows a full list of thumbnails won't fit in that budget.

If it ever needs revisiting, configuring the singleton does **not** require an `Application` class:

```kotlin
// MainActivity.onCreate, before setContent
SingletonImageLoader.setSafe { context ->
    ImageLoader.Builder(context)
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(/* … */).build() }
        .build()
}
```

`setSafe` must run before any composable calls `SingletonImageLoader.get()`. An `Application`
implementing `SingletonImageLoader.Factory` is the alternative — Coil casts the application context to
that interface (`SingletonImageLoader.kt:98`) — and buys only that ordering guarantee.

---

## 5. Integration steps

### Step 1 — version catalog

`gradle/libs.versions.toml`:

```toml
[versions]
coil = "3.5.0"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }
```

No repository changes needed: `mavenCentral()` in `settings.gradle.kts` is unfiltered (only
`google()` is group-restricted), so `io.coil-kt.coil3` resolves.

### Step 2 — the feature module that renders images

In the `:real` module's `androidMain.dependencies { }` (Compose UI lives in `androidMain`):

```kotlin
androidMain.dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
```

`:androidApp` needs **no** Coil dependency of its own. Both are `implementation` deps of the feature
module, which keeps them off the app's compile classpath but still on its runtime classpath — enough
for the ServiceLoader registration in Section 3 to reach the APK (verified in Section 9).

### Step 3 — permission

`androidApp/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

App-level is enough; manifest merge covers the library modules.

### Step 4 — the call site

```kotlin
AsyncImage(
    model = record.imageUrl,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(72.dp),
)
```

Import is `coil3.compose.AsyncImage`. `contentDescription` is required (pass `null` for decorative
images).

That's the whole integration — two dependencies, one permission, one composable.

---

## 6. Loading images in a large list

The demanding case for any image layer is a long scrolling list — hundreds or thousands of rows, each
with its own image URL, scrolled up and down repeatedly. The requirement is simple to state:
**each image is downloaded once, and every later appearance is served from cache.**

Coil satisfies this out of the box. There is no dedupe or "already fetched" bookkeeping to write. Per
row the lookup order is **memory → disk → network**, and the network is reached only on the genuine
first sight of a URL.

**Memory cache.** With no transformations set, the cache key is just the URL — image size is *not*
part of it (`MemoryCacheService.kt:60-65`). Scrolling back to a row still resident in RAM is a
hashmap hit with zero I/O.

**Disk cache.** `DefaultCacheStrategy.read()` is unconditional:

```kotlin
// Always return the disk cache response.
return ReadResult(cacheResponse)
```

No conditional GET, no `If-None-Match`, no expiry check. Once bytes are on disk, Coil serves them and
never contacts the server for that URL again.

**Three things can still evict those bytes**, all worth stating plainly:

1. **LRU eviction** once the cache exceeds its cap — and that cap is device-dependent here (Section 6.1).
2. **Android reclaiming the cache directory.** Coil's default disk cache lives under the platform
   temporary directory (Section 6.1), which is subject to being purged under storage pressure.
3. Explicitly clearing it (`SingletonImageLoader.get(context).diskCache?.clear()`).

None causes a refetch *during* a scroll session. But "downloaded once, forever" is really "downloaded
once, until the cache is reclaimed."

### 6.1 Cache size — the setting that scales with list length

Everything above holds only while an image is still *in* the cache. This is the one place where list
length changes the answer: a short list fits in any cache, and a long one may not.

**The disk cache cap is what stands between a long list and a refetch.** When the cache fills, Coil
evicts LRU — and the least-recently-used entries are precisely the top-of-list rows the user scrolls
back to. So the symptom of an undersized cache isn't a visible error; it's the beginning of the list
quietly re-downloading.

With no configuration, `DiskCache.Builder` uses `maxSizePercent = 0.02` and clamps the result to
`[10 MB, 250 MB]` (`DiskCache.kt:116-117, 193-199`):

```kotlin
val size = maxSizePercent * fileSystem.remainingFreeSpaceBytes(directory)
size.toLong().coerceIn(minimumMaxSizeBytes, maximumMaxSizeBytes)   // 10 MB … 250 MB
```

So the cap is a fraction of however empty the device happens to be, bounded at both ends:

| Free space | 2% raw | Effective cap |
|---|---|---|
| 16 GB | 320 MB | **250 MB** — hits the ceiling |
| 4 GB | 80 MB | 80 MB |
| 1 GB | 20 MB | 20 MB |
| 400 MB | 8 MB | **10 MB** — hits the floor |

(The MB figures are arithmetic on that formula, not device measurements. `remainingFreeSpaceBytes` is
whatever okio reports for the cache path.)

Note the direction: **the users whose phones are fullest get the smallest image cache**, and it looks
fine on a roomy dev device while degrading only in the field.

**The measurement that decides whether this is a problem.** Budget the list as
`rows × bytes-per-thumbnail` and compare against the table above. To measure, scroll a few dozen real
rows, then locate the cache and size it.

The default directory is `FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil3_disk_cache"`
(`coil3/disk/utils.kt`). **Where that resolves on Android is unverified** — hence discovering the path
rather than hardcoding it:

```bash
# run-as starts in /data/user/0/<pkg>, so this searches the app sandbox
adb shell run-as com.isharaw.kmpproj.storea find . -type d -name "*coil*"
adb shell run-as com.isharaw.kmpproj.storea du -sk <path from above>
```

If that comes back empty, the temp directory resolves outside the sandbox; widen the search
(`adb shell find /data/local/tmp /sdcard -name "coil3_disk_cache" 2>/dev/null`) or set an explicit
directory via the Section 4.2 snippet, which makes the location knowable by construction.

Divide by rows loaded, then multiply by the list's full row count and add headroom:

| Rows | ~30 KB thumbnails | ~250 KB images |
|---|---|---|
| 200 | 6 MB — fits anywhere | 50 MB — fits on most devices |
| 1,000 | 30 MB — fits on most devices | 250 MB — **exactly the ceiling, no headroom** |
| 5,000 | 150 MB — needs a roomy device | 1.25 GB — **far past the ceiling** |

If the total comfortably fits the effective cap, the defaults are fine and nothing needs doing. If it
doesn't, pin `maxSizeBytes` with the `setSafe` snippet in Section 4.2 — and note the ceiling is 250 MB by
default, so a list that large needs an explicit cap regardless of how much free space the device has.

Bear in mind this only bounds *repeat* viewing. A list long enough to overflow any reasonable cache
will always refetch its oldest rows eventually; at that point the question shifts from caching to
whether the images should be smaller.

**Memory cache** needs no attention either way: Coil sizes it at 20% of app memory, stepping down to
15% on `isLowRamDevice` (`coil3/util/contexts.kt`). Hardcoding a percentage would throw that
adaptation away, and with `minSdk 26` low-RAM devices are in scope.

### 6.2 Call-site rules for list rows

```kotlin
items(records, key = { it.id }) { record ->
    AsyncImage(
        model = record.imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(72.dp),   // fixed, not fillMaxWidth
    )
}
```

- **Stable `key`** so Compose doesn't reshuffle items into the wrong slots during a fling.
- **Fixed thumbnail size.** Compose defaults to `Precision.INEXACT` (`AsyncImagePainter.kt:298`), so a
  cached bitmap is reused when it's large enough; but if the same URL renders at two different sizes,
  the smaller cached copy gets re-decoded. That costs CPU during scrolling — never a network call, but
  worth avoiding.

### 6.3 Known gap: concurrent requests are not de-duplicated

Coil's default `ConcurrentRequestStrategy` is `UNCOORDINATED` — two *simultaneous* in-flight requests
for the same URL both hit the network. This only bites when the same URL appears in several rows
visible at once, and only on first sight; afterwards both are cache hits. Lists of distinct images per
row are unaffected.

If the list does repeat URLs, `DeDupeConcurrentRequestStrategy` exists — but it is
`@ExperimentalCoilApi` and requires registering a custom `NetworkFetcher.Factory` in the
`componentRegistry`, which places a second fetcher alongside the ServiceLoader-registered one.
**Not enabled**, pending confirmation that URLs actually repeat.

---

## 7. Security considerations

Image loading is a network client plus a disk cache plus a decoder, so it inherits risk from all
three. Ordered by how actionable each is in *this* repo.

### 7.1 Cached images survive logout — open, unaddressed

This app has a session: `SettingsContribution.kt:39` logs out with `sessionManager.session = null`,
and the experience graph is dropped. **Nothing clears the image cache.** Images fetched while logged
in — anything behind invoices, orders or rebates — stay on disk and in memory after logout, and are
served to whoever logs in next on the same device.

The fix is small:

```kotlin
SingletonImageLoader.get(context).apply {
    memoryCache?.clear()
    diskCache?.clear()
}
```

Note it needs a `Context`, which `SettingsContribution`'s `onLogout` lambda does not have — it is
wired through DI, not composed. The natural home is wherever the app reacts to `session == null`
(`App.kt`), where a context is in scope, rather than inside the lambda itself.

Not implemented. How far it should go is a product decision — the same argument applies to switching
business unit, where an image cached under one BU could surface under another.

### 7.2 The cache directory location is unverified

With no configuration, Coil writes to `FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil3_disk_cache"`
(`coil3/disk/utils.kt`). **Where that resolves on Android has not been verified in this project.**

That matters here: if it resolves inside app-private storage, the sandbox protects the bytes; if it
resolves anywhere else, cached image content is not protected by app isolation. This is worth
settling before anything sensitive is displayed as an image. Setting an explicit directory under
`cacheDir` — the Section 4.2 snippet, which also solves Section 6.1's sizing — makes the answer knowable by
construction rather than by inspection.

### 7.3 Stale and cross-user content (the cost of Section 6's guarantee)

`DefaultCacheStrategy` never revalidates. Section 6 sells this as the feature that satisfies the
requirement; the security-relevant flip side:

- A URL whose **content changes** while the URL stays the same will serve the old bytes indefinitely.
- A URL that serves **different content per user** (session-scoped or permission-scoped endpoints)
  will show one user's image to the next — closely related to Section 7.1.

If either applies, image URLs must be content-addressed (a hash, version or signature in the URL) so
that new content means a new cache key. That is a backend contract, not something Coil can enforce.

### 7.4 Cleartext HTTP — blocked, but by default only

`targetSdk` is 36 and the manifest sets neither `usesCleartextTraffic` nor a `networkSecurityConfig`,
so Android's default applies and plain `http://` image URLs are rejected. Good — but this is a
*default*, not a decision recorded anywhere. Adding `usesCleartextTraffic="true"` later, for any
reason, silently makes every image URL interceptable.

### 7.5 Decoder attack surface and oversized images

Decoding is the classic attack surface for image handling: Coil hands bytes to the platform decoders,
so patching depends on the OS rather than on a library upgrade — relevant at `minSdk 26`, where
devices may no longer receive updates.

Separately, a hostile or merely careless server can return a very large image. Coil decodes to the
size the composable asks for, so the **fixed thumbnail size in Section 6.2 is also the mitigation**; an
unbounded `SizeResolver.ORIGINAL` in a list is what turns one bad response into an OOM.

### 7.6 Lower priority for this repo

- **URL provenance.** Coil fetches whatever URL it is given. If image URLs ever come from
  user-generated content rather than a trusted backend, they need host validation. Today the backend
  is stubbed, so this is a note for when it isn't.
- **Certificate pinning.** Not configured. Would require supplying a custom `OkHttpClient` to the
  network fetcher. Usually not warranted for a public image CDN.
- **`INTERNET` permission** is now granted app-wide — the app previously had none.

---

## 8. Things not to do

| Don't | Why |
|---|---|
| Add `coil-network-cache-control` | It replaces `DefaultCacheStrategy` with header-driven revalidation — Coil starts contacting the server to check freshness, the opposite of Section 6's requirement |
| Reintroduce Coil 2 (`io.coil-kt:coil-compose`, `coil.compose.*`) | Different artifact, and it bundles its own OkHttp — mixing the two gives you two HTTP stacks |
| Build an `LruCache` or a manual "already fetched" set | Duplicates Section 6, and gets eviction wrong |
| Give list rows an unbounded or varying image size | Defeats memory-cache reuse (Section 6.2) and removes the Section 7.5 mitigation |
| Add Coil to `:androidApp` | Nothing there uses it; the feature module's `implementation` deps already put it on the runtime classpath (Section 5 Step 2) |

---

## 9. Verification

Compiling proves the imports resolve. It proves nothing about whether images actually load — the
failure mode in Section 3 compiles perfectly. These are the checks that discriminate:

```bash
# 1. Builds, including a flavor that does NOT include the image-rendering feature
./gradlew :androidApp:assembleStoreADebug :androidApp:assembleStoreBDebug

# 2. Permission survived manifest merge
grep -c 'android.permission.INTERNET' \
  androidApp/build/intermediates/merged_manifest/storeADebug/*/AndroidManifest.xml

# 3. The network fetcher's ServiceLoader entry reached the APK.
#    It arrives transitively from the feature module (Section 5 Step 2), and a
#    packaging{ resources{ excludes } } rule can strip it and silently break all loading.
unzip -l androidApp/build/outputs/apk/storeA/debug/*.apk | grep coil3.util
#    → META-INF/services/coil3.util.FetcherServiceLoaderTarget
```

**Runtime check.** Install, open a screen with an image, confirm it renders. Then put the device in
airplane mode and scroll away and back: cached rows still render, which is the actual requirement.

---

## 10. iOS

Coil 3 is multiplatform, but this integration is **Android-only today** and untested on iOS. Moving it
would require:

1. The composable moves from `androidMain` to `commonMain`.
2. Swap `coil-network-okhttp` for `coil-network-ktor3` (OkHttp is JVM-only) — plus a Ktor engine.
3. Nothing for the loader itself, since it is unconfigured. If Section 6.1 ever forces configuration, iOS
   would use `SingletonImageLoader.setSafe(...)` from its entry point plus a platform-expect for the
   cache directory.

---

## 11. Current state

Wired and verified on `storeA`, with a single demonstration `AsyncImage` at the top of
`features/orders/real/.../OrdersScreen.kt`. Note that `orders` is **not** in every flavor —
per `Stores.kt`, `storeB` has no `orders` feature, so that demo image does not appear there.

The `ImageLoader` is unconfigured by choice (Section 4.2). Total footprint of this feature: two catalog
entries, two lines in one module's build file, one manifest permission, one composable.

Known gaps, in priority order:

- **Cached images are not cleared at logout (Section 7.1).** The only item here that is a defect rather than
  a question. One block in the logout path closes it.
- **The default cache directory's location on Android is unverified (Section 7.2)** — worth settling before
  anything sensitive is rendered as an image.

Open questions:

- **Are the list's image URLs distinct, or do they repeat within a screenful?** Decides Section 6.3.
- **Do image URLs ever change content without changing the URL?** Decides Section 7.3.
- **What are the real image dimensions?** Decides Section 6.1 — whether the default 10–250 MB cap holds a
  full list, or whether a pinned `maxSizeBytes` needs reintroducing.
