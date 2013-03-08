var aliasMapping = new Object();
aliasMapping[escapeRegExp("http://xmltv.radiotimes.com/channels/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://radiotimes.com/films/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("	http://api.soundcloud.com/tracks/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://www.five.tv/channels/") + "([a-z0-9/]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.five.tv/") + "([a-z0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("https://pdb.five.tv/internal/seasons/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("https://pdb.five.tv/internal/shows/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(" + escapeRegExp("http://www.five.tv") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://tvblob.com/channel/") + "([a-z0-9/]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("tag:www.channel4.com,") + "([0-9]{4})" + escapeRegExp(":/programmes/") + "([a-z0-4-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.channel4.com/") + "([a-z0-4]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(" + escapeRegExp("http://www.channel4.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping["(" + escapeRegExp("http://www.4music.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("tag:www.e4.com,") + "([0-9]{4})" + escapeRegExp(":/programmes/") + "([a-z0-4-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.e4.com/") + "([a-z0-4]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(" + escapeRegExp("http://www.e4.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping["(" + escapeRegExp("http://film4.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping["(" + escapeRegExp("http://www.hulu.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping["(" + escapeRegExp("http://www.youtube.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping["(" + escapeRegExp("http://www.seesaw.com") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://pressassociation.com/channels/") + "([0-9]*)"] = {"namespace" : "pa:channel-id", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/stations/") + "([0-9]*)"] = {"namespace" : "pa:station-id", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/regions/") + "([0-9]*)"] = {"namespace" : "pa:region-id", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/platforms/") + "([0-9]*)"] = {"namespace" : "pa:platform-id", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/series/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/brands/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/episodes/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://pressassociation.com/films/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://ref.atlasapi.org/platforms/pressassociation.com/") + "([0-9]*)"] = {"namespace" : "pa:platform-id", "value" : "1"};
aliasMapping[escapeRegExp("http://ref.atlasapi.org/regions/pressassociation.com/") + "([0-9]*)"] = {"namespace" : "pa:region-id", "value" : "1"};
aliasMapping[escapeRegExp("http://ref.atlasapi.org/channels/pressassociation.com/stations/") + "([0-9]*)"] = {"namespace" : "pa:station-id", "value" : "1"};

aliasMapping[escapeRegExp("http://ref.atlasapi.org/channels/") + "([0-9]*)"] = {"namespace" : "pa:channel-id", "value" : "1"};

aliasMapping[escapeRegExp("http://people.atlasapi.org/pressassociation.com/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("https://preproduction-movida.bebanjo.net/api/title_groups/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("https://preproduction-movida.bebanjo.net/api/titles/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://bt.com/titles/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://bt.com/title_groups/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://gb.netflix.com/seasons/") + "([0-9-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://api.netflix.com/catalog/titles/series/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://gb.netflix.com/shows/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://gb.netflix.com/episodes/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://gb.netflix.com/movies/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://api.netflix.com/catalog/titles/movies/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://api.netflix.com/catalog/titles/programs/") + "([0-9]*)" + escapeRegExp("/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://youview.com/service/") + "([0-9]*)"] = {"namespace" : "youview:service", "value" : "1"};
aliasMapping[escapeRegExp("http://youview.com/scheduleevent/") + "([0-9]*)"] = {"namespace" : "youview:scheduleevent", "value" : "1"};
aliasMapping[escapeRegExp("http://youview.com/") + "([0-9]*)"] = {"namespace" : "youview:scheduleevent", "value" : "1"};
aliasMapping[escapeRegExp("http://youview.com/programme/") + "([0-9]*)"] = {"namespace" : "youview:programme", "value" : "1"};

aliasMapping[escapeRegExp("http://previewnetworks.com/film/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://(www\.)?imdb.com/title/") + "([a-z0-9]*)" + escapeRegExp("/") + "?"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://voila.metabroadcast.com/pressassociation.com/brands/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://voila.metabroadcast.com/london.metabroadcast.com/paralympics.channel4.com/the-sports/") + "([a-z0-9-]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://voila.metabroadcast.com/graph.facebook.com/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://london.metabroadcast.com/paralympics.channel4.com/") + "([a-z0-9-]*)" + escapeRegExp("/") + "([a-z0-9-]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://london.metabroadcast.com/paralympics.channel4.com/the-sports/") + "([a-z0-9-]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://lovefilm.com/shows/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://lovefilm.com/seasons/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://magpie.metabroadcast.com/pdb.five.tv/internal/shows/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://magpie.metabroadcast.com/itv.com/brand/") + "([0-9A-Z]*)" + escapeRegExp("/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://magpie.metabroadcast.com/itv.com/brand/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://magpie.metabroadcast.com/www.channel4.com/programmes/") + "([0-9a-z-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://magpie.metabroadcast.com/www.itv.com/itvplayer/video/?Filter=") + "([0-9a-zA-Z%]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://itv.com/brand/") + "([0-9A-Z]*)" + escapeRegExp("/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://itv.com/brand/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://itv.com/series/") + "([0-9A-Z_]*)" + escapeRegExp("/") + "([0-9A-Z-]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://itv.com/series/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.itv.com/") + "([a-z]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.itv.com/channels/") + "([a-z0-9#]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.itv.com/entertainment/") + "([a-z]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://wsarchive.bbc.co.uk/brands/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

//http://www.bbc.co.uk/cbbc/sja/ /channel/program? vs /genre/brand
aliasMapping[escapeRegExp("http://www.bbc.co.uk/") + "([a-z]*)" + escapeRegExp("/") + "([a-z]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.bbc.co.uk/") + "([a-z]*)" + escapeRegExp("/") + "([a-z]*)" + escapeRegExp("/episodes/") + "([a-z]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.bbc.co.uk/iplayer/search/?q=") + "([a-zA-Z0-9%]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.bbc.co.uk/programmes/") + "([0-9a-z]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.bbc.co.uk/people/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://www.bbc.co.uk/services/") + "([a-z0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://bbc.co.uk/i/") + "([a-z0-9]*)" + escapeRegExp("/")] = {"namespace" : "", "value" : "1"};
aliasMapping["(" + escapeRegExp("http://www.bbc.co.uk/iplayer") + ")"] = {"namespace" : "", "value" : "1"};

