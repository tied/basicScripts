package groovy.workflows

/**
 * A post function that will transit the Epic Issue to the Done status with a resolution,
 * when all the issues in the epic are in the Done status.
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import groovy.utils.HelperFunctions
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.basicScripts.workflows.LinkedIssuesTransition")
log.setLevel(Level.DEBUG)

final def LINK_TYPE = "Epic-Story Link"
final def ACTION_NAME = "Done"
final def STATUS_NAME = "Done"

def issue = issue as MutableIssue
def epicIssueLink = ComponentAccessor.issueLinkManager.getInwardLinks(issue.id)?.
        find {it.issueLinkType.name == LINK_TYPE}

if (! epicIssueLink)
    return

def epicIssue = epicIssueLink.sourceObject
def notDoneLinkedIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(epicIssue.id)?.
        findAll {it.issueLinkType.name == LINK_TYPE}?.
        findAll {it.destinationObject.status.name != STATUS_NAME }?.
        size()

// all the linked, with the Epic-Story Link, issues are Done, therefore transit the Epic to Done
if (! notDoneLinkedIssues) {
    new HelperFunctions().transitIssue(epicIssue, ACTION_NAME, null, "A post function closed the Epic", "Done") ?
            log.info ("Issue ${epicIssue.key} successfully transited to state ${issue.status.name}") :
            log.info ("Transition failed for issue ${epicIssue.key}")
}