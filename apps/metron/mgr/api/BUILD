COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "@joda_time//jar",
    "//core/common:onos-core-common",
    "//drivers/server:onos-drivers-server",
    "//incubator/api:onos-incubator-api",
    "//protocols/rest/api:onos-protocols-rest-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
