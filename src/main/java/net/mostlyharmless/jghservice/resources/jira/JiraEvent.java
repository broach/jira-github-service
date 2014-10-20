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

package net.mostlyharmless.jghservice.resources.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class JiraEvent
{
    private String webhookEvent;
    private Issue issue;
    private ChangeLog changelog;
    private Comment comment;
    
    public String getWebhookEvent()
    {
        return webhookEvent;
    }

    public void setWebhookEvent(String webhookEvent)
    {
        this.webhookEvent = webhookEvent;
    }

    public Issue getIssue()
    {
        return issue;
    }

    public void setIssue(Issue issue)
    {
        this.issue = issue;
    }

    public ChangeLog getChangelog()
    {
        return changelog;
    }

    public void setChangelog(ChangeLog changelog)
    {
        this.changelog = changelog;
    }

    public boolean hasChangelog()
    {
        return changelog != null;
    }
    
    public Comment getComment()
    {
        return comment;
    }

    public void setComment(Comment comment)
    {
        this.comment = comment;
    }
    
    public boolean hasComment()
    {
        return comment != null;
    }
    
    public static class Issue
    {
        private final String jiraIssueKey;
        private final String summary;
        private final String description;
        private final Reporter reporter;
        
        private final Map<String, JsonNode> customFields;
        
        public Issue(String key, String summary, String description, 
                                                 Map<String,JsonNode> customFields,
                                                 Reporter reporter)
        {
            this.jiraIssueKey = key;
            this.summary = summary;
            this.description = description;
            this.customFields = customFields;
            this.reporter = reporter;
        }
        
        public String getGithubRepo(ServiceConfig config)
        {
            String ghRepoField = config.getJira().getGithubRepoNameField();
            JsonNode node = customFields.get(ghRepoField);
            return node.get("value").textValue();
        }
        
        public boolean hasGithubRepo(ServiceConfig config)
        {
            String ghRepoField = config.getJira().getGithubRepoNameField();
            JsonNode node = customFields.get(ghRepoField);
            return (node != null && !node.isNull() && node.get("value").isTextual());
        }

        public String getEpicIssueKey(ServiceConfig config)
        {
            String jiraEpicField = config.getJira().getEpicLinkField();
            JsonNode node = customFields.get(jiraEpicField);
            return node.textValue();
        }
        
        public boolean hasEpicIssueKey(ServiceConfig config)
        {
            String jiraEpicField = config.getJira().getEpicLinkField();
            JsonNode node = customFields.get(jiraEpicField);
            return (node != null && !node.isNull() && node.isTextual());
        }
        
        public String getEpicName(ServiceConfig config)
        {
            String epicNameField = config.getJira().getEpicNameField();
            JsonNode node = customFields.get(epicNameField);
            return node.textValue();
        }
        
        public boolean hasEpicName(ServiceConfig config)
        {
            String epicNameField = config.getJira().getEpicNameField();
            JsonNode node = customFields.get(epicNameField);
            return (node != null && !node.isNull() && node.isTextual());
        }
        
        public String getJiraIssueKey()
        {
            return jiraIssueKey;
        }

        public String getSummary()
        {
            return summary;
        }

        public String getDescription()
        {
            return description;
        }

        public int getGithubIssueNumber(ServiceConfig config)
        {
            String ghIssueNumField = config.getJira().getGithubIssueNumberField();
            JsonNode node = customFields.get(ghIssueNumField);
            return node.asInt();
        }

        public boolean hasGithubIssueNumber(ServiceConfig config)
        {
            String ghIssueNumField = config.getJira().getGithubIssueNumberField();
            JsonNode node = customFields.get(ghIssueNumField);
            return (node != null && !node.isNull() && node.isNumber());
        }
        
        public Reporter getReporter()
        {
            return reporter;
        }
        
        public static class Reporter
        {
            @JsonProperty
            private String displayName;
            
            public String getDisplayName()
            {
                return displayName;
            }
        }
        
        public static class Deserializer extends JsonDeserializer<Issue>
        {
            @Override
            public Issue deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException
            {
                JsonNode node = jp.getCodec().readTree(jp);
                String key = node.get("key").textValue();
                node = node.get("fields");
                String summary = node.get("summary").textValue();
                String description = node.get("description").textValue();
                Reporter r = jp.getCodec().treeToValue(node.get("reporter"), Reporter.class);
                
                // Store the custom fields in a map
                Map<String, JsonNode> customFields = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext())
                {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    if (entry.getKey().startsWith("customfield_"))
                    {
                        customFields.put(entry.getKey(), entry.getValue());
                    }
                }
                
                return new Issue(key, summary, description, customFields, r);
                
            }
            
        }
        
    }
    
    public static class ChangeLog
    {

        private String id;
        private List<Item> items;
        
        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public List<Item> getItems()
        {
            return items;
        }

        public void setItems(List<Item> items)
        {
            this.items = items;
        }
        
        public static class Item
        {
            private String field;
            private String fieldtype;
            private String from;
            private String fromString;
            private String to;
            private String toString;
            
            public Item() {}
            
            public String getField()
            {
                return field;
            }

            public void setField(String field)
            {
                this.field = field;
            }

            public String getFieldtype()
            {
                return fieldtype;
            }

            public void setFieldtype(String fieldtype)
            {
                this.fieldtype = fieldtype;
            }

            public String getFrom()
            {
                return from;
            }

            public void setFrom(String from)
            {
                this.from = from;
            }

            public String getFromString()
            {
                return fromString;
            }

            public void setFromString(String fromString)
            {
                this.fromString = fromString;
            }

            public String getTo()
            {
                return to;
            }

            public void setTo(String to)
            {
                this.to = to;
            }

            public String getToString()
            {
                return toString;
            }

            public void setToString(String toString)
            {
                this.toString = toString;
            }
            
        }
        
    }
    
    public static class Comment
    {
        private Author author;
        private String body;

        /*
            Might want to think about:
            "visibility":{"type":"role","value":"Developers"}
            It's not present if "All users" is chosen. 
        */
        
        public Author getAuthor()
        {
            return author;
        }

        public void setAuthor(Author author)
        {
            this.author = author;
        }

        public String getBody()
        {
            return body;
        }

        public void setBody(String body)
        {
            this.body = body;
        }
        
        public static class Author
        {
            private String displayName;

            public String getDisplayName()
            {
                return displayName;
            }

            public void setDisplayName(String displayName)
            {
                this.displayName = displayName;
            }
            
        }
        
    }
}
