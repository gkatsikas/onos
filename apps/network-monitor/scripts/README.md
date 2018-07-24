Scripts
=========
Complementary scripts to install dependencies and perform testing.


Trace Replay
----
To inject a real trace into a target network, run:
```bash
bash ./replay_trace.sh <NETWORK INTERFACE> <PATH/TO/A/TRACE>
```


Mininet Custom Topologies
----
Folder [mininet][mininet] provides scripts for launching Mininet tests.

Go to mininet folder:
```bash
cd mininet/
```

Launch a custom Mininet topology and connect to remote controller (e.g., ONOS):
```bash
sudo python launch_custom_topo.py <CUSTOM TOPOLOGY NAME>
```

The aforementioned command will also start the Mininet CLI. Alternatively, the user can specify a test to be performed:
```bash
sudo python launch_custom_topo.py <CUSTOM TOPOLOGY NAME> --with-tests <INGRESS POINT> <TEST DURATION SECONDS>
```

e.g.,
```bash
sudo python launch_custom_topo.py LinearFive --with-tests h1 20
```


Dependencies
----
In order to download and install InfluxDB, please run script:

```bash
bash ./build_dependencies.sh
```

To start InfluxDB, do:
```bash
bash ./start_influxdb.sh
```

Install tcpreplay as follows:
```bash
sudo apt install tcpreplay
```


[mininet]: mininet/