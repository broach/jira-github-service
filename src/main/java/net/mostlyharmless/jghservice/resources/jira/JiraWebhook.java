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
import net.mostlyharmless.jghservice.connector.github.CreateMilestone;
import net.mostlyharmless.jghservice.connector.github.GetLabelsOnIssue;
import net.mostlyharmless.jghservice.connector.github.GetMilestones;
import net.mostlyharmless.jghservice.connector.github.GithubConnector;
import net.mostlyharmless.jghservice.resources.github.GithubEvent.Milestone;
import net.mostlyharmless.jghservice.connector.github.ModifyIssue;
import net.mostlyharmless.jghservice.connector.github.PostComment;
import net.mostlyharmless.jghservice.connector.github.SetLabelsOnIssue;
import net.mostlyharmless.jghservice.connector.jira.GetIssue;
import net.mostlyharmless.jghservice.connector.jira.JiraConnector;
import net.mostlyharmless.jghservice.resources.ServiceConfig;
import net.mostlyharmless.jghservice.resources.github.GithubEvent;

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
                
                if (event.getIssue().isEpic())
                {
                    // New epic. Create a Milestone.
                    
                    // Epics have a Name, a Summary, and a Description. In
                    // GH we just have the title and decription. 
                    String description = event.getIssue().getSummary() +
                                            "\n\n" + 
                                            event.getIssue().getDescription();
                    
                    CreateMilestone create =
                        new CreateMilestone.Builder()
                            .withRepository(repository)
                            .withTitle(event.getIssue().getEpicName(config))
                            .withDescription(description)
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
                    // Create a new issue in GH
                    
                    String title = event.getIssue().getSummary() + 
                                    " [JIRA: " + event.getIssue().getJiraIssueKey() +
                                    "]";

                    String body = event.getIssue().getDescription() +
                                    "\n\n**[Created in JIRA by " +
                                    event.getIssue().getReporter().getDisplayName() +
                                    "]**";

                    Integer milestone = null;
                    if (config.getJira().hasEpicLinkField() && repository.mapEpicsToMilestones())
                    {
                        /*
                         * Mapping Epics to Milestones takes a few operations. The 
                         * event from JIRA will have the JIRA key for the Epic issue. 
                         * That issue will have to be retrieved, and the title 
                         * looked up as a milestone in GH. If it doesn't exist, it 
                         * has to be created in GH. 
                         */

                        if (event.getIssue().hasEpicIssueKey(config))
                        {
                            String jiraEpicKey = event.getIssue().getEpicIssueKey(config);

                            JiraConnector jConn = new JiraConnector(config);

                            GetIssue get = 
                                new GetIssue.Builder()
                                    .withIssueKey(jiraEpicKey)
                                    .build();
                            try
                            {
                                JiraEvent.Issue epic = jConn.execute(get);
                                String epicName = epic.getEpicName(config);

                                GetMilestones getMs = 
                                    new GetMilestones.Builder()
                                        .withRepositoy(repository)
                                        .build();

                                List<Milestone> msList = conn.execute(getMs);


                                for (Milestone ms : msList)
                                {
                                    if (ms.getTitle().equals(epicName))
                                    {
                                        milestone = ms.getNumber();
                                        break;
                                    }
                                }

                                if (milestone == null)
                                {
                                    // Need to create this milestone in GH
                                    CreateMilestone create =
                                        new CreateMilestone.Builder()
                                            .withRepository(repository)
                                            .withTitle(epicName)
                                            .build();

                                    milestone = conn.execute(create);
                                }
                            }
                            catch (ExecutionException ex)
                            {
                                Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                            }


                        }

                    }
                    
                    List<String> labels = new LinkedList<>();
                    labels.add("JIRA: To Do");
                    
                    if (repository.labelVersions())
                    {
                        for (String version : event.getIssue().getFixVersions())
                        {
                            labels.add("Fixed in: " + version);
                        }
                        
                        for (String version : event.getIssue().getAffectsVersions())
                        {
                            labels.add("Affects: " + version);
                        }
                    }

                    String assignee = null;
                    if (config.hasUserMappings() && event.getIssue().hasAsignee())
                    {
                        assignee = config.getGithubUser(event.getIssue().getAssignee());
                    }
                    
                    CreateIssue create = 
                        new CreateIssue.Builder()
                            .withTitle(title)
                            .withBody(body)
                            .withLabels(labels)
                            .withRepository(repository)
                            .withMilestone(milestone)
                            .withAssignee(assignee)
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
            }
            else
            {
                // Issue originated in GH. Update the GH title with the JIRA
                // issue key
                
                String title = event.getIssue().getSummary() +
                        " [JIRA: " + event.getIssue().getJiraIssueKey() + "]";
                try
                {
                    GetLabelsOnIssue get = 
                        new GetLabelsOnIssue.Builder()
                            .withRepo(repository)
                            .withIssueNumber(event.getIssue().getGithubIssueNumber(config))
                            .build();
                    
                    List<String> labels = conn.execute(get);
                    labels = removeJiraStatusLabels(labels);
                    labels.add("JIRA: To Do");
                    
                    List<String> existingLabels = event.getIssue().getLabels();
                    for (String label : existingLabels)
                    {
                        if (label.endsWith("PR_Review"))
                        {
                            labels.add("JIRA: PR Review");
                        }
                    }
                    
                    ModifyIssue modify =
                        new ModifyIssue.Builder()
                            .withTitle(title)
                            .withIssueNumber(event.getIssue().getGithubIssueNumber(config))
                            .withLabels(labels)
                            .withRepository(repository)
                            .build();
                
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
        
        
        if (repository != null && event.getIssue().hasGithubIssueNumber(config))
        {
            int ghIssueNumber = 
                            event.getIssue().getGithubIssueNumber(config);
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
                        
                        List<String> existingLabels;
                        
                        try
                        {
                            existingLabels = getExistingLabels(conn, repository, ghIssueNumber);
                        }
                        catch (ExecutionException ex)
                        {
                            Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        }
                        
                        if (item.getToString().equals("Closed") 
                          || item.getToString().equals("Reopened")
                          || item.getToString().equals("Resolved")
                            || item.getToString().equals("Done"))
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
                    else if (item.getField().equals("Fix Version") && repository.labelVersions())
                    {
                        // Fix version added/removed
                        List<String> existingLabels;
                        
                        try
                        {
                            existingLabels = getExistingLabels(conn, repository, ghIssueNumber);
                            int numLabels = existingLabels.size();
                            
                            if (item.getToString() != null)
                            {
                                String newLabel = "Fixed in: " + item.getToString();
                                if (!existingLabels.contains(newLabel))
                                {
                                    existingLabels.add("Fixed in: " + item.getToString());
                                }
                            }
                            else if (item.getFromString() != null)
                            {
                                existingLabels.remove("Fixed in: " + item.getFromString());
                            }
                            
                            if (numLabels != existingLabels.size())
                            {
                                SetLabelsOnIssue set = 
                                    new SetLabelsOnIssue.Builder()
                                        .withIssueNumber(ghIssueNumber)
                                        .withRepo(repository)
                                        .withLabels(existingLabels)
                                        .build();

                                conn.execute(set);
                            }
                        }
                        catch (ExecutionException ex)
                        {
                            Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else if (item.getField().equals("Version") && repository.labelVersions())
                    {
                        // Affects version added/removed
                        List<String> existingLabels;
                        
                        try
                        {
                            existingLabels = getExistingLabels(conn, repository, ghIssueNumber);
                            int numLabels = existingLabels.size();
                            
                            if (item.getToString() != null)
                            {
                                String newLabel = "Affects: " + item.getToString();
                                if (existingLabels.contains(newLabel))
                                {
                                    existingLabels.add("Affects: " + item.getToString());
                                }
                            }
                            else if (item.getFromString() != null)
                            {
                                existingLabels.remove("Affects: " + item.getFromString());
                            }
                            
                            if (numLabels != existingLabels.size())
                            {
                                SetLabelsOnIssue set = 
                                    new SetLabelsOnIssue.Builder()
                                        .withIssueNumber(ghIssueNumber)
                                        .withRepo(repository)
                                        .withLabels(existingLabels)
                                        .build();

                                conn.execute(set);
                            }
                        }
                        catch (ExecutionException ex)
                        {
                            Logger.getLogger(JiraWebhook.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else if (item.getField().equals("assignee") && config.hasUserMappings())
                    {
                        try
                        {
                           String assignee = ModifyIssue.NO_ASSIGNEE; 

                            if (item.getToString() != null)
                            {
                                String mappedUser = config.getGithubUser(item.getTo());
                                if (mappedUser != null)
                                {
                                    assignee = mappedUser;
                                }
                            }
                            else 
                            {
                                // Need to query GH here for the current assignee and see 
                                // if it's a non-JIRA mapped user. Otherwise a re-assignment
                                // in GH to a non-Jira user will get nuked as JIRA sees it
                                // as an update to "no one assigned". 
                                
                                net.mostlyharmless.jghservice.connector.github.GetIssue get = 
                                    new net.mostlyharmless.jghservice.connector.github.GetIssue.Builder()
                                        .withRepository(repository)
                                        .withIssueNumber(ghIssueNumber)
                                        .build();
                                
                                GithubEvent.Issue issue = conn.execute(get);
                                
                                if (issue.hasAssignee())
                                {
                                    String ghAssignee = issue.getAssignee().getLogin();
                                    String jiraUser = config.getJiraUser(ghAssignee);
                                    if (jiraUser == null)
                                    {
                                        continue;
                                    }
                                }
                            }

                            ModifyIssue modify =
                                new ModifyIssue.Builder()
                                    .withAssignee(assignee)
                                    .withIssueNumber(ghIssueNumber)
                                    .withRepository(repository)
                                    .build();
                        
                            conn.execute(modify);
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
    
    private List<String> removeJiraStatusLabels(List<String> labels)
    {
        List<String> newList = new LinkedList<>();
        for (String label : labels)
        {
            if (!label.equals("JIRA: To Do") 
                && !label.equals("JIRA: In Progress")
                && !label.equals("JIRA: Needs Review")
                && !label.equals("JIRA: Closed")
                && !label.equals("JIRA: Reopened")
                && !label.equals("JIRA: Resolved"))
            {
                newList.add(label);
            }
        }
        return newList;
    }
    
    private List<String> getExistingLabels(GithubConnector conn, 
                                           ServiceConfig.Repository repository,
                                           int ghIssueNumber) throws ExecutionException
    {
        GetLabelsOnIssue getLabels =
            new GetLabelsOnIssue.Builder()
                .withIssueNumber(ghIssueNumber)
                .withRepo(repository)
                .build();

        return conn.execute(getLabels);
        
    }
    
}
