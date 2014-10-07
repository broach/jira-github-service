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
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class SetLabelsOnIssue extends GetLabelsOnIssue
{
    private final List<String> labels;
    
    public SetLabelsOnIssue(Init<?> builder)
    {
        super(builder);
        this.labels = builder.labels;
    }
    
    @Override
    public String getJson() throws JsonProcessingException
    {
        return mapper.writeValueAsString(labels);
    }

    @Override
    public String getRequestMethod()
    {
        return PUT;
    }
    
    protected static abstract class Init<T extends Init<T>> extends GetLabelsOnIssue.Init<T>
    {
        private List<String> labels = new LinkedList<>();
        
        public T addLabel(String label)
        {
            labels.add(label);
            return self();
        }
        
        public T withLabels(List<String> labels)
        {
            this.labels.addAll(labels);
            return self();
        }
        
        @Override
        public SetLabelsOnIssue build()
        {
            validate();
            return new SetLabelsOnIssue(this);
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
