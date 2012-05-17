package org.atlasapi.remotesite.channel4;

import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;

import com.metabroadcast.common.time.Clock;
import com.sun.syndication.feed.atom.Entry;

public class C4BrandClipExtractor extends BaseC4EpisodeExtractor implements ContentExtractor<Entry, Clip> {

    private final C4AtomEntryVersionExtractor versionExtractor = new C4AtomEntryVersionExtractor();

    public C4BrandClipExtractor(Clock clock) {
        super(clock);
    }

    @Override
    public Clip extract(Entry entry) {
        String clipUri = C4AtomApi.clipUri(entry);

        if (clipUri == null) {
            return null;
        }

        Clip clip = new Clip();
        clip.setCanonicalUri(clipUri);
        clip.setPublisher(Publisher.C4);
        
        clip.setLastUpdated(clock.now());

        String fourOdUri = C4AtomApi.fourOdUri(entry);
        if (fourOdUri != null) {
            clip.addAlias(fourOdUri);
        }

        clip.setTitle(title(entry));
        clip.setMediaType(MediaType.VIDEO);
        clip.setSpecialization(Specialization.TV);
        clip.setIsLongForm(false);

        clip.setDescription(description(entry));

        addImages(entry, clip);
        
        clip.addVersion(versionExtractor.extract(entry));

        return clip;
    }
    
    @Override
    protected Element getMedia(Entry source) {
        return C4AtomApi.mediaGroup(source);
    }

    private String description(Entry entry) {
        com.sun.syndication.feed.atom.Content description = entry.getSummary();
        if (description == null) {
            return null;
        }
        return description.getValue();
    }

    private String title(Entry entry) {
        com.sun.syndication.feed.atom.Content title = entry.getTitleEx();
        if (title == null) {
            return null;
        }
        return title.getValue();
    }

}
