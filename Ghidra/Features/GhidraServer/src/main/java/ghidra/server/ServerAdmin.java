/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.server;

import java.io.*;
import java.util.*;

import javax.security.auth.x500.X500Principal;

import generic.jar.ResourceFile;
import ghidra.GhidraApplicationLayout;
import ghidra.GhidraLaunchable;
import ghidra.framework.Application;
import ghidra.framework.ApplicationConfiguration;
import ghidra.util.*;

public class ServerAdmin implements GhidraLaunchable {

	private static final String CONFIG_FILE_PROPERTY = "UserAdmin.config";

	// property name defined within the sever.conf file which specifies
	// server repositories directory
	private static final String SERVER_DIR_CONFIG_PROPERTY = "ghidra.repositories.dir";

	private static final String INVOCATION_NAME_PROPERTY = "UserAdmin.invocation";

	// Immediate commands
	private static final String LIST_COMMAND = "-list";
	private static final String USERS_COMMAND = "-users";

	// Delayed commands
	private static final String MIGRATE_COMMAND = "-migrate";
	private static final String MIGRATE_ALL_COMMAND = "-migrate-all";

	/**
	 * Main method for launching the ServerAdmin Application via GhidraLauncher.
	 * The following properties may be set:
	 * <pre>
	 *   UserAdmin.invocation - identifies the name of the application used when displaying usage text.
	 *   UserAdmin.config - identifies the config file instead of passing on command line.
	 * </pre>
	 * @param args command line arguments
	 */
	@Override
	public void launch(GhidraApplicationLayout layout, String[] args) {

		// Perform static initializations if not already initialized
		// Some tests invoke main method directly which have already initialized Application
		if (!Application.isInitialized()) {
			ApplicationConfiguration configuration = new ApplicationConfiguration();
			configuration.setInitializeLogging(false);
			Application.initializeApplication(layout, configuration);
		}

		execute(args);
	}

