package ch.pam_exchange.pam_tc.configfile.api;

import com.ca.pam.extensions.core.util.MessageConstants;
import com.ca.pam.extensions.core.api.exception.ExtensionException;
import com.ca.pam.extensions.core.model.LoggerWrapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ca.pam.extensions.core.MasterAccount;
import com.ca.pam.extensions.core.TargetAccount;

import ch.pam_exchange.pam_tc.filecopy.api.FileCopy;
import ch.pam_exchange.pam_tc.filecopy.api.FileCopyMessageConstants;

public class ConfigFile {

	private static final Logger LOGGER = Logger.getLogger(ConfigFile.class.getName());
	private static final boolean EXTENDED_DEBUG = false;

	/*
	 * Constants
	 */
	private static final int DEFAULT_PORT = 22;
	private static final String LOCATION_LOCAL = "local";
	private static final String LOCATION_REMOTEUNIX = "remoteUNIX";
	private static final String LOCATION_REMOTEWINDOWS = "remoteWindows";

	private static final String FIELD_LOCATION = "location";
	private static final String FIELD_PORT = "port";
	private static final String FIELD_DOMAIN = "domain";
	private static final String FIELD_LOGINACCOUNT = "loginAccount";
	private static final String FIELD_FILENAME = "filename";
	private static final String FIELD_CREATEBACKUP = "createBackup";
	private static final String FIELD_USEREGEX = "useRegex";
	private static final String FIELD_VERIFYREGEX = "verifyRegex";
	private static final String FIELD_SEARCHREGEX = "searchRegex";
	private static final String FIELD_REPLACEREGEX = "replaceRegex";

	/*
	 * Instance variables used in the processCredentialsVerify and
	 * processCredentialsUpdate
	 */
	private String location = LOCATION_LOCAL;
	private String username = "";
	private String oldPassword = "";
	private String newPassword = "";
	private String hostname = "";
	private int port = DEFAULT_PORT;
	private MasterAccount loginAccount = null;
	// private MasterAccount searchAccount= null;
	private String loginUsername = "";
	private String loginPassword = "";
	private String domain = "";
	private String filename = "";
	private boolean createBackup = false;
	private boolean useRegex = false;
	private String verifyRegex = "";
	private String searchRegex = "";
	private String replaceRegex = "";

	/*
	 * Constructor
	 */
	public ConfigFile(TargetAccount targetAccount) {

		this.username = targetAccount.getUserName();
		LOGGER.fine(LoggerWrapper.logMessage("username= " + this.username));

		this.oldPassword = targetAccount.getOldPassword();
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("oldPassword= " + this.oldPassword));

