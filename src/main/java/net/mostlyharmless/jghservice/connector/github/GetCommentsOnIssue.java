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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import net.mostlyharmless.jghservice.resources.ObjectMapperProvider;
import net.mostlyharmless.jghservice.resources.ServiceConfig.Repository;
import net.mostlyharmless.jghservice.resources.github.GithubEvent;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 */
public class GetCommentsOnIssue implements GithubCommand<List<GithubEvent.Comment>>
{
    private final Repository repo;
    private final int issueNum;
    
    private GetCommentsOnIssue(Builder builder)
    {
        this.repo = builder.repo;
        this.issueNum = builder.issueNum;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + 
                        repo.getGithubOwner()+
                        "/" +
                        repo.getGithubName()+
                        "/issues/" +
                        issueNum +
                        "/comments");
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported for GET"); 
    }

    @Override
    public String getRequestMethod()
    {
        return GET;
    }

    @Override
    public int getExpectedResponseCode()
    {
        return 200;
    }

    @Override
    public List<GithubEvent.Comment> processResponse(String jsonResponse) throws IOException
    {
        TypeReference<List<GithubEvent.Comment>> tr = 
            new TypeReference<List<GithubEvent.Comment>>(){};
        ObjectMapper m = new ObjectMapperProvider().getContext(GithubEvent.Comment.class);
        return m.readValue(jsonResponse, tr);
    }

    public static class Builder
    {
        private Integer issueNum;
        private Repository repo;
        
        public Builder withRepository(Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withIssueNumber(int issueNum)
        {
            this.issueNum = issueNum;
            return this;
        }
        
        public GetCommentsOnIssue build()
        {
            if (repo == null || issueNum == null)
            {
                throw new IllegalStateException("Repo and issue number required.");
            }
            return new GetCommentsOnIssue(this);
        }
        
    }
    
}