	/**
	 * Main method for processing ServerAdmin command line arguments.
	 * The following properties may be set:
	 * <pre>
	 *   UserAdmin.invocation - identifies the name of the application used when displaying usage text.
	 *   UserAdmin.config - identifies the config file instead of passing on command line.
	 * </pre>
	 * @param args command line arguments
	 */
	public void execute(String[] args) {

		int ix = 0;

		String configFilePath = args.length != 0 && !args[0].startsWith("-") ? args[ix++]
				: System.getProperty(CONFIG_FILE_PROPERTY);

		System.out.println("server.conf: " + configFilePath);

		File serverRootDir = getServerDirFromConfig(configFilePath);
		if (serverRootDir == null || (args.length - ix) == 0) {
			displayUsage("");
			System.exit(-1);
			return;
		}

		try {
			serverRootDir = serverRootDir.getCanonicalFile();
		}
		catch (IOException e1) {
			System.err.println("Failed to resolve server directory: " + serverRootDir);
			System.exit(-1);
		}

		System.out.println("Using server directory: " + serverRootDir);

		File userFile = new File(serverRootDir, UserManager.USER_PASSWORD_FILE);
		if (!serverRootDir.isDirectory() || !userFile.isFile()) {
			System.err.println("Invalid Ghidra server directory!");
			System.exit(-1);
		}

		File cmdDir = CommandProcessor.getCommandDir(serverRootDir);
		if (!cmdDir.isDirectory() || !cmdDir.canWrite()) {
			System.err.println("Insufficient privilege or server not started!");
			System.exit(-1);
		}

		// Process command line
		boolean listRepositories = false;
		boolean listAllUserPermissions = false;
		Set<String> listUsernameSet = new HashSet<>();
		boolean listUsers = false;
		boolean migrationConfirmed = false;
		boolean migrationAbort = false;
		ArrayList<String> cmdList = new ArrayList<>();
		int cmdLen = 1;
		for (; ix < args.length; ix += cmdLen) {
			boolean queueCmd = true;
			String pwdHash = null;
			switch (args[ix]) {
				case CommandProcessor.ADD_USER_COMMAND:  // add user
					cmdLen = 2;
					validateSID(args, ix + 1);
					if (hasOptionalArg(args, ix + 2, CommandProcessor.PASSWORD_OPTION)) {
						++cmdLen;
						pwdHash = promptForPasswordAndGetSaltedHash(args[ix + 1]);
					}
					break;
				case CommandProcessor.REMOVE_USER_COMMAND: // remove user
					cmdLen = 2;
					validateSID(args, ix + 1);
					break;
				case CommandProcessor.RESET_USER_COMMAND: // reset user
					cmdLen = 2;
					validateSID(args, ix + 1);
					if (hasOptionalArg(args, ix + 2, CommandProcessor.PASSWORD_OPTION)) {
						++cmdLen;
						pwdHash = promptForPasswordAndGetSaltedHash(args[ix + 1]);
					}
					break;
				case CommandProcessor.SET_USER_DN_COMMAND: // set/add user with DN for PKI
					cmdLen = 3;
					validateSID(args, ix + 1);
					validateDN(args, ix + 2);
					break;
				case CommandProcessor.GRANT_USER_COMMAND: // grant repository permission
					cmdLen = 4;
					validateSID(args, ix + 1);
					validatePermission(args, ix + 2);
					validateRepositoryName(args, ix + 3, serverRootDir);
					break;
				case CommandProcessor.REVOKE_USER_COMMAND: // revoke repository access
					cmdLen = 3;
					validateSID(args, ix + 1);
					validateRepositoryName(args, ix + 2, serverRootDir);
					break;
				case LIST_COMMAND: // list repositories;
					queueCmd = false;
					listRepositories = true;
					boolean hasUsernames = false;
					while ((ix + 1) < args.length && !args[ix + 1].startsWith("-")) {
						String sid = args[++ix]; // consume next arg as user sid
						validateSID(sid);
						listUsernameSet.add(sid);
						hasUsernames = true;
					}
					if (!hasUsernames && (ix + 1) < args.length && "--users".equals(args[ix + 1])) {
						listAllUserPermissions = true;
						++ix;
					}
					break;
				case USERS_COMMAND: // list users (also affects listRepositories)
					queueCmd = false;
					listUsers = true;
					listUsernameSet.clear();
					break;
				case MIGRATE_ALL_COMMAND: // list repositories;
					queueCmd = false;
					if (!migrationConfirmed && !confirmMigration()) {
						migrationAbort = true;
					}
					migrationConfirmed = true;
					if (!migrationAbort) {
						RepositoryManager.markAllRepositoriesForIndexMigration(serverRootDir);
					}
					break;
				case MIGRATE_COMMAND: // list repositories
					queueCmd = false;
					if (ix == (args.length - 1)) {
						System.err.println(
							"Missing " + MIGRATE_COMMAND + " repository name argument");
					}
					else {
						String repositoryName = args[ix + 1];
						if (!migrationConfirmed && !confirmMigration()) {
							migrationAbort = true;
						}
						migrationConfirmed = true;
						if (!migrationAbort) {
							Repository.markRepositoryForIndexMigration(serverRootDir,
								repositoryName,
								false);
						}
					}
					break;
				default:
					displayUsage("Invalid usage!");
					System.exit(-1);
			}
			if (queueCmd) {
				addCommand(cmdList, args, ix, cmdLen, pwdHash);
			}
		}

		if (cmdList.size() != 0) {
			try {
				CommandProcessor.writeCommands(cmdList, cmdDir);
			}
			catch (IOException e) {
				System.err.println("Failed to queue commands: " + e.toString());
				System.exit(-1);
			}
			System.out.println("Command queued.");
		}

		if (listUsers) {
			UserManager.listUsers(serverRootDir);
		}
		if (listRepositories) {
			if (listUsernameSet.isEmpty()) {
				RepositoryManager.listRepositories(serverRootDir, listAllUserPermissions);
			}
			else {
				RepositoryManager.listRepositories(serverRootDir, listUsernameSet);
			}
		}
		System.out.println();
	}

	private String promptForPasswordAndGetSaltedHash(String userSID) {
		char[] pwd1 = null;
		char[] pwd2 = null;
		try {
			while (true) {
				System.out.println("Enter password for user '" + userSID + "'");
				pwd1 = getPassword("New password: ", true);
				pwd2 = getPassword("Retype new password: ", false);
				if (Arrays.equals(pwd1, pwd2)) {
					break;
				}
				System.out.println("Password entries do not match! Please try again...");
				Arrays.fill(pwd1, (char) 0);
				Arrays.fill(pwd2, (char) 0);
			}
			char[] saltedHash = HashUtilities.getSaltedHash(HashUtilities.SHA256_ALGORITHM, pwd1);
			return new String(saltedHash);
		}
		catch (IOException e) {
			System.err.println("Password entry error: " + e.getMessage());
			System.exit(-1);
			return null;
		}
		finally {
			if (pwd1 != null) {
				Arrays.fill(pwd1, (char) 0);
			}
			if (pwd2 != null) {
				Arrays.fill(pwd2, (char) 0);
			}
		}
	}

