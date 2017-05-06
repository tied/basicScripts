package groovy.utils

import com.atlassian.event.EventManager
import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.DefaultIssueEventBundle
import com.atlassian.jira.event.issue.IssueEventBundleFactory
import com.atlassian.jira.event.issue.IssueEventBundleFactoryImpl
import com.atlassian.jira.event.issue.IssueEventManager
import com.atlassian.jira.event.type.EventType
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer
import org.apache.log4j.Logger
import org.apache.log4j.Level

import java.awt.Component

class HelperFunctions {

    def userService = ComponentAccessor.getComponent(UserService)
    def userUtil = ComponentAccessor.getComponent(UserUtil)
    def issueService = ComponentAccessor.issueService
    def userManager = ComponentAccessor.userManager
    def jiraAuthenticationContext = ComponentAccessor.jiraAuthenticationContext

    def log = Logger.getLogger("com.basicScripts.HelperFunctions")
    def adminUser = userManager.getUserByName("adminBot") ?: jiraAuthenticationContext.getLoggedInUser()

    // -------------------------------- USER RELATED -------------------------------------------------------------------

    void createUser (String userName) {
        log.setLevel(Level.INFO)

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

    /**
     * Toggle the status of a user between active and inactive
     * @param userName The username to toggle it's activity
     * @param mode The mode can be active or inactive
     */
    def toggleUserStatus (String userName, String mode) {
        log.setLevel(Level.INFO)

        def activity = mode == "active"
        def user = userManager.getUserByName(userName)
        if (! user) {
            log.error "Could not find user with user name $userName"
            return
        }

        def updateUser = userService.newUserBuilder(user).active(activity).build()
        def updateUserValidationResult = userService.validateUpdateUser(updateUser)

        if (updateUserValidationResult.isValid()) {
            userService.updateUser(updateUserValidationResult)
            log.info "${updateUser.name} account became $mode"
            sendEmail(updateUser.emailAddress, "JIRA account became $mode", "Your jira became account $mode")
        } else {
            log.error "Could not togle status for user ${user.key} ${updateUserValidationResult.getErrorCollection()}"
        }
    }

    void deleteUser (String userName) {
        log.setLevel(Level.INFO)

        def user = userManager.getUserByName(userName)
        if (! user ) {
            log.info "Could not find user with username $userName"
            return
        }
        userUtil.removeUser(adminUser, user)
        log.info "User $user removed"
    }

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
        def transitionValidationResult = issueService.validateTransition(asUser, issue?.id, step?.id,
                issueInputParameters, transitionOptions)

        if (transitionValidationResult.valid) {
            log.info "Issue ${issue.key} transitioned via step ${step.name}"
            issueService.transition(asUser, transitionValidationResult).getIssue()
        }
        else {
            log.error "Failed to reansit issue ${issue.key} via step ${step.name}. " +
                    "Errors: ${transitionValidationResult.errorCollection}"
            null
        }
    }

    def sendEmail(String sendTo, String subject, String body, String sendFrom = adminUser.emailAddress,
                  String sendCC = null, String sendBcc = null) {
        SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()

        if (mailServer) {
            Email email = new Email(sendTo).with {
                setSubject(subject)
                setBody(body)
                setFrom(sendFrom)
                setCc(sendCC)
                setBcc(sendBcc)
            }
            mailServer.send(email)
        } else {
            log.warn("Could not find mail server, please check if enabled.")
        }
    }
}