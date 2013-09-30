package org.atlasapi.remotesite.pa.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.remotesite.pa.PaHelper;
import org.joda.time.Interval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class PaFeaturesProcessor {
    
    private static final String TODAY_CONTENT_GROUP_URI = "http://pressassocation.com/features/tvpicks";
    private static final String ALL_CONTENT_GROUP_URI = "http://pressassocation.com/features/tvpicks/all";
    private static final Ordering<Broadcast> BY_BROADCAST_DATE = Ordering.natural().onResultOf(Broadcast.TO_TRANSMISSION_TIME);
    
    private final ContentResolver contentResolver;
    private final ContentGroupWriter contentGroupWriter;
    private final ContentGroupResolver contentGroupResolver;

    private Interval upcomingPickInterval;
    private ContentGroup todayContentGroup;
    private ContentGroup allFeaturedContentEverContentGroup;
    
    public PaFeaturesProcessor(ContentResolver contentResolver, ContentGroupResolver contentGroupResolver, ContentGroupWriter contentGroupWriter) {
        this.contentResolver = contentResolver;
        this.contentGroupWriter = contentGroupWriter;
        this.contentGroupResolver = contentGroupResolver;
    }

    public void prepareUpdate(Interval upcomingPickInterval) {
        this.upcomingPickInterval = upcomingPickInterval;
        this.todayContentGroup = getOrCreateContentGroup(TODAY_CONTENT_GROUP_URI);
        this.allFeaturedContentEverContentGroup = getOrCreateContentGroup(ALL_CONTENT_GROUP_URI);
    }
    
    private ContentGroup getOrCreateContentGroup(String uri) {
        ResolvedContent resolvedContent = contentGroupResolver.findByCanonicalUris(ImmutableList.of(uri));
        if (resolvedContent.get(uri).hasValue()) {
            ContentGroup contentGroup = (ContentGroup) resolvedContent.get(uri).requireValue();
            contentGroup.setContents(ImmutableList.<ChildRef>of());
            return contentGroup;
        } else {
            return new ContentGroup(uri, Publisher.PA_FEATURES);
        }
    }
    
    public void process(String programmeId) {
        Map<String, Identified> resolvedContent = contentResolver.findByCanonicalUris(
                ImmutableSet.of(PaHelper.getFilmUri(programmeId), PaHelper.getEpisodeUri(programmeId),
                        PaHelper.getAlias(programmeId))).asResolvedMap();
        ArrayList<Identified> resolved = Lists.newArrayList(resolvedContent.values());
        Collections.sort(resolved, new PaIdentifiedComparator());
        Item item = (Item) Iterables.getFirst(resolved, null);
        
        Broadcast broadcast = BY_BROADCAST_DATE.min(Iterables.concat(Iterables.transform(item.getVersions(), Version.TO_BROADCASTS)));
        if (upcomingPickInterval.contains(broadcast.getTransmissionTime())) {
            todayContentGroup.addContent(item.childRef()); 
        }
        allFeaturedContentEverContentGroup.addContent(item.childRef());
    }
    
    public void finishUpdate() {
        contentGroupWriter.createOrUpdate(todayContentGroup);
        contentGroupWriter.createOrUpdate(allFeaturedContentEverContentGroup);
    }
    
}
