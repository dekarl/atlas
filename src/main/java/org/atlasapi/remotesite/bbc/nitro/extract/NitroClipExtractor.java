package org.atlasapi.remotesite.bbc.nitro.extract;

import com.metabroadcast.atlas.glycerin.model.Clip;
import com.metabroadcast.atlas.glycerin.model.Image;
import com.metabroadcast.atlas.glycerin.model.Synopses;
import com.metabroadcast.common.time.Clock;


/**
 * Extracts a {@link org.atlasapi.media.entity.Clip Atlas Clip} from a
 * {@link Clip}.
 * 
 * The "{@link org.atlasapi.media.entity.Clip#getClipOf clip of}" field is not
 * set.
 * 
 */
public class NitroClipExtractor
    extends BaseNitroItemExtractor<Clip, org.atlasapi.media.entity.Clip> {

    public NitroClipExtractor(Clock clock) {
        super(clock);
    }

    @Override
    protected org.atlasapi.media.entity.Clip createContent(NitroItemSource<Clip> source) {
        return new org.atlasapi.media.entity.Clip();
    }

    @Override
    protected String extractPid(NitroItemSource<Clip> source) {
        return source.getProgramme().getPid();
    }

    @Override
    protected String extractTitle(NitroItemSource<Clip> source) {
        return source.getProgramme().getTitle();
    }

    @Override
    protected Synopses extractSynopses(NitroItemSource<Clip> source) {
        return source.getProgramme().getSynopses();
    }

    @Override
    protected Image extractImage(NitroItemSource<Clip> source) {
        return source.getProgramme().getImage();
    }

    @Override
    protected String extractMediaType(NitroItemSource<Clip> source) {
        return source.getProgramme().getMediaType();
    }
    
}
