Network Monitor
=========
Network Monitor is a monitoring framework developed to assist the control plane of modern programmable networks.


About
----
[Network Monitor][network-monitor] collects load statistics from the underlying network and stores these statistics into an InfluxDB database.
Network Monitor is developed on top of the [ONOS SDN controller][onos] and the [InfluxDB][influx-db] timeseries database.


Setup
----
Follow the instructions in the [ONOS wiki][onos-wiki] to setup ONOS.

Also, setup the InfluxDB database following the instructions [here][influx-db].


Build & Deploy ONOS
----
To build and deploy ONOS, do:
```bash
cd $ONOS_ROOT
onos-buck run onos-local [-- [clean] [debug]]
```


Configure Network Monitor
----
The Network Monitor is configured via JSON that can be sent to the ONOS controller using ONOS's northbound REST API.
Network operators can select the desired devices to be monitored by the Network Monitor along with the desired ports where input load is expected.
Moreover, the configuration also covers system and database setup.

Example configurations can be found at the [conf][network-monitor-conf] folder.

To configure the Network Monitor, do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/network-monitor/conf/test-conf.json
```


Activate Network Monitor
----
To activate the Network Monitor using the ONOS CLI, do:
```bash
app activate network-monitor
```

The name of the application is: ''Network Monitor''.


Getting help
----
Contact katsikas.gp at gmail.com if you encounter any problems with Network Monitor.

The ONOS README is available [here][onos-readme].

[onos]: https://onosproject.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-readme]: README.onos.md
[network-monitor]: https://github.com/gkatsikas/onos/tree/network-monitor
[network-monitor-conf]: apps/network-monitor/conf/
[influx-db]: https://github.com/influxdata/influxdb
