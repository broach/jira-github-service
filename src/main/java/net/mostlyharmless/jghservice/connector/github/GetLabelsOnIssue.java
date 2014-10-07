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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GetLabelsOnIssue implements GithubCommand<List<String>>
{
    protected final ServiceConfig.Repository repo;
    protected final String issueNumber;
    
    protected GetLabelsOnIssue(Init<?> builder)
    {
        this.issueNumber = builder.issueNumber;
        this.repo = builder.repo;
    }
    
    @Override
    public URL getUrl() throws MalformedURLException
    {
        String urlString = API_URL_BASE +
                           repo.getGithubOwner()+
                           "/" +
                           repo.getGithubName()+
                           "/issues/" +
                           issueNumber +
                           "/labels";
        return new URL(urlString);
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported for GET."); 
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
    public List<String> processResponse(String jsonResponse) throws IOException
    {
        JsonNode root = new ObjectMapper().readTree(jsonResponse);
        ArrayNode arrayNode = (ArrayNode)root;
        List<String> labels = new LinkedList<>();
        
        for (JsonNode node : arrayNode)
        {
            labels.add(node.get("name").textValue());
        }
        
        return labels;
    }
    
    protected static abstract class Init<T extends Init<T>>
    {
        private ServiceConfig.Repository repo;
        private String issueNumber;
        
        protected abstract T self();
        
        public T withRepo(ServiceConfig.Repository repo)
        {
            this.repo = repo;
            return self();
        }
        
        public T withIssueNumber(int issueNumber)
        {
            this.issueNumber = String.valueOf(issueNumber);
            return self();
        }
        
        public T withIssueNumber(String issueNumber)
        {
            this.issueNumber = issueNumber;
            return self();
        }
        
        protected void validate()
        {
            if (repo == null || issueNumber == null)
            {
                throw new IllegalStateException("Repo and Issue Number cannot be null.");
            }
        }
        
        public GetLabelsOnIssue build()
        {
            validate();
            return new GetLabelsOnIssue(this);
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
