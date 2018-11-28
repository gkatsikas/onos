Metron
=========
[Metron][metron-paper] is a high performance and ultra efficient NFV service chaining platform to appear in [USENIX NSDI 2018][metron-nsdi-page].


About
----
The Metron controller is implemented as a set of [ONOS][onos] bundles that interact with each other.

Metron applications describe their packet processing logic to the controller using JSON files sent via Metron's northbound REST API.

Once an application posts a JSON configuration (i.e., a service chain), it "registers" with the controller, and the latter is responsible for configuring the data plane according to the application logic.

Metron extends ONOS with [southbound drivers][metron-driver] that allow Metron to monitor and configure commodity servers.
These drivers are now part of the [official ONOS distribution 1.13.0][onos-master] (since February 22, 2018).


Setup
----
Follow the instructions in the [ONOS wiki][onos-wiki] to setup ONOS.


Dependencies
---
In addition to the basic [ONOS dependencies][onos-dep], since version 1.14, ONOS uses Bazel 0.17.0 as a build tool.
To install Bazel 0.17.0 follow the steps below:

```bash
wget https://github.com/bazelbuild/bazel/releases/download/0.17.0/bazel-0.17.0-installer-linux-x86_64.sh
chmod +x bazel-0.17.0-installer-linux-x86_64.sh
bash bazel-0.17.0-installer-linux-x86_64.sh --user
source $HOME/.bazel/bin/bazel-complete.bash
echo 'export PATH=$PATH:$HOME/bin' >> $HOME/.bashrc
source $HOME/.bashrc
rm bazel-0.17.0-installer-linux-x86_64.sh
```


Build ONOS
----
The Metron controller is part of the ONOS tree. You can use either BUCK or Bazel to build ONOS and Metron as follows:

#### Build with BUCK ####
```bash
cd $ONOS_ROOT
rm -rf buck-out bin
onos-buck clean
tools/build/onos-buck build onos --show-output
```

#### Build with Bazel ####
```bash
cd $ONOS_ROOT
bazel clean --expunge
onos-build -Xlint:deprecation
bazel build onos --verbose_failures
```


Deploy ONOS
----
If you built ONOS and Metron using BUCK, then deploy as follows:

#### Deploy with BUCK ####
```bash
cd $ONOS_ROOT
tools/build/onos-buck run onos-local -- clean debug -Xlint:deprecation -Xlint:unchecked
```

If you built ONOS and Metron using Bazel, then deploy as follows:

#### Deploy with Bazel ####
```bash
bazel run onos-local -- debug
```


Activate Metron
----
To activate Metron using the ONOS CLI, do:
```bash
app activate metron
```

Alternatively, you can activate Metron from the [Applications tab][onos-ui-apps] of the ONOS UI.
The name of the application is: ''Metron NFV Controller''.


Deploy a Metron data plane agent
----
To connect a commodity server device to Metron, see [Metron's data plane agent][metron-agent].

You can also link an NFV server with other network elements by doing:
```bash
onos-netcfg <ONOS-CTRL-IP> ./conf/example/nfv-links.json
```


Deploy a Metron service chain
----
Metron service chains are encoded as JSON files that can be sent to the Metron controller using ONOS's northbound REST API.

Metron parses the input JSON configuration and translates it to packet processing blocks.

Example service chains can be found at the [apps][metron-apps] folder.

To deploy an example Firewall --> NAPT service chain do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/metron/apps/app-fw-napt.json
```

To deploy an example NAPT --> Snort (DPI) service chain do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/metron/apps/app-napt-snort.json
```

To deploy a standalone Snort (DPI) service chain do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/metron/apps/app-snort.json
```

Then, post your application to the controller as follows:
```bash
onos-netcfg <ONOS-CTRL-IP> <JSON>
```


Metron User Interface
----
You can access Metron's graphical user interface from your favorite browser by visiting:
```bash
http://<ONOS-CTRL-IP>:8181/onos/ui/index.html#/topo
```


Metron REST API
----
You can retrieve Metron's run-time NFV statistics by issuing HTTP get requests.
See our Metron NFV REST API at:
```bash
http://<ONOS-CTRL-IP>:8181/onos/v1/docs
```


Deactivate Metron
----
To deactivate Metron using the ONOS CLI, do:
```bash
app deactivate metron
```

Alternatively, you can deactivate Metron from the [Applications tab][onos-ui-apps] of the ONOS UI.
The name of the application is: ''Metron NFV Controller''.


Citing Metron
----
If you use Metron in your work, please cite our [paper][metron-paper]:
```
@inproceedings{katsikas-metron.nsdi18,
	author       = {Katsikas, Georgios P. and Barbette, Tom and Kosti\'{c}, Dejan and Steinert, Rebecca and Maguire Jr., Gerald Q.},
	title        = {{Metron: NFV Service Chains at the True Speed of the Underlying Hardware}},
	booktitle    = {15th USENIX Conference on Networked Systems Design and Implementation (NSDI 18)},
	series       = {NSDI'18},
	year         = {2018},
	isbn         = {978-1-931971-43-0},
	pages        = {171--186},
	numpages     = {16},
	url          = {https://www.usenix.org/system/files/conference/nsdi18/nsdi18-katsikas.pdf},
	address      = {Renton, WA},
	publisher    = {{USENIX} Association}
}
```


Getting help
----
Contact georgios.katsikas at ri.se if you encounter any problems with Metron.

The ONOS README is available [here][onos-readme].

[metron-paper]: https://people.kth.se/~dejanko/documents/publications/metron-nsdi18.pdf
[metron-nsdi-page]: https://www.usenix.org/conference/nsdi18/presentation/katsikas
[metron-driver]: https://github.com/opennetworkinglab/onos/tree/master/drivers/server
[metron-agent]: https://github.com/tbarbette/fastclick/tree/metron
[metron-apps]: ./apps/
[onos]: https://onosproject.org/
[onos-master]: https://github.com/opennetworkinglab/onos
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-ui-apps]: http://127.0.0.1:8181/onos/ui/index.html#/app
[onos-readme]: ../../README.onos.md
[onos-dep]: https://github.com/gkatsikas/onos/blob/onos-1.14/README.md
