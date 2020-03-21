COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@joda_time//jar",
    "//apps/metron/mgr/api:onos-apps-metron-mgr-api",
    "//apps/metron/mgr/app:onos-apps-metron-mgr-app",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
