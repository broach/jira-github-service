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

import java.net.MalformedURLException;
import java.net.URL;
import static net.mostlyharmless.jghservice.connector.jira.JiraCommand.API_URL_BASE;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class UpdateIssue extends CreateIssue
{
    private final String jiraIssueKey;
    
    public UpdateIssue(Init<?> builder)
    {
        super(builder);
        this.jiraIssueKey = builder.jiraIssueKey;
    }
    
    @Override
    public String getRequestMethod()
    {
        return PUT;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        return new URL(API_URL_BASE + "issue/" + jiraIssueKey);
                           
    }
    
    @Override
    public int getExpectedResponseCode()
    {
        return 204; // JIRA docs are wrong, say 200
    }
    
    @Override
    public String processResponse(String jsonResponse)
    {
        return"{}";
    }
    
    protected abstract static class Init<T extends Init<T>> extends CreateIssue.Init<T>
    {
        private String jiraIssueKey;
        
        public T withJiraIssueKey(String key)
        {
            this.jiraIssueKey = key;
            return self();
        }
        
        @Override
        public UpdateIssue build()
        {
            if (jiraIssueKey == null)
            {
                throw new IllegalStateException("Key cannot be null.");
            }
            return new UpdateIssue(this);
        }
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
