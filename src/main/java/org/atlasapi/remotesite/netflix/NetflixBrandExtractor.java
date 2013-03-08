package org.atlasapi.remotesite.netflix;

import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getAlias;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getCertificates;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getDescription;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getGenres;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPeople;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getPublisher;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getTitle;
import static org.atlasapi.remotesite.netflix.NetflixContentExtractionHelper.getYear;

import java.util.Set;

import nu.xom.Element;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Specialization;

import com.google.common.collect.ImmutableSet;

public class NetflixBrandExtractor implements NetflixContentExtractor<Brand> {

    public static final String BRAND_URL_PREFIX = "http://gb.netflix.com/shows/";

    @Override
    public Set<Brand> extract(Element source, int id) {
        Brand brand = new Brand();

        brand.setCanonicalUri(BRAND_URL_PREFIX + id);

        brand.setTitle(getTitle(source));
        brand.setYear(getYear(source));
        brand.setDescription(getDescription(source));
        brand.setGenres(getGenres(source));
        brand.setPeople(getPeople(source));
        brand.setCertificates(getCertificates(source));
        // TODO new alias
        brand.addAliasUrl(getAlias(source));
        brand.setPublisher(getPublisher());
        brand.setSpecialization(Specialization.TV);

        return ImmutableSet.of(brand);
    }
}
