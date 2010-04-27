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

package org.uriplay.beans;

import java.util.Set;

import org.uriplay.media.entity.Playlist;
import org.uriplay.media.util.ChildFinder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * {@link Projector} that takes a graph of beans (Playlists and Items) and
 * removes the outer playlist.
 * 
 * e.g. if you have  a graph representing an OPML feed of RSS feeds of podcasts,
 * remove the outer OPML playlist.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class OuterCollectionRemovingProjector implements Projector {

	public Set<Object> applyTo(Set<Object> beans) {
		
		checkPreconditionsOn(beans);
		
		ChildFinder childFinder = new ChildFinder(beans);
		beans.addAll(childFinder.getChildren());
		return Sets.<Object>newHashSet(Iterables.filter(beans, childFinder));
	}
	
	private void checkPreconditionsOn(Iterable<Object> beans) {
		
		for (Object bean : beans) {
			if (bean instanceof Playlist) {
				return; // found at least one Playlist
			}
		}
		
		throw new ProjectionException("No collections found in object graph");
	}

}
