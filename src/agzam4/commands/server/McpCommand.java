package agzam4.commands.server;

import java.util.HashMap;
import java.util.Map;

import agzam4.CommandsManager.CommandSender;
import agzam4.CommandsManager.ReceiverType;
import agzam4.commands.CommandHandler;
import agzam4.mcp.Mcp;
import agzam4gen.mcp.tools.McpTools;
import arc.struct.Seq;
import arc.util.Strings;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class McpCommand extends CommandHandler<Object> {

	{
		parms = "<tool> <toolname> [args...]";
		desc = "Runs mcp tool";
	}
	
	@Override
	public void command(String[] args, CommandSender sender, Object receiver, ReceiverType type) {
		try {
			if(require(args.length < 1, sender, "wrong arguments")) return;
			if(require(!args[0].equalsIgnoreCase("tool"), sender, "wrong arguments")) return;
			
			String tool = args[1];
			var handler = Mcp.tools.get(tool);
			var toolArgsScheme = McpTools.toolsArguments.get(tool);

			if(require(handler == null, sender, "tool not found")) return;
			if(require(toolArgsScheme == null, sender, "tool args not found")) return;
			
			String[] args2 = args[2].split(" ");
			
			Map<String, Object> toolargs = new HashMap<String, Object>();
			for (int i = 0; i < args2.length; i++) {
				toolargs.put(toolArgsScheme.get(i), args2[i]);
			}
			var res = handler.apply(null, CallToolRequest.builder(tool).arguments(toolargs).build()).content();
			for (var c : res) {
				if(!(c instanceof TextContent text)) {
					sender.sendMessage(Strings.format("<@>", c.getClass()));
					continue;
				}
				sender.sendMessage(text.text());
			}
		} catch (Exception e) {
			sender.sendMessage("[red]" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public Seq<?> complete(String[] args, Object receiver, ReceiverType type) {
		if(args.length <= 0) return Seq.with("tool");
		if(args.length <= 1) return McpTools.toolsArguments.keys().toSeq();
		var toolArgsScheme = McpTools.toolsArguments.get(args[1]);
		if(toolArgsScheme == null) return null;
		int arg = args.length - 2;
		if(arg < 0 || arg >= toolArgsScheme.size) return null;
		return Seq.with(toolArgsScheme.get(arg));
	}

}
