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

package org.atlasapi.query.v2;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.beans.AtlasErrorSummary;
import org.atlasapi.beans.AtlasModelWriter;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.query.Selection;

@Controller
public class QueryController extends BaseController {
	
	private static final int MAX_LIMIT = 50;

	public QueryController(KnownTypeQueryExecutor executor, ApplicationConfigurationFetcher configFetcher, AdapterLog log, AtlasModelWriter outputter) {
	    super(executor, configFetcher, log, outputter);
	}
	
	@RequestMapping("/3.0/discover.*")
	public void discover(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			ContentQuery filter = builder.build(request);
			if (!filter.getSelection().hasLimit()) {
				errorViewFor(request, response, new AtlasErrorSummary().withMessage("No limit specified, specify a limit <= " + MAX_LIMIT).withStatusCode(HttpStatusCode.BAD_REQUEST));
				return;
			}
			if (filter.getSelection().getLimit() > MAX_LIMIT) {
				errorViewFor(request, response, new AtlasErrorSummary().withMessage("Limit too high, specify a limit <= " + MAX_LIMIT).withStatusCode(HttpStatusCode.BAD_REQUEST));
				return;
			}
			modelAndViewFor(request, response, executor.discover(filter));
		} catch (Exception e) {
			errorViewFor(request, response, AtlasErrorSummary.forException(e));
		}
	}
	
	@RequestMapping("/3.0/content.*")
	public void content(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			ContentQuery filter = builder.build(request);

			if (!Selection.ALL.equals(filter.getSelection())) {
				throw new IllegalArgumentException("Cannot specifiy a limit or offset here");
			}
			String commaSeperatedUris = request.getParameter("uri");
			if (commaSeperatedUris == null) {
				throw new IllegalArgumentException("No uris specified");
			}
			List<String> uris = ImmutableList.copyOf(URI_SPLITTER.split(commaSeperatedUris));
			if (Iterables.isEmpty(uris)) {
				throw new IllegalArgumentException("No uris specified");
			}
			modelAndViewFor(request, response, executor.executeUriQuery(uris, filter));
		} catch (Exception e) {
			errorViewFor(request, response, AtlasErrorSummary.forException(e));
		}
	}
}