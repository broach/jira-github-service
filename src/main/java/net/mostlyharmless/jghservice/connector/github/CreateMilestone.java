/*
 * Copyright 2014 Brian Roach <roach at basho dot com>.
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.mostlyharmless.jghservice.resources.ServiceConfig.Repository;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class CreateMilestone implements GithubCommand<Integer>
{
    @JsonIgnore
    private final Repository repo;
    private final String title;
    private final String description;
    
    private CreateMilestone(Builder builder)
    {
        this.repo = builder.repo;
        this.title = builder.title;
        this.description = builder.description;
    }
    
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + 
                        repo.getGithubOwner() +
                        "/" +
                        repo.getGithubName() +
                        "/milestones");
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
        JsonNode root = new ObjectMapper().readTree(jsonResponse);
        return root.get("number").asInt();
    }
    
    public static class Builder
    {
        private Repository repo;
        private String title;
        private String description;
        
        public Builder withRepository(Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withTitle(String title)
        {
            this.title = title;
            return this;
        }
        
        public Builder withDescription(String description)
        {
            this.description = description;
            return this;
        }
        
        public CreateMilestone build()
        {
            if (repo == null || title == null)
            {
                throw new IllegalStateException("Repo and title are required");
            }
            return new CreateMilestone(this);
        }
        
    }
    
}
