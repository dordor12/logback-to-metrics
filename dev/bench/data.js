window.BENCHMARK_DATA = {
  "lastUpdate": 1773525651541,
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
      }
    ]
  }
}