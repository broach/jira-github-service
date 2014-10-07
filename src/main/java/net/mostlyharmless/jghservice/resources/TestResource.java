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

package net.mostlyharmless.jghservice.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import net.mostlyharmless.jghservice.resources.ServiceConfig.Repository;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
@Path("/test")
public class TestResource
{
    @Inject
    ServiceConfig config;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode injectTest()
    {
        List<Repository> repos = config.getRepositories();
        ArrayNode arrayNode =  JsonNodeFactory.instance.arrayNode();
        for (Repository repo : repos)
        {
            arrayNode.add(repo.getJiraName());
        }
        
        return arrayNode;
    }
}
