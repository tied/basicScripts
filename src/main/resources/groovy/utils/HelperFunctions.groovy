package groovy.utils

import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.util.UserUtil
import org.apache.log4j.Logger
import org.apache.log4j.Level

class HelperFunctions {

    def log = Logger.getLogger("com.basicScripts.HelperFunctions")
    def adminUser = ComponentAccessor.getUserManager().getUserByName("adminBot")
    def userService = ComponentAccessor.getComponent(UserService)
    def userUtil = ComponentAccessor.getComponent(UserUtil)

    void createTestUsers (String ... userNames) {
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
            } else {
                log.error "Could not create user with userName $userName. ${result.errorCollection}"
            }
        }
    }

    void deleteUsers (String ... userNames) {
        log.setLevel(Level.INFO)

        userNames.each { String userName ->
            def user = ComponentAccessor.getUserManager().getUserByName(userName)
            userUtil.removeUser(adminUser, user)
            log.info "User $user removed"
        }
    }
}