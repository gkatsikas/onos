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


Dependencies
---
In addition to the basic [ONOS dependencies][onos-dep], since version 2.0, ONOS uses Bazel as the main build tool along with Java 11.
To install Bazel version VER (e.g., 0.21.0) follow the steps below:

```bash
wget https://github.com/bazelbuild/bazel/releases/download/0.19.0/bazel-VER-installer-linux-x86_64.sh
chmod +x bazel-VER-installer-linux-x86_64.sh
bash bazel-VER-installer-linux-x86_64.sh --user
source $HOME/.bazel/bin/bazel-complete.bash
echo 'export PATH=$PATH:$HOME/bin' >> $HOME/.bashrc
source $HOME/.bashrc
rm bazel-VER-installer-linux-x86_64.sh


Build ONOS
----
To build ONOS, do:
```bash
cd $ONOS_ROOT
bazel clean --expunge
bazel build onos --verbose_failures
```

Deploy ONOS
----
To deploy ONOS, do:
```bash
cd $ONOS_ROOT
bazel run onos-local -- clean
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


Network Monitor's REST API
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
Contact katsikas.gp at gmail.com if you encounter any problems with Network Monitor.

[onos]: https://onosproject.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-ui-apps]: http://127.0.0.1:8181/onos/ui/index.html#/app
[influx-db]: https://github.com/influxdata/influxdb
[network-monitor-conf]: ./conf/
