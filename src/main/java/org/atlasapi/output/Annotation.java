package org.atlasapi.output;

import java.util.Set;

import com.google.common.base.Functions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.metabroadcast.common.text.MoreStrings;

public enum Annotation {

    DESCRIPTION,
    EXTENDED_DESCRIPTION,
    BRAND_SUMMARY,   
    SERIES_SUMMARY,
    SUB_ITEMS,
    CLIPS,
    PEOPLE,
    TOPICS,
    SEGMENT_EVENTS,
    RELATED_LINKS,
    KEY_PHRASES,
    BROADCASTS,   
    LOCATIONS,
    FIRST_BROADCAST,
    NEXT_BROADCAST,
    AVAILABLE_LOCATIONS;
    
    private static final ImmutableSet<Annotation> defaultAnnotations = ImmutableSet.of(
        DESCRIPTION,
        EXTENDED_DESCRIPTION,
        SUB_ITEMS,
        BROADCASTS,
        LOCATIONS,
        PEOPLE,
        CLIPS
    );
    public static final BiMap<String, Annotation> LOOKUP = HashBiMap.create(Maps.uniqueIndex(ImmutableList.copyOf(Annotation.values()), Functions.compose(MoreStrings.TO_LOWER, Functions.toStringFunction())));
    
    public static final Set<Annotation> defaultAnnotations() {
        return defaultAnnotations;
    }
    
}