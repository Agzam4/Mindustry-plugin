package agzam4.bans;

import java.io.IOException;
import java.util.Map;

import arc.files.Fi;
import arc.util.ArcRuntimeException;

public class FileIpList extends IpList {

	public final Fi file;
	public boolean autosave;

	public FileIpList(Fi file) {
		this.file = file;
	}
	
	public FileIpList(Fi file, boolean autosave) {
		this.file = file;
		this.autosave = autosave;
	}
	
	@Override
	public void addSubnet(String subnetCidr) {
		super.addSubnet(subnetCidr);
		save();
	}
	
	@Override
	public void addIp(String ipStr) {
		super.addIp(ipStr);
		save();
	}
	
	@Override
	public void removeIp(String ipStr) {
		super.removeIp(ipStr);
		save();
	}
	
	@Override
	public void removeSubnet(String subnetCidr) {
		super.removeSubnet(subnetCidr);
		save();
	}
	
	
    public void save() {
    	save(file);
    }

    public void save(Fi file) {
        try (var writer = file.writer(false)) {
            for (Map.Entry<Long, Long> entry : ranges.entrySet()) {
                long start = entry.getKey();
                long end = entry.getValue();

                while (end >= start) {
                    int maxSubnet = 32;
                    while (maxSubnet > 0) {
                        long mask = (0xFFFFFFFFL << (32 - (maxSubnet - 1))) & 0xFFFFFFFFL;
                        if ((start & mask) != start) break;
                        long currentEnd = start | (~mask & 0xFFFFFFFFL);
                        if (currentEnd > end) break;
                        maxSubnet--;
                    }
                    if (maxSubnet == 32) {
                        writer.write(longToIp(start) + "\n");
                        start++;
                    } else {
                        writer.write(longToIp(start) + "/" + maxSubnet + "\n");
                        long mask = (0xFFFFFFFFL << (32 - maxSubnet)) & 0xFFFFFFFFL;
                        start = (start | (~mask & 0xFFFFFFFFL)) + 1;
                    }
                }
            }
        } catch (IOException e) {
            throw new ArcRuntimeException("Failed to save IP list to " + file.path(), e);
        }
    }

    public void load() {
    	load(file);
    }
    
    public void load(Fi file) {
        ranges.clear();
        if (!file.exists()) return;
        try (var reader = file.reader(8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                if (line.contains("/")) {
                	super.addSubnet(line);
                } else if (line.contains("-")) {
                    String[] parts = line.split("-");
                    if (parts.length == 2) {
                        mergeAndAdd(ipToLong(parts[0].trim()), ipToLong(parts[1].trim()));
                    }
                } else {
                	super.addIp(line);
                }
            }
        } catch (IOException e) {
            throw new ArcRuntimeException("Failed to load IP list from " + file.path(), e);
        }
    }
    
    
    
}
