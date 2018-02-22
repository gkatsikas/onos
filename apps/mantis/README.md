Mantis
=========
Mantis is a workload prediction framework developed to assist SDN and NFV network controllers.


About
----
Mantis collects load statistics from the underlying network and applies machine learning techniques to predict near future load.

Mantis is developed on top of the [ONOS SDN controller][onos] and the [InfluxDB][influx-db] timeseries database.


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


Configure Mantis
----
Mantis configuration is encoded as JSON files that can be sent to the ONOS controller using ONOS's northbound REST API.

Network operators can select the desired devices to be monitored by Mantis along with the desired ports where input load is expected.

Example configurations can be found at the [conf][mantis-conf] folder.

To configure Mantis, do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/mantis/conf/test-conf.json
```


Activate Mantis
----
To activate Mantis using the ONOS CLI, do:
```bash
app activate mantis
```

Alternatively, you can activate Mantis from the [Applications tab][onos-ui-apps] of the ONOS UI.

The name of the application is: ''Mantis Load Predictor''.


Mantis REST API
----
You can retrieve Mantis' run-time statistics by issuing HTTP get requests.

See our Mantis REST API at:
```bash
http://<ONOS CTRL IP>:8181/onos/v1/docs
```


Deactivate Mantis
----
To deactivate Mantis using the ONOS CLI, do:
```bash
app deactivate mantis
```

Alternatively, you can deactivate Mantis from the [Applications tab][onos-ui-apps] of the ONOS UI.

The name of the application is: ''Mantis Load Predictor''.


Getting help
----
Contact georgios.katsikas at ri.se if you encounter any problems with Mantis.

The ONOS README is available [here][onos-readme].

[onos]: https://onosproject.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-ui-apps]: http://127.0.0.1:8181/onos/ui/index.html#/app
[onos-readme]: README.onos.md
[mantis-conf]: ./conf/
[influx-db]: https://github.com/influxdata/influxdb
