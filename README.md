# alphainfo-java

[![Maven Central](https://img.shields.io/maven-central/v/io.alphainfo/client.svg)](https://central.sonatype.com/artifact/io.alphainfo/client)
[![Java 11+](https://img.shields.io/badge/java-11+-blue.svg)](https://adoptium.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Java client for the [alphainfo.io](https://alphainfo.io) Structural Intelligence API.**

Detect structural regime changes in any time series — biomedical, financial, industrial, geophysical, network, ML-ops. One API, no training, no per-domain tuning.

## Install

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("io.alphainfo:client:1.5.10")
}
```

Maven:

```xml
<dependency>
    <groupId>io.alphainfo</groupId>
    <artifactId>client</artifactId>
    <version>1.5.10</version>
</dependency>
```

## 30-second try

**Step 1 — [get a free API key](https://alphainfo.io/register)**.

**Step 2**:

```java
import io.alphainfo.AlphaInfoClient;
import io.alphainfo.Models.AnalyzeRequest;
import java.util.ArrayList;
import java.util.List;

List<Double> signal = new ArrayList<>();
for (int i = 0; i < 200; i++) signal.add(Math.sin(i / 10.0));
for (int i = 0; i < 200; i++) signal.add(Math.sin(i / 10.0) * 3);

try (var client = new AlphaInfoClient("ai_...")) {
    var result = client.analyze(
            new AnalyzeRequest().signal(signal).samplingRate(100));
    System.out.println(result.confidenceBand);   // stable | transition | unstable
    System.out.println(result.structuralScore);  // 0.0 → 1.0
}
```

## Async

Every call has an `Async` counterpart returning `CompletableFuture`:

```java
client.analyzeAsync(req).thenAccept(result -> { ... });
client.fingerprintAsync(req).thenAccept(fp -> { ... });
```

## Structural fingerprint

```java
import io.alphainfo.AlphaInfoConstants;
import io.alphainfo.Models.FingerprintResult;

FingerprintResult fp = client.fingerprint(
        new AnalyzeRequest().signal(signal).samplingRate(250));

if (fp.isComplete()) {
    List<Double> vector = fp.getVector();   // 5D, for pgvector / Qdrant / Faiss
} else {
    System.out.println("unavailable: " + fp.fingerprintReason);
}
```

**Minimum signal length:**

| Case | Minimum samples | Constant |
|---|---|---|
| No baseline | 192 | `AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES` |
| With baseline | 50 | `AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES_WITH_BASELINE` |

Below the threshold the SDK logs a `WARNING` (via `java.util.logging`) at call time so you can fall back to `analyze()` before paying a round-trip. `getVector()` returns `null` when incomplete — never fills missing dimensions with 0.

## Error handling

```java
import io.alphainfo.*;

try {
    client.analyze(req);
} catch (AuthException e) {
    // Invalid API key — get one at https://alphainfo.io/register
} catch (RateLimitException e) {
    Thread.sleep(e.getRetryAfterSeconds() * 1000L);
} catch (ValidationException e) {
    // Bad input
} catch (NotFoundException e) {
    // analysis_id not found
} catch (ApiException e) {
    // 5xx
} catch (NetworkException e) {
    // Transport
}
```

Everything inherits from `AlphaInfoException` (extends `RuntimeException` — no checked exceptions to propagate).

## Zero-auth exploration

```java
Map<String, Object> g = AlphaInfoClient.guide();
HealthStatus h = AlphaInfoClient.health();
```

## Configuration

```java
OkHttpClient http = new OkHttpClient.Builder()
    .readTimeout(60, TimeUnit.SECONDS)
    .build();
var client = new AlphaInfoClient("ai_...", "https://www.alphainfo.io", http);
```

## Kotlin

The Java SDK is fully usable from Kotlin. Idiomatic usage:

```kotlin
AlphaInfoClient("ai_...").use { client ->
    val fp = client.fingerprint(
        Models.AnalyzeRequest().apply {
            signal = listOf(...)
            samplingRate = 100.0
        }
    )
    if (fp.isComplete) println(fp.vector)
}
```

## Links

- [Web](https://alphainfo.io)
- [Python SDK](https://pypi.org/project/alphainfo/)
- [JS/TS SDK](https://www.npmjs.com/package/alphainfo)
- [Go SDK](https://pkg.go.dev/github.com/qgidev/alphainfo-go)
- [Encoding guide](https://www.alphainfo.io/v1/guide)

## About

Built by **QGI Quantum Systems LTDA** — São Paulo, Brazil.
Contact: contato@alphainfo.io · api@alphainfo.io

## License

MIT
