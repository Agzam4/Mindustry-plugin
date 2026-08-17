package agzam4.mcp;

import agzam4proc.apt.config.ConfigAnnotations.Config;

@Config("mcp")
public class McpConfigSpec {

	/** Determines if MCP integration is enabled **/
	public static boolean enabled = true;

	/** The name of the server instance **/
	public static String name = "mindustry-server";

	/** Custom system instructions or prompt for the MCP server **/
	public static String instructions = "";
	
}
