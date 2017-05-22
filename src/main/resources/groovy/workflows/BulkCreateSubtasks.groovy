package groovy.workflows

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.config.PriorityManager
import com.atlassian.jira.issue.MutableIssue
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.basicScripts.workflows.BulkCreateSubtasks")
log.setLevel(Level.DEBUG)

def subtasksSummaries = [
        "Set up your machine",
        "Sign up to HipChat",
        "Be social and say hello to @all",
        "Learn how to use the coffee machine",
        "Make a coffee"
]

def ASSIGNEE_USERNAME = "alice"
def LINE_MANAGER = "admin"
def issue = issue as MutableIssue

def assignee = ComponentAccessor.getUserManager().getUserByName(ASSIGNEE_USERNAME)
def defaultPriority = ComponentAccessor.getComponent(PriorityManager).getDefaultPriority()
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def reporter = ComponentAccessor.getUserManager().getUserByName(LINE_MANAGER)
def subtaskIssueType = ComponentAccessor.getComponent(IssueTypeManager).getIssueTypes().find {it.name == "Sub-task"}

def subtaskManager = ComponentAccessor.getSubTaskManager()
def issueService = ComponentAccessor.getIssueService()

if (issue.isSubTask())
    return

subtasksSummaries.each {
    def inputParams = issueService.newIssueInputParameters()

    inputParams.setSummary(it)
    inputParams.setProjectId(issue.projectObject.id)
    inputParams.setPriorityId(defaultPriority?.id ?: issue.priority.id)
    inputParams.setIssueTypeId(subtaskIssueType.id)
    inputParams.setReporterId(reporter?.key ?: currentUser.key)
    inputParams.setAssigneeId(assignee?.key)

    def validationResult = issueService.validateSubTaskCreate(currentUser, issue.id, inputParams)

    if (validationResult.isValid()) {
        def subtask = issueService.create(currentUser, validationResult).issue
        subtaskManager.createSubTaskIssueLink(issue, subtask, currentUser)
    }
    else {
        log.error("Subtask creation failed. ${validationResult.errorCollection} ")
    }
}