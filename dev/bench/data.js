window.BENCHMARK_DATA = {
  "lastUpdate": 1773526361189,
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
      }
    ]
  }
}