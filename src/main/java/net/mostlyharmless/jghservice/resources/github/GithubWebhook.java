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
import net.mostlyharmless.jghservice.resources.ServiceConfig;
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
    private final Pattern jiraCommentPattern =
        Pattern.compile("\\[posted via JIRA by .+\\]\\*\\*\\*$");
    private final Pattern githubIssueMention =
        Pattern.compile("#(\\d+)");
    private final Pattern jiraIssueMention =
        Pattern.compile("(([A-Z]+)-\\d+)");
    private final Pattern extractCustomFieldNumber =
        Pattern.compile("customfield_(\\d+)");
    
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
        JiraConnector conn = new JiraConnector(config);
        
        
        if (event.hasIssue())
        {
            String title = event.getIssue().getTitle();
            Matcher m = jiraIssuePattern.matcher(title);
            if (m.find())
            {
                // Originated from JIRA, need to update JIRA with issue number and external link

                String githubIssueField = config.getJira().getGithubIssueNumberField();
                String jiraIssueKey = m.group(1);

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

                String jiraProjectKey = repo.getJiraProjectKey();
                String jiraRepoName = repo.getJiraName();
                int githubIssueNumber = event.getIssue().getNumber();

                String body = event.getIssue().getBody() +
                                "\n\n[Created in Github by " +
                                event.getIssue().getUser().getLogin() +
                                " ]";
                
                CreateIssue.Builder builder = 
                    new CreateIssue.Builder()
                        .withProjectKey(jiraProjectKey)
                        .withIssuetype("Task")
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
                    else
                    {
                        builder.withCustomField(field.getName(), field.getValue());
                    }
                }

                try
                {
                    String jiraKey = conn.execute(builder.build());
                    createExternalLink(conn, jiraKey, event);
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
        
        // Do the reverse for GH issue number mentions
        
        
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
                        body = body + " \n\n[posted via Github by " +
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
                    else
                    {
                        // The issue isn't in JIRA. Check to see if we should
                        // import it. This is for when the service is added to
                        // an existing repo
                        String repoName = event.getRepository().getName();
                        if (config.getRepoForGithubName(repoName).importOnComment())
                        {
                            processOpenedEvent(event);
                        }
                    }
                }
            }
        }
    }
}
