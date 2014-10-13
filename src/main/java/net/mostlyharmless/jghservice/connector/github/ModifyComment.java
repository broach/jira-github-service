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
public class ModifyComment implements GithubCommand<Integer>
{
    @JsonProperty
    private final String body;
    @JsonIgnore
    private final ServiceConfig.Repository repo;
    @JsonIgnore 
    private final int commentId;
    
    private ModifyComment(Builder builder)
    {
        this.body = builder.body;
        this.repo = builder.repo;
        this.commentId = builder.commentId;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase +
                       repo.getGithubOwner() + 
                        "/" + 
                        repo.getGithubName() + 
                        "/issues/comments/" +
                        commentId);
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        return root.get("id").asInt();
    }
    
    public static class Builder
    {
        private String body;
        private ServiceConfig.Repository repo;
        private Integer commentId;
        
        public Builder withRepository(ServiceConfig.Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withBody(String body)
        {
            this.body = body;
            return this;
        }
        
        public Builder withCommentId(int id)
        {
            this.commentId = id;
            return this;
        }
        
        public ModifyComment build()
        {
            if (repo == null || commentId == null || body == null)
            {
                throw new IllegalStateException("Body, Repo, and commentId cannot be null");
            }
            return new ModifyComment(this);
        }
        
    }
    
}
