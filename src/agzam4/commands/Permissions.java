package agzam4.commands;

import arc.util.Strings;

public enum Permissions {

	logs,
	sensitiveData,
	traceAdmins,
	forceRunwave, brushSandbox, longname, whitelist, manageMaps, manageMapCreators, manageMcp;
	
	public final String name;
	
	Permissions() {
		name = Strings.camelToKebab(name());
	}
}
