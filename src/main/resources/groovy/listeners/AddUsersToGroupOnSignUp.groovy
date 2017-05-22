package groovy.listeners

import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Level
import org.apache.log4j.Logger

/**
 * A script that assigns users to group upon their sing-up.
 * The group they will be assigned depends on their username.
 */

def log = Logger.getLogger("com.basicScripts.listeners.AddUserToGroupUponSignUp")
log.setLevel(Level.INFO)

final def ADMIN_GROUP = "jira-administrators"
final def USER_GROUP  = "jira-software-users"
final def DEFAULT_GROUP = "default-group"

def newUserName = event.user.name as String
def newUser = ComponentAccessor.userManager.getUserByName(newUserName)
def groupName

if (newUserName.contains("-admin")) {
    groupName = ADMIN_GROUP
}
else if (newUserName.contains("-user")) {
    groupName = USER_GROUP
}
else {
    groupName = DEFAULT_GROUP
}

try {
    def group = ComponentAccessor.groupManager.getGroup(groupName)
    ComponentAccessor.groupManager.addUserToGroup(newUser, group)
    log.info "User: $newUserName assigned to group: ${groupName}"
}
catch (all) {
    log.error "A error occured while trying to assign user: $newUserName to group: ${groupName}. ${all.getMessage()}"
}