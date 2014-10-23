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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import net.mostlyharmless.jghservice.connector.UnexpectedResponseException;
import net.mostlyharmless.jghservice.connector.github.GithubCommand;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
public class JiraConnector
{
    private final String encodedUserPass;
    private final String apiUrlBase;
    
    public JiraConnector(ServiceConfig config)
    {
        encodedUserPass = 
            DatatypeConverter.printBase64Binary((config.getJira().getUsername() 
                                                 + ":" 
                                                 + config.getJira().getPassword())
                                                .getBytes());
        String base = config.getJira().getUrl();
        if (!base.endsWith("/"))
        {
            base = base + "/";
        }
        
        apiUrlBase = base;
        
    }
    
    public <T> T execute(JiraCommand<T> command) throws ExecutionException
    {
        try
        {
            HttpURLConnection conn = (HttpURLConnection) command.getUrl(apiUrlBase).openConnection();
            conn.setRequestMethod(command.getRequestMethod());
            conn.setRequestProperty("Authorization", "Basic " + encodedUserPass);
            conn.setDoOutput(true);
            
            if (!command.getRequestMethod().equals(GithubCommand.GET))
            {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf8");
                try (OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream()))
                {
                    wr.write(command.getJson());
                }
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode != command.getExpectedResponseCode())
            {
                Logger.getLogger(JiraConnector.class.getName()).log(Level.SEVERE, "Wrong Response code;", command.getUrl(apiUrlBase).toString());
                throw new ExecutionException(new UnexpectedResponseException(responseCode, 
                    conn.getResponseMessage()));
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                while ((line = br.readLine()) != null)
                {
                    sb.append(line);
                }

                return command.processResponse(sb.toString());
            }
            
        }
        catch (IOException ex)
        {
            throw new ExecutionException(ex);
        }
    }
}
