package com.exlibris.dps.repository.plugin.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.digitool.common.streams.ScriptUtil;
import com.exlibris.digitool.exceptions.ScriptException;
import com.exlibris.dps.sdk.plugin.StartUpCheckPlugin;

public class DiskSpaceCheckerPlugin implements StartUpCheckPlugin {

	private static final ExLogger log = ExLogger.getExLogger(DiskSpaceCheckerPlugin.class);

	private static final String PATH = "PATH";
	private static final String SERVER_ROLE = "SERVER_ROLE";
	private static final String WARNING_THRESHOLD = "WARNING_THRESHOLD";
	private static final String ERROR_THRESHOLD = "ERROR_THRESHOLD";
	private static final String FATAL_THRESHOLD = "FATAL_THRESHOLD";
	private static final String SLASH ="/";
	private static final String PERCENT ="%";

	private String path;
	private int warnThreshold;
	private int errorThreshold;
	private int fatalThreshold;
	private Severity severity;
	private String errorMessage;
	private ROLE serverRole;

	@Override
	public boolean execute() {
		try {
			int diskSpaceUsagePrecent = getDiskUsagePercent();
			if (diskSpaceUsagePrecent >= fatalThreshold) {
				severity = Severity.FATAL;
			} else if (diskSpaceUsagePrecent >= errorThreshold) {
				severity = Severity.ERROR;
			} else if (diskSpaceUsagePrecent >= warnThreshold) {
				severity = Severity.WARN;
			} else {
				return true;
			}
			log.info("Disk Space Checker : Used disk space under the mount of "+ path + " is " + diskSpaceUsagePrecent + PERCENT);
		} catch (ScriptException e) {
			severity = Severity.ERROR;
			errorMessage +=  "Disk Space Checker : Failed to run disk space check script on "+ path;
		}
		return false;
	}

	@Override
	public String getCheckType() {
		return "DiskSpaceCheckerPlugin";
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public ROLE[] getRole() {
		ROLE[] roles;
		if (ROLE.ALL.equals(serverRole)) {
			roles = new ROLE[]{ROLE.REP,ROLE.DEP,ROLE.DEL,ROLE.PER};
		} else {
			roles = new ROLE[]{serverRole};
		}
		return roles;
	}

	@Override
	public Severity getSeverity() {
		return severity;
	}

	private int getDiskUsagePercent() throws ScriptException {
		List<String> args = new ArrayList<>();
		args.add("-h");
		args.add(path);
		String result = ScriptUtil.runScript("df", args);
		String [] parts = result.trim().replaceAll(" +", " ").split(" ");
		String percent = parts[parts.length -2];
		percent = percent.substring(0,percent.length()-1);
		return Integer.parseInt(percent);
	}

	public void initParams(Map<String, String> parameters) {
		path = parameters.get(PATH) == null ? "/exlibris" : parameters.get(PATH);
		if (!path.startsWith(SLASH)){
			path = SLASH + path;
		}
		serverRole = ROLE.valueOf(parameters.get(SERVER_ROLE) == null ? "ALL" : parameters.get(SERVER_ROLE));
		warnThreshold = Integer.parseInt(parameters.get(WARNING_THRESHOLD) == null ? "75" : parameters.get(WARNING_THRESHOLD));
		errorThreshold = Integer.parseInt(parameters.get(ERROR_THRESHOLD) == null ? "90" : parameters.get(ERROR_THRESHOLD));
		fatalThreshold = Integer.parseInt(parameters.get(FATAL_THRESHOLD) == null ? "95" : parameters.get(FATAL_THRESHOLD));
		errorMessage = "";
	}


}
