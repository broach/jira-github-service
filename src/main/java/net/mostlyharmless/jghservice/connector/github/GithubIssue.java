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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class GithubIssue
{
    private String title;
    private String body;
    private final List<String> labels = new LinkedList<>();
    
    public GithubIssue withTitle(String title)
    {
        this.title = title;
        return this;
    }
    
    public GithubIssue withBody(String body)
    {
        this.body = body;
        return this;
    }
    
    public GithubIssue withLabel(String label)
    {
        this.labels.add(label);
        return this;
    }
    
    public GithubIssue withLabels(List<String> labels)
    {
        this.labels.addAll(labels);
        return this;
    }
    
    public JsonNode getJson()
    {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode newNode = factory.objectNode();
        
        return null;
        
    }
}
