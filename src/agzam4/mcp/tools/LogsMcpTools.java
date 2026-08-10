package agzam4.mcp.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

import agzam4.logs.LogEvents;
import agzam4.logs.Logs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.util.Strings;

public class LogsMcpTools {

	
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
		 for (int i = list.length-1; i >= 0; i--) {
			var item = list[i];
			if(item == null) continue;;
			boolean first = result.isEmpty();
			if(!first) result.append('\n');
			result.append(Strings.format("#@ [@] [@]: @", 
					item.globalId, 
					new SimpleDateFormat(first ? "yyyy-MM-dd'T'HH:mm:ss" : "HH:mm:ss").format(new Date(item.timestamp)),
					item.tag >= LogEvents.events.length ? "unknow" : Strings.camelToKebab(LogEvents.events[item.tag].getSimpleName().replace("LogEvent", "")).replace('-', '_'),
					item.message
			));
		}
		return result.toString();
	}
	
}
