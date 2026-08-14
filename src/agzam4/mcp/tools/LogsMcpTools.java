package agzam4.mcp.tools;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import agzam4.logs.LogEvents.LogEvent;
import agzam4.logs.Logs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;

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

	private static int contentDisplayLimit = 25;
	
	/**
	 * Return logs interval from up to id
	 * @param timeFrom - time of oldest log in ISO format
	 * @param timeTo - time of latest log in ISO format
	 */
	@McpTool
	public static String logsContent(String timeFrom, String timeTo) {
		long from = Instant.parse(timeFrom).toEpochMilli();
		long to = Instant.parse(timeTo).toEpochMilli();
		Log.info("=== Logs @ to @ ===", timeFrom, timeTo);
		
		if(TimeUnit.DAYS.toMillis(30) < to - from) throw new RuntimeException("Time differencse can't increase 1 day");
		
		Seq<String> lines = new Seq<String>();
		
		long latest = Logs.lastId();
		int lastTag = -1;
		long lastId = latest;
		
		main:
		while (true) {
			Log.info("Left: @", latest);
			if(latest <= 0) break;
			
			var list = Logs.filtredPage(latest, Logs.maxPageSize, from, to, new int[0]);
			latest -= Logs.maxPageSize;

			for (int i = list.length-1; i >= 0; i--) {
				var log = list[i];
				if(log == null) {
					continue;
				}
				if(log.tag != lastTag) {
					if(0 <= lastTag && lastTag < Logs.builders.length)
						if(lastId - log.globalId - 1 > 0)
							lines.add(Strings.format("[@+@] [@]", log.globalId+1, lastId - log.globalId - 1, Logs.builders[lastTag].instance().mcpTag()));
						else
							lines.add(Strings.format("[@] [@]", lastId, Logs.builders[lastTag].instance().mcpTag()));
							
					if(lines.size > contentDisplayLimit) {
						lines.add(Strings.format("Only @ logs was displayed, search [@, @] for more", contentDisplayLimit, timeFrom, Instant.ofEpochMilli(log.timestamp).toString()));
						break main;
					}
					lastTag = log.tag;
					lastId = log.globalId;
				}
			}
		}
		
		return lines.reverse().toString("\n");
	}
	
	private static String format(String format, long t) {
		return new SimpleDateFormat(format).format(new Date(t));
	}

	
}
