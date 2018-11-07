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

package org.onosproject.metron.impl.dataplane;

// Metron libraries

import org.onosproject.metron.api.common.Constants;
import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.dataplane.TagService;
import org.onosproject.metron.api.structures.Pair;

import org.onosproject.metron.impl.classification.trafficclass.TrafficClass;

// ONOS libraries
import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.nic.MacRxFilterValue;
import org.onosproject.drivers.server.devices.nic.MplsRxFilterValue;
import org.onosproject.drivers.server.devices.nic.VlanRxFilterValue;
import org.onosproject.drivers.server.devices.nic.RxFilterValue;

// Apache libraries
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

// Guava
import com.google.common.collect.Sets;

// Other libraries
import org.slf4j.Logger;

// Java libraries
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the runtime association of traffic classes to tags
 * and handles load imbalances by manipulating those tags.
 */
@Component(immediate = true)

@Service
public final class TagManager implements TagService {

    private static final Logger log = getLogger(TagManager.class);

    public static final Map<Integer, String> LB_STATUS_MAP = Collections.unmodifiableMap(
            new ConcurrentHashMap<Integer, String>() {
                {
                    put(new Integer(LB_OK), "Successful load balancing for group of traffic classes");
                    put(new Integer(LB_INVALID_GROUP), "No tags for group of traffic classes");
                    put(new Integer(LB_NO_GROUP_CAN_BE_SPLIT), "No splittable group of traffic classes");
                    put(new Integer(LB_NO_GROUPS_CAN_BE_MERGED), "No traffic classes subgroups to merge");
                    put(new Integer(LB_NO_AVAILABLE_TAG), "Failed to allocate a new tag for traffic class group");
                }
            }
    );

    // Currently static de/inflation factors are supported
    private static final int DEFLATION_FACTOR = 2;
    private static final int INFLATION_FACTOR = 2;
    private static final int MIN_GROUP_SIZE_TO_DEFLATE = 2;

    // CPU information
    private static final int NO_ACTIVE_CORES = 0;

    /**
     * Application information for the Tag Manager.
     */
    private static final String APP_NAME = Constants.SYSTEM_PREFIX + ".tag.manager";
    private static final String COMPONET_LABEL = "Tag and Load Manager";
    private ApplicationId appId = null;

    /**
     * Associates each group of traffic classes with a tagging mechanism
     * (e.g., Rule-based, MPLS, VLAN, or MAC).
     */
    private Map<URI, RxFilter> taggingMechanisms = null;

    /**
     * Associates each group of traffic classes with a set of available tags.
     */
    private Map<URI, Set<RxFilterValue>> availableTags = null;

    /**
     * Associates each group of traffic classes with a dynamic map of tags to
     * buckets of traffic classes.
     * This data structure 'breathes' with the load of each group of
     * traffic classes.
     */
    private Map<URI, Map<RxFilterValue, Set<TrafficClassInterface>>> tagMap = null;

    /**
     * Keeps track of the load balancing status of each group of traffic classes.
     */
    private Map<URI, String> lbStatus = null;

    /**
     * The Tag Manager requires the ONOS core service to register.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    /**
     * Constructor.
     */
    public TagManager() {
        this.init();
    }

    @Activate
    protected void activate() {
        // Register the Tag Manager with the core
        this.appId = coreService.registerApplication(APP_NAME);

        log.info("[{}] Started", label());
    }

    @Deactivate
    protected void deactivate() {
        // Clean up your memory
        this.taggingMechanisms.clear();
        this.availableTags.clear();
        this.tagMap.clear();
        this.lbStatus.clear();

        log.info("[{}] Stopped", label());
    }

    /********************************** Tagging Mechanisms. ********************************/
    @Override
    public RxFilter getTaggingMechanismOfTrafficClassGroup(URI tcGroupId) {
        return this.taggingMechanisms.get(tcGroupId);
    }

    @Override
    public void setTaggingMechanismOfTrafficClassGroup(URI tcGroupId, RxFilter rxFilter) {
        this.taggingMechanisms.put(tcGroupId, rxFilter);
    }

