Network Monitor
=========
Network Monitor is a monitoring framework developed to assist the control plane of modern programmable networks.


About
----
Network Monitor collects load statistics from the underlying network and stores these statistics into an InfluxDB database.

Network Monitor is developed on top of the [ONOS SDN controller][onos] and the [InfluxDB][influx-db] timeseries database.


Setup
----
Follow the instructions in the [ONOS wiki][onos-wiki] to setup ONOS.

Also, setup the InfluxDB database following the instructions [here][influx-db].


Build & Deploy
----
To build and deploy ONOS, do:
```bash
./tools/build/onos-buck run onos-local -- clean debug -Xlint:deprecation -Xlint:unchecked
```


Configure Network Monitor
----
Network Monitor configuration is encoded as JSON files that can be sent to the ONOS controller using ONOS's northbound REST API.

Network operators can select the desired devices to be monitored by Network Monitor along with the desired ports where input load is expected.

Example configurations can be found at the [conf][network-monitor-conf] folder.

To configure Network Monitor, do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/network-monitor/conf/test-conf.json
```


Activate Network Monitor
----
To activate Network Monitor using the ONOS CLI, do:
```bash
app activate network-monitor
```

Alternatively, you can activate Network Monitor from the [Applications tab][onos-ui-apps] of the ONOS UI.

The name of the application is: ''Network Monitor''.


Network Monitor REST API
----
You can retrieve Network Monitor's run-time statistics by issuing HTTP get requests.

See our Network Monitor REST API at:
```bash
http://<ONOS CTRL IP>:8181/onos/v1/docs
```


Deactivate Network Monitor
----
To deactivate Network Monitor using the ONOS CLI, do:
```bash
app deactivate network-monitor
```

Alternatively, you can deactivate Network Monitor from the [Applications tab][onos-ui-apps] of the ONOS UI.

The name of the application is: ''Network Monitor''.


Getting help
----
Contact georgios.katsikas at ri.se if you encounter any problems with Network Monitor.

The ONOS README is available [here][onos-readme].

[onos]: https://onosproject.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-ui-apps]: http://127.0.0.1:8181/onos/ui/index.html#/app
[onos-readme]: README.onos.md
[network-monitor-conf]: ./conf/
[influx-db]: https://github.com/influxdata/influxdb
