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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class UpdateVersionsOnIssue implements JiraCommand<String>
{
    private final List<String> fixVersionsAdded = new LinkedList<>();
    private final List<String> fixVersionsRemoved = new LinkedList<>();
    private final List<String> affectsVersionsAdded = new LinkedList<>();
    private final List<String> affectsVersionsRemoved = new LinkedList<>();
    private final String jiraIssueKey;
        
    private UpdateVersionsOnIssue(Builder builder)
    {
        this.fixVersionsAdded.addAll(builder.fixVersionsAdded);
        this.fixVersionsRemoved.addAll(builder.fixVersionsRemoved);
        this.affectsVersionsAdded.addAll(builder.affectsVersionsAdded);
        this.affectsVersionsRemoved.addAll(builder.affectsVersionsRemoved);
        this.jiraIssueKey = builder.jiraIssueKey;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + "issue/" + jiraIssueKey);
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode root = factory.objectNode();
        ObjectNode update = factory.objectNode();
        root.put("update", update);
        
        if (!fixVersionsAdded.isEmpty() || !fixVersionsRemoved.isEmpty())
        {
            ArrayNode fixVersions = factory.arrayNode();
            for (String version : fixVersionsAdded)
            {
                ObjectNode add = factory.objectNode();
                ObjectNode addVersion = factory.objectNode();
                addVersion.put("name", version);
                add.put("add", addVersion);
                fixVersions.add(add);
            }
            
            for (String version : fixVersionsRemoved)
            {
                ObjectNode remove = factory.objectNode();
                ObjectNode removeVersion = factory.objectNode();
                removeVersion.put("name", version);
                remove.put("remove", removeVersion);
                fixVersions.add(remove);
            }
            
            update.put("fixVersions", fixVersions);
        }
        
        if (!affectsVersionsAdded.isEmpty() || !affectsVersionsRemoved.isEmpty())
        {
            ArrayNode affectsVersions = factory.arrayNode();
            for (String version : affectsVersionsAdded)
            {
                ObjectNode add = factory.objectNode();
                ObjectNode addVersion = factory.objectNode();
                addVersion.put("name", version);
                add.put("add", addVersion);
                affectsVersions.add(add);
            }
            
            for (String version : affectsVersionsRemoved)
            {
                ObjectNode remove = factory.objectNode();
                ObjectNode removeVersion = factory.objectNode();
                removeVersion.put("name", version);
                remove.put("remove", removeVersion);
                affectsVersions.add(remove);
            }
            
            update.put("versions", affectsVersions);
        }
        
        return new ObjectMapper().writeValueAsString(root);
        
    }

    @Override
    public String getRequestMethod()
    {
        return PUT;
    }

    @Override
    public int getExpectedResponseCode()
    {
        return 204; // JIRA docs are wrong, say 200
    }

    @Override
    public String processResponse(String jsonResponse) throws IOException
    {
        return jsonResponse;
    }
    
    public static class Builder
    {
        private final List<String> fixVersionsAdded = new LinkedList<>();
        private final List<String> fixVersionsRemoved = new LinkedList<>();
        private final List<String> affectsVersionsAdded = new LinkedList<>();
        private final List<String> affectsVersionsRemoved = new LinkedList<>();
        private String jiraIssueKey;
        
        public Builder withIssueKey(String jiraIssueKey)
        {
            this.jiraIssueKey = jiraIssueKey;
            return this;
        }
        
        public Builder addFixVersion(String version)
        {
            fixVersionsAdded.add(version);
            return this;
        }
        
        public Builder removeFixVersion(String version)
        {
            fixVersionsRemoved.add(version);
            return this;
        }
        
        public Builder addAffectsVersion(String version)
        {
            affectsVersionsAdded.add(version);
            return this;
        }
        
        public Builder removeAffectsVersion(String version)
        {
            affectsVersionsRemoved.add(version);
            return this;
        }
        
        public UpdateVersionsOnIssue build()
        {
            return new UpdateVersionsOnIssue(this);
        }
        
    }
    
}
