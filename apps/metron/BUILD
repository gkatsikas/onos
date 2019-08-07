COMPILE_DEPS = CORE_DEPS + KRYO + CLI + REST + JACKSON + [
    "@joda_time//jar",
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/server:onos-drivers-server",
    "//incubator/api:onos-incubator-api",
    "//protocols/rest/api:onos-protocols-rest-api",
]

BUNDLES = [
    "//apps/metron:onos-apps-metron",
]

REQUIRED_APPS = [
    "org.onosproject.drivers.server",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for interacting with the Metron NFV Controller",
    api_package = "org.onosproject.metron.impl.rest",
    api_title = "Metron NFV Controller REST API",
    api_version = "1.0.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/metron",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.metron",
    category = "Traffic Engineering",
    description = "Metron realizes NFV service chains at the true speed of the underlying hardware",
    included_bundles = BUNDLES,
    origin = "KTH Royal Institute of Technology &amp; RISE SICS",
    required_apps = REQUIRED_APPS,
    title = "Metron NFV Controller",
    url = "https://github.com/gkatsikas/onos/tree/metron-ctrl-1.14.0",
)
