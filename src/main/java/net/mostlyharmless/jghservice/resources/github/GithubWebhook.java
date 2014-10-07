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

package net.mostlyharmless.jghservice.resources.github;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.mostlyharmless.jghservice.connector.jira.CreateIssue;
import net.mostlyharmless.jghservice.connector.jira.JiraConnector;
import net.mostlyharmless.jghservice.connector.jira.PostComment;
import net.mostlyharmless.jghservice.connector.jira.UpdateIssue;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
@Path("/ghwh")
public class GithubWebhook
{
    @Inject
    ServiceConfig config;
    
    private static final Pattern jiraIssuePattern = 
        Pattern.compile("\\[JIRA: ([-A-Z0-9]+)\\]");
    private final Pattern jiraCommentPattern =
            Pattern.compile("\\[posted via JIRA by .+\\]\\*\\*\\*$");
    
    private static final String GITHUB_ISSUE_OPENED = "opened";
    private static final String GITHUB_COMMENT_CREATED = "created";
    
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response githubWebhook(GithubEvent event)
    {
        switch(event.getAction())
        {
            case GITHUB_ISSUE_OPENED:
                processOpenedEvent(event);
                break;
            case GITHUB_COMMENT_CREATED:
                processCreatedEvent(event);
                break;
            default:
                break;
                
        }
        return Response.ok().build();
    }
    
    private void processOpenedEvent(GithubEvent event)
    {
        JiraConnector conn = new JiraConnector(config.getJira().getUsername(),
                                               config.getJira().getPassword());
        
        String title = event.getIssue().getTitle();
        Matcher m = jiraIssuePattern.matcher(title);
        if (m.find())
        {
            // Originated from JIRA, need to update JIRA with issue number
            
            String githubIssueField = config.getJira().getGithubIssueNumberField();
            
            UpdateIssue update = 
                new UpdateIssue.Builder()
                    .withCustomField(githubIssueField, String.valueOf(event.getIssue().getNumber()))
                    .withJiraIssueKey(m.group(1))
                    .build();
            try
            {
                conn.execute(update);
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            // Originated in github. Create in JIRA
            
            String githubIssueField = config.getJira().getGithubIssueNumberField();
            String jiraRepoField = config.getJira().getGithubRepoNameField();
            ServiceConfig.Repository repo = 
                config.getRepoForGithubName(event.getRepository().getName());
            
            String jiraProjectKey = repo.getJiraProjectKey();
            String jiraRepoName = repo.getJiraName();
            String githubIssueNumber = String.valueOf(event.getIssue().getNumber());
            
            CreateIssue.Builder builder = 
                new CreateIssue.Builder()
                    .withProjectKey(jiraProjectKey)
                    .withIssuetype("Task")
                    .withSummary(event.getIssue().getTitle())
                    .withDescription(event.getIssue().getBody())
                    .withCustomField(githubIssueField, githubIssueNumber)
                    .withCustomField(jiraRepoField, "value", jiraRepoName);
            
            // populate any custom fields from repo config
            for (ServiceConfig.Repository.JiraField field : repo.getJiraFields())
            {
                if (field.getType().equals("object"))
                {
                    builder.withCustomField(field.getName(), field.getKey(), field.getValue());
                }
                else
                {
                    builder.withCustomField(field.getName(), field.getValue());
                }
            }
            try
            {
                conn.execute(builder.build());
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void processCreatedEvent(GithubEvent event)
    {
        JiraConnector conn = new JiraConnector(config.getJira().getUsername(),
                                               config.getJira().getPassword());
        
        if (event.hasComment())
        {
            String body = event.getComment().getBody();
            Matcher m = jiraCommentPattern.matcher(body);
        
            if (!m.find())
            {
                // Comment originating on GH, post to JIRA
                
                // The JIRA issue key is in the title
                String issueTitle = event.getIssue().getTitle();
                m = jiraIssuePattern.matcher(issueTitle);
                if (m.find())
                {
                    body = body + " \n[posted via Github by " +
                        event.getComment().getUser().getLogin() +
                        "]";
                    
                    
                    PostComment post = 
                        new PostComment.Builder()
                            .withIssueKey(m.group(1))
                            .withComment(body)
                            .build();
                    try
                    {
                        conn.execute(post);
                    }
                    catch (ExecutionException ex)
                    {
                        Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
                
            }
            
        }
    }
    
}
