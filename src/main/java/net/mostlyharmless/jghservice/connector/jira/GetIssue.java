/*
 * Copyright 2014 Brian Roach <roach at basho dot com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mostlyharmless.jghservice.connector.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.mostlyharmless.jghservice.resources.ObjectMapperProvider;
import net.mostlyharmless.jghservice.resources.jira.JiraEvent;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class GetIssue implements JiraCommand<JiraEvent.Issue>
{
    private final String issueKey;
    
    private GetIssue(Builder builder)
    {
        this.issueKey = builder.issueKey;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        return new URL(API_URL_BASE + "issue/" + issueKey);
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported on GET operations"); 
    }

    @Override
    public String getRequestMethod()
    {
        return GET;
    }

    @Override
    public int getExpectedResponseCode()
    {
        return 200;
    }

    @Override
    public JiraEvent.Issue processResponse(String jsonResponse) throws IOException
    {
        ObjectMapper m = new ObjectMapperProvider().getContext(JiraEvent.Issue.class);
        return m.readValue(jsonResponse, JiraEvent.Issue.class);
    }
    
    public static class Builder
    {
        private String issueKey;
        
        public Builder withIssueKey(String issueKey)
        {
            this.issueKey = issueKey;
            return this;
        }
        
        public GetIssue build()
        {
            if (issueKey == null || issueKey.isEmpty())
            {
                throw new IllegalStateException("Issue key cannot be null or empty.");
            }
            return new GetIssue(this);
        }
        
    }
    
}
