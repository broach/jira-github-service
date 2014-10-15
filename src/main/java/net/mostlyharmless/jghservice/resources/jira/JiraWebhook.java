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

package net.mostlyharmless.jghservice.resources.jira;

import java.util.LinkedList;
import java.util.List;
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
import net.mostlyharmless.jghservice.connector.github.CreateIssue;
import net.mostlyharmless.jghservice.connector.github.GetLabelsOnIssue;
import net.mostlyharmless.jghservice.connector.github.GithubConnector;
import net.mostlyharmless.jghservice.connector.github.ModifyIssue;
import net.mostlyharmless.jghservice.connector.github.PostComment;
import net.mostlyharmless.jghservice.connector.github.SetLabelsOnIssue;
import net.mostlyharmless.jghservice.resources.ServiceConfig;

/**
 *
 * @author Brian Roach <roach at mostlyharmless dot net>
 */
@Path("/jwh")
public class JiraWebhook
{
    private final static String JIRA_ISSUE_CREATED = "jira:issue_created";
    private final static String JIRA_ISSUE_UPDATED = "jira:issue_updated";
    
    private final Pattern githubCommentPattern =
            Pattern.compile("\\[posted via Github by .+\\]$");
    
    @Inject
    ServiceConfig config;
    
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response jiraWebhook(JiraEvent event)
    {
        switch(event.getWebhookEvent())
        {
            case JIRA_ISSUE_CREATED:
                processCreateEvent(event);
                break;
            case JIRA_ISSUE_UPDATED:
                processUpdateEvent(event);
                break;
            default:
                break;
        }
        return Response.ok().build();
    }
    
    private void processCreateEvent(JiraEvent event)
    {
        // If there's no mapped repo (or "Do Not Link To Repo") ... 
        // we don't care about this.
        String ghRepo = event.getIssue().getGithubRepo(config);
        ServiceConfig.Repository repository = 
            config.getRepoForJiraName(ghRepo);
        
        if (repository != null)
        {
            
            GithubConnector conn = new GithubConnector(config);
            
            if (!event.getIssue().hasGithubIssueNumber(config))
            {
                // Originating in Jira if there's no GH #
                // Create a new issue in GH
                
                String title = event.getIssue().getSummary() + 
                                " [JIRA: " + event.getIssue().getJiraIssueKey() +
                                "]";
                
                String body = event.getIssue().getDescription() +
                                "\n\n**[Created in JIRA by " +
                                event.getIssue().getReporter().getDisplayName() +
                                "]**";
                                
                
                CreateIssue create = 
                    new CreateIssue.Builder()
                        .withTitle(title)
                        .withBody(body)
                        .addLabel("JIRA: To Do")
                        .withRepository(repository)
                        .build();
                try
                {
                    conn.execute(create);
                }
                catch (ExecutionException ex)
                {
                    Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else
            {
                // Issue originated in GH. Update the GH title with the JIRA
                // issue key
                
                String title = event.getIssue().getSummary() +
                        " [JIRA: " + event.getIssue().getJiraIssueKey() + "]";
                
                ModifyIssue modify =
                    new ModifyIssue.Builder()
                        .withTitle(title)
                        .withIssueNumber(event.getIssue().getGithubIssueNumber(config))
                        .addLabel("JIRA: To Do")
                        .withRepository(repository)
                        .build();
                try
                {
                    conn.execute(modify);
                }
                catch (ExecutionException ex)
                {
                    Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void processUpdateEvent(JiraEvent event)
    {
        // If there's no repo (or "Do Not Link To Repo") ... we don't care about this.
        String ghRepo = event.getIssue().getGithubRepo(config);
        ServiceConfig.Repository repository = 
            config.getRepoForJiraName(ghRepo);
        
        if (repository != null)
        {
            GithubConnector conn = new GithubConnector(config);
            if (event.hasComment())
            {
                String body = event.getComment().getBody();
                Matcher m = githubCommentPattern.matcher(body);
                
                if (!m.find())
                {
                    // Comment was created on JIRA, post to GH 
                    JiraEvent.Comment c = event.getComment();
                    body = body + " \n\n***[posted via JIRA by " + 
                        c.getAuthor().getDisplayName() + "]***";
                    
                    PostComment post = 
                        new PostComment.Builder()
                            .withBody(body)
                            .withIssueNumber(event.getIssue().getGithubIssueNumber(config))
                            .withRepo(repository)
                            .build();
                    try
                    {
                        conn.execute(post);
                    }
                    catch (ExecutionException ex)
                    {
                        Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            if (event.hasChangelog())
            {
                List<JiraEvent.ChangeLog.Item> items = event.getChangelog().getItems();
                for (JiraEvent.ChangeLog.Item item : items)
                {
                    if (item.getField().equals("status"))
                    {
                        // Status change in JIRA, update GH issue
                        int ghIssueNumber = 
                            event.getIssue().getGithubIssueNumber(config);
                        
                        GetLabelsOnIssue getLabels =
                            new GetLabelsOnIssue.Builder()
                                .withIssueNumber(ghIssueNumber)
                                .withRepo(repository)
                                .build();
                        
                        List<String> existingLabels;
                        
                        try
                        {
                            existingLabels = conn.execute(getLabels);
                        }
                        catch (ExecutionException ex)
                        {
                            Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        }
                        
                        if (item.getToString().equals("Closed") 
                          || item.getToString().equals("Reopened")
                          || item.getToString().equals("Resolved"))
                        {
                            // Issue closed/reopened in JIRA, close/reopen in GH
                            ModifyIssue.Builder builder =
                                new ModifyIssue.Builder()
                                    .withIssueNumber(ghIssueNumber)
                                    .withRepository(repository);
                            
                            if (item.getToString().equals("Reopened"))
                            {
                                builder.withState("open");
                            }
                            else
                            {
                                builder.withState("closed");
                            }

                            List<String> labels = removeJiraStatusLabels(existingLabels);
                            
                            labels.add("JIRA: " + item.getToString());
                            
                            builder.withLabels(labels);
                            try
                            {
                                conn.execute(builder.build());
                            }
                            catch (ExecutionException ex)
                            {
                                Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else if (item.getToString().equals("In Progress") 
                                 || item.getToString().equals("Needs Review")
                                 || item.getToString().equals("To Do"))
                        {
                            // Issue moved on JIRA board.
                            List<String> labels = removeJiraStatusLabels(existingLabels);
                            labels.add("JIRA: " + item.getToString());
                            
                            SetLabelsOnIssue set = 
                                new SetLabelsOnIssue.Builder()
                                    .withIssueNumber(ghIssueNumber)
                                    .withRepo(repository)
                                    .withLabels(labels)
                                    .build();
                            try
                            {
                                conn.execute(set);
                            }
                            catch (ExecutionException ex)
                            {
                                Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                    }
                }
            }
        }
    }
    
    private List<String> removeJiraStatusLabels(List<String> labels)
    {
        List<String> newList = new LinkedList<>();
        for (String label : labels)
        {
            if (label.equals("JIRA: To Do") 
                || label.equals("JIRA: In Progress")
                || label.equals("JIRA: Needs Review")
                || label.equals("JIRA: Closed")
                || label.equals("JIRA: Reopened")
                || label.equals("JIRA: Resolved"))
            {
                continue;
            }
            newList.add(label);
            
        }
        return newList;
    }
    
}
