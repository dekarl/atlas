package org.atlasapi.remotesite;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.scheduling.ScheduledTask;


public class CreateYouTubeContentGroupTask extends ScheduledTask {

    private static final String CONTENT_GROUP_URI = "http://lyrabird.bbc.co.uk/youtube";
    private final ContentResolver contentResolver;
    private final ContentGroupWriter contentGroupWriter;

    public CreateYouTubeContentGroupTask(ContentResolver contentResolver, 
            ContentGroupWriter contentGroupWriter) {
        
        this.contentResolver = checkNotNull(contentResolver);
        this.contentGroupWriter = checkNotNull(contentGroupWriter);
    }
    
    @Override
    protected void runTask() {
        Item ided = (Item) contentResolver.findByCanonicalUris(ImmutableSet.of("http://coyote.metabroadcast.com/Okmj0xxpqxM"))
                                .getFirstValue()
                                .requireValue();
        ContentGroup contentGroup = new ContentGroup(CONTENT_GROUP_URI, Publisher.BBC_LYREBIRD);
        contentGroup.addContent(ided.childRef());
        contentGroupWriter.createOrUpdate(contentGroup);
    }

}