aliasMapping[escapeRegExp("http://devapi.bbcredux.com/channels/") + "([a-z0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://g.bbcredux.com/programme/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
aliasMapping[escapeRegExp("http://g.bbcredux.com/programme/") + "([a-z0-9]*)" + escapeRegExp("/") + "([a-z0-9/]*)" + escapeRegExp("/") + "2011-07-12/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};

aliasMapping["(^BBW_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^BBC_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^AAL_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^DCO_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^DCO_[^_]+$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^NAT_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^UKT_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^UKV_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^HIS_[^_]+_?[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^COM_[^_]+_(s|S)?\d+)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^GNX_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^T_[^_]+_[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};
aliasMapping["(^([A-Z0-9][a-z0-9]*\s)+[sS]?[0-9]*$)"] = {"namespace" : "", "value" : "1"};

aliasMapping["(^[a-zA-Z0-9_-]*$)"] = {"namespace" : "", "value" : "1"};

//URLS FROM FACEBOOK
var fbAliasMapping = new Object();
fbAliasMapping[escapeRegExp("http://graph.facebook.com/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
fbAliasMapping[escapeRegExp("http://www.facebook.com/pages/") + "([A-Za-z0-9-%]*)" + escapeRegExp("/") + "([0-9]*)"] = {"namespace" : "", "value" : "1"};
fbAliasMapping[escapeRegExp("http://www.facebook.com/") + "([A-Za-z0-9%]*)"] = {"namespace" : "", "value" : "1"};

// CATCH ALL THE THINGS!
fbAliasMapping["(^http.*$)"] = {"namespace" : "", "value" : "1"};

//db.children.find().forEach(function(c){findAllAliases(c);});
//db.topLevelItems.find().forEach(function(c){findAllAliases(c);});
//db.containers.find().forEach(function(c){findAllAliases(c);});
//db.programmeGroups.find().forEach(function(c){findAllAliases(c);});
//db.channels.find().forEach(function(c){findAllAliases(c);});
//db.channelGroups.find().forEach(function(c){findAllAliases(c);});
//db.people.find().forEach(function(c){findAllAliases(c);});

function findAllAliases(c) {
	var isFB = false;
	if (c.publisher == "graph.facebook.com") {
		isFB = true;
	}
	
	if (typeof(c._id) != "object") {
		if (isFB) {
			findFBAlias(c._id);
		} else {
			findAlias(c._id);
		}
//	} else {
//		print(c._id);
	}
	if (c.uri) {
		if (isFB) {
			findFBAlias(c.uri);
		} else {
			findAlias(c.uri);
		}
	}
	if (c.aliases) {
		for (var i = 0; i < c.aliases.length; i++) {
			if (isFB) {
				findFBAlias(c.aliases[i]);
			} else {
				findAlias(c.aliases[i]);
			}
		}
	}
}

function escapeRegExp(unescaped){
	return unescaped.replace(/([.*+?^=!:${}()|[\]\/\\])/g, "\\$1");
}

function findAlias(uri) {
	// iterate over all regexes, try and match one
	var found = false;
	uri = uri.trim();
	
	for (var aliasRegex in aliasMapping) {
		var re = new RegExp(aliasRegex);
		if (re.test(uri)) {
			found = true;
			break;
		} 
	}
	
	if (!found) {
		print("no match found for: " + uri);
	}
}

function findFBAlias(uri) {
	// iterate over all regexes, try and match one
	var found = false;
	uri = uri.trim();
	
	for (var aliasRegex in fbAliasMapping) {
		var re = new RegExp(aliasRegex);
		if (re.test(uri)) {
			found = true;
			break;
		} 
	}
	
	if (!found) {
		print("(FB) no match found for: " + uri);
	}
}