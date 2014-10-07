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
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PostComment implements GithubCommand<Integer>
{
    @JsonProperty
    private final String body;
    @JsonIgnore
    private final ServiceConfig.Repository repo;
    @JsonIgnore
    private final String issueNumber;
    
    private PostComment(Builder builder)
    {
        this.body = builder.body;
        this.repo = builder.repo;
        this.issueNumber = builder.issueNumber;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        String urlString =  API_URL_BASE + 
                            repo.getGithubOwner() + 
                            "/" + 
                            repo.getGithubName() + 
                            "/issues/" +
                            issueNumber +
                            "/comments";
        return new URL(urlString);
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
    public Integer processResponse(String jsonResponse) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        return root.get("id").asInt();
    }
    
    public static class Builder
    {
        private String body;
        private ServiceConfig.Repository repo;
        private String issueNumber;
        
        public Builder() {}
        
        public Builder withRepo(ServiceConfig.Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withIssueNumber(int issueNumber)
        {
            this.issueNumber = String.valueOf(issueNumber);
            return this;
        }
        
        public Builder withIssueNumber(String issueNumber)
        {
            this.issueNumber = issueNumber;
            return this;
        }
        
        public Builder withBody(String body)
        {
            this.body = body;
            return this;
        }
        
        public PostComment build()
        {
            if (repo == null || issueNumber == null || body == null)
            {
                throw new IllegalStateException("Body, Repo, and Issue Number cannot be null");
            }
            return new PostComment(this);
        }
        
    }
        
    
}
