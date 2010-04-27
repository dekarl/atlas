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


package org.uriplay.remotesite.ted;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jherd.remotesite.http.CommonsHttpClient;
import org.jherd.remotesite.http.RemoteSiteClient;
import org.uriplay.remotesite.html.HtmlDescriptionOfItem;
import org.uriplay.remotesite.html.HtmlNavigator;

public class TedTalkClient implements RemoteSiteClient<HtmlDescriptionOfItem>  {

	private final RemoteSiteClient<Reader> client;

	public TedTalkClient(RemoteSiteClient<Reader> client) {
		this.client = client;
	}

	public TedTalkClient() {
		this(new CommonsHttpClient());
	}

	public HtmlDescriptionOfItem get(String uri) throws Exception {
		Reader in = client.get(uri);
		
		HtmlNavigator html = new HtmlNavigator(in);
		HtmlDescriptionOfItem item = new HtmlDescriptionOfItem();

		item.setTitle(html.metaTagContents("title"));
		item.setDescription(html.metaTagContents("description"));
		item.setVideoSource(extractMp4LocationUriFrom(html));
		item.setThumbnail("http://ted.streamguys.net/TEDTalksvideo_tile_144.jpg");
		String flashFileUri = extractFlashFileFrom(html);
		item.setFlashFile(flashFileUri);
		item.setThumbnail(extractThumbnailUriFrom(html));
		return item;
	}

	private String extractThumbnailUriFrom(HtmlNavigator html) {
		return html.linkTarget("image_src");
	}

	private String extractFlashFileFrom(HtmlNavigator html) {
		String script = html.firstElementOrNull("//script[contains(., 'hs:')]").getValue();
		if (script != null) {
			Pattern pattern = Pattern.compile("hs:\\\"(.*?)\\.flv");
			Matcher matcher = pattern.matcher(script);
			if (matcher.find()) {
				return "http://video.ted.com/" + matcher.group(1) + ".flv";
			}
		} 
		return null;
	}

	private String extractMp4LocationUriFrom(HtmlNavigator html) {
		String src = html.firstElementOrNull("//a[contains(., 'Watch high-res video (MP4)')]").getAttributeValue("href");
		if (src != null) {
			return "http://www.ted.com" + src;
		} else {
			return null;
		}
	}

}
