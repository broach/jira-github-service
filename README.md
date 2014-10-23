jira-github-service
===================

Web service that sits between Github and JIRA web hooks in order to sync the two systems.

## Current status
Overall this should work for anyone using JIRA agile and Github. See the `configExample` file for configuration. Note you must have the `context.xml` for the webapp pointed at a config.

There's still a bit of cruft in the code that could be cleaned up, but it works.

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
* Option to have existing issues imported to JIRA on next comment. 

### Pull Requests
* Mention in body via either Github's #xxx or JIRA's PROJECT-xxx creates external link on JIRA issue.
* JIRA's PROJECT-xxx mention will automatically cause Github's #xxx to be added to body and show up on linked issue.
* If you forget to add it to the body, a comment on the PR will now also trigger linking. 

### Epics & Milestones (optional)
* Epics created in JIRA are created in Github as Milestones
* Issues created in JIRA with an Epic link are put under the Milestone in Github
* Issues created in Github with (JIRA) Milestone are mapped to the Epic in JIRA. 
* Note that neither JIRA or Github send out a notification when an Epic or Milestone is assigned to an existing issue.

### Versions (optional)
* Affected Version/s and Fix Version/s in JIRA are labeled in GH
* Editing either in an issue in JIRA updates the labels in Github
 
### Assigned user mapping (optional)
* Mapped JIRA <-> Github usernames in config allow for users assigned to issues to be synced.
* Assign / Unassign user in JIRA -> Github
* Assign / Unassign user in Github -> JIRA
* Note that assigning an unmapped user in either system results in the opposite system to show unassigned.
