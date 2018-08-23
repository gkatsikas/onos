COMPILE_DEPS = CORE_DEPS + KRYO + CLI + REST + JACKSON + [
    "@javax_ws_rs_api//jar",
    "@joda_time//jar",
    "//core/api:onos-api",
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/server:onos-drivers-server",
    "//incubator/api:onos-incubator-api",
    "//utils/rest:onlab-rest",
    "//protocols/rest/api:onos-protocols-rest-api",
]

BUNDLES = [
    "//apps/metron:onos-apps-metron",
]

REQUIRED_APPS = [
    "org.onosproject.drivers.server",
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/metron",
    api_title = "Metron NFV Controller REST API",
    api_version = "1.0.0",
    api_description = "REST API for interacting with the Metron NFV Controller",
    api_package = "org.onosproject.metron.impl.rest",
)

onos_app(
    title = "Metron NFV Controller",
    app_name = "org.onosproject.metron",
    category = "Traffic Processing &amp; Steering",
    origin = "KTH Royal Institute of Technology &amp; RISE SICS",
    url = "https://bitbucket.org/nslab/onos/src/metron-ctrl/",
    description = "Metron realizes NFV service chains at the speed of the underlying hardware",
    included_bundles = BUNDLES,
    required_apps = REQUIRED_APPS,
)
