package agzam4.commands;

import arc.util.Strings;

public enum Permissions {

	logs,
	sensitiveData,
	forceRunwave, brushSandbox, longname, whitelist;
	
	public final String name;
	
	Permissions() {
		name = Strings.camelToKebab(name());
	}
}