	private char[] getPassword(String prompt, boolean echoWarn) throws IOException {

		boolean success = false;
		char[] password = null;
		int c;
		try {
			Console cons = System.console();
			if (cons != null) {
				password = cons.readPassword(prompt);
			}
			else {
				if (echoWarn) {
					System.out.println("*** WARNING! Password entry will NOT be masked ***");
				}

				System.out.print(prompt);

				while (true) {
					c = System.in.read();
					if (c <= 0 || c == '\n') {
						break;
					}
					if (c == '\r') {
						continue;
					}
					if (password == null) {
						password = new char[1];
					}
					else {
						char[] newPass = new char[password.length + 1];
						// copy prior entry into expanded array and clear old array
						for (int i = 0; i < password.length; i++) {
							newPass[i] = password[i];
							password[i] = 0;
						}
						password = newPass;
					}
					password[password.length - 1] = (char) c;
				}
			}
			success = true;
			return password;

		}
		finally {
			if (!success && password != null) {
				Arrays.fill(password, (char) 0);
			}
		}
	}

	/**
	 * Determine if option specified as args[argOffset]
	 * @param args command line args
	 * @param argOffset index within args array of option
	 * @param option option string
	 * @return true if option specified
	 */
	private boolean hasOptionalArg(String[] args, int argOffset, String option) {
		return (argOffset < args.length && args[argOffset].contentEquals(option));
	}

