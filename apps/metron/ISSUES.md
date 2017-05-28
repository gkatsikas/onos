ISSUES
=======

  * Get rid of the relay functionality of the Topology Manager.

  * Merge inflate/deflateLoad in Orchestrator. They execute very similar code

  * Associate a service chain with input and output points in the network.

  * When a service chain is totally offloaded, ensure that the switch redirects traffic to the correct output point.

  * Fix the way DPI is handled.

  * Fix the toIPRewriterConfig of Operation.java.

  * Consider adding a new Device Type COMMODITY_SERVER in org.onosproject.net.Device.Type.
  Use this device type in the server driver to characterize a RestServerSBDevice.

  * If the NfvSynthesizer is deactived, there is no way to generate a Click configuration.
    Add a DFS in ServiceChain to generate: (i) a fully softwarerized Click configuration or
    (ii) a Click configuration with offloaded classifiers.

  * Check the value of MAX_PIPELINE_PORT_NB in Constants.java.

  * Abstract the I/O type from the application.

  * Fix warning: ServiceChainManager.java [1:1]:  Component org.onosproject.metron.impl.servicechain.ServiceChainManager is using the
    deprecated inheritance feature and inherits from org.onosproject.net.provider.AbstractListenerProviderRegistry.
    This feature will be removed in future versions.

  * Write a test for the ServiceChainManager according to the DhcpManager.

  * The StronglyConsistentServiceChainStore is currently deactivated by commenting the Service annotation above the class.
    The reason is that this version of the store does not properly store all the graph information (loses the edges for some reason).

  * The OpenFlowRuleConfiguration is superfluous as it re-implements methods that appear in RuleConfiguration.
    I should find a way to re-use RuleConfiguration without affecting the KryoNamespace builder of the distributed store.

  * Turn the binary classification tree into n-ary. This will likely reduce the time it takes to compute the traffic classes.

  * Replace the StackNode implementation with a Java Stack to offload some functionality from the ClassificationTree (e.g., push, pop, etc).


ADVICE
=======

  * Input an external configuration file with the app ID of Metron applications (org.onosproject.metron.apps) and a label `nfv`.
    To do so, use the onos-netcfg command after you boot ONOS and Metron. See the examples in the `apps` folder.

  * When you add a new processing block into impl/processing/blocks, ensure that you also update the ProcessingBlockLauncher.java.

  * In order for ONOS to be able to store large service chains in its eventually consistent distributed store, you need to modify one key timer in ONOS:

      * CoreEventDispatcher.java: `private static final long DEFAULT_EXECUTE_MS = 90_000;`
      * This is a safe value that will allow you to deploy very large service chains of up to 10000 rules.
