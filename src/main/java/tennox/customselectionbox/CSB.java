package tennox.customselectionbox;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "TeNNoX_CustomSelectionBox", name = "CustomSelectionBox", version = "1.3")
public class CSB {
	@SidedProxy(clientSide = "tennox.customselectionbox.CSBClientProxy", serverSide = "tennox.customselectionbox.CSBCommonProxy")
	public static CSBCommonProxy proxy;
	private static net.minecraftforge.common.config.Configuration config;
	public static Logger logger;
	public static float red;
	public static float green;
	public static float blue;
	public static float alpha;
	public static float thickness;
	public static float offset;
	public static float blinkalpha;
	public static float blinkspeed;
	public static boolean diffButtonLoc;
	public static boolean disableDepthBuffer;
	public static int breakAnimation;

	public static int NONE = 0;
	public static int SHRINK = 1;
	public static int DOWN = 2;
	public static int ALPHA = 3;
	public static int LASTANIMATION = 3;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.registerTickHandler();
		MinecraftForge.EVENT_BUS.register(this);

		logger = event.getModLog();
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		int r = config.get("general", "red", 0).getInt();
		int g = config.get("general", "green", 0).getInt();
		int b = config.get("general", "blue", 0).getInt();
		int a = config.get("general", "alpha", 255).getInt();
		int t = config.get("general", "thickness", 4).getInt();
		int o = config.get("general", "offset", 4).getInt();
		int ba = config.get("general", "blink_alpha", 100).getInt();
		int bs = config.get("general", "blink_speed", 30).getInt();
		diffButtonLoc = config.get("general", "different config button location", false).getBoolean(false);
		disableDepthBuffer = config.get("general", "disable_depth", false).getBoolean(false);
		int bra = config.get("general", "break_animation", 0).getInt();
		config.save();

		setRed(r / 255.0F);
		setGreen(g / 255.0F);
		setBlue(b / 255.0F);
		setAlpha(a / 255.0F);
		setThickness(t);
		setOffset(o / 1000.0F);
		setBlinkAlpha(ba / 255.0F);
		setBlinkSpeed(bs / 100.0F);
		setBreakAnimation(bra);

