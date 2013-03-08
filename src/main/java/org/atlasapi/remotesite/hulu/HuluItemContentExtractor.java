package org.atlasapi.remotesite.hulu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.FetchException;
import org.atlasapi.remotesite.html.HtmlNavigator;
import org.codehaus.jackson.map.ObjectMapper;
import org.jaxen.JaxenException;
import org.jdom.Element;
import org.joda.time.Duration;

import com.google.common.collect.Sets;
import com.metabroadcast.common.intl.Countries;

public class HuluItemContentExtractor implements ContentExtractor<HtmlNavigator, Episode> {
    
	private static final String SOCIAL_FEED = "SocialFeed.facebook_template_data.watch = ";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern INFO_PATTERN = Pattern.compile("^.*Season\\s*(\\d+).*Ep\\.\\s*(\\d+)\\|(\\d+):(\\d+):?(\\d*)\\|.*$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES);

    @SuppressWarnings("unchecked")
    @Override
    public Episode extract(HtmlNavigator source) {
        try {
            Episode item = new Episode();
            item.setPublisher(Publisher.HULU);
            
            Version version = new Version();
            Encoding encoding = new Encoding();
            Location embedLocation = new Location();
            Location linkLocation = new Location();
        
            embedLocation.setPolicy(usPolicy());
            linkLocation.setPolicy(usPolicy());
            encoding.addAvailableAt(embedLocation);
            encoding.addAvailableAt(linkLocation);
            version.addManifestedAs(encoding);
            
            item.addVersion(version);
            item.setTags(getTags(source));

            List<Element> elements = source.allElementsMatching("//div[@class='description-shift']");
            if (elements.isEmpty()) {
                elements = source.allElementsMatching("//div[@class='description']");
            }
            for (Element infoElement: elements) {
                Matcher matcher = INFO_PATTERN.matcher(infoElement.getValue());
                if (matcher.matches() && matcher.groupCount() > 3) {
                    item.setSeriesNumber(Integer.valueOf(matcher.group(1)));
                    item.setEpisodeNumber(Integer.valueOf(matcher.group(2)));

                    Integer first = Integer.valueOf(matcher.group(3));
                    Integer second = Integer.valueOf(matcher.group(4));
                    Integer third = null;
                    if (matcher.group(5) != null && matcher.group(5).length() > 0) {
                        third = Integer.valueOf(matcher.group(5));
                    }

                    Integer duration = 0;
                    if (third != null) {
                        duration = first * 60 * 60 + second * 60 + third;
                    } else {
                        duration = first * 60 + second;
                    }
                    version.setDuration(Duration.standardSeconds(duration));
                    break;
                }
            }
            
            Element shareElement = source.firstElementOrNull("//div[@id='share-copy-code-div']/script");
            if (shareElement != null && shareElement.getValue() != null) {
                String embedCode = shareElement.getValue();
                String search = "var failsafeEmbedCode = '";
                int index = embedCode.indexOf(search) + search.length();
                
                embedCode = embedCode.substring(index, embedCode.length()).trim();
                embedLocation.setEmbedCode(embedCode.substring(0, embedCode.length()-2));
            }

            for (Element element : source.allElementsMatching("//body/script")) {
                String value = element.getValue();

                if (value.startsWith(SOCIAL_FEED)) {
                    Map<String, Object> attributes;
                    try {
                        attributes = mapper.readValue(value.replace(SOCIAL_FEED, ""), HashMap.class);
                    } catch (Exception e) {
                        throw new FetchException("Unable to map JSON values", e);
                    }

                    if (attributes.containsKey("video") && attributes.get("video") instanceof Map) {
                        Map<String, Object> videoAttributes = (Map<String, Object>) attributes.get("video");
                        if (videoAttributes.containsKey("video_title")) {
                            item.setTitle((String) videoAttributes.get("video_title"));
                        }
                        embedLocation.setUri((String) videoAttributes.get("video_src"));
                        embedLocation.setAvailable(true);
                        embedLocation.setTransportIsLive(true);
                        embedLocation.setTransportType(TransportType.EMBED);
                        
                        item.setThumbnail((String) videoAttributes.get("preview_img"));
                        item.setImage((String) videoAttributes.get("preview_img"));
                    }

                    String videoLink = (String) attributes.get("video_link");
                    Matcher matcher = HuluRssAdapter.URI_PATTERN.matcher(videoLink);
                    if (matcher.matches()) {
                        String uri = matcher.group(1);
                        item.setCanonicalUri(uri);
                        item.setCurie("hulu:" + uri.replace(HuluItemAdapter.BASE_URI, ""));
                    }
                    
                    linkLocation.setUri(videoLink);
                    linkLocation.setAvailable(true);
                    linkLocation.setTransportType(TransportType.LINK);
                    
                    // TODO new alias
                    item.addAliasUrl(videoLink);
                    item.setDescription((String) attributes.get("video_description"));
                    item.setIsLongForm(true);
                    
                    item.setContainer(brandFrom(attributes));
                    break;
                }
            }

            return item;
        } catch (JaxenException e) {
            throw new FetchException("Unable to navigate HTML document", e);
        }
    }

	private Policy usPolicy() {
		Policy policy = new Policy();
		policy.addAvailableCountry(Countries.US);
		return policy;
	}

	private Brand brandFrom(Map<String, Object> attributes) {
		String brandUri = (String) attributes.get("show_link");
		Brand brand = new Brand(brandUri, PerPublisherCurieExpander.CurieAlgorithm.HULU.compact(brandUri), Publisher.HULU);
		brand.setDescription((String) attributes.get("show_description"));
		brand.setTitle((String) attributes.get("show_title"));
		return brand;
	}
	
	public static Set<String> getTags(HtmlNavigator source) throws JaxenException {
	    List<Element> elements = source.allElementsMatching("//*[@class='tags-content-cell']/a");
        Set<String> tags = Sets.newHashSet();
        for (Element element : elements) {
            tags.add(element.getAttributeValue("href").replace("/search/search_tag?query=", ""));
        }
        return tags;
	}
}
