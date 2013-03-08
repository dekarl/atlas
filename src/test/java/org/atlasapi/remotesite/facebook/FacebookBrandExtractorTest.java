package org.atlasapi.remotesite.facebook;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.CrewMember;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class FacebookBrandExtractorTest {

    private final FacebookBrandExtractor extractor = new FacebookBrandExtractor();
    
    @Test
    public void testExtractsBrandFromPageWithSimpleDetails() {
        FacebookPage page = new FacebookPage();
        page.setId("12345");
        page.setName("name");
        page.setPlotOutline("plotoutline");
        page.setLink("a link");
        page.setWebsite("http://www.awebsite.com, http://anotherwebsite.com");
        page.setUsername("username");
        
        
//        page.setDirectedBy("A Director, Another Person");
//        page.setStarring("Film Star, T.V. Star");
//        page.setWrittenBy("A Writer, Another Writer");
        
        Brand brand = extractor.extract(page);
        
        assertThat(brand.getCanonicalUri(), is("http://graph.facebook.com/12345"));
        assertThat(brand.getTitle(), is("name"));
        assertThat(brand.getDescription(), is("plotoutline"));
        // TODO new alias
        assertThat(brand.getAliasUrls(), hasItems(
            "a link", 
            "http://www.awebsite.com", 
            "http://anotherwebsite.com",
            "http://graph.facebook.com/username"
        ));
        
    }

    @Test
    public void testExtractsUrlFromTradedoublerLink() {
        
        FacebookPage page = new FacebookPage();
        page.setWebsite("http://www.channel4.com/programmes/father-ted?cntsrc=sn_father-ted_facebook I-tunes: http://clkuk.tradedoubler.com/click?p=23708&a=1614398&url=http%3A%2F%2Fitunes.apple.com%2FWebObjects%2FMZStore.woa%2Fwa%2FviewTVShow%3Fid%3D281913183%26uo%3D6%26partnerId%3D2003 ");

        Brand brand = extractor.extract(page);

        // TODO new alias
        assertThat(brand.getAliasUrls(), hasItems(
            "http://www.channel4.com/programmes/father-ted", 
            "http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewTVShow?id=281913183&uo=6&partnerId=2003"
        ));
        assertThat(brand.getAliasUrls(), not(hasItems("I-tunes:")));
    }

    @Test
    public void testExtractsUrlFromWebsite() {
        
        FacebookPage page = new FacebookPage();
        page.setWebsite("http://www.FOX.com/glee; http://www.twitter.com/GLEEonFOX; http://www.myspace.com/gleeonfox");
        Brand brand = extractor.extract(page);
        
     // TODO new alias
        assertThat(brand.getAliasUrls(), hasItems(
            "http://www.FOX.com/glee", 
            "http://www.twitter.com/GLEEonFOX",
            "http://www.myspace.com/gleeonfox"
        ));
        
    }
    
    @Test
    public void testExtractsActors() {
        assertExtractedActors("Hugh Laurie as Dr. Gregory House, " +
        		"Robert Sean Leonard as Dr. James Wilson, " +
        		"Omar Epps as Dr. Eric Foreman, " +
        		"Jesse Spencer (Dr. Robert Chase), " +
        		"Peter Jacobson as Dr. Chris Taub, " +
        		"Odette Annable as Dr. Jessica Adams & " +
        		"Charlyne Yi as Dr. Chi Park",
    		ImmutableMap.<String,String>builder()
    		    .put("Hugh Laurie", "Dr. Gregory House")
    		    .put("Robert Sean Leonard", "Dr. James Wilson")
    		    .put("Omar Epps", "Dr. Eric Foreman")
    		    .put("Jesse Spencer", "Dr. Robert Chase")
    		    .put("Peter Jacobson", "Dr. Chris Taub")
    		    .put("Odette Annable", "Dr. Jessica Adams")
    		    .put("Charlyne Yi", "Dr. Chi Park")
    		    .build()
        );
        
        Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();

        String gleeCast = "Dianna Agron, Chris Colfer, Jessalyn Gilsig, " +
                "Jane Lynch, Jayma Mays, Kevin McHale, " +
                "Lea Michele, Cory Monteith, Heather Morris," +
                " Matthew Morrison, Mike O'Malley, Amber Riley, " +
                "Naya Rivera, Mark Salling, Jenna Ushkowitz";

        Map<String, String> expected = Maps.newHashMap();
        for (String actor : splitter.split(gleeCast)) {
            expected.put(actor, null);
        }
        
        assertExtractedActors(gleeCast, expected);
        
        String fatherTedCast = "Dermot Morgan, Ardal O'Hanlon, " +
        		"Frank Kelly, Pauline McLynn";
        
        expected = Maps.newHashMap();
        for (String actor : splitter.split(fatherTedCast)) {
            expected.put(actor, null);
        }
        
        assertExtractedActors(fatherTedCast, expected);
        
        String simpsonsCast = "Giving voice to the Simpsons and other " +
        		"Springfield citizens are Dan Castellaneta, Julie Kavner, " +
        		"Nancy Cartwright, Yeardley Smith, Harry Shearer, " +
        		"and Hank Azaria.";
        
        expected = Maps.newHashMap();
        //expected.put("Dan Castellaneta", null); //Extra-points.
        expected.put("Julie Kavner", null);
        expected.put("Nancy Cartwright", null);
        expected.put("Yeardley Smith", null);
        expected.put("Harry Shearer", null);
        expected.put("Hank Azaria", null);
        
        assertExtractedActors(simpsonsCast, expected);
    }

    private void assertExtractedActors(String in, Map<String, String> out) {
        FacebookPage page = new FacebookPage();
        page.setStarring(in);
        
        Brand brand = extractor.extract(page);
        
        List<Actor> people = brand.actors();
        
        Map<String, String> extracted = Maps.newHashMap();
        for (Actor actor : people) {
            extracted.put(actor.name(), actor.character());
        }
        
        assertThat(extracted.keySet(), is(out.keySet()));
        for (Entry<String, String> expected : out.entrySet()) {
            assertThat(extracted.get(expected.getKey()), is(expected.getValue()));
        }
    }
    
    @Test
    public void testExtractsWriters() {
        FacebookPage page = new FacebookPage();
        page.setWrittenBy("Matt Groening (creator/executive producer), " +
        		"James L. Brooks, Al Jean,  Ian Maxtone-Graham,  " +
        		"Matt Selman,  John Frink (executive producers)"
		);

        Brand brand = extractor.extract(page);
        
        List<String> names = Lists.transform(brand.people(), new Function<CrewMember, String>() {
            @Override
            public String apply(@Nullable CrewMember input) {
                return input.name();
            }
        });
        
        assertThat(names, hasItems("Matt Groening","James L. Brooks",
            "Al Jean", "Ian Maxtone-Graham","Matt Selman", "John Frink"));
    }
}