		logger.info("red=" + getRed() + " green=" + getGreen() + " blue=" + getBlue() + " alpha=" + getAlpha());
		logger.info("thickness=" + getThickness() + " offset=" + getOffset() + " diffbuttonloc=" + diffButtonLoc);
		logger.info("blinkalpha=" + getBlinkAlpha() + " blinkspeed=" + getBlinkSpeed());
	}

	public static void save() {
		config.load();
		config.get("general", "red", 0).set(getRedInt());
		config.get("general", "green", 0).set(getGreenInt());
		config.get("general", "blue", 0).set(getBlueInt());
		config.get("general", "alpha", 255).set(getAlphaInt());
		config.get("general", "thickness", 4).set(getThicknessInt());
		config.get("general", "offset", 4).set(getOffsetInt());
		config.get("general", "blink_alpha", 100).set(getBlinkAlphaInt());
		config.get("general", "blink_speed", 30).set(getBlinkSpeedInt());
		config.get("general", "disable_depth", false).set(disableDepthBuffer);
		config.get("general", "break_animation", 0).set(breakAnimation);
		config.save();
		logger.info("SAVED: red=" + getRed() + " green=" + getGreen() + " blue=" + getBlue() + " alpha=" + getAlpha());
		logger.info("SAVED: thickness=" + getThickness() + " offset=" + getOffset());
		logger.info("SAVED: blinkalpha=" + getBlinkAlpha() + " blinkspeed=" + getBlinkSpeed());
	}

	public static void reset(boolean mc) {
		setRed(0.0F);
		setGreen(0.0F);
		setBlue(0.0F);
		setAlpha(mc ? 0.4F : 1.0F);
		setThickness(mc ? 2.0F : 4.0F);
		setOffset(mc ? 0.002F : 0.004F);
		setBlinkAlpha(mc ? 0.0F : 0.390625F);
		setBlinkSpeed(0.2F);
		disableDepthBuffer = false;
		setBreakAnimation(0);
		save();
	}

	public static void openSettingsGUI() {
		Minecraft mc = Minecraft.getMinecraft();
		mc.gameSettings.saveOptions();
		mc.displayGuiScreen(new CSBSettingsGUI(mc.currentScreen));
	}

	@SubscribeEvent
	public void onDrawBlockSelectionBox(DrawBlockHighlightEvent e) {
		drawSelectionBox(e.player, e.target, 0, e.currentItem, e.partialTicks);
		e.setCanceled(true);
	}

	public static void drawSelectionBox(EntityPlayer player, MovingObjectPosition block, int par3, ItemStack par4ItemStack, float par5) {
		if (par3 == 0 && block.typeOfHit == MovingObjectType.BLOCK) {
			float breakProgress = getBlockDamage(player, block);
			if (disableDepthBuffer) {
				GL11.glDisable(GL11.GL_DEPTH_TEST);
			}
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glLineWidth(getThickness());
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDepthMask(false);
			float f1 = getOffset();

			Minecraft mc = Minecraft.getMinecraft();
			Block b = mc.theWorld.getBlock(block.blockX, block.blockY, block.blockZ);

			if (b != Blocks.air) {
				b.setBlockBoundsBasedOnState(mc.theWorld, block.blockX, block.blockY, block.blockZ);

				double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) par5;
				double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) par5;
				double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) par5;

				AxisAlignedBB bb = b.getSelectedBoundingBoxFromPool(mc.theWorld, block.blockX, block.blockY, block.blockZ).expand(f1, f1, f1).getOffsetBoundingBox(-d0, -d1, -d2);
				if (breakAnimation == DOWN)
					bb = bb.expand(0f, -breakProgress / 2, 0f).getOffsetBoundingBox(0f, -breakProgress / 2, 0f);
				if (breakAnimation == SHRINK)
					bb = bb.expand(-breakProgress / 2, -breakProgress / 2, -breakProgress / 2);

				if (breakAnimation == ALPHA)
					drawBlinkingBlock(bb, breakProgress);
				else
					drawBlinkingBlock(bb, getBlinkAlpha());
				GL11.glColor4f(getRed(), getGreen(), getBlue(), getAlpha());
				drawOutlinedBoundingBox(bb);

			}

			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_BLEND);
			if (disableDepthBuffer) {
				GL11.glEnable(GL11.GL_DEPTH_TEST);
			}
		}
	}

	private static float getBlockDamage(EntityPlayer player, MovingObjectPosition block) {
		try {
			Field f;
			try {
				f = RenderGlobal.class.getDeclaredField("damagedBlocks");
			} catch (NoSuchFieldException e) {
				f = RenderGlobal.class.getDeclaredField("field_72738_E");
			}
			f.setAccessible(true);
			HashMap<Integer, DestroyBlockProgress> map = (HashMap<Integer, DestroyBlockProgress>) f.get(Minecraft.getMinecraft().renderGlobal);
			for (Entry<Integer, DestroyBlockProgress> entry : map.entrySet()) {
				DestroyBlockProgress prg = entry.getValue();
				if (prg.getPartialBlockX() == block.blockX && prg.getPartialBlockY() == block.blockY && prg.getPartialBlockZ() == block.blockZ) {
					if (prg.getPartialBlockDamage() >= 0 && prg.getPartialBlockDamage() <= 10)
						return prg.getPartialBlockDamage() / 10f;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0f;
	}

	private static void drawOutlinedBoundingBox(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawing(3);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.draw();
		tessellator.startDrawing(3);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.draw();
		tessellator.startDrawing(1);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.draw();
	}

	private static void drawBlinkingBlock(AxisAlignedBB par1AxisAlignedBB, float alpha) {
		Tessellator tessellator = Tessellator.instance;

		if (alpha > 0.0F) {
			if (getBlinkSpeed() > 0 && CSB.breakAnimation != ALPHA)
				alpha *= (float) Math.abs(Math.sin(Minecraft.getSystemTime() / 100.0D * getBlinkSpeed()));

			GL11.glColor4f(getRed(), getGreen(), getBlue(), alpha);
			renderDown(par1AxisAlignedBB);
			renderUp(par1AxisAlignedBB);
			renderNorth(par1AxisAlignedBB);
			renderSouth(par1AxisAlignedBB);
			renderWest(par1AxisAlignedBB);
			renderEast(par1AxisAlignedBB);
		}
	}

	public static void renderUp(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.draw();
	}

	public static void renderDown(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);

		tessellator.draw();
	}

	public static void renderNorth(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.draw();
	}

	public static void renderSouth(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.draw();
	}

	public static void renderWest(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.draw();
	}

	public static void renderEast(AxisAlignedBB par1AxisAlignedBB) {
		Tessellator tessellator = Tessellator.instance;

		tessellator.startDrawingQuads();
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
		tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
		tessellator.draw();
	}

	public static void renderBlock() {
	}

	public static float getRed() {
		return between(red, 0.0F, 1.0F);
	}

	public static float getGreen() {
		return between(green, 0.0F, 1.0F);
	}

	public static float getBlue() {
		return between(blue, 0.0F, 1.0F);
	}

	public static float getAlpha() {
		return between(alpha, 0.0F, 1.0F);
	}

	public static float getThickness() {
		return between(thickness, 0.1F, 7.0F);
	}

	public static float getOffset() {
		return between(offset, 0.0F, 1.0F);
	}

	public static float getBlinkAlpha() {
		return between(blinkalpha, 0.0F, 1.0F);
	}

	public static float getBlinkSpeed() {
		return between(blinkspeed, 0.0F, 1.0F);
	}

	public static void setRed(float r) {
		red = between(r, 0.0F, 1.0F);
	}

	public static void setGreen(float g) {
		green = between(g, 0.0F, 1.0F);
	}

	public static void setBlue(float b) {
		blue = between(b, 0.0F, 1.0F);
	}

	public static void setAlpha(float a) {
		alpha = between(a, 0.0F, 1.0F);
	}

	public static void setThickness(float t) {
		thickness = between(t, 0.1F, 7.0F);
	}

	public static void setOffset(float o) {
		offset = between(o, 0.002F, 1.0F);
	}

	public static void setBlinkAlpha(float ba) {
		blinkalpha = between(ba, 0.0F, 1.0F);
	}

	public static void setBlinkSpeed(float s) {
		blinkspeed = between(s, 0.0F, 1.0F);
	}

	public static void setBreakAnimation(float a) {
		breakAnimation = (int) between(a, 0, LASTANIMATION);
	}

	public static int getRedInt() {
		return Math.round(getRed() * 256.0F);
	}

	public static int getGreenInt() {
		return Math.round(getGreen() * 256.0F);
	}

	public static int getBlueInt() {
		return Math.round(getBlue() * 256.0F);
	}

	public static int getAlphaInt() {
		return Math.round(getAlpha() * 256.0F);
	}

	public static int getThicknessInt() {
		return Math.round(getThickness());
	}

	public static int getOffsetInt() {
		return Math.round(getOffset() * 1000.0F);
	}

	public static int getBlinkAlphaInt() {
		return Math.round(getBlinkAlpha() * 256.0F);
	}

	public static int getBlinkSpeedInt() {
		return Math.round(getBlinkSpeed() * 100.0F);
	}

	private static float between(float i, float x, float y) {
		if (i < x)
			i = x;
		if (i > y)
			i = y;
		return i;
	}
}