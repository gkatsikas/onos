/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.metron.impl.processing;

import org.onosproject.metron.api.processing.ProcessingBlockClass;
import org.onosproject.metron.api.processing.ProcessingBlockInterface;

import org.onosproject.metron.impl.processing.blocks.CheckIpHeader;
import org.onosproject.metron.impl.processing.blocks.Classifier;
import org.onosproject.metron.impl.processing.blocks.DecIpTtl;
import org.onosproject.metron.impl.processing.blocks.Discard;
import org.onosproject.metron.impl.processing.blocks.EtherEncap;
import org.onosproject.metron.impl.processing.blocks.EtherMirror;
import org.onosproject.metron.impl.processing.blocks.FromBlackboxDevice;
import org.onosproject.metron.impl.processing.blocks.FromDevice;
import org.onosproject.metron.impl.processing.blocks.FromDpdkDevice;
import org.onosproject.metron.impl.processing.blocks.FromSnortDevice;
import org.onosproject.metron.impl.processing.blocks.IpAddrPairRewriter;
import org.onosproject.metron.impl.processing.blocks.IpAddrRewriter;
import org.onosproject.metron.impl.processing.blocks.IpClassifier;
import org.onosproject.metron.impl.processing.blocks.IpFilter;
import org.onosproject.metron.impl.processing.blocks.IpRewriter;
import org.onosproject.metron.impl.processing.blocks.LookupIpRouteMp;
import org.onosproject.metron.impl.processing.blocks.MarkIpHeader;
import org.onosproject.metron.impl.processing.blocks.OpenFlowClassifier;
import org.onosproject.metron.impl.processing.blocks.Print;
import org.onosproject.metron.impl.processing.blocks.Queue;
import org.onosproject.metron.impl.processing.blocks.SimpleEthernetClassifier;
import org.onosproject.metron.impl.processing.blocks.StoreEtherAddress;
import org.onosproject.metron.impl.processing.blocks.Strip;
import org.onosproject.metron.impl.processing.blocks.TcpRewriter;
import org.onosproject.metron.impl.processing.blocks.ToBlackboxDevice;
import org.onosproject.metron.impl.processing.blocks.ToDevice;
import org.onosproject.metron.impl.processing.blocks.ToDpdkDevice;
import org.onosproject.metron.impl.processing.blocks.ToSnortDevice;
import org.onosproject.metron.impl.processing.blocks.UdpRewriter;
import org.onosproject.metron.impl.processing.blocks.Unstrip;

/**
 * Given a particular processing block class, return an
 * instance of the respective block.
 */
public final class ProcessingBlockLauncher {

    private ProcessingBlockLauncher() {}

    /**
     * Maps a processing block to an instance of this block.
     * TODO: Abstract Linux/DPDK/Netmap I/O
     *
     * @param blockClass the class of the processing block
     * @param instanceId the ID of the processing block's instance
     * @param instanceConf the configuration parameters of the processing block's instance
     * @param instanceConfFile the configuration file of the processing block
     * @return an instance of the requested processing block
     */
    public static ProcessingBlockInterface getInstance(
            ProcessingBlockClass blockClass,
            String instanceId,
            String instanceConf,
            String instanceConfFile) {

        if        (blockClass == ProcessingBlockClass.CHECK_IP_HEADER) {
            return new CheckIpHeader(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.CLASSIFIER) {
            return new Classifier(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.DEC_IP_TTL) {
            return new DecIpTtl(instanceId, instanceConf, instanceConfFile);
        } else if ((blockClass == ProcessingBlockClass.DIRECT_IP_LOOKUP) ||
                   (blockClass == ProcessingBlockClass.LINEAR_IP_LOOKUP) ||
                   (blockClass == ProcessingBlockClass.LOOKUP_IP_ROUTE_MP) ||
                   (blockClass == ProcessingBlockClass.RADIX_IP_LOOKUP) ||
                   (blockClass == ProcessingBlockClass.STATIC_IP_LOOKUP)) {
            return new LookupIpRouteMp(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.DISCARD) {
            return new Discard(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.ETHER_ENCAP) {
            return new EtherEncap(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.ETHER_MIRROR) {
            return new EtherMirror(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.FROM_BLACKBOX_DEVICE) {
            return new FromBlackboxDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.FROM_DEVICE) {
            return new FromDevice(instanceId, instanceConf, instanceConfFile);
        } else if ((blockClass == ProcessingBlockClass.FROM_DPDK_DEVICE) ||
                   (blockClass == ProcessingBlockClass.FROM_NET_FRONT) ||
                   (blockClass == ProcessingBlockClass.POLL_DEVICE)) {
            return new FromDpdkDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.FROM_SNORT_DEVICE) {
            return new FromSnortDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.IP_ADDR_PAIR_REWRITER) {
            return new IpAddrPairRewriter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.IP_ADDR_REWRITER) {
            return new IpAddrRewriter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.IP_CLASSIFIER) {
            return new IpClassifier(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.IP_FILTER) {
            return new IpFilter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.IP_REWRITER) {
            return new IpRewriter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.MARK_IP_HEADER) {
            return new MarkIpHeader(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.OPENFLOW_CLASSIFIER) {
            return new OpenFlowClassifier(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.PRINT) {
            return new Print(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.QUEUE) {
            return new Queue(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.SIMPLE_ETHERNET_CLASSIFIER) {
            return new SimpleEthernetClassifier(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.STORE_ETHER_ADDRESS) {
            return new StoreEtherAddress(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.STRIP) {
            return new Strip(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.TCP_REWRITER) {
            return new TcpRewriter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.TO_BLACKBOX_DEVICE) {
            return new ToBlackboxDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.TO_DEVICE) {
            return new ToDevice(instanceId, instanceConf, instanceConfFile);
        } else if ((blockClass == ProcessingBlockClass.TO_DPDK_DEVICE) ||
                   (blockClass == ProcessingBlockClass.TO_NET_FRONT)) {
            return new ToDpdkDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.TO_SNORT_DEVICE) {
            return new ToSnortDevice(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.UDP_REWRITER) {
            return new UdpRewriter(instanceId, instanceConf, instanceConfFile);
        } else if (blockClass == ProcessingBlockClass.UNSTRIP) {
            return new Unstrip(instanceId, instanceConf, instanceConfFile);
        } else {
            return null;
        }
    }

}
