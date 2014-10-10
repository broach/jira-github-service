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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import net.mostlyharmless.jghservice.resources.ObjectMapperProvider;
import net.mostlyharmless.jghservice.resources.jira.JiraEvent;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class SearchIssues implements JiraCommand<List<JiraEvent.Issue>>
{
    @JsonProperty
    private final String jql;
    
    private SearchIssues(Builder builder)
    {
        this.jql = builder.jql;
    }

    @Override
    public URL getUrl() throws MalformedURLException
    {
        return new URL(API_URL_BASE + "search");
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        return mapper.writeValueAsString(this);
    }

    @Override
    public String getRequestMethod()
    {
        return POST;
    }

    @Override
    public int getExpectedResponseCode()
    {
        return 200;
    }

    @Override
    public List<JiraEvent.Issue> processResponse(String jsonResponse) throws IOException
    {
        ObjectMapper m = new ObjectMapperProvider().getContext(JiraEvent.Issue.class);
        ObjectNode root = (ObjectNode) m.readTree(jsonResponse);
        JavaType t = m.getTypeFactory().constructCollectionType(List.class, JiraEvent.Issue.class);
        return m.convertValue(root.get("issues"), t);
    }
    
    public static class Builder
    {
        private String jql;
        
        public Builder withJQL(String query)
        {
            this.jql = query;
            return this;
        }
        
        public SearchIssues build()
        {
            return new SearchIssues(this);
        }
    }
    
}
