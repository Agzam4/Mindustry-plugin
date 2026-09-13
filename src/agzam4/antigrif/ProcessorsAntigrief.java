package agzam4.antigrif;

import static mindustry.Vars.charset;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import agzam4.managers.Players;
import agzam4gen.config.AntigriefConfig;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.logic.LVar;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

public class ProcessorsAntigrief {

	
	public static void init() {
		LogicBuild build = (LogicBuild) Blocks.microProcessor.newBuilding();
		Vars.netServer.admins.addActionFilter(action -> {
			if(action.type == ActionType.placeBlock) {
				if(action.block instanceof LogicBlock logic) {
					if(action.config instanceof byte[] bs) {
						if(Players.gamePlaytime(action.player) > AntigriefConfig.processorMaxDrawInstructuonsPlaytime) return true;
						
//						Log.info(bs.length);
//						build.readCompressed(bs, true);
			            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bs)))){
			                int version = stream.read();

			                int bytelen = stream.readInt();
			                if(bytelen > 1024 * 100) throw new IOException("Malformed logic data! Length: " + bytelen);
			                byte[] bytes = new byte[bytelen];
			                stream.readFully(bytes);

			                int total = Math.min(stream.readInt(), 6000);

			                if(version == 0){
			                    //old version just had links, ignore those

			                    for(int i = 0; i < total; i++){
			                        stream.readInt();
			                    }
			                }else{
			                    for(int i = 0; i < total; i++){
			                        String name = stream.readUTF();
			                        short x = stream.readShort(), y = stream.readShort();
			                    }
			                }
			                var code = new String(bytes, charset);
			                int index = -1;
			                int amount = 0;
			                while (true) {
				                index = code.indexOf("draw", index+1);
								if(index == -1) break;
								amount++;
								if(amount > AntigriefConfig.processorMaxDrawInstructuons) return false;
							}
			            }catch(Exception ignored){
			            	ignored.printStackTrace();
			                //invalid logic doesn't matter here
			            }
					}
				}
			}
			return true;
		});
		
		
	}
	
	
}
