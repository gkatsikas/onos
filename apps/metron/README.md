Metron
=========
[Metron][metron-paper] is an ultra high performance and efficient NFV service chaining platform, appeared in [USENIX NSDI 2018][metron-nsdi-page].


About
----
Metron's control plane is based on the [ONOS SDN controller][onos], which we extended with [southbound drivers][metron-driver] that allow Metron to monitor and configure commodity servers.
These drivers are now part of the [official ONOS distribution][onos-master] (since February 22, 2018).

[Metron's data plane][metron-agent] extends [FastClick][fastclick], which in turn uses [DPDK][dpdk] as a high performance network I/O subsystem.

This repository provides the source code of ONOS 1.15.0 (Peacock) extended with the Metron controller as an overlay application.


Setup ONOS
----
Follow the instructions in the [ONOS wiki][onos-wiki] to setup ONOS.


Dependencies
---
In addition to the basic [ONOS dependencies][onos-dep], since version 1.14, ONOS uses Bazel as a build tool.
The right Bazel version for ONOS 1.15.0 (Peacock) is 0.24.0. To install Bazel follow the steps below:

```bash
VER="0.24.0"
wget https://github.com/bazelbuild/bazel/releases/download/$VER/bazel-$VER-installer-linux-x86_64.sh
chmod +x bazel-$VER-installer-linux-x86_64.sh
bash bazel-$VER-installer-linux-x86_64.sh --user
source $HOME/.bazel/bin/bazel-complete.bash
echo 'export PATH=$PATH:$HOME/bin' >> $HOME/.bashrc
source $HOME/.bashrc
rm bazel-$VER-installer-linux-x86_64.sh
```


Build ONOS
----
The Metron controller is part of the ONOS tree. You can use Bazel to build ONOS and Metron as follows:
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


Activate Metron
----
To activate Metron using the ONOS CLI, do:
```bash
app activate metron
```

Alternatively, you can activate Metron from the Applications tab of the ONOS UI.
The name of the application is: ''Metron NFV Controller''.


Deploy a Metron data plane agent
----
To connect a commodity server device to Metron, see [Metron's data plane agent][metron-agent].


Service Chain Specification
----
Metron service chains are encoded as JSON files that can be sent to the Metron controller using ONOS's northbound REST API.
Metron parses the input JSON configuration and translates it to packet processing blocks.
Depending on the operator's needs, Metron allows the following deployment types:

  * Network-wide deployment, where ONOS uses a combination of programmable switches and servers. In this case Metron offloads traffic classification into OpenFlow switches and tags packets in a way that a server's programmable NICs can understand, thus dispatch to the correct CPU core for further stateful processing.
Example network-wide applications can be found [here][metron-net-apps].

  * Server-level deployment, where ONOS uses a single commodity server to run a service chain. First Metron uses the server's NICs to perform service chain offloading and CPU core dispatching operations, while the stateful service chain operations run in software.
Example server-level applications can be found [here][metron-server-apps].

Before you deploy one of the example service chains, you need to understand (thus be able to modify) the JSON-based service chain specification.
One or more service chains can be described via a single JSON file.
Every JSON file begins with the field 'apps' which tells ONOS that a certain application will follow.
Metron applications begin with 'org.onosproject.metron.apps' and for each app we have 3 options:

  * 'add' to create a service chain
  * 'remove' to tear down a service chain
  * 'remove-all' to tear down all deployed service chains

Then, a field 'serviceChains' follows which expects a list of one or more service chain descriptions.

Add Service Chain
--------
If you want to add a service chain, then describe it using the following key attributes:

  * 'name' the name of the service chain (string)
  * 'type' the type of a service chain (string)
  * 'networkId' the network ID of a service chain (integer)
  * 'cpuCores' the number of CPU cores to start with (integer)
  * 'maxCpuCores' the maximum number of CPU cores that this service chain can have (integer)
  * 'scale' a boolean flag that specifies whether a service chain is allowed to scale between 'cpuCores' and 'maxCpuCores'. If false, the service chain exhibits a static allocation of 'maxCpuCores'.
  * 'autoScale' a boolean flag that specifies whether scaling will be handled by the controller or the local server agent. If true, the agent undertakes to scale a service chain. Default behavior is false.
  * 'scope' the desired deployment scope of a service chain (string). Currently Metron offers 1 network-wide and 2 server-level deployments as follows:
      * Network-wide deployments with 'scope': 'network-mac' indicate that Metron uses a programmable switch to offload traffic classification and tag packets using their destination MAC addresses. Then, at the server Metron uses NIC Virtual Machine Device queues (VMDq) to match incoming packets' destination MAC address and dispatch these packets to the correct CPU core.
      * Server-level deployments with 'scope': 'server-rules' indicate that Metron uses NICs for traffic classification and dispatching, while the stateful part of a service chain still runs in software.
      * Server-level deployments with 'scope': 'server-rss' indicate that Metron relies on Receive-Side Scaling (RSS) for traffic dispatching. In this case no offloading is allowed and Metron runs the entire service chain in software. This is an option to maintain backward compatibility with regular FastClick service chains.
  * 'components' list provides a high level description of the Network Functions (NFs) that comprise a service chain. Each NF (i.e., list item) has:
      * 'name' a sensitive keyword used by subsequent JSON fields
      * 'type' can be either 'click' for Click-based NFs or 'standalone' for blackbox NFs
      * 'class' which can be chosen from the following list:
          * blackbox for any custom software stack you desire to launch
          * dpi for Deep Packet Inspection (DPI)
          * dispatcher for any (offloadable) device to CPU core traffic dispatcher (a classifier associated with hardware queues)
          * firewall for Firewall or Access Control List (ACL)
          * ids for Intrusion Detection System (IDS)
          * ips for Intrusion Prevention System (IPS)
          * ipdecrypt for IP decryption
          * ipencrypt for IP encryption
          * l2switch for L2 switch
          * l3switch for L3 switch
          * loadbalancer for Load Balancer (LB)
          * monitor for monitoring NFs
          * napt for Network Address and Port Translator
          * nat for Network Address Translator
          * proxy for Proxy
          * router for IPv4 router
          * transparent for a simple forwarding element (no processing, just I/O)
          * wanopt for Wide Area Network (WAN) optimizer
  * 'processors' list provides more details about the components of a service chain (i.e., the different NFs that comprise the service chain). Each processor (i.e., list item) has a:
      * 'name' (e.g., nf1)
      * a set of packet processing 'blocks' each being a respective Click element. Each block has:
          * 'block' name
          * 'instance' name
          * 'configArgs' string-based configuration arguments. Blackbox blocks might take EXEC 'blackbox exec path', ARGS 'blackbox arguments', and any random arguments in the form of KEY VALUE pairs.
          * 'configFile' file-based configuration. For example, an 'IPClassifier' element requires a configFile with IP classification rules encoded using Click's IPFilter/IPClassifier format. An example configFile for IPClassifier (or IPFilter) elements can be found [here][metron-example-ipclassifier].
      * 'graph' list where we describe the connections of the blocks. Graph edges are described by grouping graph vertices 'src' or 'dst' each encoded as follows:
          * 'instance' the instance name of a block
          * 'port' the port of the block (this port is a Click port)
  * 'topology' where we describe the underlying
      * 'network' as follows:
          * 'ingressPoints' the ingress device where traffic is expected to enter the service chain
              * 'deviceId' the ONOS device ID of the ingress point
              * 'portIds' a list of ONOS port IDs (integers) of the ingress point
          * 'egressPoints' the egress (set of) device(s) where traffic is expected to leave the service chain
              * 'deviceId' the ONOS device ID of the egress point
              * 'portIds' a list of ONOS port IDs (integers) of the egress point
          * 'targetDevice' a desired ONOS server device ID where the service chain will be deployed. In the case of network-wide deployments, Metron will choose to offload part of the service chain on a device that preceeds the target device.
      * 'server' encodes how the components (i.e., NFs) of a service chain are interconnected. Each connection is encoded as an edge between 'src' and 'dst' JSON fields. Each of these fields is encoded as follows:
          * 'entity' can be 'source' (indicates the traffic origin), an NF name as described above (e.g., 'nf1'), or 'destination' (indicates traffic sink)
          * 'interface' indicates a virtual interface name of the component. Entities 'source' and 'destination' do not need a virtual interface ('-' can be used), but NF entities require one.


Remove Service Chain
--------

To remove one or more service chains, then encode 'serviceChains' list as follows:

  * 'name' the service chain's name as specified in the 'add' section above
  * 'type' the service chain's type as specified in the 'add' section above
  * 'networkId' the service chain's network ID as specified in the 'add' section above

An example JSON file to remove a service chain follows:
```bash
{
    "apps" : {
        "org.onosproject.metron.apps" : {
            "remove" : {
                "serviceChains" : [
                    {
                        "name"     : "fw-8rules-hw-napt",
                        "type"     : "generic",
                        "networkId": 1
                    }
                ]
            }
        }
    }
}
```

Remove All Service Chains
-------

To remove all runnning service chains, use the simple JSON configuration below:
```bash
{
    "apps" : {
        "org.onosproject.metron.apps" : {
            "removeAll" : {
            }
        }
    }
}
```


Deploy a Metron service chain
----
Once an instance of the ONOS controller has been deployed, the Metron controller application has started, and a Metron agent has been launched, we can deploy a service chain.
For example, to deploy a server-level Firewall->NAPT service chain do:
```bash
vi $ONOS_ROOT/apps/metron/apps/apps-server-level/metron-srv-flowdir-add-singleport-fw-3rules-napt.json according to your needs (e.g., topology is important to change)
onos-netcfg <ONOS CTRL IP> $ONOS_ROOT/apps/metron/apps/apps-server-level/metron-srv-flowdir-add-singleport-fw-3rules-napt.json
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

Alternatively, you can deactivate Metron from the Applications tab of the ONOS UI.
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
	publisher    = {USENIX Association}
}
```


Getting help
----
Contact katsikas.gp at gmail.com or barbette at kth.se if you encounter any problems with Metron.

[metron-paper]: https://www.usenix.org/system/files/conference/nsdi18/nsdi18-katsikas.pdf
[metron-nsdi-page]: https://www.usenix.org/conference/nsdi18/presentation/katsikas
[onos]: https://onosproject.org/
[metron-driver]: https://github.com/opennetworkinglab/onos/tree/master/drivers/server
[metron-agent]: https://github.com/tbarbette/fastclick/tree/metron
[metron-server-apps]: https://github.com/gkatsikas/onos/tree/metron-ctrl-1.15.0/apps/metron/apps/apps-server-level
[metron-net-apps]: https://github.com/gkatsikas/onos/tree/metron-ctrl-1.15.0/apps/metron/apps/apps-network-wide
[metron-example-ipclassifier]: https://github.com/gkatsikas/onos/blob/metron-ctrl-1.15.0/apps/metron/apps/firewall/3rules-flow-dir.json
[onos-master]: https://github.com/opennetworkinglab/onos
[fastclick]: https://github.com/tbarbette/fastclick
[dpdk]: https://dpdk.org/
[onos-wiki]: https://wiki.onosproject.org/display/ONOS/Wiki+Home
[onos-dep]: https://github.com/opennetworkinglab/onos/blob/onos-1.15/README.md
