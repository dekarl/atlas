package org.atlasapi.remotesite.channel4.epg;

import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;

@Deprecated
public class C4SynthesizedItemUpdater {

    private final ContentResolver contentResolver;
    private final ContentWriter contentWriter;
    private final C4SynthesizedItemMerger merger;

    public C4SynthesizedItemUpdater(ContentResolver contentResolver, ContentWriter contentWriter) {
        this.contentResolver = contentResolver;
        this.contentWriter = contentWriter;
        this.merger = new C4SynthesizedItemMerger();
    }

    public void findAndUpdateFromPossibleSynthesized(String broadcastId, Episode canonicalEpisode, String brandUri) {
        if (broadcastId == null) {
            return;
        }
        
        //try to find a synthesized item contained in the brand.
        String synthItemUri = synthItemUri(broadcastId, brandUri);
        if(findAndMergeSynthItem(canonicalEpisode, synthItemUri)) {
            return;
        }

        //try to find a new-style, synthesized item.
        String altSynthItemUri = synthesizedBrandlessEpisodeUriFrom(broadcastId);
        findAndMergeSynthItem(canonicalEpisode, altSynthItemUri);
    }

    private boolean findAndMergeSynthItem(Episode canonicalEpisode, String synthItemUri) {
        Maybe<Identified> maybeSynthEpisode = contentResolver.findByCanonicalUris(ImmutableList.of(synthItemUri)).get(synthItemUri);

        if(maybeSynthEpisode.hasValue()) {
            Episode synthEpisode = (Episode) maybeSynthEpisode.requireValue();
            //merge synthesized details into canonical item
            merger.merge(synthEpisode, canonicalEpisode);
            
            //remove details from the synthesized item leaving just a stub.
            synthEpisode.setVersions(ImmutableSet.<Version>of());
            contentWriter.createOrUpdate(synthEpisode);
            return true;
        } else {
            return false;
        }
    }

    private String synthItemUri(String broadcastId, String brandUri) {
        return brandUri+"/synthesized/"+broadcastId.substring(broadcastId.indexOf(':')+1);
    }
    
    private String synthesizedBrandlessEpisodeUriFrom(String broadcastId) {
        return "http://www.channel4.com/programmes/synthesized/" + broadcastId;
    }
}
