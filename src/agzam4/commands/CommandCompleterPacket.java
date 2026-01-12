package agzam4.commands;

import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.io.TypeIO;
import mindustry.net.Packet;

public class CommandCompleterPacket extends Packet {

	// Packets
	
	public String command;
	public Seq<String> suggestions;
	
	
	@Override
	public void write(Writes buffer) {
        buffer.b((byte)suggestions.size);
        for(int i = 0; i < suggestions.size; i++){
            TypeIO.writeString(buffer, suggestions.get(i));
        }
	}
	
	@Override
	public void read(Reads buffer) {
        int totalSuggestions = buffer.b();
        suggestions = new Seq<>(totalSuggestions);
        for(int i = 0; i < totalSuggestions; i++){
        	suggestions.add(TypeIO.readString(buffer));
        }
	}
	
}
