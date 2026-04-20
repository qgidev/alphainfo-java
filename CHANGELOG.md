# Changelog

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
