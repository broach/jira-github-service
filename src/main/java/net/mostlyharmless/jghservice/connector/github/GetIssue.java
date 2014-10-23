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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.mostlyharmless.jghservice.resources.ObjectMapperProvider;
import net.mostlyharmless.jghservice.resources.ServiceConfig.Repository;
import net.mostlyharmless.jghservice.resources.github.GithubEvent;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GetIssue implements GithubCommand<GithubEvent.Issue>
{
    private final Repository repo;
    private final Integer issueNumber;

    private GetIssue(Builder builder)
    {
        this.repo = builder.repo;
        this.issueNumber = builder.issueNumber;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + 
                        repo.getGithubOwner()+
                        "/" +
                        repo.getGithubName()+
                        "/issues/" +
                        issueNumber);
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported got GET."); 
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
    public GithubEvent.Issue processResponse(String jsonResponse) throws IOException
    {
        ObjectMapper m = new ObjectMapperProvider().getContext(GithubEvent.Comment.class);
        return m.readValue(jsonResponse, GithubEvent.Issue.class);
    }
    
    public static class Builder
    {
        private Repository repo;
        private Integer issueNumber;
        
        public Builder withRepository(Repository repo)
        {
            this.repo = repo;
            return this;
        }
        
        public Builder withIssueNumber(int issueNumber)
        {
            this.issueNumber = issueNumber;
            return this;
        }
        
        public GetIssue build()
        {
            return new GetIssue(this);
        }
        
    }
}
