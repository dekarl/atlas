package org.atlasapi.remotesite.netflix;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.AttributeNotFoundException;
import org.joda.time.Duration;

public interface NetflixContentExtractor<T extends Content> {
    
    Set<T> extract(Element source, int id);
}
