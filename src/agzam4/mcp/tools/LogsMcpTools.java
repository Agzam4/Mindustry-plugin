package agzam4.mcp.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import agzam4.logs.LogEvents;
import agzam4.logs.LogEvents.ChatMessageLogEvent;
import agzam4.logs.LogEvents.LogEvent;
import agzam4.logs.LogEvents.PlayerCommandLogEvent;
import agzam4.logs.LogEvents.ServerStartLogEvent;
import agzam4.logs.Logs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Strings;
import arc.util.serialization.Jval;

public class LogsMcpTools {

	private static final long groupInterval = TimeUnit.SECONDS.toMillis(10);
    
	/**
	 * Return id of latest log
	 */
	@McpTool
	public static String lastLogId() {
		return String.valueOf(Logs.lastId());
	}
	
	/**
	 * Return logs interval from up to id
	 * @param id - id of log
	 * @param limit - maximum of logs (from id to past)
	 */
	@McpTool
	public static String logs(int id, int limit) {
		 var list = Logs.filtredPage(id, limit, 0, 999999999999999L, new int[0]);
		 StringBuilder result = new StringBuilder();
		 long groupNextTime = 0;
		 for (int i = 0; i < list.length; i++) {
//		 for (int i = list.length-1; i >= 0; i--) {
			var item = list[i];
			if(item == null) continue;;
			boolean first = result.isEmpty();
			
			// Time groups headers
			if(first) { // First, full time header
				result.append("=== ").append(format("yyyy-MM-dd HH:mm:ss", item.timestamp)).append(" ===");
				groupNextTime = item.timestamp + groupInterval;
			}
			if(groupNextTime < item.timestamp) { // Each group header
				String delta = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(item.timestamp - groupNextTime + groupInterval));
				result.append("\n+").append(delta).append("s ").append("=".repeat(24 - delta.length()));
				groupNextTime = item.timestamp + groupInterval;
			}
			
			String tag = "unknow";
			String content = item.message;
			try {
				LogEvent e = Logs.builders[item.tag].read(item.message);
				content = e.mcpText();
				tag = e.mcpTag();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			result.append('\n');
			result.append(Strings.format("#@ [@] @", item.globalId, tag, content));
		}
		return result.toString();
	}
	
	
	
	
	private static String format(String format, long t) {
		return new SimpleDateFormat(format).format(new Date(t));
	}
	
}
