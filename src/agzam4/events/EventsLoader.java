package agzam4.events;

import static mindustry.Vars.platform;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.I18NBundle;
import arc.util.Log;
import arc.util.OS;
import arc.util.Time;
import arc.util.io.PropertiesUtils;
import arc.util.serialization.Json;
import arc.util.serialization.Jval;
import arc.util.serialization.Jval.Jformat;
import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods.ModLoadException;

public class EventsLoader {

    private static final String[] metaFiles = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson", "events.json", "events.hjson", "event.json", "event.hjson"};
    private static ModClassLoader mainLoader = new ModClassLoader(EventsLoader.class.getClassLoader());
    private static Json json = new Json();
    
    public static class EventMeta {
    	
        public Seq<String> events = Seq.with();
        
    }
	
    private static Fi resolveRoot(Fi fi){
        if(OS.isMac && (!(fi instanceof ZipFi))) fi.child(".DS_Store").delete();
        Fi[] files = fi.list();
        return files.length == 1 && files[0].isDirectory() ? files[0] : fi;
    }

	private static EventMeta findMeta(Fi file) {
		Fi metaFile = null;
		for(String name : metaFiles) {
			if((metaFile = file.child(name)).exists()){
				break;
			}
		}
		if(!metaFile.exists()){
			return null;
		}
		EventMeta meta = json.fromJson(EventMeta.class, Jval.read(metaFile.readString()).toString(Jformat.plain));
		return meta;
	}
    
	public static Seq<ServerEvent> load(Fi sourceFile) throws Exception {
		Seq<ServerEvent> events = new Seq<>();
		
        Time.mark();

        ZipFi rootZip = null;

        try{
            Fi zip = resolveRoot(sourceFile.isDirectory() ? sourceFile : (rootZip = new ZipFi(sourceFile)));

            EventMeta meta = findMeta(zip);

            if(meta == null){
                Log.warn("Mod @ doesn't have a '[mod/plugin/event].[h]json' file, skipping.", zip);
                throw new ModLoadException("Invalid file: No mod.json found.");
            }

            ClassLoader loader = null;

        	Fi bunglesDir = zip.child("assets").child("bundles");
        	
            for (int i = 0; i < meta.events.size; i++) {
            	try {
                    Fi mainFile = zip;
                    String mainClass = meta.events.get(i);
                    String[] path = (mainClass.replace('.', '/') + ".class").split("/");
                    for (String str : path) {
                        if (str.isEmpty()) continue;
                        mainFile = mainFile.child(str);
                    }
                    
                    if(mainFile.exists()) {
                    	loader = platform.loadJar(sourceFile, mainLoader);
                    	mainLoader.addChild(loader);
                    	Class<?> main = Class.forName(mainClass, true, loader);

                    	//detect mods that incorrectly package mindustry in the jar
                    	if(main.getSuperclass().getName().equals("agzam4.events.ServerEvent") && main.getSuperclass().getClassLoader() != EventsLoader.class.getClassLoader()){
                    		throw new ModLoadException(
                    				"This mod/plugin has loaded Mindustry dependencies from its own class loader. " +
                    						"You are incorrectly including Mindustry dependencies in the mod JAR - " +
                    						"make sure Mindustry is declared as `compileOnly` in Gradle, and that the JAR is created with `runtimeClasspath`!"
                    				);
                    	}
                    	ServerEvent event = (ServerEvent) main.getDeclaredConstructor().newInstance();
                    	event.bundle = I18NBundle.createEmptyBundle();
                    	
                        Fi bungleFile = bunglesDir.child(event.name + ".properties");
    					if(bungleFile.exists()) {
    						ObjectMap<String, String> properties = new ObjectMap<>();
    						PropertiesUtils.load(properties, bungleFile.reader());
    						event.bundle.setProperties(properties);
    					} else {
    						Log.info("bungle not found for: @", event.name);
    					}
    					event.init();
                        
                    	Log.info("[cyan]Event [blue]@[] loaded!", event.name);
                    	events.add(event);
                    } else {
                    	Log.info("main file not found");
                    }
    			
				} catch (Exception e) {
					Log.err(e);
				}
            }

            
        }catch(Exception e){
            //delete root zip file so it can be closed on windows
            if(rootZip != null) rootZip.delete();
            throw e;
        }
		return events;
    }
	
}
