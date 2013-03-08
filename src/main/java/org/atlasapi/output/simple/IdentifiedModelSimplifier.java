package org.atlasapi.output.simple;

import java.math.BigInteger;
import java.util.Set;

import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.simple.Aliased;
import org.atlasapi.media.entity.simple.Audit;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.output.Annotation;

import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;

public abstract class IdentifiedModelSimplifier<F extends Identified, T extends Aliased> implements ModelSimplifier<F, T> {

    protected final SubstitutionTableNumberCodec idCodec = new SubstitutionTableNumberCodec();
    private final PublisherSimplifier publisherSimplifier = new PublisherSimplifier();
    
    protected void copyIdentifiedAttributesTo(Identified identified, Aliased aliased, Set<Annotation> annotations) {
        
        aliased.setUri(identified.getCanonicalUri());
        if (identified.getId() != null) {
            aliased.setId(idCodec.encode(BigInteger.valueOf(identified.getId())));
        }
        
        if (annotations.contains(Annotation.DESCRIPTION)
         || annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
            // TODO add in new aliases
            aliased.setAliases(identified.getAliasUrls());
            aliased.setCurie(identified.getCurie());
        }
        
        if (annotations.contains(Annotation.AUDIT)) {
            Audit audit = new Audit();
            audit.setLastUpdated(identified.getLastUpdated());
            audit.setEquivalenceLastUdpated(identified.getEquivalenceUpdate());
            aliased.setAudit(audit);
        }
    }

    protected PublisherDetails toPublisherDetails(Publisher publisher) {
        return publisherSimplifier.simplify(publisher);
    }
}
