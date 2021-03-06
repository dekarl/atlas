/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */


package org.atlasapi.remotesite.dailymotion;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.html.HtmlDescriptionOfItem;
import org.atlasapi.remotesite.html.HtmlDescriptionSource;

import com.metabroadcast.common.media.MimeType;

public class DailyMotionItemGraphExtractor implements ContentExtractor<HtmlDescriptionSource, Item>  {

	@Override
	public Item extract(HtmlDescriptionSource src) {
		
		String itemUri = src.getUri();
		
		Encoding encoding = encoding();
		encoding.addAvailableAt(embedLocation(src.getItem()));
		encoding.addAvailableAt(linkLocation(itemUri));
		
		Version version = new Version();
		version.addManifestedAs(encoding);
		
		Item item = item(itemUri, src.getItem());
		item.addVersion(version);

		return item;
	}
	
	private Item item(String itemUri, HtmlDescriptionOfItem htmlItem) {
		Item item = new Item();
		item.setCanonicalUri(itemUri);
		item.setTitle(htmlItem.getTitle());
		item.setDescription(htmlItem.getDescription());
		item.setPublisher(Publisher.DAILYMOTION);
		item.setThumbnail(htmlItem.getThumbnail());
		item.setCurie(PerPublisherCurieExpander.CurieAlgorithm.DM.compact(itemUri));
		return item;
	}
	
	private Encoding encoding() {
		Encoding encoding = new Encoding();
		encoding.setDataContainerFormat(MimeType.APPLICATION_XSHOCKWAVEFLASH);
		return encoding;
	}
	
	private Location linkLocation(String itemUri) {
		Location location = new Location();
		location.setTransportType(TransportType.LINK);
		location.setUri(itemUri);
		return location;
	}

	private Location embedLocation(HtmlDescriptionOfItem item) {
		Location location = new Location();
		location.setTransportType(TransportType.EMBED);
		location.setEmbedCode(embedCode(item.getVideoSource()));
		return location;
	}

	private String embedCode(String videoSource) {
		return "<object width=\"480\" height=\"381\"><param name=\"movie\" value=\"" + videoSource + "&related=0\">" +
				"</param><param name=\"allowFullScreen\" value=\"true\"></param><param name=\"allowScriptAccess\" value=\"always\">" +
				"</param><embed src=\"" + videoSource + "&related=0\" type=\"application/x-shockwave-flash\" width=\"480\" height=\"381\" " +
				"allowFullScreen=\"true\" allowScriptAccess=\"always\"></embed></object>";
	}
}
