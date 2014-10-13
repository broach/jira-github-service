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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GetProjectKeys implements JiraCommand<List<String>>
{

    private GetProjectKeys(Builder builder)
    {
        
    }
    
    @Override
    public URL getUrl(String apiUrlBase) throws MalformedURLException
    {
        return new URL(apiUrlBase + "project");
    }

    @Override
    public String getJson() throws JsonProcessingException
    {
        throw new UnsupportedOperationException("Not supported for GET commands"); 
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
        List<String> list = new LinkedList<>();
        ArrayNode jArray = (ArrayNode) new ObjectMapper().readTree(jsonResponse);
        for (JsonNode node : jArray)
        {
            list.add(node.get("key").textValue());
        }
        return list;
    }
    
    public static class Builder
    {
        public GetProjectKeys build()
        {
            return new GetProjectKeys(this);
        }
    }
}
