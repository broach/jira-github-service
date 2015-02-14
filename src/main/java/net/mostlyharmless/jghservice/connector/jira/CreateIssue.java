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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class CreateIssue implements JiraCommand<String>
{

    protected final String projectKey;
    protected final String issuetype;
    protected final String summary;
    protected final String description;
    protected final Map<String, JsonNode> customFields = new HashMap<>();
    protected final List<String> fixVersions = new LinkedList<>();
    protected final List<String> affectsVersions= new LinkedList<>();
    protected final String assignee;
    
    public final static String NO_ASSIGNEE = "";
    
    protected CreateIssue(Init<?> builder)
    {
        this.projectKey = builder.projectKey;
        this.issuetype = builder.issuetype;
        this.summary = builder.summary;
        this.description = builder.description;
        this.customFields.putAll(builder.customFields);
        this.affectsVersions.addAll(builder.affectsVersions);
        this.fixVersions.addAll(builder.fixVersions);
        this.assignee = builder.assignee;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + "issue");
                           
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode newNode = factory.objectNode();
        ObjectNode fields = factory.objectNode();
        
        if (projectKey != null)
        {
            ObjectNode project = factory.objectNode();
            project.put("key", projectKey);
            fields.put("project", project);
        }
        
        if (issuetype != null)
        {
            ObjectNode iType = factory.objectNode();
            iType.put("name", issuetype);
            fields.put("issuetype", iType);
        }
        
        if (summary != null)
        {
            fields.put("summary", summary);
        }
        
        if (description != null)
        {
            fields.put("description", description);
        }
        
        if (!fixVersions.isEmpty())
        {
            ArrayNode array = factory.arrayNode();
            for (String version : fixVersions)
            {
                ObjectNode node = factory.objectNode();
                node.put("name", version);
                array.add(node);
            }
            fields.put("fixVersions", array);
        }
        
        if (!affectsVersions.isEmpty())
        {
            ArrayNode array = factory.arrayNode();
            for (String version : affectsVersions)
            {
                ObjectNode node = factory.objectNode();
                node.put("name", version);
                array.add(node);
            }
            fields.put("versions", array);
        }
        
        if (assignee != null)
        {
            ObjectNode node = factory.objectNode();
            if (assignee.isEmpty())
            {
                node.put("name", factory.nullNode());
            }
            else
            {
                node.put("name", assignee);
            }
            fields.put("assignee", node);
        }
        
        for (Map.Entry<String, JsonNode> entry : customFields.entrySet())
        {
            fields.put(entry.getKey(), entry.getValue());
        }
        
        newNode.put("fields", fields);
        
        return new ObjectMapper().writeValueAsString(newNode);
        
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
        return root.get("key").textValue();
    }
    
    protected abstract static class Init<T extends Init<T>>
    {
        private String projectKey;
        private String issuetype;
        private final Map<String, JsonNode> customFields = new HashMap<>();
        private String summary;
        private String description;
        private List<String> affectsVersions = new LinkedList<>();
        private List<String> fixVersions = new LinkedList<>();
        private String assignee;
        
        public T withProjectKey(String projectKey)
        {
            this.projectKey = projectKey;
            return self();
        }
        
        public T withIssuetype(String issuetype)
        {
            this.issuetype = issuetype;
            return self();
        }
        
        public T withSummary(String summary)
        {
            this.summary = summary;
            return self();
        }
        
        public T withDescription(String description)
        {
            this.description = description;
            return self();
        }
        
        public T withCustomField(String fieldname, String value)
        {
            TextNode text = JsonNodeFactory.instance.textNode(value);
            customFields.put(fieldname, text);
            return self();
        }
        
        public T withCustomField(String fieldname, int value)
        {
            NumericNode number = JsonNodeFactory.instance.numberNode(value);
            customFields.put(fieldname, number);
            return self();
        }
        
        public T withCustomField(String fieldName, String key, String value)
        {
            JsonNode fieldNode = customFields.get(fieldName);
            if (fieldNode == null || !fieldNode.isObject())
            {
                fieldNode = JsonNodeFactory.instance.objectNode();
                customFields.put(fieldName, fieldNode);
            }

            ((ObjectNode)fieldNode).put(key, value);
            return self();
        }
        
        public T withCustomField(String fieldName, String key, int value)
        {
            JsonNode fieldNode = customFields.get(fieldName);
            if (fieldNode == null || !fieldNode.isObject())
            {
                fieldNode = JsonNodeFactory.instance.objectNode();
                customFields.put(fieldName, fieldNode);
            }

            ((ObjectNode)fieldNode).put(key, value);
            return self();
        }
        
        public T withCustomArrayField(String fieldName, String key, String value)
        {
            JsonNode fieldNode = customFields.get(fieldName);
            if (fieldNode == null || !fieldNode.isArray())
            {
                fieldNode = JsonNodeFactory.instance.arrayNode();
                customFields.put(fieldName, fieldNode);
            }
            
            JsonNode nodeInArray = JsonNodeFactory.instance.objectNode();
            ((ObjectNode)nodeInArray).put(key,value);
            
            ((ArrayNode)fieldNode).add(nodeInArray);
            return self();
        }
        
        public T withCustomArrayField(String fieldName, String key, int value)
        {
            JsonNode fieldNode = customFields.get(fieldName);
            if (fieldNode == null || !fieldNode.isArray())
            {
                fieldNode = JsonNodeFactory.instance.arrayNode();
                customFields.put(fieldName, fieldNode);
            }
            
            JsonNode nodeInArray = JsonNodeFactory.instance.objectNode();
            ((ObjectNode)nodeInArray).put(key,value);
            
            ((ArrayNode)fieldNode).add(nodeInArray);
            return self();
        }
        
        public T withFixVersions(List<String> fixVersions)
        {
            this.fixVersions.clear();
            this.fixVersions.addAll(fixVersions);
            return self();
        }
        
        public T withAffectsVersions(List<String> affectsVersions)
        {
            this.affectsVersions.clear();
            this.affectsVersions.addAll(affectsVersions);
            return self();
        }
        public T withAssignee(String assignee)
        {
            this.assignee = assignee;
            return self();
        }
        
        
        protected void validate()
        {
            if (projectKey == null 
                || issuetype == null
                || summary == null)
            {
                throw new IllegalStateException("Project Key, Issuetype and summary are required");
            }
        }
        
        public CreateIssue build()
        {
            validate();
            return new CreateIssue(this);
        }
        
        protected abstract T self();
    }
    
    public static class Builder extends Init<Builder>
    {
        @Override
        protected Builder self()
        {
            return this;
        }
    }
    
}
