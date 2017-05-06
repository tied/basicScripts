package groovy.utils

import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer
import org.apache.log4j.Logger
import org.apache.log4j.Level

class HelperFunctions {

    def userService = ComponentAccessor.getComponent(UserService)
    def userUtil = ComponentAccessor.getComponent(UserUtil)
    def issueService = ComponentAccessor.issueService
    def userManager = ComponentAccessor.userManager
    def jiraAuthenticationContext = ComponentAccessor.jiraAuthenticationContext

    def log = Logger.getLogger("com.basicScripts.HelperFunctions")
    def adminUser = userManager.getUserByName("adminBot") ?: jiraAuthenticationContext.getLoggedInUser()

    // -------------------------------- USER RELATED -------------------------------------------------------------------

    void createTestUsers (String ...userNames) {
        log.setLevel(Level.INFO)

        userNames?.each { String userName ->
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

    def deactivateUser(String userName) {
        log.setLevel(Level.INFO)

        def user = userManager.getUserByName(userName)
        if (! user) {
            log.error "Could not find user with user name $userName"
            return
        }

        def updateUser = userService.newUserBuilder(user).active(false).build()
        def updateUserValidationResult = userService.validateUpdateUser(updateUser)

        if (updateUserValidationResult.isValid()) {
            userService.updateUser(updateUserValidationResult)
            log.info "${updateUser.name} deactivated"
            sendEmail(updateUser.emailAddress, "JIRA account deactivated", "Your jira account deactivated")
        } else {
            log.error "Could not deacrtivate user ${user.key} ${updateUserValidationResult.getErrorCollection()}"
        }
    }

    void deleteUsers (String ...userNames) {
        log.setLevel(Level.INFO)

        userNames?.each { String userName ->
            def user = userManager.getUserByName(userName)
            if (! user ) {
                log.info "Could not find user with username $userName"
                return
            }
            userUtil.removeUser(adminUser, user)
            log.info "User $user removed"
        }
    }

    // ------------------------------------------------- ISSUE RELATED -------------------------------------------------

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

    // ---------------------------------------------------- GENERAL UTIL FUNCTIONS -------------------------------------

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