package groovy.scriptedFields

import com.atlassian.jira.issue.MutableIssue

enableCache = {-> false}

def issue = issue as MutableIssue
final def STATUS_CATEGORY = "Complete"

def numOfSubtasks = issue?.subTaskObjects?.size()
if (! numOfSubtasks)
    return

def percent = (issue?.subTaskObjects?.findAll {it.status.statusCategory.name == STATUS_CATEGORY}?.size() / numOfSubtasks) * 100

return """
<progress id='subtasks-completed' value='$percent' max='100'/>
"""