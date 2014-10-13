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
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class ModifyIssue extends CreateIssue
{
    @JsonIgnore
    private final String issueNumber;
    @JsonProperty
    private final String state;
    
    public ModifyIssue(Init<?> init)
    {
        super(init);
        this.issueNumber = init.issueNumber;
        this.state = init.state;
    }
    
    @Override
    public int getExpectedResponseCode()
    {
        return 200;
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + repo.getGithubOwner() + 
                        "/" + repo.getGithubName() + "/issues/" + issueNumber );
    }
    
    @Override
    public String getJson() throws JsonProcessingException 
    {
        return mapper.writeValueAsString(this);
    }
    
    protected static abstract class Init<T extends Init<T>> extends CreateIssue.Init<T>
    {
        private String issueNumber;
        private String state;
       
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
    
        public T withState(String state)
        {
            this.state = state;
            return self();
        }
        
        @Override
        protected void validate()
        {
            super.validate();
            if (issueNumber == null)
            {
                throw new IllegalStateException("Issue number cannot be null");
            }
        }
        
        @Override
        public ModifyIssue build()
        {
            return new ModifyIssue(this);
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
