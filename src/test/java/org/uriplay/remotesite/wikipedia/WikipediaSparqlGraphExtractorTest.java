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

package org.uriplay.remotesite.wikipedia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.jherd.hamcrest.Matchers.hasPropertyValue;
import static org.uriplay.remotesite.wikipedia.Constants.AARON_SORKIN_DBPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.FILM_TYPE_URL;
import static org.uriplay.remotesite.wikipedia.Constants.PERSON_TYPE_URL;
import static org.uriplay.remotesite.wikipedia.Constants.SLUMDOG_DBPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.SLUMDOG_WIKIPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.TELEVISION_SHOW_TYPE_URL;
import static org.uriplay.remotesite.wikipedia.Constants.THOMAS_SCHLAMME_DBPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.THOMAS_SCHLAMME_WIKIPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.WEST_WING_DBPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.Constants.WEST_WING_IMDB_URI;
import static org.uriplay.remotesite.wikipedia.Constants.WEST_WING_WIKIPEDIA_URI;
import static org.uriplay.remotesite.wikipedia.WikipediaSparqlSource.CONTAINED_IN_ID;
import static org.uriplay.remotesite.wikipedia.WikipediaSparqlSource.DESCRIPTION_ID;
import static org.uriplay.remotesite.wikipedia.WikipediaSparqlSource.SAMEAS_ID;
import static org.uriplay.remotesite.wikipedia.WikipediaSparqlSource.TITLE_ID;

import org.jherd.beans.Representation;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.uriplay.media.entity.Brand;
import org.uriplay.media.entity.Item;
import org.uriplay.media.entity.Playlist;
import org.uriplay.remotesite.youtube.YouTubeGraphExtractor;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * Unit test for {@link YouTubeGraphExtractor}
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class WikipediaSparqlGraphExtractorTest extends MockObjectTestCase {

	WikipediaSparqlGraphExtractor extractor = new WikipediaSparqlGraphExtractor();
	ResultSet propertiesResultSet = mock(ResultSet.class, "propertiesResultSet");
	ResultSet containedInResultSet = mock(ResultSet.class, "containedInResultSet");
	Binding binding = mock(Binding.class, "binding");
	Binding containedInBinding = mock(Binding.class, "containedInBinding");
	Model model = mock(Model.class);
	ResultBinding resultBinding = new ResultBinding(model, binding);
	ResultBinding containedInResultBinding = new ResultBinding(model, containedInBinding);
	
	WikipediaSparqlSource source;

	private void forTheWestWingTvShow() {
		resource(WEST_WING_WIKIPEDIA_URI, WEST_WING_DBPEDIA_URI, TELEVISION_SHOW_TYPE_URL, "The West Wing", "A tv show");
		containedIn(AARON_SORKIN_DBPEDIA_URI);
	}

	private void forSlumdogMillionaire() {
		resource(SLUMDOG_WIKIPEDIA_URI, SLUMDOG_DBPEDIA_URI, FILM_TYPE_URL, "Slumdog Millionaire", "A film of a book of a tv show");
	}
	
	private void forThomasSchlamme() {
		resource(THOMAS_SCHLAMME_WIKIPEDIA_URI, THOMAS_SCHLAMME_DBPEDIA_URI, PERSON_TYPE_URL, "Thomas Schlamme", "The producer of the West Wing");
	}

	private void resource(String wikipediaUri, String dbpediaUri, String type, String titleTxt, String descriptionTxt) {
		source = new WikipediaSparqlSource(wikipediaUri);
		source.setCanonicalDbpediaUri(dbpediaUri);
		source.setRootProperties(propertiesResultSet);
		source.setRootTypes(Sets.newHashSet(type));

		Model m = ModelFactory.createDefaultModel();
		final Literal title = m.createTypedLiteral(titleTxt);
		final Literal description = m.createTypedLiteral(descriptionTxt);
		
		final Resource sameAs = new ResourceImpl(WEST_WING_IMDB_URI);
		
		checking(new Expectations() {{
			exactly(2).of(propertiesResultSet).hasNext(); will(onConsecutiveCalls(returnValue(true), returnValue(false)));
			allowing(propertiesResultSet).next(); will(returnValue(resultBinding));

			one(binding).get(Var.alloc(TITLE_ID)); will(returnValue(title.asNode()));
			one(model).asRDFNode(title.asNode()); will(returnValue(title));
			
			one(binding).get(Var.alloc(DESCRIPTION_ID)); will(returnValue(description.asNode()));
			one(model).asRDFNode(description.asNode()); will(returnValue(description));
			
			one(binding).get(Var.alloc(SAMEAS_ID)); will(returnValue(sameAs.asNode()));
			one(model).asRDFNode(sameAs.asNode()); will(returnValue(sameAs));
		}});
	}
	

	private void containedIn(String aaronSorkinDbpediaUri) {
		source.setContainedInProperties(containedInResultSet);
		
		final Resource containedIn = new ResourceImpl(AARON_SORKIN_DBPEDIA_URI);
		
		checking(new Expectations() {{
			exactly(2).of(containedInResultSet).hasNext(); will(onConsecutiveCalls(returnValue(true), returnValue(false)));
			allowing(containedInResultSet).next(); will(returnValue(containedInResultBinding));

			one(containedInBinding).get(Var.alloc(CONTAINED_IN_ID)); will(returnValue(containedIn.asNode()));
			one(model).asRDFNode(containedIn.asNode()); will(returnValue(containedIn));
		}});
		
	}

	public void testCreatesPropertyValuesForRootResourceForTvShows() throws Exception {
		
		forTheWestWingTvShow();

		Representation representation = extractor.extractFrom(source);
		assertEquals(Brand.class, representation.getType(WEST_WING_WIKIPEDIA_URI));
		assertThat(representation, hasPropertyValue(WEST_WING_WIKIPEDIA_URI, "title", "The West Wing"));
		assertThat(representation, hasPropertyValue(WEST_WING_WIKIPEDIA_URI, "description", "A tv show"));
		assertThat(representation, hasPropertyValue(WEST_WING_WIKIPEDIA_URI, "aliases", Sets.newHashSet(WEST_WING_IMDB_URI, WEST_WING_DBPEDIA_URI)));
		assertThat(representation, hasPropertyValue(WEST_WING_WIKIPEDIA_URI, "containedIn", Sets.newHashSet(AARON_SORKIN_DBPEDIA_URI)));
	}
	
	public void testCreatesPropertyValuesForRootResourceForFilms() throws Exception {
		
		forSlumdogMillionaire();

		Representation representation = extractor.extractFrom(source);
		assertEquals(Item.class, representation.getType(SLUMDOG_WIKIPEDIA_URI));
		assertThat(representation, hasPropertyValue(SLUMDOG_WIKIPEDIA_URI, "title", "Slumdog Millionaire"));
		assertThat(representation, hasPropertyValue(SLUMDOG_WIKIPEDIA_URI, "description", "A film of a book of a tv show"));
	}
	
	public void testCreatesPropertyValuesForRootResourceForPeople() throws Exception {
		
		forThomasSchlamme();

		Representation representation = extractor.extractFrom(source);
		assertEquals(Playlist.class, representation.getType(THOMAS_SCHLAMME_WIKIPEDIA_URI));
		assertThat(representation, hasPropertyValue(THOMAS_SCHLAMME_WIKIPEDIA_URI, "title", "Thomas Schlamme"));
		assertThat(representation, hasPropertyValue(THOMAS_SCHLAMME_WIKIPEDIA_URI, "description", "The producer of the West Wing"));
	}

}
