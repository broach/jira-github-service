jira-github-service
===================

Web service that sits between Github and JIRA.

See the `configExample` file for configuration. Note you must have the `context.xml` for the webapp pointed at a config.

## Current features
### Issues
* Created in Github -> JIRA
* Created in JIRA -> Github
* External link added to JIRA issue.
* Comments in Github -> JIRA
* Comments in JIRA -> Github
* Close / Resolve in JIRA -> closed on Github
* Reopen Issue in JIRA -> reopen in Github
* Issues moved along agile board in JIRA -> tags in Github reflect agile state

### Pull Requests
* Mention in body via either Github's #xxx or JIRA's PROJECT-xxx creates external link on JIRA issue.
* JIRA's PROJECT-xxx mention will automatically cause Github's #xxx to be added to body and show up on linked issue.
* If you forget to add it to the body, a comment on the PR will now also trigger linking. 