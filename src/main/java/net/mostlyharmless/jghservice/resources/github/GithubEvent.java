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

package net.mostlyharmless.jghservice.resources.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GithubEvent
{

    @JsonProperty
    private String action;
    @JsonProperty
    private Issue issue;
    @JsonProperty("pull_request")
    private PullRequest pullRequest;
    @JsonProperty
    private Repository repository;
    @JsonProperty
    private Comment comment;
        
    public String getAction()
    {
        return action;
    }

    public boolean hasIssue()
    {
        return issue != null;
    }
    
    public Issue getIssue()
    {
        return issue;
    }

    public boolean hasPullRequest()
    {
        return pullRequest != null;
    }
    
    public PullRequest getPullRequest()
    {
        return pullRequest;
    }
    
    public Repository getRepository()
    {
        return repository;
    }

    public Comment getComment()
    {
        return comment;
    }

    public boolean hasComment()
    {
        return comment != null;
    }
    
    public static class Issue
    {
        @JsonProperty
        private String title;
        @JsonProperty
        private int number;
        @JsonProperty
        private String body;
        @JsonProperty
        private String html_url;
        // Because github can't decide what an issue actually is
        @JsonProperty("pull_request")
        private PullRequest pullRequest;
        @JsonProperty
        private User user;

        public String getTitle()
        {
            return title;
        }

        public int getNumber()
        {
            return number;
        }

        public String getBody()
        {
            return body;
        }

        public String getUrl()
        {
            return html_url;
        }
        
        public boolean isReallyAPullRequest()
        {
            return pullRequest != null;
        }
        
        public User getUser()
        {
            return user;
        }
    }
    
    public static class PullRequest
    {
        @JsonProperty
        private String title;
        @JsonProperty
        private int number;
        @JsonProperty
        private String body;
        @JsonProperty
        private String html_url;

        public String getTitle()
        {
            return title;
        }

        public int getNumber()
        {
            return number;
        }

        public String getBody()
        {
            return body;
        }

        public String getUrl()
        {
            return html_url;
        }
        
    }
    
    public static class Repository
    {
        @JsonProperty
        private String name;

        public String getName()
        {
            return name;
        }

    }
    
    public static class Comment
    {
        @JsonProperty
        private String body;
        @JsonProperty
        private User user;
        @JsonProperty
        private int id;

        public String getBody()
        {
            return body;
        }

        public User getUser()
        {
            return user;
        }

        public int getId()
        {
            return id;
        }
        
        public static class User
        {
            @JsonProperty
            private String login;

            public String getLogin()
            {
                return login;
            }
        }
    }
    
    public static class User
    {
        @JsonProperty
        private String login;
        
        public String getLogin()
        {
            return login;
        }
    }
    
}
