# Changelog

## [1.5.12] - 2026-04-20

Added automatic domain inference; `domain` field on `AnalyzeRequest`
now optional with sensible default.

- New nested `Models.DomainInference` class (inferred, confidence,
  fallbackUsed, reasoning).
- `AnalysisResult.domainApplied` — populated by server 1.5.12+.
- `AnalysisResult.domainInference` — populated only when the caller
  set `req.domain = "auto"`.
- New `client.analyzeAuto(AnalyzeRequest)` + `client.analyzeAutoAsync`
  helpers — sugar for `analyze(...)` with `req.domain = "auto"`.
- Javadoc on `analyze()` updated to explain "auto", aliases, and the
  "Did you mean …?" suggestion path.

Backwards-compatible — explicit-domain callers unaffected.

## [1.5.11] - 2026-04-20

### Connection cleanup improvements.

- `close()` is now fully idempotent and defensive — wrapped in
  try/ignore blocks so a double-close (or close on a partially
  initialized client) can never throw.
- `AlphaInfoClient` already implements `AutoCloseable`; the idiomatic
  path (`try (var c = new AlphaInfoClient(...))`) is unchanged and
  documented in the README.
- Javadoc added spelling out the close contract.

## [1.5.10] - 2026-04-20

### Initial release — parity with Python SDK 1.5.10.

- `AlphaInfoClient` with `analyze`, `fingerprint`, `analyzeBatch`,
  `analyzeMatrix`, `analyzeVector`, `auditList`, `auditReplay`,
  `health`, `plans`, `guide`, `version`. Every call has an `Async`
  counterpart returning `CompletableFuture`.
- Static `AlphaInfoClient.guide()` / `health()` — no API key needed.
- `AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES` (192) and
  `MIN_FINGERPRINT_SAMPLES_WITH_BASELINE` (50).
- Honest fingerprint contract: `FingerprintResult.simLocal` and friends
  are `Double` (boxed, nullable); `getVector()` returns `null` when
  incomplete — never substitutes zeros.
- `RuntimeException` hierarchy: `AuthException`, `RateLimitException`
  (with `retryAfterSeconds`), `ValidationException`, `NotFoundException`,
  `ApiException`, `NetworkException`, all inheriting from
  `AlphaInfoException`.
- `java.util.logging` WARNING when `fingerprint()` is called with a
  signal shorter than the threshold.
- Built on OkHttp 4 + Jackson 2. Targets Java 11+.
