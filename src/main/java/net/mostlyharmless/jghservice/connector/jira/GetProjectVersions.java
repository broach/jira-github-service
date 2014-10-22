/*
 * Copyright 2014 Brian Roach <roach at mostlyharmless dot net>.
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GetProjectVersions implements JiraCommand<List<String>>
{
    private final String jiraProjectKey;
    
    private GetProjectVersions(Builder builder)
    {
        this.jiraProjectKey = builder.jiraProjectKey;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + "project/" + jiraProjectKey + "/versions");
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported for GET."); 
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
    public List<String> processResponse(String jsonResponse) throws IOException
    {
        List<String> versions = new LinkedList<>();
        ArrayNode array = (ArrayNode) new ObjectMapper().readTree(jsonResponse);
        for (JsonNode version : array)
        {
            versions.add(version.get("name").textValue());
        }
        return versions;
    }
    
    public static class Builder
    {
        private final String jiraProjectKey;
        
        public Builder(String jiraProjectKey)
        {
            this.jiraProjectKey = jiraProjectKey;
        }
        
        public GetProjectVersions build()
        {
            return new GetProjectVersions(this);
        }
        
    }
}
