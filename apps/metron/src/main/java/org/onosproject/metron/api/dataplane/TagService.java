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

package org.onosproject.metron.api.dataplane;

import org.onosproject.metron.api.classification.trafficclass.TrafficClassInterface;
import org.onosproject.metron.api.structures.Pair;

import org.onosproject.drivers.server.devices.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.RxFilterValue;

import java.net.URI;
import java.util.Set;
import java.util.Map;

/**
 * Services provided by a Tag Manager.
 */
public interface TagService {

    /**
     * Different stata of the load balancing process.
     */
    public static final int LB_OK                      =  0;
    public static final int LB_INVALID_GROUP           = -1;
    public static final int LB_NO_GROUP_CAN_BE_SPLIT   = -2;
    public static final int LB_NO_GROUPS_CAN_BE_MERGED = -3;
    public static final int LB_NO_AVAILABLE_TAG        = -4;

    /********************************** Tagging Mechanisms. ********************************/

    /**
     * Get the tagging mechanism for a specific group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return the tagging mechanism of this group of traffic classes
     */
    RxFilter getTaggingMechanismOfTrafficClassGroup(URI tcGroupId);

    /**
     * Set the tagging mechanism of a specific traffic class.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param rxFilter the tagging mechanism of this group of traffic classes
     */
    void setTaggingMechanismOfTrafficClassGroup(URI tcGroupId, RxFilter rxFilter);

    /************************************ Available Tags. **********************************/

    /**
     * Returns the available tags of all groups of traffic classes.
     *
     * @return map of group traffic class IDs to their set of available tags
     */
    Map<URI, Set<RxFilterValue>> availableTags();

    /**
     * Returns the available tags of a particular group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return set of available tags for a particular group of traffic classes
     */
    Set<RxFilterValue> availableTagsOfTrafficClassGroup(URI tcGroupId);

    /**
     * Sets the available tags of a particular group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param tags set of available tags for a particular group of traffic classes
     */
    void setAvailableTagsOfTrafficClassGroup(URI tcGroupId, Set<RxFilterValue> tags);

    /*********************************** Dynamic Tag Maps. *********************************/

    /**
     * Returns the used tags of all groups of traffic classes.
     *
     * @return map of group traffic class IDs to their
     *         active tags used by different subgroups
     */
    Map<URI, Map<RxFilterValue, Set<TrafficClassInterface>>> tagMap();

    /**
     * Returns the map of tags to traffic class subgroups of a given group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return map of traffic class subgroups to their active tags
     */
    Map<RxFilterValue, Set<TrafficClassInterface>> tagMapOfTrafficClassGroup(URI tcGroupId);

    /**
     * Returns the traffic class subgroup associated with a given tag.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param tag the tag associated with a subgroup of traffic classes
     * @return traffic class subgroup that uses the given tag
     */
    Set<TrafficClassInterface> getTrafficClassGroupWithTag(URI tcGroupId, RxFilterValue tag);

    /**
     * Returns whether there is a traffic class subgroup associated with a given tag.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param tag the tag associated with a subgroup of traffic classes
     * @return boolean association status
     */
    boolean hasTrafficClassGroupWithTag(URI tcGroupId, RxFilterValue tag);

    /**
     * Associates a tag with a particular group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param subgroup a subgroup of traffic classes of this group
     * @param tag the tag to be associated with the subgroup of traffic classes
     */
    void mapTagToTrafficClassSubgroup(
        URI                        tcGroupId,
        Set<TrafficClassInterface> subgroup,
        RxFilterValue              tag
    );

    /**
     * Removes the mapping between a subgroup of traffic classes with a tag.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param tag the tag to be removed
     */
    void removeTagOfTrafficClassSubgroup(URI tcGroupId, RxFilterValue tag);

    /**
     * Returns the number of active CPU cores used by a given group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return number of active CPU cores of this group of traffic classes
     */
    int getNumberOfActiveCoresOfTrafficClassGroup(URI tcGroupId);

    /**
     * Goes through the active (i.e., used) tags for a specific group
     * of traffic classes and returns the first active tag.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return the first used tag
     */
    RxFilterValue getFirstUsedTagOfTrafficClassGroup(URI tcGroupId);

    /**
     * Goes through the available tags for a specific group of traffic classes
     * and returns the first unused tag.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return the first unused tag
     */
    RxFilterValue getFirstUnusedTagOfTrafficClassGroup(URI tcGroupId);

    /**
     * Performs all necessary actions to properly estabish the tagging
     * mechanisms for a new group of traffic classes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @param tcGroup the group of traffic classes
     * @param rxFilter the tagging mechanism of this group of traffic classes
     * @param rxFilterValues the tags available for this group of traffic classes
     */
    void establishTaggingForTrafficClassGroup(
        URI                        tcGroupId,
        Set<TrafficClassInterface> tcGroup,
        RxFilter                   rxFilter,
        Set<RxFilterValue>         rxFilterValues
    );

    /*********************************** Load Balancing. ***********************************/

    /**
     * Goes through the traffic classe of a given group and implements
     * a deflation scheme that splits these traffic classes into
     * 2 subgroups. Serves for load balancing purposes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return returns the set of traffic classes that require tag modification
     *         alogn with the actual tag
     */
    Pair<RxFilterValue, Set<TrafficClassInterface>> deflateTrafficClassGroup(
        URI tcGroupId
    );

    /**
     * Goes through the traffic classes of a given group and implements
     * an inflation scheme that merges these traffic classes into
     * larger groups. Serves for load balancing purposes.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return returns the set of traffic classes that require tag modification
     *         alogn with the actual tag
     */
    Pair<RxFilterValue, Set<TrafficClassInterface>> inflateTrafficClassGroup(
        URI tcGroupId
    );

    /**
     * Given a load balancing action taken by the inflate/deflate
     * methods above, get a more descriptive message of what actually happened.
     *
     * @param tcGroupId the ID of group of traffic classes
     * @return load balancing message
     */
    String getLbStatusOfTrafficClassGroup(URI tcGroupId);

}
