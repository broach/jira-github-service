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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;

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
        //@JsonProperty("customfield_10025")
        private final String githubIssueNumber;
        // @JsonProperty("customfield_10500")
        private final String githubRepo;
        
        public Issue(String key, String summary, String description, 
                                                 String ghIssueNumber,
                                                 String ghRepo)
        {
            this.jiraIssueKey = key;
            this.summary = summary;
            this.description = description;
            this.githubIssueNumber = ghIssueNumber;
            this.githubRepo = ghRepo;
            
        }
        
        public String getGithubRepo()
        {
            return githubRepo;
        }
        
        public boolean hasGithubRepo()
        {
            return githubRepo != null;
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

        public String getGithubIssueNumber()
        {
            return githubIssueNumber;
        }

        public boolean hasGithubIssueNumber()
        {
            return githubIssueNumber != null;
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
                String ghRepo = node.get("customfield_10500").get("value").textValue();
                String ghIssueNumber = node.get("customfield_10025").textValue();
                
                return new Issue(key, summary, description, ghIssueNumber, ghRepo);
                
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
