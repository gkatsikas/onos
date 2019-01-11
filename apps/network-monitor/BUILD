REQUIRED_APPS = [
//    "org.onosproject.drivers.server",
]

BUNDLES = [
    "//apps/network-monitor:onos-apps-network-monitor",
]

COMPILE_DEPS = CORE_DEPS + KRYO + CLI + METRICS + REST + JACKSON + [
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "@joda_time//jar",
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@com_google_code_gson_gson//jar",
    "@commons-math3//jar",
//    "//lib:commons-math3",
]

BUNDLES = [
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@com_google_code_gson_gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@commons-math3//jar",
//  "//lib:commons-math3",
]

EXCLUDED_BUNDLES = [
    "@metrics_influxdb//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@com_google_code_gson_gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@commons-math3//jar",
//  "//lib:commons-math3",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    web_context = "/onos/networkmonitor",
    api_title = "Network Monitor REST API",
    api_version = "1.0.0",
    api_description = "REST API for interacting with the Network Monitor",
    api_package = "org.onosproject.networkmonitor.impl.rest",
)

onos_app (
    title = "Network Monitor",
    app_name = "org.onosproject.networkmonitor",
    category = "Monitoring",
    description = "Network monitoring application with persistent storage through influxDB.",
    origin = "RISE SICS &amp; KTH Royal Institute of Technology",
    url = "https://github.com/gkatsikas/onos/tree/network-monitor",
    included_bundles = BUNDLES,
    excluded_bundles = EXCLUDED_BUNDLES,
    required_apps = REQUIRED_APPS,
)
