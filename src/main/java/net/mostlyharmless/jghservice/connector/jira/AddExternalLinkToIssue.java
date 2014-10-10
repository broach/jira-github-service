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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class AddExternalLinkToIssue implements JiraCommand<String>
{
    private final static ObjectNode APPLICATION = 
        JsonNodeFactory.instance.objectNode()
            .put("type","github.com")
            .put("name", "Github");
    
    private final static ObjectNode ICON =
        JsonNodeFactory.instance.objectNode()
            .put("url16x16","https://github.com/favicon.ico")
            .put("title","Github");
        
        
    private final String jiraIssueKey;
    private final String url;
    private final String title;
    private final String relationship;
    
    private AddExternalLinkToIssue(Builder builder)
    {
        this.jiraIssueKey = builder.jiraIssueKey;
        this.title = builder.title;
        this.url = builder.url;
        this.relationship = builder.relationship;
    }
    
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        return new URL(API_URL_BASE + "issue/" + jiraIssueKey + "/remotelink");
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        String globalId = "jira=" + jiraIssueKey + "&gh=" + url;
        
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("globalId", globalId);
        root.put("application", APPLICATION);
        if (relationship != null)
        {
            root.put("relationship", relationship);
        }
        ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.put("icon", ICON);
        object.put("url", url);
        object.put("title", title);
        root.put("object", object);
        
        return new ObjectMapper().writeValueAsString(root);
        
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
        private String url;
        private String title;
        private String relationship;
        
        public Builder withJiraIssueKey(String key)
        {
            this.jiraIssueKey = key;
            return this;
        }
        
        public Builder withUrl(String url)
        {
            this.url = url;
            return this;
        }
        
        public Builder withTitle(String title)
        {
            this.title = title;
            return this;
        }
        
        public Builder withRelationship(String relationship)
        {
            this.relationship = relationship;
            return this;
        }
        
        public AddExternalLinkToIssue build()
        {
            if (jiraIssueKey == null || url == null || title == null)
            {
                throw new IllegalStateException("key, url, and title are required.");
            }
            return new AddExternalLinkToIssue(this);
        }
        
    }
    
}
