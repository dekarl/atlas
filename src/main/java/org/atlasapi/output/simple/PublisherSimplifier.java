package org.atlasapi.output.simple;

import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.simple.PublisherDetails;

public class PublisherSimplifier {

    public  PublisherDetails simplify(Publisher publisher) {

        if (publisher == null) {
            return null;
        }
        
        PublisherDetails details = new PublisherDetails(publisher.key());
        
        if (publisher.country() != null) {
            details.setCountry(publisher.country().code());
        }
        
        details.setName(publisher.title());
        return details;
    }
}
