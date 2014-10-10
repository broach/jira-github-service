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

package net.mostlyharmless.jghservice.connector.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class UpdatePullRequest implements GithubCommand<Integer>
{
    @JsonIgnore
    private final ServiceConfig.Repository repo;
    @JsonIgnore
    private final int prNumber;
    @JsonProperty
    private final String title;
    @JsonProperty
    private final String body;
    @JsonProperty
    private final String state;
    
    
    private UpdatePullRequest(Builder builder)
    {
        this.repo = builder.repo;
        this.title = builder.title;
        this.body = builder.body;
        this.state = builder.state;
        this.prNumber = builder.prNumber;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        return new URL(API_URL_BASE + repo.getGithubOwner() + 
                        "/" + repo.getGithubName() + "/pulls/" +
                        prNumber);
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
        return 200;
    }

    @Override
    public Integer processResponse(String jsonResponse) throws IOException
    {
        JsonNode root = new ObjectMapper().readTree(jsonResponse);
        return root.get("number").asInt();
    }
    
    public static class Builder
    {
        private ServiceConfig.Repository repo;
        private String title;
        private String body;
        private String state;
        private Integer prNumber;
        
        public Builder withPullRequestNumber(int prNumber)
        {
            this.prNumber = prNumber;
            return this;
        }
        
        public Builder withRepository(ServiceConfig.Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withTitle(String title)
        {
            this.title = title;
            return this;
        }
        
        public Builder withBody(String body)
        {
            this.body = body;
            return this;
        }
        
        public Builder withState(String state)
        {
            this.state = state;
            return this;
        }
        
        public UpdatePullRequest build()
        {
            if (repo == null || prNumber == null)
            {
                throw new IllegalStateException("PR number and Repository must be supplied.");
            }
            return new UpdatePullRequest(this);
        }
        
    }
    
}
