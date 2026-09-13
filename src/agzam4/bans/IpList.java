package agzam4.bans;

import java.util.Map;
import java.util.TreeMap;

public class IpList {
	
    protected final TreeMap<Long, Long> ranges = new TreeMap<>();

    public boolean has(String ipStr) {
        long ip = ipToLong(ipStr);
        Map.Entry<Long, Long> floor = ranges.floorEntry(ip);
        return floor != null && ip <= floor.getValue();
    }

    public void removeIp(String ipStr) {
        long ip = ipToLong(ipStr);
        splitAndRemove(ip, ip);
    }

    public void removeSubnet(String subnetCidr) {
        String[] parts = subnetCidr.split("/");
        if(parts.length == 1) {
        	removeIp(subnetCidr);
        	return;
        }
        long ip = ipToLong(parts[0]);
        int prefix = Integer.parseInt(parts[1]);
        long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long startIp = ip & mask;
        long endIp = startIp | (~mask & 0xFFFFFFFFL);
        splitAndRemove(startIp, endIp);
    }

    protected void splitAndRemove(long start, long end) {
        Map.Entry<Long, Long> floor = ranges.floorEntry(start);
        if (floor != null && floor.getValue() >= start) {
            long floorStart = floor.getKey();
            long floorEnd = floor.getValue();
            if (floorEnd > end) {
                ranges.put(floorStart, start - 1);
                ranges.put(end + 1, floorEnd);
                return;
            } else {
                ranges.put(floorStart, start - 1);
            }
        }

        Map.Entry<Long, Long> ceiling = ranges.ceilingEntry(start);
        while (ceiling != null && ceiling.getKey() <= end) {
            long ceilingStart = ceiling.getKey();
            long ceilingEnd = ceiling.getValue();
            if (ceilingEnd > end) ranges.put(end + 1, ceilingEnd);
            ranges.remove(ceilingStart);
            ceiling = ranges.ceilingEntry(start);
        }
    }

    
    public void addIp(String ipStr) {
        long ip = ipToLong(ipStr);
        mergeAndAdd(ip, ip);
    }

    public void addSubnet(String subnetCidr) {
        String[] parts = subnetCidr.split("/");
        if(parts.length == 1) {
        	addIp(subnetCidr);
        	return;
        }
        long ip = ipToLong(parts[0]);
        int prefix = Integer.parseInt(parts[1]);
        long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long startIp = ip & mask;
        long endIp = startIp | (~mask & 0xFFFFFFFFL);
        mergeAndAdd(startIp, endIp);
    }

    protected void mergeAndAdd(long start, long end) {
        Map.Entry<Long, Long> floor = ranges.floorEntry(start);
        if (floor != null && floor.getValue() >= start - 1) {
            start = floor.getKey();
            end = Math.max(end, floor.getValue());
        }
        Map.Entry<Long, Long> ceiling = ranges.ceilingEntry(start);
        while (ceiling != null && ceiling.getKey() <= end + 1) {
            end = Math.max(end, ceiling.getValue());
            ranges.remove(ceiling.getKey());
            ceiling = ranges.ceilingEntry(start);
        }
        ranges.put(start, end);
    }

    public static long ipToLong(String ipStr) {
        String[] parts = ipStr.split("\\.");
        long ip = 0;
        for (int i = 0; i < 4; i++) {
            ip |= (Long.parseLong(parts[i]) << (24 - i * 8));
        }
        return ip;
    }
    
    public static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }
    
    public long size() {
        long totalCount = 0;
        for (Map.Entry<Long, Long> entry : ranges.entrySet()) {
            totalCount += (entry.getValue() - entry.getKey() + 1);
        }
        return totalCount;
    }
    
}
