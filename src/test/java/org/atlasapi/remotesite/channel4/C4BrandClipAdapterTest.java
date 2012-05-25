package org.atlasapi.remotesite.channel4;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.FixedResponseHttpClient;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.time.SystemClock;

public class C4BrandClipAdapterTest extends TestCase {
    
    SimpleHttpClient client = new FixedResponseHttpClient(
            ImmutableMap.<String, String>builder()  
            .put("https://pmlsc.channel4.com/pmlsd/hestons-mission-impossible/video.atom", fileContentsFromResource("hestons-mission-impossible.atom"))
            .build());

    C4AtomApiClient apiClient = new C4AtomApiClient(client, "https://pmlsc.channel4.com/pmlsd/", Optional.<String>absent());

    
	private String fileContentsFromResource(String resourceName)  {
        try {
            return Files.toString(new File(Resources.getResource(getClass(), resourceName).getFile()), Charsets.UTF_8);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return null;
    }
	   
	public void testExtractingClips() throws Exception {
		
		List<Clip> clips = new C4BrandClipAdapter(apiClient, new SystemClock()).fetch("http://www.channel4.com/programmes/hestons-mission-impossible");
		assertEquals(5, clips.size());
		
		for (Clip clip: clips) {
		    assertNotNull(clip.getCanonicalUri());
		    assertNotNull(clip.getTitle());
		    assertEquals(MediaType.VIDEO, clip.getMediaType());
		    assertFalse(clip.getIsLongForm());
		    
		    assertFalse(clip.getVersions().isEmpty());
		    for (Version version: clip.getVersions()) {
		        assertFalse(version.getManifestedAs().isEmpty());
		        for (Encoding encoding: version.getManifestedAs()) {
		            assertEquals(2, encoding.getAvailableAt().size());
		            
		            Location link = locationOfType(encoding.getAvailableAt(), TransportType.LINK);
		            assertNotNull(link);
		            assertNotNull(link.getUri());
		            
		            Location embed = locationOfType(encoding.getAvailableAt(), TransportType.EMBED);
		            assertNotNull(embed);
                    assertNull(embed.getUri());
                    assertNotNull(embed.getEmbedId());
                    assertTrue(embed.getEmbedCode().contains(embed.getEmbedId()));
                    assertEquals(TransportSubType.BRIGHTCOVE, embed.getTransportSubType());
		        }
		    }
		}
	}
	
	private Location locationOfType(Iterable<Location> locations, TransportType type) {
	    for (Location location: locations) {
	        if (type.equals(location.getTransportType())) {
	            return location;
	        }
	    }
	    return null;
	}
}
