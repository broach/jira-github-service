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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class PostComment implements JiraCommand<String>
{
    @JsonIgnore
    private final String jiraIssueKey;
    @JsonProperty
    private final String body;
    
    private PostComment(Builder builder)
    {
        this.jiraIssueKey = builder.jiraIssueKey;
        this.body = builder.body;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + "issue/" + jiraIssueKey + "/comment");
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
        return 201;
    }

    @Override
    public String processResponse(String jsonResponse) throws IOException
    {
        JsonNode root = new ObjectMapper().readTree(jsonResponse);
        return root.get("id").textValue();
    }
    
    public static class Builder
    {
        private String jiraIssueKey;
        private String body;
        
        public Builder withIssueKey(String jiraIssueKey)
        {
            this.jiraIssueKey = jiraIssueKey;
            return this;
        }
        
        public Builder withComment(String comment)
        {
            this.body = comment;
            return this;
        }
        
        public PostComment build()
        {
            if (jiraIssueKey == null || body == null)
            {
                throw new IllegalStateException("Key and body required.");
            }
            return new PostComment(this);
        }
    }
    
}
