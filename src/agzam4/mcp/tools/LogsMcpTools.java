package agzam4.mcp.tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import agzam4.logs.LogEvents;
import agzam4.logs.Logs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.util.Strings;

public class LogsMcpTools {

	private static final long groupInterval = TimeUnit.SECONDS.toMillis(5);
	
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
		 long groupTime = 0;
		 for (int i = list.length-1; i >= 0; i--) {
			var item = list[i];
			if(item == null) continue;;
			boolean first = result.isEmpty();
			
			// Time groups headers
			if(first) { // First, full time header
				result.append("=== ").append(format("yyyy-MM-dd HH:mm:ss", item.timestamp)).append(" ===");
				groupTime = item.timestamp;
			}
			if(groupTime + groupInterval < item.timestamp) { // Each group header
				String delta = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(item.timestamp - (groupTime + groupInterval)));
				result.append("\n+").append(delta).append("s ").append("=".repeat(24 - delta.length()));
				groupTime = item.timestamp;
			}
			
			if(!first) result.append('\n');
			result.append(Strings.format("#@ [@] @", 
					item.globalId, 
					item.tag >= LogEvents.events.length ? "unknow" : Strings.camelToKebab(LogEvents.events[item.tag].getSimpleName().replace("LogEvent", "")).replace('-', '_'),
					item.message
			));
		}
		return result.toString();
	}
	
	
	private static String format(String format, long t) {
		return new SimpleDateFormat(format).format(new Date(t));
	}
	
}