		this.newPassword = targetAccount.getPassword();
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("newPassword= " + this.newPassword));

		this.location = targetAccount.getTargetApplication().getExtendedAttribute(FIELD_LOCATION);
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_LOCATION + "= " + this.location));

		if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
			this.hostname = targetAccount.getTargetApplication().getTargetServer().getHostName();
			LOGGER.fine(LoggerWrapper.logMessage("hostname= " + this.hostname));
			try {
				this.port = Integer
						.parseUnsignedInt(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_PORT));
			} catch (Exception e) {
				LOGGER.warning("Using default port");
				this.port = DEFAULT_PORT;
			}
			LOGGER.fine(LoggerWrapper.logMessage(FIELD_PORT + "= " + this.port));

			this.domain = targetAccount.getTargetApplication().getExtendedAttribute(FIELD_DOMAIN);
			LOGGER.fine(LoggerWrapper.logMessage(FIELD_DOMAIN + "= " + this.domain));

			this.loginAccount = targetAccount.getMasterAccount(FIELD_LOGINACCOUNT);
			this.loginUsername = loginAccount.getUserName();
			if (this.loginUsername == null || this.loginUsername.isEmpty()) {
				LOGGER.severe(LoggerWrapper.logMessage("loginUsername is empty"));
			} else {
				LOGGER.fine(LoggerWrapper.logMessage("loginUsername= " + this.loginUsername));

				if (LOCATION_REMOTEUNIX.equals(this.location) && this.domain.length() > 0) {
					// if remote system is UNIX and domain is used, build the domain
					// loginAccount

					if (this.domain.contains(".")) {
						this.loginUsername = this.loginUsername + "@" + this.domain;
					} else {
						this.loginUsername = this.domain + "\\" + this.loginUsername;
					}
					LOGGER.fine("Updated loginUsername= " + this.loginUsername);
				}
			}
			this.loginPassword = loginAccount.getPassword();
			if (this.loginPassword == null || this.loginPassword.isEmpty()) {
				LOGGER.severe(LoggerWrapper.logMessage("loginPassword is empty"));
			} else {
				if (EXTENDED_DEBUG)
					LOGGER.fine(LoggerWrapper.logMessage("loginPassword= " + this.loginPassword));
			}

			// searchAccount= targetAccount.getMasterAccount("searchAccount");
			// LOGGER.info("searchUsername= "+searchAccount.getUserName());
			// LOGGER.info("searchPassword= "+searchAccount.getPassword());
		}

		this.filename = targetAccount.getExtendedAttribute(FIELD_FILENAME).replace("\\", "/");
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_FILENAME + "= " + this.filename));

		this.createBackup = "true".equals(targetAccount.getExtendedAttribute(FIELD_CREATEBACKUP));
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_CREATEBACKUP + "= " + this.createBackup));

		/*
		 * regex options
		 */
		this.useRegex = "true".equals(targetAccount.getExtendedAttribute(FIELD_USEREGEX));
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_USEREGEX + "= " + this.useRegex));

		if (this.useRegex) {
			this.verifyRegex = targetAccount.getExtendedAttribute(FIELD_VERIFYREGEX);
			if (this.verifyRegex == null)
				this.verifyRegex = "";
			LOGGER.fine(LoggerWrapper.logMessage(FIELD_VERIFYREGEX + "= " + this.verifyRegex));

			this.searchRegex = targetAccount.getExtendedAttribute(FIELD_SEARCHREGEX);
			if (this.searchRegex == null)
				this.searchRegex = "";
			LOGGER.fine(LoggerWrapper.logMessage(FIELD_SEARCHREGEX + "= " + this.searchRegex));

			this.replaceRegex = targetAccount.getExtendedAttribute(FIELD_REPLACEREGEX);
			if (this.replaceRegex == null)
				this.replaceRegex = "";
			LOGGER.fine(LoggerWrapper.logMessage(FIELD_REPLACEREGEX + "= " + this.replaceRegex));
		}
	}

	/**
	 * Verifies credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 * 
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while verifying the
	 *                            credential
	 *
	 */
	public void configFileCredentialVerify() throws ExtensionException {

		String tmpFilename = "";
		boolean passwordFound = true;
		FileCopy fc = null;

		if (LOCATION_LOCAL.equals(this.location)) {
			File f = new File(this.filename);
			if (!f.exists()) {
				LOGGER.severe(LoggerWrapper.logMessage("File '" + this.filename + "' not found"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, this.filename);
			} else {
				/*
				 * For local, just use the filename
				 */
				tmpFilename = this.filename;
			}

		} else {
			// remote ...

			if (LOCATION_REMOTEWINDOWS.equals(this.location)) {
				fc = new FileCopy(this.loginUsername, this.loginPassword, this.domain, this.hostname);
			} else {
				fc = new FileCopy(this.loginUsername, this.loginPassword, this.hostname, this.port);
			}

			tmpFilename = fc.getTempFilename().replace("\\", "/");
			LOGGER.fine(LoggerWrapper.logMessage("tmpFilename= " + tmpFilename));

			fc.copyFromRemote(filename, tmpFilename);
			LOGGER.fine(LoggerWrapper.logMessage("File copy complete"));
		}

		/*
		 * password verify 
		 */
		try {
			/*
			 * Load content from file and verify password (or pattern) is found
			 */
			String content = new String(Files.readAllBytes(Paths.get(tmpFilename)), "UTF-8");
			if (EXTENDED_DEBUG)
				LOGGER.fine(LoggerWrapper.logMessage("File content= " + content));

			if (this.useRegex) {
				/*
				 * Regex match
				 */
				String patternStr = this.verifyRegex.replace("$USERNAME$", this.username).replace("$PASSWORD$",
						this.newPassword);
				if (EXTENDED_DEBUG)
					LOGGER.fine(LoggerWrapper.logMessage("Effective verifyRegex pattern= " + patternStr));

				Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE | Pattern.DOTALL);
				Matcher matcher = pattern.matcher(content);
				if (matcher.find()) {
					LOGGER.fine(LoggerWrapper.logMessage("Password is found"));
					passwordFound = true;
				} else {
					LOGGER.fine(LoggerWrapper.logMessage("Password is not found"));
					// throw new ExtensionException(ConfigFileMessageConstants.ERR_PASSWORD, false);
					passwordFound = false;
				}
			} else {
				/*
				 * Simple match
				 */
				if (this.newPassword.isEmpty() || content.indexOf(this.newPassword) == -1) {
					LOGGER.warning(LoggerWrapper.logMessage("Password not found in file"));
					// throw new ExtensionException(ConfigFileMessageConstants.ERR_PASSWORD, false);
					passwordFound = false;
				} else {
					LOGGER.fine(LoggerWrapper.logMessage("Password found in file"));
					passwordFound = true;
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
			throw new ExtensionException(ConfigFileMessageConstants.ERR_EXCEPTION, false);
		} finally {
			/*
			 * remove tmp file received from remote
			 */
			if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
				try {
					LOGGER.fine(LoggerWrapper.logMessage("Remove file, filename= " + tmpFilename));
					File f = new File(tmpFilename);
					f.delete();
				} catch (Exception e) {
					LOGGER.warning(LoggerWrapper.logMessage("File delete - " + e.getMessage()));
				}
			}
		}

		if (!passwordFound) {
			LOGGER.info(LoggerWrapper.logMessage("Config File password verified - Not OK"));
			throw new ExtensionException(ConfigFileMessageConstants.ERR_PASSWORD, false);
		}
	}

	/**
	 * Updates credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 * 
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while update the
	 *                            credential
	 */
	public void configFileCredentialUpdate() throws ExtensionException {

		boolean passwordUpdated = false;
		String tmpFilename = "";
		FileCopy fc = null;

		/*
		 * remote: Copy remote file to TCF server If a backup is required, copy it back
		 * to remote server local: Create a backup of file
		 */

		if (LOCATION_LOCAL.equals(this.location)) {
			File f = new File(this.filename);
			if (!f.exists()) {
				LOGGER.severe(LoggerWrapper.logMessage("File '" + this.filename + "' not found"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, this.filename);
			} else {
				/*
				 * no need to copy to a tmp file on local
				 */
				tmpFilename = this.filename;

				if (this.createBackup) {
					fc = new FileCopy();
					String backupFilename = fc.getBackupFilename(this.filename);
					LOGGER.fine(LoggerWrapper.logMessage("backupFilename= " + backupFilename));
					fc.copyLocal(tmpFilename, backupFilename);
					LOGGER.info(LoggerWrapper.logMessage("Backup copy complete - " + backupFilename));
				}
			}

		} else {
			if (LOCATION_REMOTEWINDOWS.equals(this.location)) {
				fc = new FileCopy(this.loginUsername, this.loginPassword, this.domain, this.hostname);
			} else {
				fc = new FileCopy(this.loginUsername, this.loginPassword, this.hostname, this.port);
			}

			tmpFilename = fc.getTempFilename().replace("\\", "/");
			LOGGER.fine(LoggerWrapper.logMessage("tmpFilename= " + tmpFilename));
			fc.copyFromRemote(filename, tmpFilename);
			LOGGER.fine(LoggerWrapper.logMessage("File copy complete"));

			if (this.createBackup) {
				String backupFilename = fc.getBackupFilename(this.filename);
				LOGGER.fine(LoggerWrapper.logMessage("backupFilename= " + backupFilename));
				fc.copyToRemote(tmpFilename, backupFilename);
				LOGGER.info(LoggerWrapper.logMessage("Backup copy complete - " + backupFilename));
			}
		}

		/*
		 * The file is available in tmpFilename (copied from remote or just the original
		 * filename). Verify that password (or placeholder) is found Update with content
		 * with new password
		 */
		String newContent = "";
		try {
			String content = new String(Files.readAllBytes(Paths.get(tmpFilename)), "UTF-8");

			if (this.useRegex) {
				String searchStr = searchRegex.replace("$USERNAME$", this.username).replace("$PASSWORD$",
						this.newPassword);
				if (EXTENDED_DEBUG)
					LOGGER.fine(LoggerWrapper.logMessage("Effective searchRegex searchStr= " + searchStr));

				Pattern pattern = Pattern.compile(searchStr, Pattern.MULTILINE | Pattern.DOTALL);
				Matcher matcher = pattern.matcher(content);
				if (matcher.find()) {
					passwordUpdated = true;
					String replaceStr = replaceRegex.replace("$USERNAME$", this.username).replace("$PASSWORD$",
							this.newPassword);
					if (EXTENDED_DEBUG)
						LOGGER.fine(LoggerWrapper.logMessage("Effective replaceRegex replaceStr= " + replaceStr));

					newContent = content.replaceAll(searchStr, replaceStr);
					if (EXTENDED_DEBUG)
						LOGGER.fine(LoggerWrapper.logMessage("newContent: " + newContent));
				} else {
					/*
					 * Password not found in content
					 */
					passwordUpdated = false;
					LOGGER.severe(LoggerWrapper.logMessage("Current password not found in config file"));
					throw new ExtensionException(ConfigFileMessageConstants.ERR_PASSWORD, false);
				}
			} else {
				/*
				 * Simple match
				 */
				if (content.indexOf(this.oldPassword) == -1) {
					passwordUpdated = false;
					LOGGER.severe(LoggerWrapper.logMessage("Current password not found in config file"));
					throw new ExtensionException(ConfigFileMessageConstants.ERR_PASSWORD, false);
				} else {
					passwordUpdated = true;
					newContent = content.replace(this.oldPassword, this.newPassword);
					if (EXTENDED_DEBUG)
						LOGGER.fine(LoggerWrapper.logMessage("newContent: " + newContent));
				}
			}

			if (passwordUpdated) {
				/*
				 * save newContent to tmpFilename.
				 * if remote, copy file
				 */
				Files.write(Paths.get(tmpFilename), newContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

				if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
					fc.copyToRemote(tmpFilename, this.filename);
					LOGGER.info(LoggerWrapper.logMessage("Copy file to remote complete - " + this.filename));
				} else {
					LOGGER.info(LoggerWrapper.logMessage("Local file updated - " + this.filename));
					// local file on TCF
					// tmpFilename is the same as filename -- no need to copy
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("--> General Exception"), e);
			throw new ExtensionException(ConfigFileMessageConstants.ERR_EXCEPTION, false);
		} finally {
			/*
			 * remove tmp file from remote
			 */
			if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
				try {
					LOGGER.fine(LoggerWrapper.logMessage("Remove file, filename= " + tmpFilename));
					File f = new File(tmpFilename);
					f.delete();
				} catch (Exception e) {
					LOGGER.warning(LoggerWrapper.logMessage("File delete - " + e.getMessage()));
				}
			}
		}
	}

}
