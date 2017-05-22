package groovy.scriptedFields

import com.atlassian.jira.component.ComponentAccessor

ComponentAccessor.attachmentManager.getAttachments(issue)?.unique {it.filename}?.size() as Double