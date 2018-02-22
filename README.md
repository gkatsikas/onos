Mantis
=========
Mantis is a workload prediction framework developed to assist the control plane of modern programmable networks.


About
----
Mantis relies on network monitoring and run-time machine learning tools.
Mantis comprises of the following two main modules:


Mantis Monitor
--------
[Mantis monitor][mantis-monitor] collects load statistics from the underlying network and stores these statistics into the InfluxDB database.
Mantis monitor is developed on top of the [ONOS SDN controller][onos] and the [InfluxDB][influx-db] timeseries database.


Mantis Predictor
--------
[Mantis predictor][mantis-predictor] fetches network load statistics from InfluxDB (as produced by the Mantis Monitor) and applies machine learning techniques to predict near future network load.
Mantis predictor uses [Kapacitor][kapacitor] as a processing and prediction tool and stores the results in the [InfluxDB][influx-db] timeseries database.


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


Configure Mantis Monitor
----
The Mantis Monitor is configured via JSON that can be sent to the ONOS controller using ONOS's northbound REST API.
Network operators can select the desired devices to be monitored by the Mantis Monitor along with the desired ports where input load is expected.
Moreover, the configuration also covers system and database setup.

Example configurations can be found at the [conf][mantis-monitor-conf] folder.

To configure the Mantis Monitor, do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/mantis/conf/test-conf.json
```


Activate Mantis Monitor
----
To activate the Mantis Monitor using the ONOS CLI, do:
```bash
app activate mantis
```

The name of the application is: ''Mantis Load Monitoring''.


Build & Deploy Mantis Predictor
----
To build and deploy the Mantis Predictor, follow the instructions [here][mantis-predictor].


Getting help
----
Contact katsikas.gp at gmail.com if you encounter any problems with Mantis.

The ONOS README is available [here][onos-readme].

[onos]: https://onosproject.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-readme]: README.onos.md
[mantis-monitor]: https://ghetto.sics.se/nigsics/mantis-monitor/tree/mantis-monitor
[mantis-monitor-conf]: apps/mantis/conf/
[mantis-predictor]: https://ghetto.sics.se/nigsics/mantis-predictor
[influx-db]: https://github.com/influxdata/influxdb
[kapacitor]: https://www.influxdata.com/time-series-platform/kapacitor/
