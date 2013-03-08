package org.atlasapi.remotesite.facebook;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.remotesite.FixedResponseHttpClient;
import org.junit.Test;

import com.google.common.io.Resources;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.social.facebook.FacebookInteracter;

public class FacebookAdapterTest {

    @Test
    public void testFetchesFacebookPage() {
        
        String requestUrl = "https://graph.facebook.com/7608631709";
        SimpleHttpClient httpClient = FixedResponseHttpClient.respondTo(requestUrl+"?", Resources.getResource(getClass(), "house.json"));
        FacebookInteracter interacter = new FacebookInteracter(httpClient, null);
        
        FacebookAdapter adapter = new FacebookAdapter(interacter, null);
        
        Brand fetched = adapter.fetch(requestUrl);
        
        assertThat(fetched.getCanonicalUri(), is("http://graph.facebook.com/7608631709"));
        assertThat(fetched.getTitle(), is("House"));
        assertThat(fetched.getDescription(), startsWith("DR. GREGORY HOUSE (Laurie), devoid of bedside manner"));
        // TODO new alias
        assertThat(fetched.getAliasUrls(), hasItem("http://www.facebook.com/House"));
        assertThat(fetched.getAliasUrls(), hasItem("http://graph.facebook.com/House"));
    }

}