    /************************************ Available Tags. **********************************/
    @Override
    public Map<URI, Set<RxFilterValue>> availableTags() {
        return this.availableTags;
    }

    @Override
    public Set<RxFilterValue> availableTagsOfTrafficClassGroup(URI tcGroupId) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");

        if (this.availableTags == null) {
            return null;
        }

        return this.availableTags.get(tcGroupId);
    }

    @Override
    public void setAvailableTagsOfTrafficClassGroup(URI tcGroupId, Set<RxFilterValue> tags) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");
        checkNotNull(tags, "Tags for group traffic class ID " + tcGroupId + " are NULL");

        this.availableTags.put(tcGroupId, tags);
    }

    /*********************************** Dynamic Tag Maps. *********************************/
    @Override
    public Map<URI, Map<RxFilterValue, Set<TrafficClassInterface>>> tagMap() {
        return this.tagMap;
    }

    @Override
    public Map<RxFilterValue, Set<TrafficClassInterface>> tagMapOfTrafficClassGroup(URI tcGroupId) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");

        return this.tagMap.get(tcGroupId);
    }

    @Override
    public Set<TrafficClassInterface> getTrafficClassGroupWithTag(URI tcGroupId, RxFilterValue tag) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");
        checkNotNull(tag, "Tag for group traffic class ID " + tcGroupId + " is NULL");

        if (!this.tagMap.containsKey(tcGroupId)) {
            return null;
        }

        return this.tagMap.get(tcGroupId).get(tag);
    }

    @Override
    public boolean hasTrafficClassGroupWithTag(URI tcGroupId, RxFilterValue tag) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");
        checkNotNull(tag, "Tag for group traffic class ID " + tcGroupId + " is NULL");

        if (!this.tagMap.containsKey(tcGroupId)) {
            return false;
        }

        return this.tagMap.get(tcGroupId).get(tag) != null;
    }

    @Override
    public void mapTagToTrafficClassSubgroup(URI tcGroupId, Set<TrafficClassInterface> subgroup, RxFilterValue tag) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");
        checkNotNull(subgroup, "Subgroup of traffic class group ID " + tcGroupId + " is NULL");
        checkNotNull(tag, "Tag for group traffic class ID " + tcGroupId + " is NULL");

        Map<RxFilterValue, Set<TrafficClassInterface>> groupMap = null;
        if (!this.tagMap.containsKey(tcGroupId)) {
            groupMap = new ConcurrentSkipListMap<RxFilterValue, Set<TrafficClassInterface>>();
            this.tagMap.put(tcGroupId, groupMap);
        } else {
            groupMap = this.tagMap.get(tcGroupId);
        }

        groupMap.put(tag, subgroup);
    }

    @Override
    public void removeTagOfTrafficClassSubgroup(URI tcGroupId, RxFilterValue tag) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");
        checkNotNull(tag, "Tag for group traffic class ID " + tcGroupId + " is NULL");

        if (!this.tagMap.containsKey(tcGroupId)) {
            return;
        }

        if (!this.tagMap.get(tcGroupId).containsKey(tag)) {
            return;
        }

        Set<TrafficClassInterface> subgroup = this.tagMap.get(tcGroupId).get(tag);
        subgroup.clear();

        this.tagMap.get(tcGroupId).remove(tag);
    }

    @Override
    public int getNumberOfActiveCoresOfTrafficClassGroup(URI tcGroupId) {
        checkNotNull(tcGroupId, "Group traffic class ID " + tcGroupId + " is NULL");

        if (!this.tagMap.containsKey(tcGroupId)) {
            return NO_ACTIVE_CORES;
        }

        return this.tagMap.get(tcGroupId).size();
    }

    @Override
    public RxFilterValue getFirstUsedTagOfTrafficClassGroup(URI tcGroupId) {
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);

        // There is no tag for this group
        if (groupTagMap == null) {
            return null;
        }

        RxFilterValue tag = null;
        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            tag = entry.getKey();

            // Return the first available and valid tag
            if (tag != null) {
                break;
            }
        }

        return tag;
    }

    @Override
    public RxFilterValue getFirstUnusedTagOfTrafficClassGroup(URI tcGroupId) {
        Set<RxFilterValue> availableTags = this.availableTagsOfTrafficClassGroup(tcGroupId);

        if (availableTags == null) {
            return null;
        }

        for (RxFilterValue tag : availableTags) {
            // This tag is already occupied by another group of traffic classes
            if (this.hasTrafficClassGroupWithTag(tcGroupId, tag)) {
                continue;
            }

            return tag;
        }

        return null;
    }

    @Override
    public void establishTaggingForTrafficClassGroup(
            URI tcGroupId,
            Set<TrafficClassInterface> tcGroup,
            RxFilter rxFilter,
            Set<RxFilterValue> rxFilterValues) {
        // Inform the tag manager about the the tagging mechanism of this group of traffic classes
        this.setTaggingMechanismOfTrafficClassGroup(tcGroupId, rxFilter);

        // Inform the tag manager about the tags that can be used by this group of traffic classes
        this.setAvailableTagsOfTrafficClassGroup(tcGroupId, rxFilterValues);

        // Allocate a tag for this group of traffic classes
        RxFilterValue tag = this.getFirstUnusedTagOfTrafficClassGroup(tcGroupId);
        checkNotNull(
                tag,
                "[" + label() + "] Failed to allocate a new tag for traffic class group: " + tcGroupId
        );

        // Map this group of traffic classes to this tag
        this.mapTagToTrafficClassSubgroup(tcGroupId, tcGroup, tag);
    }

    /*********************************** Load Balancing. ***********************************/

    @Override
    public Pair<RxFilterValue, Set<TrafficClassInterface>> deflateTrafficClassGroup(URI tcGroupId, int overloadedCore) {
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);
        if (groupTagMap == null) {
            this.setLbStatusOfTrafficClassGroup(
                    tcGroupId,
                    this.getLoadBalancingReport(tcGroupId, LB_INVALID_GROUP)
            );
            return null;
        }

        // Here we will store the group of traffic classes to be deflated
        Set<TrafficClassInterface> tcGroup = null;
        RxFilterValue tag = null;
        int tcGroupSize = -1;

        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            if (entry.getKey().cpuId() != overloadedCore)
                continue;

            tcGroupSize = entry.getValue().size();

            // It is important to find a splittable group that has more than one traffic classes
            if (tcGroupSize < MIN_GROUP_SIZE_TO_DEFLATE) {
                log.info("[{}] \t Not enough traffic classes to perform deflation", label());
                continue;
            }

            tag = entry.getKey();
            tcGroup = entry.getValue();

            log.info("[{}] \t Deflating group {} with {} TCs", label(), tcGroupId, tcGroupSize);
        }

        // Problem
        if ((tcGroup == null) || (tag == null)) {
            this.setLbStatusOfTrafficClassGroup(
                    tcGroupId,
                    this.getLoadBalancingReport(tcGroupId, LB_NO_GROUP_CAN_BE_SPLIT)
            );
            return null;
        }

        // Here we will store the new group of traffic classes
        Set<TrafficClassInterface> newTcGroup = Sets.newConcurrentHashSet();

        int i = 0;
        Iterator<TrafficClassInterface> tcIterator = tcGroup.iterator();

        while (tcIterator.hasNext()) {
            // Skip the first half traffic classes
            if (i >= (tcGroupSize / DEFLATION_FACTOR)) {
                tcIterator.next();
                i++;

                continue;
            }

            // Get a traffic class
            TrafficClassInterface t = tcIterator.next();

            // Create a copy since this one is going to be deleted
            TrafficClassInterface newT = new TrafficClass(t);
            newT.setId(t.id());
            newT.buildBinaryTree();

            // Add the copy to the new group
            newTcGroup.add(newT);

            // Remove the original one
            tcIterator.remove();

            i++;
        }

        // Update the size of the initial group
        tcGroupSize = tcGroup.size();
        int newTcGroupSize = newTcGroup.size();

        // Give me a new available tag for this new group of traffic classes.
        RxFilterValue newTag = this.getFirstUnusedTagOfTrafficClassGroup(tcGroupId);
        if (newTag == null) {
            this.setLbStatusOfTrafficClassGroup(
                    tcGroupId,
                    this.getLoadBalancingReport(tcGroupId, LB_NO_AVAILABLE_TAG)
            );
            return null;
        }

        // Map this new group of traffic classes to the new tag
        this.mapTagToTrafficClassSubgroup(tcGroupId, newTcGroup, newTag);

        log.info("[{}] \t 1st group with {} TCs and tag: {} (CPU {})", label(), tcGroupSize, tag, tag.cpuId());
        log.info("[{}] \t 2nd group with {} TCs and tag: {} (CPU {})", label(), newTcGroupSize, newTag, newTag.cpuId());

        // Successful load balancing
        this.setLbStatusOfTrafficClassGroup(
                tcGroupId,
                this.getLoadBalancingReport(tcGroupId, LB_OK)
        );

        // Return the affected traffic classes along with their tag
        return new Pair<RxFilterValue, Set<TrafficClassInterface>>(newTag, newTcGroup);
    }

    @Override
    public Pair<Pair<RxFilterValue, RxFilterValue>, Set<TrafficClassInterface>> inflateTrafficClassGroup(URI tcGroupId, int underloadedCore, Set<Integer> inflateCandidates) {
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);
        if (groupTagMap == null) {
            log.error("No traffic class group !");
            this.setLbStatusOfTrafficClassGroup(
                    tcGroupId,
                    this.getLoadBalancingReport(tcGroupId, LB_INVALID_GROUP)
            );
            return null;
        }

        Pair<RxFilterValue, RxFilterValue> tags = this.getSmallestSubgroups(tcGroupId, underloadedCore, inflateCandidates);

        RxFilterValue smallestTag = tags.getKey();
        RxFilterValue coreTag = tags.getValue();

        if (smallestTag == null || coreTag == null) {
            log.error("Could not find smaller tag {} or core tag {} !", smallestTag, coreTag);
            this.setLbStatusOfTrafficClassGroup(
                    tcGroupId,
                    this.getLoadBalancingReport(tcGroupId, LB_NO_GROUPS_CAN_BE_MERGED)
            );
            return null;
        }

        // Get the subgroups
        Set<TrafficClassInterface> smallestTcEntries = groupTagMap.get(smallestTag);
        Set<TrafficClassInterface> coreTcEntries = groupTagMap.get(coreTag);

        log.info(
                "[{}] \t 1st inflated group with {} TCs and tag {} for core {}",
                label(), smallestTcEntries.size(), smallestTag, smallestTag.cpuId()
        );
        log.info(
                "[{}] \t 2nd inflated group with {} TCs and tag {} for core {}",
                label(), coreTcEntries.size(), coreTag, coreTag.cpuId()
        );

        // Merge the smallest with the second smallest
        Set<TrafficClassInterface> copyOfSmallestTcEntries =
                Sets.newConcurrentHashSet(smallestTcEntries);
        coreTcEntries.addAll(copyOfSmallestTcEntries);

        // Remove the smallest to minimize the migration impact
        this.removeTagOfTrafficClassSubgroup(tcGroupId, smallestTag);

        log.info(
                "[{}] \t Both groups are merged into a group with {} TCs and tag {}",
                label(), coreTcEntries.size(), coreTag
        );

        // Successful load balancing
        this.setLbStatusOfTrafficClassGroup(
                tcGroupId,
                this.getLoadBalancingReport(tcGroupId, LB_OK)
        );

        /**
         * Return the affected traffic classes along with their tag.
         * ATTENTION: The opposite tag has to be used here! Think of it ;)
         */
        return new Pair<Pair<RxFilterValue, RxFilterValue>, Set<TrafficClassInterface>>(new Pair<>(coreTag, smallestTag), copyOfSmallestTcEntries);
    }

    @Override
    public String getLbStatusOfTrafficClassGroup(URI tcGroupId) {
        return this.lbStatus.get(tcGroupId);
    }

    /**
     * Set a descriptive message of the load balancing status
     * of a particular group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param message   the load balancing status
     */
    private void setLbStatusOfTrafficClassGroup(URI tcGroupId, String message) {
        this.lbStatus.put(tcGroupId, message + ": Traffic class ID " + tcGroupId);
    }

    /************************************** Internal services. ***********************************/

    /**
     * Allocates memory for the data structures.
     */
    private void init() {
        this.taggingMechanisms = new ConcurrentHashMap<URI, RxFilter>();
        this.availableTags = new ConcurrentHashMap<URI, Set<RxFilterValue>>();
        this.tagMap = new ConcurrentHashMap<URI, Map<RxFilterValue, Set<TrafficClassInterface>>>();
        this.lbStatus = new ConcurrentHashMap<URI, String>();
    }

    /**
     * Given a load balancing status code returned by the inflate/deflate
     * methods above, get a more descriptive message of what actually happened.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param status    load balancing status report
     * @return load balancing message
     */
    private String getLoadBalancingReport(URI tcGroupId, int status) {
        return LB_STATUS_MAP.get(new Integer(status)) + ": " + tcGroupId.toString();
    }

    /**
     * During the inflation process for a given group of traffic classes, we need to find
     * which subgroups to merge together. In order to minimize the reconfiguration impact,
     * we prefer to merge the two smallest subgroups in order to reduce the number of rules
     * to be updated. This methods returns these 2 smallest subgroups.
     *
     * @param tcGroupId         the ID of group of traffic classes
     * @param inflateCandidates
     * @return the tags of the 2 smallest subgroups in a group of traffic classes
     */
    private Pair<RxFilterValue, RxFilterValue> getSmallestSubgroups(URI tcGroupId, int excludeCore, Set<Integer> inflateCandidates) {
        // Get the set of subgroups of this group of traffic classes
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);

        int smallestGroupSize = -1;

        RxFilterValue smallestTag = null;
        RxFilterValue coreTag = null;

        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            RxFilterValue tag = entry.getKey();
            if (tag.cpuId() == excludeCore) {
                coreTag = tag;
                continue;
            }
            if (!inflateCandidates.contains(tag.cpuId()))
                continue;
            Set<TrafficClassInterface> tcGroup = entry.getValue();
            int tcGroupSize = tcGroup.size();

            if (smallestGroupSize < 0) {
                smallestTag = tag;
                smallestGroupSize = tcGroupSize;
            } else if (tcGroupSize < smallestGroupSize) {
                smallestTag = tag;
                smallestGroupSize = tcGroupSize;
            }
        }
        return new Pair<>(smallestTag, coreTag);
    }

    /**
     * Prints the map of tags of a particular group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     */
    private void printTagMapForTrafficClassGroup(URI tcGroupId) {
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);
        if (groupTagMap == null) {
            return;
        }

        log.info("[{}] Group of traffic classes with ID: {}", label(), tcGroupId);

        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            RxFilterValue tag = entry.getKey();
            Set<TrafficClassInterface> tcGroup = entry.getValue();

            log.info("[{}] \t Tag {} --> {} traffic classes", label(), tag, tcGroup.size());
        }
    }

    /**
     * Prints the value of a tag according to its type.
     *
     * @param tag tag to print
     */
    private void printTag(RxFilterValue tag) {
        if (tag instanceof MacRxFilterValue) {
            MacRxFilterValue mt = (MacRxFilterValue) tag;
            log.info("[{}] TAG {}", label(), mt.toString());
        } else if (tag instanceof VlanRxFilterValue) {
            VlanRxFilterValue vt = (VlanRxFilterValue) tag;
            log.info("[{}] TAG {}", label(), vt.toString());
        } else if (tag instanceof MplsRxFilterValue) {
            MplsRxFilterValue mt = (MplsRxFilterValue) tag;
            log.info("[{}] TAG {}", label(), mt.toString());
        } else {
            log.error("[{}] Unknown tag: {}", label(), tag);
        }
    }

    /**
     * Returns a label with the Tag Manager's identity.
     * Serves for printing.
     *
     * @return label to print
     */
    private static String label() {
        return COMPONET_LABEL;
    }

}