	/**
	 * Add command args as next command in cmdList.
	 * @param cmdList command list
	 * @param args command line args
	 * @param argOffset args index of command start
	 * @param argCnt number of args to consume
	 * @param pwdHash optional password has to append to end of command
	 */
	private static void addCommand(ArrayList<String> cmdList, String[] args, int argOffset,
			int argCnt, String pwdHash) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < argCnt; i++) {
			if (i > 0) {
				buf.append(' ');
			}
			buf.append(args[argOffset + i]);
		}
		if (pwdHash != null) {
			buf.append(' ');
			buf.append(pwdHash);
		}
		cmdList.add(buf.toString());
	}

	private static boolean confirmMigration() {
		System.out.print("\nWARNING!  Please confirm the requested migration of one or more\n" +
			"Ghidra Server repositories.  Once migrated to indexed storage,\n" +
			"any attempt to use these server repositories with a Ghidra Server\n" +
			"older than version 5.5 will corrupt the data storage.\n" +
			"\nWould you like to continue? [y/n]: ");
		try {
			if ('y' == System.in.read()) {
				System.out.println();
				return true;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("\nAll repository data migration(s) has been aborted.");
		return false;
	}

	/**
	 * Validate properly formatted Distinguished Name as command arg
	 * Example:  'CN=Doe John, OU=X, OU=Y, OU=DoD, O=U.S. Government, C=US'
	 * @param args command args
	 * @param i argument index
	 */
	private void validateDN(String[] args, int i) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage!");
			System.exit(-1);
		}
		String dn = args[i];
		try {
			X500Principal x500User = new X500Principal(dn);
			args[i] = "\"" + x500User.getName() + "\"";
		}
		catch (Exception e) {
			Msg.error(CommandProcessor.class, "Invalid DN: " + dn);
			System.exit(-1);
		}
	}

	/**
	 * Validate username/sid
	 * @param args command args
	 * @param i argument index
	 */
	private void validateSID(String[] args, int i) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage, expected username/sid");
			System.exit(-1);
		}
		validateSID(args[i]);
	}

	private void validateSID(String sid) {
		if (!UserManager.isValidUserName(sid)) {
			displayUsage("Invalid username/sid: " + sid);
			System.exit(-1);
		}
	}

	/**
	 * Validate repository permission arg (repository name to follow)
	 * @param args command args
	 * @param i argument index
	 */
	private void validatePermission(String[] args, int i) {
		if (args.length < (i + 1) || CommandProcessor.parsePermission(args[i]) < 0) {
			displayUsage("Invalid usage, expected grant permission +r, +w or +a");
			System.exit(-1);
		}
	}

	/**
	 * Validate existing repository name as command arg
	 * @param args command args
	 * @param i argument index
	 * @param rootDirFile base repositories directory
	 */
	private void validateRepositoryName(String[] args, int i, File rootDirFile) {
		if (args.length < (i + 1)) {
			displayUsage("Invalid usage, expected repository name");
			System.exit(-1);
		}
		String repName = args[i];
		File f = new File(rootDirFile, NamingUtilities.mangle(repName));
		if (!f.isDirectory()) {
			Msg.error(CommandProcessor.class, "Repository not found: " + repName);
			System.exit(-1);
		}
		if (repName.contains(" ")) {
			args[i] = "\"" + repName + "\"";
		}
	}

	/**
	 * Parse contents of specified configFilePath as server.conf to determine
	 * repositories root directory.  If configFilePath corresponds to a directory,
	 * that directory will be treated as the repositories root directory.
	 * @param configFilePath path to server.conf or repositories root directory
	 * @return repositories root directory
	 */
	private File getServerDirFromConfig(String configFilePath) {

		if (configFilePath == null) {
			return null;
		}

		File configFile = new File(configFilePath);
		if (!configFile.exists()) {
			System.out.println("Config file not found: " + configFile.getAbsolutePath());
			return null;
		}

		if (configFile.isDirectory()) {
			// If specified path is a directory treat as the server root
			return configFile;
		}

		System.out.println("Using config file: " + configFilePath);

		Properties config = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream(configFile);
			config.load(in);
		}
		catch (IOException e) {
			System.out.println("Failed to read " + configFile.getName() + ": " + e.getMessage());
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}

		String p = config.getProperty(SERVER_DIR_CONFIG_PROPERTY);
		if (p == null) {
			System.out.println("Failed to find property: " + SERVER_DIR_CONFIG_PROPERTY);
			return null;
		}
		File dir = new File(p);
		if (!dir.isAbsolute()) {
			// Make relative repositories dir relative to installation root
			ResourceFile installRoot = Application.getInstallationDirectory();
			if (installRoot == null || installRoot.getFile(false) == null) {
				System.out.println("Failed to resolve installation root directory!");
				return null;
			}
			dir = new File(installRoot.getFile(false), p);
		}
		return dir;
	}

	/**
	 * Display an optional message followed by usage syntax.
	 * @param msg optional error message to proceed usage display
	 */
	private void displayUsage(String msg) {
		if (msg != null) {
			System.err.println(msg);
		}
		String invocationName = System.getProperty(INVOCATION_NAME_PROPERTY);
		System.err.println("Usage: " +
			(invocationName != null ? invocationName : "java " + ServerAdmin.class.getName()) +
			(invocationName != null ? "" : " <configPath>") + " [<command>] [<command>] ...");
		System.err.println("\nSupported commands:");
		System.err.println("  -add <sid> [--p]");
		System.err.println(
			"      Add a new user to the server identified by their sid identifier [optional --p prompts for password]");
		System.err.println("  -grant <sid> [+r|+w|+a] <repository-name>");
		System.err.println(
			"      Grant access permission for a user, identified by sid, to the named repository");
		System.err.println("  -revoke <sid> <repository-name>");
		System.err.println(
			"      Revoke access for a user, identified by sid, to a named repository");
		System.err.println("  -remove <sid>");
		System.err.println(
			"      Remove the specified user from the server's user list and revoke all repository access");
		System.err.println("  -reset <sid> [--p]");
		System.err.println(
			"      Reset the specified user's server login password [optional --p prompts for password]");
		System.err.println("  -dn <sid> \"<dname>\"");
		System.err.println(
			"      When PKI authentication is used, add the specified X500 Distinguished Name for a user");
		System.err.println("  -list [--users]");
		System.err.println(
			"      Output list of repositories to the console (user access list will be included with -users)");
		System.err.println("  -list <sid> [<sid>...]");
		System.err.println("      Output list of repository permissions for each user specified");
		System.err.println("  -users");
		System.err.println("      Output list of users to console which have server access");
		System.err.println("  -migrate \"<repository-name>\"");
		System.err.println(
			"      Migrate the specified repository to the latest file system storage schema (see svrREADME.html)");
		System.err.println("  -migrate-all");
		System.err.println(
			"      Migrate the all repositories to the latest file system storage schema (see svrREADME.html)");
		System.err.println();
	}
}
