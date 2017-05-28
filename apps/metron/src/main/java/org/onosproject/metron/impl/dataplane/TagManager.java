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
import org.onosproject.drivers.server.devices.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.MacRxFilterValue;
import org.onosproject.drivers.server.devices.MplsRxFilterValue;
import org.onosproject.drivers.server.devices.VlanRxFilterValue;
import org.onosproject.drivers.server.devices.RxFilterValue;

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
import static org.onosproject.metron.api.dataplane.TagService.LB_OK;
import static org.onosproject.metron.api.dataplane.TagService.LB_INVALID_GROUP;
import static org.onosproject.metron.api.dataplane.TagService.LB_NO_GROUP_CAN_BE_SPLIT;
import static org.onosproject.metron.api.dataplane.TagService.LB_NO_GROUPS_CAN_BE_MERGED;
import static org.onosproject.metron.api.dataplane.TagService.LB_NO_AVAILABLE_TAG;

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
                put(new Integer(LB_INVALID_GROUP), "Invalid ID for group of traffic classes");
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
     * Associates each group of traffic classes with a tagging mechanism (e.g., MPLS, VLAN, or MAC).
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
        // Register the Tag Manager with the core.
        this.appId = coreService.registerApplication(APP_NAME);

        log.info("[{}] Started", this.label());
    }

    @Deactivate
    protected void deactivate() {
        // Clean up your memory
        this.taggingMechanisms.clear();
        this.availableTags.clear();
        this.tagMap.clear();
        this.lbStatus.clear();

        log.info("[{}] Stopped", this.label());
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
        URI                        tcGroupId,
        Set<TrafficClassInterface> tcGroup,
        RxFilter                   rxFilter,
        Set<RxFilterValue>         rxFilterValues) {
        // Inform the tag manager about the the tagging mechanism of this group of traffic classes
        this.setTaggingMechanismOfTrafficClassGroup(tcGroupId, rxFilter);

        // Inform the tag manager about the tags that can be used by this group of traffic classes
        this.setAvailableTagsOfTrafficClassGroup(tcGroupId, rxFilterValues);

        // Allocate a tag for this group of traffic classes
        RxFilterValue tag = this.getFirstUnusedTagOfTrafficClassGroup(tcGroupId);
        checkNotNull(
            tag,
            "[" + this.label() + "] Failed to allocate a new tag for traffic class group: " + tcGroupId
        );

        // Map this group of traffic classes to this tag
        this.mapTagToTrafficClassSubgroup(tcGroupId, tcGroup, tag);
    }

    /*********************************** Load Balancing. ***********************************/

    @Override
    public Pair<RxFilterValue, Set<TrafficClassInterface>> deflateTrafficClassGroup(URI tcGroupId) {
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
            tcGroupSize = entry.getValue().size();

            // It is important to find a splittable group that has more than one traffic classes
            if (tcGroupSize < MIN_GROUP_SIZE_TO_DEFLATE) {
                log.info("[{}] Not enough traffic classes to perform deflation", this.label());
                continue;
            }

            tag = entry.getKey();
            tcGroup = entry.getValue();

            log.info("[{}] Deflating group {} with {} TCs", this.label(), tcGroupId, tcGroupSize);

            break;
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
        Set<TrafficClassInterface> newTcGroup = Sets.<TrafficClassInterface>newConcurrentHashSet();

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

            // Add the copy to the new group
            newTcGroup.add(newT);

            // Remove the original one
            tcIterator.remove();

            i++;
        }

        // Update the size of the initial group
        tcGroupSize = tcGroup.size();
        int newTcGroupSize =  newTcGroup.size();

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

        log.info("[{}] 1st group with {} TCs and tag: {}", this.label(), tcGroupSize,    tag);
        log.info("[{}] 2nd group with {} TCs and tag: {}", this.label(), newTcGroupSize, newTag);

        // Successful load balancing
        this.setLbStatusOfTrafficClassGroup(
            tcGroupId,
            this.getLoadBalancingReport(tcGroupId, LB_OK)
        );

        // Return the affected traffic classes along with their tag
        return new Pair<RxFilterValue, Set<TrafficClassInterface>>(newTag, newTcGroup);
    }

    @Override
    public Pair<RxFilterValue, Set<TrafficClassInterface>> inflateTrafficClassGroup(URI tcGroupId) {
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);
        if (groupTagMap == null) {
            this.setLbStatusOfTrafficClassGroup(
                tcGroupId,
                this.getLoadBalancingReport(tcGroupId, LB_INVALID_GROUP)
            );
            return null;
        }

        /**
         * We merge the 2 smallest groups to minimize the impact
         * on the number of flows being modified.
         */
        Map<Integer, RxFilterValue> smallest = this.getTwoSmallestSubgroups(tcGroupId);

        RxFilterValue smallestTag    = smallest.get(new Integer(0));
        RxFilterValue secSmallestTag = smallest.get(new Integer(1));

        if ((smallestTag == null) || (secSmallestTag == null)) {
            this.setLbStatusOfTrafficClassGroup(
                tcGroupId,
                this.getLoadBalancingReport(tcGroupId, LB_NO_GROUPS_CAN_BE_MERGED)
            );
            return null;
        }

        // Get the subgroups
        Set<TrafficClassInterface> smallestTcEntries = groupTagMap.get(smallestTag);
        Set<TrafficClassInterface> secSmallestTcEntries = groupTagMap.get(secSmallestTag);

        log.info(
            "[{}] 1st smallest group with {} TCs and tag {}",
            this.label(), smallestTcEntries.size(), smallestTag
        );
        log.info(
            "[{}] 2nd smallest group with {} TCs and tag {}",
            this.label(), secSmallestTcEntries.size(), secSmallestTag
        );

        // Merge the smallest with the second smallest
        Set<TrafficClassInterface> copyOfSmallestTcEntries =
                Sets.<TrafficClassInterface>newConcurrentHashSet(smallestTcEntries);
        secSmallestTcEntries.addAll(copyOfSmallestTcEntries);

        // Remove the smallest to minimize the migration impact
        this.removeTagOfTrafficClassSubgroup(tcGroupId, smallestTag);

        log.info(
            "[{}] Both groups are merged into a group with {} TCs and tag {}",
            this.label(), secSmallestTcEntries.size(), secSmallestTag
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
        return new Pair<RxFilterValue, Set<TrafficClassInterface>>(secSmallestTag, copyOfSmallestTcEntries);
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
     * @param message the load balancing status
     */
    private void setLbStatusOfTrafficClassGroup(URI tcGroupId, String message) {
        this.lbStatus.put(tcGroupId, message);
    }

    /************************************** Internal services. ***********************************/

    /**
     * Allocates memory for the data structures.
     */
    private void init() {
        this.taggingMechanisms = new ConcurrentHashMap<URI, RxFilter>();
        this.availableTags     = new ConcurrentHashMap<URI, Set<RxFilterValue>>();
        this.tagMap            = new ConcurrentHashMap<URI, Map<RxFilterValue, Set<TrafficClassInterface>>>();
        this.lbStatus          = new ConcurrentHashMap<URI, String>();
    }

    /**
     * Given a load balancing status code returned by the inflate/deflate
     * methods above, get a more descriptive message of what actually happened.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param status load balancing status report
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
     * @param tcGroupId the ID of group of traffic classes
     * @return the tags of the 2 smallest subgroups in a group of traffic classes
     */
    private Map<Integer, RxFilterValue> getTwoSmallestSubgroups(URI tcGroupId) {
        Map<Integer, RxFilterValue> outGroups = new ConcurrentHashMap<Integer, RxFilterValue>();

        // Get the set of subgroups of this group of traffic classes
        Map<RxFilterValue, Set<TrafficClassInterface>> groupTagMap = this.tagMapOfTrafficClassGroup(tcGroupId);

        int smallestGroupSize = -1;
        int secSmallestGroupSize = -1;

        RxFilterValue smallestTag = null;
        RxFilterValue secSmallestTag = null;

        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            RxFilterValue tag = entry.getKey();
            Set<TrafficClassInterface> tcGroup = entry.getValue();
            int tcGroupSize = tcGroup.size();

            if (smallestGroupSize < 0) {
                smallestTag = tag;
                smallestGroupSize = tcGroupSize;
            } else if (secSmallestGroupSize < 0) {
                if (smallestGroupSize > tcGroupSize) {
                    secSmallestTag = smallestTag;
                    secSmallestGroupSize = smallestGroupSize;

                    smallestTag = tag;
                    smallestGroupSize = tcGroupSize;
                } else {
                    secSmallestTag = tag;
                    secSmallestGroupSize = tcGroupSize;
                }
            } else {
                if (smallestGroupSize > tcGroupSize) {
                    secSmallestTag = smallestTag;
                    secSmallestGroupSize = smallestGroupSize;

                    smallestTag = tag;
                    smallestGroupSize = tcGroupSize;
                } else if (secSmallestGroupSize > tcGroupSize) {
                    secSmallestTag = tag;
                    secSmallestGroupSize = tcGroupSize;
                }
            }
        }

        outGroups.put(new Integer(0), smallestTag);
        outGroups.put(new Integer(1), secSmallestTag);

        return outGroups;
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

        log.info("[{}] Group of traffic classes with ID: {}", this.label(), tcGroupId);

        for (Map.Entry<RxFilterValue, Set<TrafficClassInterface>> entry : groupTagMap.entrySet()) {
            RxFilterValue tag = entry.getKey();
            Set<TrafficClassInterface> tcGroup = entry.getValue();

            log.info("[{}] \t Tag {} --> {} traffic classes", this.label(), tag, tcGroup.size());
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
            log.info("[{}] TAG {}", this.label(), mt.toString());
        } else if (tag instanceof VlanRxFilterValue) {
            VlanRxFilterValue vt = (VlanRxFilterValue) tag;
            log.info("[{}] TAG {}", this.label(), vt.toString());
        } else if (tag instanceof MplsRxFilterValue) {
            MplsRxFilterValue mt = (MplsRxFilterValue) tag;
            log.info("[{}] TAG {}", this.label(), mt.toString());
        } else {
            log.error("[{}] Unknown tag: {}", this.label(), tag);
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
