window.BENCHMARK_DATA = {
  "lastUpdate": 1773528863944,
  "repoUrl": "https://github.com/dordor12/logback-to-metrics",
  "entries": {
    "JMH Benchmark": [
      {
        "commit": {
          "author": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "committer": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "id": "393b7fa8b2e36b824066fa9b02f3ec37b81c3863",
          "message": "feat: High-performance rewrite with cardinality protection & observability",
          "timestamp": "2026-03-11T21:34:39Z",
          "url": "https://github.com/dordor12/logback-to-metrics/pull/12/commits/393b7fa8b2e36b824066fa9b02f3ec37b81c3863"
        },
        "date": 1773525650926,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20319210.156620335,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 4006010.803724341,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1159635.0691511787,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7051741.880699033,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3894246.2015355127,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1181801.4382611439,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6332851.765289239,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 19843121.780102022,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "committer": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "id": "9cee06a6de6ffa4e567661ed1a3f4ff592843773",
          "message": "feat: High-performance rewrite with cardinality protection & observability",
          "timestamp": "2026-03-11T21:34:39Z",
          "url": "https://github.com/dordor12/logback-to-metrics/pull/12/commits/9cee06a6de6ffa4e567661ed1a3f4ff592843773"
        },
        "date": 1773526074578,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20353816.397852615,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 3969015.8402654924,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1163159.407066533,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7122392.178128524,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3906961.8316545673,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1163024.8268192422,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6232284.647004861,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 19887147.75117498,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "committer": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "id": "91bb9818df3cf8996794ff1314c0d4ddcba5bfd4",
          "message": "feat: High-performance rewrite with cardinality protection & observability",
          "timestamp": "2026-03-11T21:34:39Z",
          "url": "https://github.com/dordor12/logback-to-metrics/pull/12/commits/91bb9818df3cf8996794ff1314c0d4ddcba5bfd4"
        },
        "date": 1773526360566,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20666455.44019895,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 3992665.233959901,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1148494.225680499,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7270750.973508062,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3967637.993019824,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1167483.710655229,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6225639.3976663845,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 20044960.46319969,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "committer": {
            "name": "dordor12",
            "username": "dordor12"
          },
          "id": "33a4088cfd9f0f1a32917c3f4fa146ba0c7f93ea",
          "message": "feat: High-performance rewrite with cardinality protection & observability",
          "timestamp": "2026-03-11T21:34:39Z",
          "url": "https://github.com/dordor12/logback-to-metrics/pull/12/commits/33a4088cfd9f0f1a32917c3f4fa146ba0c7f93ea"
        },
        "date": 1773527920458,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20254008.882228952,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 4004639.6289343825,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1163531.0179654495,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7158839.26108701,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3977254.665669487,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1163271.712072113,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6258844.50292136,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 20197585.966176104,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "50987668+dordor12@users.noreply.github.com",
            "name": "Dor Amid",
            "username": "dordor12"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "46cc6a7130b573a4cef0dc8a5d14c7595dedd5be",
          "message": "feat: High-performance rewrite with cardinality protection & observability (#12)\n\n* feat: High-performance rewrite with cardinality protection, self-observability & JMH benchmarks\n\n- Rewrite appender to extract data directly from event objects (no JSON round-tripping)\n- Add CacheKey record for zero-allocation hot-path lookups\n- Add circuit breaker flags for counter/histogram saturation\n- Add runtime cardinality protection with auto-blacklisting of high-cardinality tags\n- Add self-observability metrics (timers, counters, gauges) with toggle flag\n- Add JMH benchmark suite with realistic 20-event workload (55M+ ops/s hot path)\n- Add benchmark CI workflow with GitHub Pages dashboard (benchmark-action)\n- Extract StructuredArguments and LogstashMarkers directly (no encoder needed)\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* docs: Add enableSelfObservability flag to README configuration\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* test: Add integration tests for MDC, LogstashMarkers, blacklist, counter-only, mixed sources, self-observability\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* fix: Fix flaky testSelfMetricsRegistered — check appender fields instead of global registry gauges\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n---------\n\nCo-authored-by: Dor Amid <dor.amid@taboola.com>\nCo-authored-by: Claude Opus 4.6 <noreply@anthropic.com>",
          "timestamp": "2026-03-15T00:40:58+02:00",
          "tree_id": "66ef5db60221d3771ec19940b60cd2ce2217b878",
          "url": "https://github.com/dordor12/logback-to-metrics/commit/46cc6a7130b573a4cef0dc8a5d14c7595dedd5be"
        },
        "date": 1773528283816,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20650689.619089402,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 4026574.480542685,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1140088.1620608994,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7153563.017405018,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3903549.3076993763,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1166361.9739093266,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6204041.894544212,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 20235617.94008305,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Dor Amid",
            "username": "dordor12",
            "email": "50987668+dordor12@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "46cc6a7130b573a4cef0dc8a5d14c7595dedd5be",
          "message": "feat: High-performance rewrite with cardinality protection & observability (#12)\n\n* feat: High-performance rewrite with cardinality protection, self-observability & JMH benchmarks\n\n- Rewrite appender to extract data directly from event objects (no JSON round-tripping)\n- Add CacheKey record for zero-allocation hot-path lookups\n- Add circuit breaker flags for counter/histogram saturation\n- Add runtime cardinality protection with auto-blacklisting of high-cardinality tags\n- Add self-observability metrics (timers, counters, gauges) with toggle flag\n- Add JMH benchmark suite with realistic 20-event workload (55M+ ops/s hot path)\n- Add benchmark CI workflow with GitHub Pages dashboard (benchmark-action)\n- Extract StructuredArguments and LogstashMarkers directly (no encoder needed)\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* docs: Add enableSelfObservability flag to README configuration\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* test: Add integration tests for MDC, LogstashMarkers, blacklist, counter-only, mixed sources, self-observability\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n* fix: Fix flaky testSelfMetricsRegistered — check appender fields instead of global registry gauges\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>\n\n---------\n\nCo-authored-by: Dor Amid <dor.amid@taboola.com>\nCo-authored-by: Claude Opus 4.6 <noreply@anthropic.com>",
          "timestamp": "2026-03-14T22:40:58Z",
          "url": "https://github.com/dordor12/logback-to-metrics/commit/46cc6a7130b573a4cef0dc8a5d14c7595dedd5be"
        },
        "date": 1773528290842,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20549114.272717517,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 4028420.759441896,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1148209.3749419225,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7130330.488511068,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3915471.1562540224,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1163623.5865568542,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6252439.388303803,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 20793325.868621826,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "GitHub Action",
            "username": "actions-user",
            "email": "action@github.com"
          },
          "committer": {
            "name": "GitHub Action",
            "username": "actions-user",
            "email": "action@github.com"
          },
          "id": "89fef2b7b5ba519d6ad14a049cedd245aa16f627",
          "message": " [Gradle Release Plugin] - pre tag commit:  '0.3.0'.",
          "timestamp": "2026-03-14T22:45:54Z",
          "url": "https://github.com/dordor12/logback-to-metrics/commit/89fef2b7b5ba519d6ad14a049cedd245aa16f627"
        },
        "date": 1773528863493,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.coldPathRegistration",
            "value": 20259177.50492526,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPath",
            "value": 3990082.327489505,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathFullFeatures",
            "value": 1163695.9769901615,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathNoObservability",
            "value": 7211437.83303343,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithCardinality",
            "value": 3846649.0337585313,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.realisticHotPathWithHistograms",
            "value": 1179439.4394714483,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventHotPath",
            "value": 6244401.974184006,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.github.dordor12.LogbackToMetricsAppenderBenchmark.singleEventNoObservability",
            "value": 20340944.223219782,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}