package groovy.utils

import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.workflow.TransitionOptions
import org.apache.log4j.Logger
import org.apache.log4j.Level

class HelperFunctions {

    def log = Logger.getLogger("com.basicScripts.HelperFunctions")
    def adminUser = ComponentAccessor.getUserManager().getUserByName("adminBot")
    def userService = ComponentAccessor.getComponent(UserService)
    def userUtil = ComponentAccessor.getComponent(UserUtil)
    def issueService = ComponentAccessor.getIssueService()

    // -------------------------------- USER RELATED ------------------------------------------

    void createTestUsers (String ...userNames) {
        log.setLevel(Level.INFO)

        userNames.each { String userName ->
            UserService.CreateUserRequest createUserRequest = UserService.CreateUserRequest.withUserDetails(
                    adminUser,
                    userName,
                    "password",
                    "$userName@example.com".toString(),
                    "Test User - $userName".toString()
            )

            UserService.CreateUserValidationResult result = userService.validateCreateUser(createUserRequest)

            if (result.valid) {
                def newUser = userService.createUser(result)
                log.info "User: $newUser created "
            }
            else {
                log.error "Could not create user with userName $userName. ${result.errorCollection}"
            }
        }
    }

    void deleteUsers (String ...userNames) {
        log.setLevel(Level.INFO)

        userNames.each { String userName ->
            def user = ComponentAccessor.getUserManager().getUserByName(userName)
            userUtil.removeUser(adminUser, user)
            log.info "User $user removed"
        }
    }

    // --------------------------------------- ISSUE RELATED -----------------------------------------------

    def transitIssue (Issue issue, String actionName, ApplicationUser asUser = adminUser,
                        String comment = null, String resolution = null) {
        log.setLevel(Level.INFO)
        def resolutionId = ComponentAccessor.constantsManager.resolutions.find {it.name == resolution}?.id

        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.setResolutionId(resolutionId)
        issueInputParameters.setComment(comment)
        issueInputParameters.setSkipScreenCheck(true)

        def transitionOptions = new TransitionOptions.Builder()
                .skipConditions()
                .skipPermissions()
                .skipValidators()
                .build()

        def step = ComponentAccessor.workflowManager.getWorkflow(issue).allActions.find {it.name == actionName}
        def transitionValidationResult = issueService.validateTransition(asUser, issue?.id, step?.id, issueInputParameters, transitionOptions)

        if (transitionValidationResult.valid) {
            log.info "Issue ${issue.key} transitioned via step ${step.name}"
            issueService.transition(asUser, transitionValidationResult).getIssue()
        }
        else {
            log.error "Failed to reansit issue ${issue.key} via step ${step.name}"
            null
        }
    }

}