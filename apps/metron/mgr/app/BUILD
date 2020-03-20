COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "@javax_ws_rs_api//jar",
    "@joda_time//jar",
    "//apps/metron/mgr/api:onos-apps-metron-mgr-api",
    "//apps/virtual/api:onos-apps-virtual-api",
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//drivers/server:onos-drivers-server",
    "//protocols/rest/api:onos-protocols-rest-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
