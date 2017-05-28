Metron
=========
[Metron][metron-paper] is a high performance and ultra efficient NFV service chaining platform to appear in [USENIX NSDI 2018][metron-nsdi-page].


About
----
Metron's control plane is based on the [ONOS SDN controller][onos], which we extended with [southbound drivers][metron-driver] that allow Metron to monitor and configure commodity servers.
These drivers are now part of the [official ONOS distribution 1.13.0][onos-master] (since February 22, 2018).

[Metron's data plane][metron-agent] extends [FastClick][fastclick], which in turn uses [DPDK][dpdk] as a high performance network I/O subsystem.

This repository provides the source code of ONOS extended with the Metron controller.


Setup
----
Follow the instructions in the [ONOS wiki][onos-wiki] to setup ONOS.


Build & Deploy
----
To build and deploy ONOS, do:
```bash
./tools/build/onos-buck run onos-local -- clean debug -Xlint:deprecation -Xlint:unchecked
```


Activate Metron
----
To activate Metron using the ONOS CLI, do:
```bash
app activate metron
```

Alternatively, you can activate Metron from the ''Applications'' tab of the ONOS UI.
The name of the application is: ''Metron NFV Controller''.


Deploy a Metron data plane agent
----
To connect a commodity server device to Metron, see [Metron's data plane agent][metron-agent].


Deploy a Metron service chain
----
Metron service chains are encoded as JSON files that can be sent to the Metron controller using ONOS's northbound REST API.
Metron parses the input JSON configuration and translates it to packet processing blocks.
Example service chains can be found at the [apps][metron-apps] folder.

To deploy an example Firewall --> NAPT service chain do:
```bash
onos-netcfg <ONOS CTRL IP> full-path-to/apps/metron/apps/app-fw-napt.json
```


Citing Metron
----
If you use Metron in your work, please cite our [paper][metron-paper]:
```
@inproceedings{katsikas-metron.nsdi18,
	author       = {Katsikas, Georgios P. and Barbette, Tom and Kosti\'{c}, Dejan and Steinert, Rebecca and Maguire Jr., Gerald Q.},
	title        = {{Metron: NFV Service Chains at the True Speed of the Underlying Hardware}},
	booktitle    = {To appear in the proceedings of the 15th USENIX Conference on Networked Systems Design and Implementation},
	series       = {NSDI'18},
	year         = {2018},
	url          = {https://people.kth.se/~dejanko/documents/publications/metron-nsdi18.pdf},
	address      = {Renton, WA},
	publisher    = {{USENIX} Association}
}
```


Getting help
----
Contact katsikas.gp at gmail.com if you encounter any problems with Metron.

The ONOS README is available [here][onos-readme].

[metron-paper]: https://people.kth.se/~dejanko/documents/publications/metron-nsdi18.pdf
[metron-nsdi-page]: https://www.usenix.org/conference/nsdi18/presentation/katsikas
[onos]: https://onosproject.org/
[metron-driver]: https://github.com/opennetworkinglab/onos/tree/master/drivers/server
[metron-agent]: https://github.com/tbarbette/fastclick/tree/metron
[metron-apps]: apps/metron/apps/
[onos-master]: https://github.com/opennetworkinglab/onos
[fastclick]: https://github.com/tbarbette/fastclick
[dpdk]: https://dpdk.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-readme]: README.onos.md
