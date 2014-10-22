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

package net.mostlyharmless.jghservice.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import net.mostlyharmless.jghservice.connector.jira.GetProjectKeys;
import net.mostlyharmless.jghservice.connector.jira.JiraConnector;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
@Singleton
@XmlRootElement(name="config")
public class ServiceConfig
{
    @XmlElement
    private Jira jira;
    @XmlElement
    private Github github;
    @XmlElement(name="repositories")
    @XmlJavaTypeAdapter(RepositoryAdapter.class)
    private Map<String, Map<String,Repository>> repositories;
    
    private List<String> jiraProjectNames;
    
    public Github getGithub()
    {
        return github;
    }

    public Jira getJira()
    {
        return jira;
    }

    public Repository getRepoForJiraName(String jiraName)
    {
        return repositories.get("jira").get(jiraName);
    }
    
    public Repository getRepoForGithubName(String ghName)
    {
        return repositories.get("github").get(ghName);
    }
    
    public List<Repository> getRepositories()
    {
        ArrayList<Repository> list = new ArrayList<>();
        for (Map.Entry<String,Repository> entry : repositories.get("jira").entrySet())
        {
            list.add(entry.getValue());
        }
        return list;
    }
    
    public synchronized List<String> getProjectKeys()
    {
        if (null == jiraProjectNames)
        {
            JiraConnector conn = new JiraConnector(this);
            
            GetProjectKeys get = new GetProjectKeys.Builder().build();
            try
            {
                jiraProjectNames = conn.execute(get);
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(ServiceConfig.class.getName()).log(Level.SEVERE, "getProjectKeys() failed", ex);
            }
            
        }
        
        return jiraProjectNames;
        
    }
    
    public static class Jira
    {
        @XmlElement
        private String username;
        @XmlElement
        private String password;
        @XmlElement
        private String url;
        @XmlElement
        private String githubIssueNumberField;
        @XmlElement
        private String githubRepoNameField;
        @XmlElement
        private String epicLinkField;
        @XmlElement
        private String epicNameField;

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }

        public String getUrl()
        {
            return url;
        }

        public String getGithubIssueNumberField()
        {
            return githubIssueNumberField;
        }

        public String getGithubRepoNameField()
        {
            return githubRepoNameField;
        }
        
        public String getEpicLinkField()
        {
            return epicLinkField;
        }
        
        public boolean hasEpicLinkField()
        {
            return epicLinkField != null;
        }
        
        public String getEpicNameField()
        {
            return epicNameField;
        }
        
        public boolean hasEpicNameField()
        {
            return epicNameField != null;
        }
        
    }
    
    public static class Github
    {
        @XmlElement
        private String username;
        @XmlElement
        private String password;
        @XmlElement
        private String url;

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }

        public String getUrl()
        {
            return url;
        }
        
        
        
    }
    
    public static class Repositories
    {
        @XmlElement(name="repository")
        private List<Repository> entries = new ArrayList<>();
        
        List<Repository> entries()
        {
            return Collections.unmodifiableList(entries);
        }
        
        void addEntry(Repository entry)
        {
            entries.add(entry);
        }
        
    }
    
    public static class Repository
    {
        @XmlElement
        private String githubName;
        @XmlElement
        private String githubOwner;
        @XmlElement
        private String jiraName;
        @XmlElement
        private String jiraProjectKey;
        @XmlElement
        private boolean importOnComment = false;
        @XmlElementWrapper(name="jiraFields")
        @XmlElement(name="field")
        private List<JiraField> jiraFields;
        @XmlElement
        private boolean mapEpicsToMilestones = false;
        @XmlElement
        private boolean labelVersions = false;

        public String getGithubName()
        {
            return githubName;
        }

        public String getGithubOwner()
        {
            return githubOwner;
        }

        public String getJiraName()
        {
            return jiraName;
        }

        public String getJiraProjectKey()
        {
            return jiraProjectKey;
        }

        public List<JiraField> getJiraFields()
        {
            return jiraFields;
        }
        
        public boolean mapEpicsToMilestones()
        {
            return mapEpicsToMilestones;
        }

        public boolean importOnComment()
        {
            return importOnComment;
        }
        
        public boolean labelVersions()
        {
            return labelVersions;
        }
        
        public static class JiraField
        {
            @XmlAttribute
            private String type;
            @XmlElement
            private String name;
            @XmlElement
            private String key;
            @XmlElement
            private String value;

            public String getType()
            {
                return type;
            }

            public String getName()
            {
                return name;
            }

            public String getKey()
            {
                return key;
            }

            public String getValue()
            {
                return value;
            }
            
            
            
        }
    }
    
    public static class RepositoryAdapter extends XmlAdapter<Repositories, Map<String, Map<String,Repository>>>
    {

        @Override
        public Map<String, Map<String, Repository>> unmarshal(Repositories repos) throws Exception
        {
            Map<String, Map<String, Repository>> newMap = new HashMap<>();
            Map<String,Repository> ghMap = new HashMap<>();
            Map<String,Repository> jiraMap = new HashMap<>();
            newMap.put("github", ghMap);
            newMap.put("jira", jiraMap);
            
            for (Repository repo : repos.entries())
            {                
                String ghKey = repo.getGithubName();
                String jiraKey = repo.getJiraName();
                
                ghMap.put(ghKey, repo);
                jiraMap.put(jiraKey, repo);
             }
            return newMap;
        }

        @Override
        public Repositories marshal(Map<String, Map<String, Repository>> v) throws Exception
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        
        
    }
}
