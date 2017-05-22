package groovy.workflows

import com.atlassian.jira.issue.MutableIssue

def subtasksSummaries = [
        "Set up your machine",
        "Sign up to HipChat",
        "Be social and say hello to @all"
]
def issue = issue as MutableIssue

if (issue.isSubTask())
    return


