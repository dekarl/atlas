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

package org.atlasapi.remotesite.bbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.common.collect.Lists;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class SlashProgrammesVersionRdfTest extends TestCase {
	
	DateTime nov25th8pm = new DateTime(2007, 11, 25, 20, 00, 00, 00, DateTimeZone.UTC);
	DateTime nov26th8pm = new DateTime(2007, 11, 26, 20, 00, 00, 00, DateTimeZone.UTC);
	
	public void testFindsLatestTransmissionDateForOneShowing() throws Exception {
		
		SlashProgrammesVersionRdf versionRdf = new SlashProgrammesVersionRdf();
		versionRdf.broadcasts = Lists.newArrayList(new SlashProgrammesVersionRdf.BbcBroadcast().atTime("2007-11-25T20:00:00Z"));
		
		assertThat(versionRdf.lastTransmitted(), is(nov25th8pm));
	}
	
	public void testFindsLatestTransmissionDateForTwoShowings() throws Exception {
		
		SlashProgrammesVersionRdf versionRdf = new SlashProgrammesVersionRdf();
		versionRdf.broadcasts = Lists.newArrayList(new SlashProgrammesVersionRdf.BbcBroadcast().atTime("2007-11-25T20:00:00Z"), 
				                                        new SlashProgrammesVersionRdf.BbcBroadcast().atTime("2007-11-26T20:00:00Z"));
		
		assertThat(versionRdf.lastTransmitted(), is(nov26th8pm));
	}

}
