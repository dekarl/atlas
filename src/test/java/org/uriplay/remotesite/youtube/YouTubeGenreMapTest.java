/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.remotesite.youtube;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Set;

import org.uriplay.genres.GenreMap;

import junit.framework.TestCase;

import com.google.common.collect.Sets;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class YouTubeGenreMapTest extends TestCase {
	
	GenreMap genreMap = new YouTubeGenreMap();
	
	public void testMapsGenresFromYoutubeGenresToUriplayGenres() throws Exception {
		
		Set<String> genres = Sets.newHashSet("http://uriplay.org/genres/youtube/comedy");
		assertThat(genreMap.map(genres), hasItem("http://uriplay.org/genres/uriplay/comedy"));
	}
	
	public void testIncludesBothOriginalAndMappedGenres() throws Exception {
		
		Set<String> genres = Sets.newHashSet("http://uriplay.org/genres/youtube/comedy");
		Set<String> mappedGenres = genreMap.map(genres);
		assertThat(mappedGenres.size(), is(2));
		assertThat(mappedGenres, hasItem("http://uriplay.org/genres/youtube/comedy"));
	}
	
	public void testIsNotCaseSensitive() throws Exception {
		
		Set<String> genres = Sets.newHashSet("http://uriplay.org/genres/youtube/Comedy");
		Set<String> mappedGenres = genreMap.map(genres);
		assertThat(mappedGenres.size(), is(2));
		assertThat(mappedGenres, hasItem("http://uriplay.org/genres/youtube/Comedy"));
		assertThat(mappedGenres, hasItem("http://uriplay.org/genres/uriplay/comedy"));
	}
	
	public void testReturnsGenresAsInputForUnknownGenres() throws Exception {
		
		Set<String> genres = Sets.newHashSet("http://example.com/genres/unknown");
		assertThat(genreMap.map(genres), is(genres));
	}
	
}
