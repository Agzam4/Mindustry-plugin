package agzam4.mcp.tools;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import agzam4.logs.LogEvents;
import agzam4.logs.LogEvents.LogEntity;
import agzam4.logs.LogEvents.LogEvent;
import agzam4.logs.Logs;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.entities.part.DrawPart.PartFunc;

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

	private static int contentDisplayLimit = 300;
	
	/**
	 * Calculates and returns the frequency (rate) of log tags grouped by time sub-intervals.
	 * Use this to analyze which tags were dominant during specific periods. 
	 * @param timeFrom Start of the time interval in ISO-8601 format (e.g., "2007-12-03T10:15:30.00Z").
	 * @param timeTo End of the time interval in ISO-8601 format (e.g., "2007-12-03T10:15:30.00Z"). Max duration between timeFrom and timeTo is 1 day.
	 * @param parts Number of equal chunks (sub-intervals) to split the main interval into for trend analysis.
	 * @return A formatted string showing sorted tag statistics for each time chunk.
	 */
	@McpTool
	public static String logsTagsRate(String timeFrom, String timeTo, int parts) {
		long from = Instant.parse(timeFrom).toEpochMilli();
		long to = Instant.parse(timeTo).toEpochMilli();
		Log.info("=== Logs @ to @ ===", timeFrom, timeTo);
		
		if(TimeUnit.DAYS.toMillis(30) < to - from) throw new RuntimeException("Time differencse can't increase 1 day");
		
		long delta = (to - from);
		long latest = Logs.lastId();
		
		Seq<String> partsResult = Seq.with();

		int[][] amount = new int[parts][LogEvents.events.length];
		long[] minTime = new long[parts];
		long[] maxTime = new long[parts];
		long[] minId = new long[parts];
		long[] maxId = new long[parts];
		for (int i = 0; i < parts; i++) {
			minTime[i] = to;
			minId[i] = latest;
		}
		
		while (true) {
			if(latest <= 0) break;
			
			var list = Logs.filtredPage(latest, Logs.maxPageSize, from, to, new int[0]);
			latest -= Logs.maxPageSize;

			for (int i = list.length-1; i >= 0; i--) {
				var log = list[i];
				if(log == null) continue;
				int part = (int)((log.timestamp - from)*parts/delta);
				if(0 <= log.tag && log.tag < amount.length) {
					amount[part][log.tag]++;
					minTime[part] = Math.min(minTime[part], log.timestamp);
					maxTime[part] = Math.max(maxTime[part], log.timestamp);
					minId[part] = Math.min(minId[part], log.globalId);
					maxId[part] = Math.max(maxId[part], log.globalId);
				}
			}
		}
		
//		for (int part = 0; part < parts; part++) {
//
//			long t1 = from + (part * delta)/parts;
//			long t2 = from + ((part+1) * delta)/parts;
//			
//			LogEntity first = null, last = null;
//			int max = 0;
//			
//			while (true) {
//				Log.info("Left: @", latest);
//				if(latest <= 0) break;
//				
//				var list = Logs.filtredPage(latest, Logs.maxPageSize, t1, t2, new int[0]);
//				latest -= Logs.maxPageSize;
//
//				for (int i = list.length-1; i >= 0; i--) {
//					var log = list[i];
//					if(log == null) {
//						continue;
//					}
//					if(t1 <= log.timestamp && log.timestamp < t2) {
//						if(0 <= log.tag && log.tag < amount.length) {
//							amount[log.tag]++;
//							max = Math.max(max, amount[log.tag]);
//						}
//						
//						if(first == null) {
//							first = log;
//						}
//						last = log;
//					}
//					
////					latest = log.globalId-1;
//				}
//			}
//			if(first == null) continue;
//			latest = last.globalId-1;
//		}
		
		for (int part = 0; part < parts; part++) {
			StringBuilder result = new StringBuilder();
			result.append("=== Interval ")
			.append(format("yyyy-MM-dd HH:mm:ss", minTime[part])).append(" to ")
			.append(format("yyyy-MM-dd HH:mm:ss", maxTime[part]))
			.append(" ===\n");
			result.append("Ids range: [").append(minId[part]).append(",").append(maxId[part]).append("]");
			
			var amounts = amount[part];
			
			int max = 0;
			for (int i = 0; i < amounts.length; i++) max = Math.max(max, amounts[i]);
			
			while (max > 0) {
				int nextMax = 0;
				for (int tag = 0; tag < amounts.length; tag++) {
					if(amounts[tag] == max) {
						result.append("\n[").append(Logs.builders[tag].instance().mcpTag()).append("]").append(" x").append(amounts[tag]);
						amounts[tag] = 0;
						continue;
					}
					nextMax = Math.max(nextMax, amounts[tag]);
				}
				max = nextMax;
			}
			partsResult.add(result.toString());
		}
		
		return partsResult.reverse().toString("\n");
	}
	
	private static String format(String format, long t) {
		return new SimpleDateFormat(format).format(new Date(t));
	}

//	/**
//	 * Return logs interval from up to id
//	 * @param id - id of log
//	 * @param limit - maximum of logs (from id to past)
//	 * @param player - id of player
//	 */
//	@McpTool
//	public static String search(
//			int id, int limit, 
//			int player
//			) {
//		 var list = Logs.filtredPage(id, limit, 0, 999999999999999L, new int[0]);
//		 
//		 StringBuilder result = new StringBuilder();
//		 long groupNextTime = 0;
//		 for (int i = 0; i < list.length; i++) {
////		 for (int i = list.length-1; i >= 0; i--) {
//			var item = list[i];
//			if(item == null) continue;;
//			boolean first = result.isEmpty();
//			
//			// Time groups headers
//			if(first) { // First, full time header
//				result.append("=== ").append(format("yyyy-MM-dd HH:mm:ss", item.timestamp)).append(" ===");
//				groupNextTime = item.timestamp + groupInterval;
//			}
//			if(groupNextTime < item.timestamp) { // Each group header
//				String delta = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(item.timestamp - groupNextTime + groupInterval));
//				result.append("\n+").append(delta).append("s ").append("=".repeat(24 - delta.length()));
//				groupNextTime = item.timestamp + groupInterval;
//			}
//			
//			String tag = "unknow";
//			String content = item.message;
//			try {
//				LogEvent e = Logs.builders[item.tag].read(item.message);
//				content = e.mcpText();
//				tag = e.mcpTag();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//			result.append('\n');
//			result.append(Strings.format("#@ [@] @", item.globalId, tag, content));
//		}
//		return result.toString();
//	}
	
}
