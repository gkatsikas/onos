COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//apps/metron/mgr/api:onos-apps-metron-mgr-api",
    "//apps/metron/mgr/app:onos-apps-metron-mgr-app",
    "//protocols/rest/api:onos-protocols-rest-api",
    "//utils/rest:onlab-rest-native",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for interacting with the Metron NFV Controller",
    api_package = "org.onosproject.metron.web",
    api_title = "Metron NFV Controller REST API",
    api_version = "/onos/v1/metron",
    test_deps = TEST_DEPS,
    web_context = "/onos/metron",
    deps = COMPILE_DEPS,
)
