package org.atlasapi.remotesite.channel4.epg;

import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class C4EpgRelatedLinkBrandUpdaterTest {

    private final Mockery context = new Mockery();
    private final C4BrandUpdater backingUpdater = context.mock(C4BrandUpdater.class);
    
    private final C4EpgRelatedLinkBrandUpdater updater = new C4EpgRelatedLinkBrandUpdater(backingUpdater);
    
    @Test(expected=IllegalArgumentException.class)
    public void testDoesntUpdateInvalidUriAndThrowsException() {
        context.checking(new Expectations(){{
            never(backingUpdater).createOrUpdateBrand(with(any(String.class)));
        }});
        updater.createOrUpdateBrand("this is not a URI");
    }

    @Test()
    public void testUpdatesBrandForBrandOnlyRelatedLink() {
        checkUpdates("http://pmlsc.channel4.com/pmlsd/freshly-squeezed.atom", 
            "http://www.channel4.com/programmes/freshly-squeezed");
    }
    
    @Test()
    public void testUpdatesBrandForSeriesRelatedLink() {
        checkUpdates("http://pmlsc.channel4.com/pmlsd/freshly-squeezed/episode-guide/series-1.atom", 
                "http://www.channel4.com/programmes/freshly-squeezed");
    }
    @Test()
    public void testUpdatesBrandForEpisodeRelatedLink() {
        checkUpdates("http://pmlsc.channel4.com/pmlsd/freshly-squeezed/episode-guide/series-1/episode-3.atom", 
                "http://www.channel4.com/programmes/freshly-squeezed");
    }

    private void checkUpdates(String input, final String delegate) {
        context.checking(new Expectations(){{
            one(backingUpdater).createOrUpdateBrand(delegate);
        }});
        updater.createOrUpdateBrand(input);
    }

}
