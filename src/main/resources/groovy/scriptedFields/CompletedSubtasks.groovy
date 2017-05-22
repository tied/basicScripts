package groovy.scriptedFields

/**
 * A scripted field that will add a progress bar in the issue view screen and will
 * indicates the number of the completed subtasks as a percentage
 * Template should be HTML
 * For the progress bar styling use css/progressBar.css
 */
import com.atlassian.jira.issue.MutableIssue

enableCache = {-> false}

def issue = issue as MutableIssue
final def STATUS_CATEGORY = "Complete"

def numOfSubtasks = issue.subTaskObjects?.size()
if (! numOfSubtasks)
    return

def percent = (issue.subTaskObjects?.findAll {it.status.statusCategory.name == STATUS_CATEGORY}?.size() / numOfSubtasks) * 100
"""<progress id='subtasks-completed' value='$percent' max='100'/>"""