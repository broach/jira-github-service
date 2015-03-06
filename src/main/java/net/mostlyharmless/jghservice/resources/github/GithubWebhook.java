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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import net.mostlyharmless.jghservice.connector.github.GetCommentsOnIssue;
import net.mostlyharmless.jghservice.connector.github.GithubConnector;
import net.mostlyharmless.jghservice.connector.github.ModifyComment;
import net.mostlyharmless.jghservice.connector.github.UpdatePullRequest;
import net.mostlyharmless.jghservice.connector.jira.AddExternalLinkToIssue;
import net.mostlyharmless.jghservice.connector.jira.CreateIssue;
import net.mostlyharmless.jghservice.connector.jira.GetIssue;
import net.mostlyharmless.jghservice.connector.jira.JiraConnector;
import net.mostlyharmless.jghservice.connector.jira.PostComment;
import net.mostlyharmless.jghservice.connector.jira.SearchIssues;
import net.mostlyharmless.jghservice.connector.jira.UpdateIssue;
import net.mostlyharmless.jghservice.connector.jira.UpdateVersionsOnIssue;
import net.mostlyharmless.jghservice.resources.ServiceConfig;
import net.mostlyharmless.jghservice.resources.github.GithubEvent.Milestone;
import net.mostlyharmless.jghservice.resources.ServiceConfig.Repository;
import net.mostlyharmless.jghservice.resources.github.GithubEvent.Comment;
import net.mostlyharmless.jghservice.resources.jira.JiraEvent;

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
    private static final Pattern jiraCommentPattern =
        Pattern.compile("\\[posted via JIRA by .+\\]\\*\\*\\*$");
    private static final Pattern githubIssueMention =
        Pattern.compile("#(\\d+)");
    private static final Pattern jiraIssueMention =
        Pattern.compile("(([A-Z]+)-\\d+)");
    private static final Pattern extractCustomFieldNumber =
        Pattern.compile("customfield_(\\d+)");
    private static final Pattern extractFixedVersion =
        Pattern.compile("^Fixed in: (.+)$");
    private static final Pattern extractAffectsVersion =
        Pattern.compile("^Affects: (.*)$");
    private static final Pattern extractImportCommand = 
        Pattern.compile("^create jira issue.*", Pattern.CASE_INSENSITIVE);
    
    private static final String GITHUB_ISSUE_OPENED = "opened";
    private static final String GITHUB_COMMENT_CREATED = "created";
    private static final String GITHUB_ASSIGNED = "assigned";
    private static final String GITHUB_UNASSIGNED = "unassigned";
    private static final String GITHUB_LABELED = "labeled";
    private static final String GITHUB_UNLABELED = "unlabeled";
    
    private static final Logger LOGGER = Logger.getLogger(GithubWebhook.class.getName());
    
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
            case GITHUB_ASSIGNED:
            case GITHUB_UNASSIGNED:
                processAssigned(event);
                break;
            case GITHUB_LABELED:
            case GITHUB_UNLABELED:
                processLabeled(event);
                break;
            default:
                break;
                
        }
        return Response.ok().build();
    }
    
    private String processOpenedEvent(GithubEvent event)
    {
        JiraConnector conn = new JiraConnector(config);
        String jiraIssueKey = null;
        
        if (event.hasIssue())
        {
            String title = event.getIssue().getTitle();
            Matcher m = jiraIssuePattern.matcher(title);
            if (m.find())
            {
                // Originated from JIRA, need to update JIRA with issue number and external link

                String githubIssueField = config.getJira().getGithubIssueNumberField();
                jiraIssueKey = m.group(1);

                UpdateIssue update = 
                    new UpdateIssue.Builder()
                        .withCustomField(githubIssueField, event.getIssue().getNumber())
                        .withJiraIssueKey(jiraIssueKey)
                        .build();
                try
                {
                    conn.execute(update);
                    createExternalLink(conn, jiraIssueKey, event);
                }
                catch (ExecutionException ex)
                {
                    Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else
            {
                // Originated in github. Create in JIRA and add external link

                String githubIssueField = config.getJira().getGithubIssueNumberField();
                String jiraRepoField = config.getJira().getGithubRepoNameField();
                ServiceConfig.Repository repo = 
                    config.getRepoForGithubName(event.getRepository().getName());

                if (repo == null)
                {
                    LOGGER.log(Level.INFO, "No repo defined for: " + event.getRepository().getName());
                    return null;
                }
                
                String jiraProjectKey = repo.getJiraProjectKey();
                String jiraRepoName = repo.getJiraName();
                int githubIssueNumber = event.getIssue().getNumber();

                String body = event.getIssue().getBody() +
                                "\n\n[Created in Github by " +
                                event.getIssue().getUser().getLogin() +
                                " ]";
                
                /*
                 * If we're mapping milestones to eipcs, have some 
                 * work to do here. 
                 */
                String epicJiraKey = null;
                if (repo.mapEpicsToMilestones() && event.getIssue().hasMilestone())
                {
                    Milestone ms = event.getIssue().getMilestone();
                    String epicName = ms.getTitle();
                    
                    // Of course, they can't make this easy. Querying custom fields
                    // requires a format of cf[xxxx] rather than, you know, the field
                    // name.
                    m = extractCustomFieldNumber.matcher(config.getJira().getEpicNameField());
                    m.find();
                    String cfNumber = m.group(1);
                    String jql = "project = " + jiraProjectKey +
                                " and cf[" + cfNumber +
                                "] = \"" + epicName + "\""; 
                    
                    SearchIssues search = 
                        new SearchIssues.Builder()
                            .withJQL(jql)
                            .build();
                    try
                    {
                        List<JiraEvent.Issue> epicList = conn.execute(search);
                        // Should only return one or zero.
                        if (!epicList.isEmpty())
                        {
                            JiraEvent.Issue epic = epicList.get(0);
                            epicJiraKey = epic.getJiraIssueKey();
                        }
                        
                    }
                    catch (ExecutionException ex)
                    {
                        Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    
                }
                
                CreateIssue.Builder builder = 
                    new CreateIssue.Builder()
                        .withProjectKey(jiraProjectKey)
                        .withIssuetype("Story")
                        .withSummary(event.getIssue().getTitle())
                        .withDescription(body)
                        .withCustomField(githubIssueField, githubIssueNumber)
                        .withCustomField(jiraRepoField, "value", jiraRepoName);

                // populate any custom fields from repo config
                for (ServiceConfig.Repository.JiraField field : repo.getJiraFields())
                {
                    if (field.getType().equals("object"))
                    {
                        builder.withCustomField(field.getName(), field.getKey(), field.getValue());
                    }
                    else if (field.getType().equals("array"))
                    {
                        builder.withCustomArrayField(field.getName(), field.getKey(), field.getValue());
                    }
                    else
                    {
                        builder.withCustomField(field.getName(), field.getValue());
                    }
                }

                if (epicJiraKey != null)
                {
                    builder.withCustomField(config.getJira().getEpicLinkField(), epicJiraKey);
                }
                
                if (repo.labelVersions())
                {
                    List<String> fixVersions = new LinkedList<>();
                    List<String> affectsVersions = new LinkedList<>();
                    for (GithubEvent.Issue.Label label : event.getIssue().getLabels())
                    {
                        m = extractFixedVersion.matcher(label.getName());
                        if (m.find())
                        {
                            fixVersions.add(m.group(1));
                        }
                        else
                        {
                            m = extractAffectsVersion.matcher(label.getName());
                            if (m.find())
                            {
                                affectsVersions.add(m.group(1));
                            }
                        }
                    }
                    builder.withAffectsVersions(affectsVersions)
                            .withFixVersions(fixVersions);
                }
                
                if (config.hasUserMappings() && event.getIssue().hasAssignee())
                {
                    String jiraAssignee = 
                        config.getJiraUser(event.getIssue().getAssignee().getLogin());
                    
                    builder.withAssignee(jiraAssignee);
                }
                
                try
                {
                    jiraIssueKey = conn.execute(builder.build());
                    createExternalLink(conn, jiraIssueKey, event);
                }
                catch (ExecutionException ex)
                {
                    Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        else if (event.hasPullRequest())
        {
            linkPullRequestToIssue(conn, event);
            
        }
        
        return jiraIssueKey;
    }
    
    private void createExternalLink(JiraConnector conn, String jiraIssueKey, GithubEvent event) throws ExecutionException
    {
        AddExternalLinkToIssue.Builder builder = 
            new AddExternalLinkToIssue.Builder()
                .withJiraIssueKey(jiraIssueKey);
        
        if (event.hasIssue())
        {
            if (event.getIssue().isReallyAPullRequest())
            {
                builder.withRelationship("pull request");
            }
            else
            {
                builder.withRelationship("issue");
            }
            GithubEvent.Issue issue = event.getIssue();
            builder.withTitle(issue.getTitle())
                .withUrl(issue.getUrl());
        }
        else
        {
            GithubEvent.PullRequest pr = event.getPullRequest();
            builder.withRelationship("pull request")
                .withTitle(pr.getTitle())
                .withUrl(pr.getUrl());
        }
        
        AddExternalLinkToIssue add = builder.build();
        
        conn.execute(add);
                
    }
    
    private List<String> scanForGithubIssueMentions(String body)
    {
        // Scan body for Github issue mentions. Github does *not* notify 
        // via the webhook when it updates an issue with a PR link due to
        // a mention (or note it in the PR or issue metadata)
        Matcher m = githubIssueMention.matcher(body);
        List<String> ghIssueNumbers = new LinkedList<>();
        while (m.find())
        {
            ghIssueNumbers.add(m.group(1));
        }
        return ghIssueNumbers;
    }
    
    private List<String> scanForJiraIssueMentions(String body)
    {
        // Scan body for direct JIRA issue key mentions. 
        List<String> directJiraMentions = new LinkedList<>();
        Matcher m = jiraIssueMention.matcher(body);
        while (m.find())
        {
            if (config.getProjectKeys().contains(m.group(2)))
            {
                directJiraMentions.add(m.group(1));
            }
        }
        return directJiraMentions;
    }
    
    private void linkPullRequestToIssue(JiraConnector conn, GithubEvent event)
    {
        // PR created, see if it mentions a GH isse or JIRA issue
        // If it does, update in JIRA. 
        String body;
        if (event.hasPullRequest())
        {
            body = event.getPullRequest().getBody();
        }
        else // It's a pull request comment, disguised as an issue (wrapped in an Enigma)
        {
            body = event.getComment().getBody();
        }

        List<String> ghIssueNumbers = scanForGithubIssueMentions(body);
        List<String> directJiraMentions = scanForJiraIssueMentions(body);

        // For GH issues, we have to query JIRA and get back the issue
        // keys that have the issue in the github issue id field and add those
        // to the jira keys. 
        List<String> ghIssueMentions = new LinkedList<>();

        // Of course, they can't make this easy. Querying custom fields
        // requires a format of cf[xxxx] rather than, you know, the field
        // name.
        Matcher m = extractCustomFieldNumber.matcher(config.getJira().getGithubIssueNumberField());
        m.find();
        String cfNumber = m.group(1);
        ServiceConfig.Repository repo = 
            config.getRepoForGithubName(event.getRepository().getName());
        String jiraProjectKey = repo.getJiraProjectKey();

        Map<String, String> jiraKeyToGhNum = new HashMap<>();
        
        for (String ghIssueNum : ghIssueNumbers)
        {
            String jql = "project = " + jiraProjectKey +
                    " and cf[" + cfNumber +
                    "] = " + ghIssueNum;

            SearchIssues search = 
                new SearchIssues.Builder()
                    .withJQL(jql)
                    .build();
            try
            {
                List<JiraEvent.Issue> issues = conn.execute(search);
                for (JiraEvent.Issue issue : issues)
                {
                    ghIssueMentions.add(issue.getJiraIssueKey());
                    jiraKeyToGhNum.put(issue.getJiraIssueKey(), ghIssueNum);
                }
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Now we have all the JIRA issues mentioned in this PR, either
        // directly or indirectly. Update them with the link to the PR
        List<String> jiraIssueKeys = new LinkedList<>();
        jiraIssueKeys.addAll(directJiraMentions);
        jiraIssueKeys.addAll(ghIssueMentions);
        for (String jKey : jiraIssueKeys)
        {
            try
            {
                createExternalLink(conn, jKey, event);
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // For direct JIRA mentions, we want to update the PR
        // with the GH issue #. For GH Issue nums, the JIRA key. Unfortunately when 
        // you add an external link to a JIRA issue it doesn't send an 
        // issue update out. It seems as though editing a PR in GH doesn't
        // send out a update notice which is kinda annoying on one hand,
        // but should work well here. 

        GithubConnector ghConn = new GithubConnector(config);

        for (String jKey : jiraIssueKeys)
        {
            try
            {
                if (jiraKeyToGhNum.containsKey(jKey))
                {
                    String ghIssueNum = jiraKeyToGhNum.get(jKey);
                    body = body.replace("#" + ghIssueNum, "#" + ghIssueNum + " (" + jKey + ")");
                }
                else
                {
                    // Get GH issue number from issue in JIRA
                    GetIssue get = new GetIssue.Builder().withIssueKey(jKey).build();
                    JiraEvent.Issue issue = conn.execute(get);
                    // update this PR body with the GH issue number
                    if (issue.hasGithubIssueNumber(config))
                    {
                        body = body.replace(jKey, jKey + " (#" + issue.getGithubIssueNumber(config) +")");
                    }
                }
                
                if (event.hasPullRequest())
                {
                    UpdatePullRequest update = 
                        new UpdatePullRequest.Builder()
                            .withRepository(repo)
                            .withBody(body)
                            .withPullRequestNumber(event.getPullRequest().getNumber())
                            .build();

                    ghConn.execute(update);

                }
                else // pull request comment
                {
                    ModifyComment modify = 
                        new ModifyComment.Builder()
                            .withBody(body)
                            .withRepository(repo)
                            .withCommentId(event.getComment().getId())
                            .build();

                    ghConn.execute(modify);
                }
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void processCreatedEvent(GithubEvent event)
    {
        JiraConnector conn = new JiraConnector(config);
        
        if (event.hasIssue() && event.hasComment())
        {
            if (event.getIssue().isReallyAPullRequest())
            {
                // Allow comments on pull requests to link to an issue
                linkPullRequestToIssue(conn, event);
                // Allow for importing PRs without issues into JIRA
                createNewJiraIssueFromPR(conn, event);
            }
            else
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
                        postCommentToJira(conn, m.group(1), 
                                          event.getComment().getUser().getLogin(), 
                                          body);
                    }
                    else
                    {
                        // The issue isn't in JIRA. Check to see if we should
                        // import it. This is for when the service is added to
                        // a GH repo with existing issues.
                        Repository repo = config.getRepoForGithubName(event.getRepository().getName());
                        if (repo.importOnComment())
                        {
                            String jiraIssueKey = processOpenedEvent(event);
                            
                            // Now we have to import comments
                            GithubConnector ghConn = new GithubConnector(config);
                            
                            GetCommentsOnIssue get =
                                new GetCommentsOnIssue.Builder()
                                    .withRepository(repo)
                                    .withIssueNumber(event.getIssue().getNumber())
                                    .build();
                            try
                            {
                                List<GithubEvent.Comment> comments = ghConn.execute(get);
                                for (Comment comment : comments)
                                {
                                    postCommentToJira(conn, jiraIssueKey, 
                                                      comment.getUser().getLogin(), 
                                                      comment.getBody());
                                }
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
    }
    
    private void postCommentToJira(JiraConnector conn, String jiraIssueKey, String user, String body)
    {
        body = body + " \n\n[posted via Github by " +
                            user +
                            "]";

        PostComment post = 
            new PostComment.Builder()
                .withIssueKey(jiraIssueKey)
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
    
    private void processAssigned(GithubEvent event)
    {
        if (config.hasUserMappings())
        {
            GithubEvent.User ghUser = event.getAssignee();
            String jiraUser = config.getJiraUser(ghUser.getLogin());
            
            if (jiraUser != null)
            {
                // Get the current issue from JIRA
                String title = event.getIssue().getTitle();
                Matcher m = jiraIssuePattern.matcher(title); 
                if (m.find())
                {
                    JiraConnector conn = new JiraConnector(config);
                    GetIssue get = 
                        new GetIssue.Builder()
                            .withIssueKey(m.group(1))
                            .build();
                    try
                    {
                        JiraEvent.Issue issue = conn.execute(get);
                        String jiraCurrentAssignee = issue.getAssignee();
                        
                        if (event.getAction().equals(GITHUB_UNASSIGNED))
                        {
                            if (jiraCurrentAssignee != null && 
                                jiraCurrentAssignee.equals(jiraUser))
                            {
                                // Update Jira issue with no one assigned
                                UpdateIssue update = 
                                    new UpdateIssue.Builder()
                                        .withJiraIssueKey(m.group(1))
                                        .withAssignee(UpdateIssue.NO_ASSIGNEE)
                                        .build();
                                
                                conn.execute(update);
                            }
                        }
                        else
                        {
                            if (jiraCurrentAssignee == null ||
                                !jiraCurrentAssignee.equals(jiraUser))
                            {
                                UpdateIssue update = 
                                    new UpdateIssue.Builder()
                                        .withJiraIssueKey(m.group(1))
                                        .withAssignee(jiraUser)
                                        .build();
                                
                                conn.execute(update);
                            }
                        }
                    }
                    catch (ExecutionException ex)
                    {
                        Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    private void processLabeled(GithubEvent event)
    {
        // Should always be true but ... meh.
        if (event.hasLabel() && event.hasIssue())
        {
            // We're only looking for Version labels here.
            GithubEvent.Issue.Label label = event.getLabel();
            
            if (label.getName().startsWith("Fixed in:") ||
                label.getName().startsWith("Affects:"))
            {
                String title = event.getIssue().getTitle();
                Matcher m = jiraIssuePattern.matcher(title);
                if (m.find())
                {
                    String jiraIssueKey = m.group(1);

                    // Git the Jira issue and check the versions
                    JiraConnector conn = new JiraConnector(config);
                    
                    GetIssue get = 
                        new GetIssue.Builder()
                            .withIssueKey(jiraIssueKey)
                            .build();
                    try
                    {
                        JiraEvent.Issue jiraIssue = conn.execute(get);
                        
                        m = extractFixedVersion.matcher(label.getName());
                        if (m.find())
                        {
                            String version = m.group(1);
                            if (event.getAction().equals(GITHUB_LABELED) &&
                                !jiraIssue.getFixVersions().contains(version))
                            {
                                UpdateVersionsOnIssue update =
                                    new UpdateVersionsOnIssue.Builder()
                                        .withIssueKey(jiraIssueKey)
                                        .addFixVersion(version)
                                        .build();
                                
                                conn.execute(update);
                            }
                            else if (event.getAction().equals(GITHUB_UNLABELED) &&
                                     jiraIssue.getFixVersions().contains(version))
                            {
                                UpdateVersionsOnIssue update =
                                    new UpdateVersionsOnIssue.Builder()
                                        .withIssueKey(jiraIssueKey)
                                        .removeFixVersion(version)
                                        .build();
                                
                                conn.execute(update);
                            }

                        }
                        else
                        {
                            m = extractAffectsVersion.matcher(label.getName());
                            if (m.find())
                            {
                                String version = m.group(1);
                                if (event.getAction().equals(GITHUB_LABELED) &&
                                    !jiraIssue.getAffectsVersions().contains(version))
                                {
                                    UpdateVersionsOnIssue update =
                                        new UpdateVersionsOnIssue.Builder()
                                            .withIssueKey(jiraIssueKey)
                                            .addAffectsVersion(version)
                                            .build();
                                    
                                    conn.execute(update);
                                }
                                else if (event.getAction().equals(GITHUB_UNLABELED) &&
                                            jiraIssue.getAffectsVersions().contains(version))
                                {
                                    UpdateVersionsOnIssue update =
                                        new UpdateVersionsOnIssue.Builder()
                                            .withIssueKey(jiraIssueKey)
                                            .removeAffectsVersion(version)
                                            .build();
                                    
                                    conn.execute(update);
                                }
                            }
                        }
                        
                    }
                    catch (ExecutionException ex)
                    {
                        Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
            
            
        }
    }

    private void createNewJiraIssueFromPR(JiraConnector conn, GithubEvent event)
    {
        // PR created, see if we should create a JIRA issue
        String body;
        if (event.hasPullRequest())
        {
            body = event.getPullRequest().getBody();
        }
        else // It's a pull request comment, disguised as an issue (wrapped in an Enigma)
        {
            body = event.getComment().getBody();
        }
        
        Matcher m = extractImportCommand.matcher(body);
        if (m.find())
        {
            // Alrighty then. Create a new issue in JIRA to review this PR.
            String jiraRepoField = config.getJira().getGithubRepoNameField();
            ServiceConfig.Repository repo = 
                config.getRepoForGithubName(event.getRepository().getName());

            if (repo == null)
            {
                LOGGER.log(Level.INFO, "No repo defined for: {}", event.getRepository().getName());
                return;
            }
            
            String jiraProjectKey = repo.getJiraProjectKey();
            
            CreateIssue.Builder builder = 
                    new CreateIssue.Builder()
                        .withProjectKey(jiraProjectKey)
                        .withIssuetype("Story")
                        .withSummary("Review submitted PR")
                        .withDescription("The linked PR was imported from Github. It needs to be reviewed")
                        .withCustomField(jiraRepoField, "value", "Do Not Link To Repo");
            
            // populate any custom fields from repo config
            for (ServiceConfig.Repository.JiraField field : repo.getJiraFields())
            {
                switch (field.getType())
                {
                    case "object":
                        builder.withCustomField(field.getName(), field.getKey(), field.getValue());
                        break;
                    case "array":
                        builder.withCustomArrayField(field.getName(), field.getKey(), field.getValue());
                        break;
                    default:
                        builder.withCustomField(field.getName(), field.getValue());
                        break;
                }
            }
            
            try
            {
                String jiraIssueKey = conn.execute(builder.build());
                createExternalLink(conn, jiraIssueKey, event);
            }
            catch (ExecutionException ex)
            {
                Logger.getLogger(GithubWebhook.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
    }
}
