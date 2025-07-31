package agzam4;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import arc.func.Boolf;
import arc.math.geom.Point2;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.storage.CoreBlock;

public class Images {
	
	private static int[][] mapColors = null;
	
	public static BufferedImage placeholder = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
	
	public static void init() {
		try {
			BufferedImage colors = ImageIO.read(Game.class.getResourceAsStream("/colors.png"));
			Log.info("file: @", colors);
			mapColors = new int[Vars.content.blocks().size][];
			Point2 id = new Point2(0,0);
	    	Vars.content.blocks().each(b -> {
	    		int index = id.x++;
				int size = b.size*3;
				mapColors[index] = new int[size*size];
				for (int i = 0; i < size*size; i++) {
					int rgb = colors.getRGB(id.y/9, id.y%9);
					id.y++;
					if(i == size*size/2) {
						java.awt.Color col = new Color(rgb);
						b.mapColor.set(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f);
						b.hasColor = true;
					}
					mapColors[index][i] = rgb;//b.mapColor.rgb888();
				}
	    	});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static BufferedImage screenshot(Player player) {
		if(player == null) return placeholder;
		int dx = (int) (player.tileX() - Vars.world.width()/6);
		int dy = (int) (player.tileY() - Vars.world.height()/6);
		
		BufferedImage screen = screenshot(dx, dy, Vars.world.width()/3, Vars.world.height()/3, false);
		drawData(screen, false, dx, dy);
		return screen;
	}
	
	public static BufferedImage screenshot(int sx, int sy, int w, int h, boolean single) {
		if(mapColors == null) return placeholder;
		BufferedImage screen = new BufferedImage(single ? w : w*3, single ? h : h*3, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < screen.getHeight(); y++) {
			for (int x = 0; x < screen.getWidth(); x++) {
				screen.setRGB(x, y, x | (y << 8));
			}
		}
		Log.info("screenshot (@;@) @x@", sx, sy, w, h);
		
		Boolf<Tile> hasBlock = t -> t != null && t.build != null;
		
		try {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					Tile t = Vars.world.tile(sx+x, sy+h-y-1);
					if(t == null) continue;
					if(t.block() != null && t.block() != Blocks.air) {
						int size = t.block().size;
						int dx = t.x-t.centerX()+size/2 - 1 + (size%2);
						int dy = t.centerY()-t.y+size/2;
						int[] arr = mapColors[t.blockID()];
						if(single) {
							screen.setRGB(x, y, mapColors[t.blockID()][dx*3+1+dy*3*size + size]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int index = dx*3+px + (dy*3+py)*size*3;
									if(index < 0 || index >= arr.length) continue;
									screen.setRGB(x*3+px, y*3+py, arr[index]);
								}
							}
						}
						continue;
					}
					if(single && (hasBlock.get(Vars.world.tile(x-1, h-y-1)) 
							|| hasBlock.get(Vars.world.tile(x+1, h-y-1)) 
							|| hasBlock.get(Vars.world.tile(x, h-y-2))
							|| hasBlock.get(Vars.world.tile(x, h-y)))) {
						screen.setRGB(x, y, Color.black.getRGB());
						continue;
					}
					if(t.floor() != null && t.floor() != Blocks.air) {
						if(single) {
							screen.setRGB(x, y, mapColors[t.floorID()][4]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int rgba = mapColors[t.floorID()][px+py*3];
									screen.setRGB(x*3+px, y*3+py, rgba);
								}
							}
						}
					}
					if(t.overlay() != null && t.overlay() != Blocks.air) {
						if(single) {
							screen.setRGB(x, y, mapColors[t.overlayID()][4]);
						} else {
							for (int py = 0; py < 3; py++) {
								for (int px = 0; px < 3; px++) {
									int rgba = mapColors[t.overlayID()][px+py*3];
									if(!isTransparent(rgba)) screen.setRGB(x*3+px, y*3+py, rgba);
								}
							}
						}
						continue;
					}
				}
			}	
		} catch (Exception e) {
			Log.err(e);
		}
		return screen;
	}

	private static boolean isTransparent(int rgba) {
		return (rgba & 0x000000ff) == 0;
	}
	
	private static void drawData(BufferedImage screen, boolean single, int dx, int dy) {
		int scale = single ? 1 : 3;
		int sdx = -dx*scale;
		int sdy = -dy*scale;
		Graphics2D g = (Graphics2D) screen.getGraphics();
		g.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < Groups.player.size(); i++) {
			var p = Groups.player.index(i);								
			if(p.unit() != null) {
				for (int pl = 0; pl < p.unit().plans.size; pl++) {
					var plan = p.unit().plans.get(pl);
					g.setColor(plan.breaking ? Color.red : Color.yellow);
					g.drawLine(p.tileX()*scale+sdx, screen.getHeight()-(p.tileY()*scale+sdy), 
							plan.x*scale+sdx, screen.getHeight()-(plan.y*scale+sdy));
				}
			}
			drawString(screen, g, p.plainName(), p.color, p.tileX()*scale+sdx, p.tileY()*scale+sdy);
		}
		for (int y = 0; y < Vars.world.width(); y++) {
			for (int x = 0; x < Vars.world.height(); x++) {
				Tile t = Vars.world.tile(x, y);
				if(t == null) continue;
				if(!t.isCenter()) continue;
				if(t.block() == null) continue;
				if(!t.block().hasBuilding()) continue;
				if(t.block() instanceof CoreBlock) {
					drawString(screen, g, t.block().localizedName, t.team().color, x*scale+sdx, y*scale+sdy);
				}
				if(t.block() instanceof LogicBlock) {
					drawString(screen, g, "P", Pal.logicOperations, x*scale+sdx, y*scale+sdy);
				}
			}
		}
		Groups.unit.each(un -> {
			g.setColor(new Color(un.team.color.rgb888()));
			if(!single) {
				if(!un.spawnedByCore) drawString(screen, g, un.type.name, un.team.color, un.tileX()*scale+sdx, un.tileY()*scale+sdy);
			}
			g.fillOval(un.tileX()*scale+sdx, screen.getHeight()-(un.tileY()*scale+sdy),4,4);
		});
		g.dispose();		
	}

	

	private static void drawString(BufferedImage img, Graphics2D g, String text, arc.graphics.Color color, int x, int y) {
		g.setColor(Color.black);
		g.drawString(text, x - g.getFontMetrics().stringWidth(text)/2, (img.getHeight()-y-1)-5);
		g.setColor(new Color(color.rgb888()));
		g.drawString(text, x - g.getFontMetrics().stringWidth(text)/2-1, (img.getHeight()-y-1)-4);
	}
}
