package org.atlasapi.remotesite.channel4.epg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;

import com.google.common.base.Preconditions;

public class C4EpgRelatedLinkBrandUpdater implements C4BrandUpdater {

    private final Pattern brandUriPattern = Pattern.compile("http://.+\\.channel4\\.com/[^/]+/([^./]+).*");

    private final C4BrandUpdater delegate;
    
    public C4EpgRelatedLinkBrandUpdater(C4BrandUpdater delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public Brand createOrUpdateBrand(String uri) {
        Matcher matcher = brandUriPattern.matcher(uri);
        Preconditions.checkArgument(matcher.matches(), "Invalid URI for brand fetch: " + uri);
        
        return delegate.createOrUpdateBrand("http://www.channel4.com/programmes/"+matcher.group(1));
    }

    @Override
    public boolean canFetch(String uri) {
        return brandUriPattern.matcher(uri).matches();
    }

}
