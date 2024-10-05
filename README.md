# SymantecPAM-ConfigFile
Symantec PAM Target Connector for plain text configuration files.

Having passwords as plain text in configuration files should never be used. 
However, there may be old systems and applications around where there are
no options than having a configuraion file with the username/password stored
in clear. It is what it is and you still need to update passwords in such 
configuration files.

This connector will allow password verify and update for users defined configuration files.

The connector copies the configuration file to the TCF server into the Tomcat temp directory.  
For a verify operation this now local file is inspected and it is validated if the
current password is found in the file.  
For an update operation a backup file is sent to the originating server (remote or local). This
is a copy of the original configuration file with a new filename. For simple match (without  Regex)
the current password is located in the file and replaced with a new password. 
For Regex search/replace the given regex for search is used to identify where the 
username/password is located. It is then replaced using the regex for replace. 
If the regex for search/replace is done correctly, it will allow update of the password in the 
configuration file even when the current password (in file) is not known 
to PAM. The updated file is send to the remote server.  
Finally, the temporary file on the TCF server is deleted.

## Build ConfigFile connector

### Environment
The environment used is as follows:

- CentOS 9 (with SELINUX)
- Java JDK, version 17.0.12
- Apache Tomcat, version 10.1.30
- Symantec PAM, version 4.2.0.826
- capamextensioncore, version 4.21.0.82

### Installation
- Download the project sources from GitHub.
- Add the `capamextensioncore.jar` from Symantec PAM as part of local Maven repository.
- Edit the files `configfile_messages.properties` and `ConfigFileMessageConstants.java`
and adjust the message numbers to to match your environment.
It is important that the numbers does not conflict with any other numbers from other connectors.
- There is an important variable in the `ConfigFile.java` file. It is the constant `EXTENDED_DEBUG`.
If this is set to `true` when compiling the connector, additional debugging information may be written to the
`catalina.out` log file. If it is written will depend on the loglevel for the connector as
defined in the Tomcat `logging.properties` file. If extended debugging is enabled at compile time,
additional information including current and new passwords may be visible in the `catalina.out` log file.
This should not be enabled when compiling the connector for a production environment.
- Run the command `mvnw package` to compile the connector.
- Copy the target connector `configfile.war` to the Tomcat `webapps_targetconnector` directory.
- It is recommended to enable logging from the connector by adding the following to the
Tomcat `logging.properties` file.

```
#
# Target Connectors
#
ch.pam_exchange.pam_tc.configfile.api.level = FINE
ch.pam_exchange.pam_tc.configfile.api.handlers= java.util.logging.ConsoleHandler

ch.pam_exchange.pam_tc.filecopy.api.level = FINE
ch.pam_exchange.pam_tc.filecopy.api.handlers= java.util.logging.ConsoleHandler
```

- Finally start/restart Tomcat

## ConfigFile configuration

### Appliction

When adding an application for the ConfigFile connector, you should use a password
composition policy without special characters. The password inserted in the configuration
file is not escaped.

![ConfigFile Appliction](/docs/ConfigFile-Application.png)

The fields used are:

- Location of keystore file  
API/CLI field: `location`  
This can be `Remote UNIX`, `Remote Windows` or `Local TCF`. Set the radio button
accordingly.


- Port (for Remote UNIX) 
API/CLI field: `port`   
This is the port used when establishing an SSH connection to a remote UNIX server.


### Account

The information for a Config File account will require a login account. This account
is an account configured in PAM. It can be a domain or local account.

![ConfigFile Account](/docs/ConfigFile-Account.png)

The fields used are:

API/CLI field name: `loginAccount`  
Valid value is the internal ID for an account defined in PAM.  
If the application uses a remote Windows or UNIX system, select an account with
login permissions to the remote server. The account must have read/write permissions
to the configuration path and filename.

- Config file (path+) filename    
API/CLI field name: `filename`  
Specify the path + filename for the configuration file.
For Windows servers the protocol used is SMB and you must specify a share/path to the file.
This can be `c$/tmp/tomcat-users.xml` or any other network share on the server.
For UNIX servers just specify the path and filename for the configuration file.
The login account must have read/write permissions to the path and file.
For files on the local TCF server, specify the path+filename. The account running Tomcat
must have read/write permissions to the path and file.

- Create backup file    
API/CLI field name: `createBackup`  
Valid values are `true` and `false`  
If this is checked a backup of the keystore file is created. The login account
must have permissions to create a new file in the path for the configuration file.


- Use Regex for verify/update    
API/CLI field name: `useRegex`  
Valid values are `true` and `false`  
This will enable use of regex in verify, search and replace of password in the configuration
file. If this is not enabled a simple text search for the current password is used. The password
update will search for the current password (exact match) and replace it with a new password.

- Verify regex    
API/CLI field name: `verifyRegex`  
This is the regex used to verify if the configuration file has the correct password.
The username of the account is specified as `$USERNAME$` (upper case). The password of the
account is specified as `$PASSWORD$` (upper case). Both will be replaces with the account
username and current password.

- Update regex (search)    
API/CLI field name: `searchRegex`  
This is the regex used with identifying where in the configuration file the username/password
is to be found. Use `()` to group the search. They are used in the replace regex.

- Update regex (replace)    
API/CLI field name: `replaceRegex`  
This is the regex used when replacing the password. It uses the groups found in the search
regex. Groups are referenced as `$1`, `$2`, etc.
The username of the account is specified as `$USERNAME$` (upper case). The password of the
account is specified as `$PASSWORD$` (upper case). Both will be replaces with the account
username and new password.


## Example configuration file

The example used to test the connector is a tomcat-users.xml file. It is available
on a remote Windows, remote Linux and locally on the TCF server.

```
<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">
  <role rolename="tomcat"/>
  <role rolename="role1"/>
  <user username="tomcat" password="<must-be-changed>" roles="tomcat"/>
  <user username="both" password="<must-be-changed>" roles="tomcat,role1"/>
  <user username="role1" password="<must-be-changed>" roles="role1"/>
</tomcat-users>
```

In the example the account name is `tomcat` and the regex defaults is what will
work for this configuration file.

```
<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">
  <role rolename="tomcat"/>
  <role rolename="role1"/>
  <user username="tomcat" password="50LBED2hPg7zoh96PhyoJ7Qc" roles="tomcat"/>
  <user username="both" password="<must-be-changed>" roles="tomcat,role1"/>
  <user username="role1" password="<must-be-changed>" roles="role1"/>
</tomcat-users>
```

## Login accounts in PAM
In PAM it is necessary to setup accounts, which can login to the remote servers.
For the Windows server it can be a local user or a domain user. What is important is
that the user can connect to the `c$` share or a share where the configuration file
is located.
For the (remote) Linux server an account is required to login to the server. The permissions
for the user must be sufficient to write to the directory and the file used.

**Windows login account**

![Login Account for Windows](/docs/LoginAccount-Windows.png)


## Version history

1.0.0 - Initial release

