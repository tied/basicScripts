package groovy.scriptedFields

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def issue = issue as Issue
ComponentAccessor.attachmentManager.getAttachments(issue)?.unique {it.filename}?.size() as Double