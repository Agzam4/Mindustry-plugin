package agzam4.mcp.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import agzam4.mcp.McpUlits;
import agzam4proc.apt.mcp.McpAnnotations.McpTool;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.world.meta.BlockGroup;

public class GlossaryMcpTools {

	/**
	 * Info about game content
	 */
	@McpTool
	public static String glossary() {
		StringBuilder resources = new StringBuilder();
		resources.append("# Items").append('\n');
		Vars.content.items().each(i -> {
			resources.append("- ").append(i).append('\n');
		});
		resources.append("# Blocks").append('\n');
		for (var g : BlockGroup.values()) {
			if(g == BlockGroup.none) continue;
			resources.append("## ").append(g).append('\n');
			Vars.content.blocks().select(b -> b.group == g).sort((b1,b2) -> b1.health - b2.health).each(b -> {
				resources.append("- ").append(b.name).append('\n');
			});
		}
		resources.append("# Units").append('\n');
		resources.append("## Air").append('\n');
		Vars.content.units().select(u -> !u.hidden && u.flying && !u.naval).sort((u1,u2) -> (int)u1.health - (int)u2.health).each(u -> {
			resources.append("- ").append(u.name).append('\n');
		});
		resources.append("## Ground").append('\n');
		Vars.content.units().select(u -> !u.hidden && !u.flying && !u.naval).sort((u1,u2) -> (int)u1.health - (int)u2.health).each(u -> {
			resources.append("- ").append(u.name).append('\n');
		});
		resources.append("## Naval").append('\n');
		Vars.content.units().select(u -> !u.hidden && u.naval).sort((u1,u2) -> (int)u1.health - (int)u2.health).each(u -> {
			resources.append("- ").append(u.name).append('\n');
		});
		return resources.toString();
	}

	/**
	 * Info about content by name
	 * @param name - name of block, unit or other content from glossary
	 */
	@McpTool
	public static String contentInfo(String name) {
		// UnitTypes
		var content = Vars.content.byName(name);
		if(content == null) throw new RuntimeException("content not found");

		Seq<Field> currentClassFields = new Seq<>();
		Class<?> current = content.getClass();

		while (current != null) {
			if(current == UnlockableContent.class) break;
			Field[] declaredFields = current.getDeclaredFields();
			for (Field field : declaredFields) {
				if(!Modifier.isPublic(field.getModifiers())) continue;
				currentClassFields.add(field);
			}
			current = current.getSuperclass();
		}

		StringBuilder resources = new StringBuilder();
		currentClassFields.each(f -> {
			try {
				var result = f.get(content);
				if(result == null) return;
				resources.append(McpUlits.snakeCase(f.getName())).append(": ").append(result).append('\n');
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		});
		return resources.toString();
	}
	
}
