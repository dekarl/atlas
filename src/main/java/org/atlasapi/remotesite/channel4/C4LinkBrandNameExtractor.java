package org.atlasapi.remotesite.channel4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;

class C4LinkBrandNameExtractor {

    private final Pattern channel4Link = Pattern.compile(
        "https?://.+\\.channel4\\.com/[^/]+/([^./]+).*"
    );
    
    private static final String PROGRAMMES_BASE = "http://www.channel4.com/programmes/";
    
    public Optional<String> brandNameFrom(String url) {
       Matcher matcher = channel4Link.matcher(url);
       if (matcher.matches()) {
           return Optional.of(matcher.group(1));
       }
       return Optional.absent();
    }
    
    public Optional<String> canonicalBrandUriFrom(String url) {
        Optional<String> brandName = brandNameFrom(url);
        if (brandName.isPresent()) {
            return Optional.of(PROGRAMMES_BASE + brandName.get());
        }
        return Optional.absent();
    }
}
