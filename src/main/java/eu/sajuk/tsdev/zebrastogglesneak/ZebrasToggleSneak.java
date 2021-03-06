package eu.sajuk.tsdev.zebrastogglesneak;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLModDisabledEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

@Mod(modid = "@MOD_ID@", name = "@MOD_NAME@", version = "@MOD_VERSION@", clientSideOnly = true,
     acceptedMinecraftVersions = "[@MINECRAFT_VERSION@]", canBeDeactivated = true,
     updateJSON = "http://tsdev.3zebras.eu/minecraft/zebrastogglesneak_promotions.json?mod=@MOD_VERSION@&mcv=@MINECRAFT_VERSION@",
     guiFactory = "eu.sajuk.tsdev.zebrastogglesneak.ZebasToggleSneakGuiFactory")
public class ZebrasToggleSneak {

	public static Configuration config;
	private final int configVersionMod = 1;
	private int configVersionFile = 0;
	public boolean toggleSneak = true;
	public boolean toggleSprint = false;
	public boolean flyBoost = false;
	public float flyBoostFactor = 4.0F;
	public int keyHoldTicks = 7;
	private final String statusDisplayOpts[] = {"no display", "color coded", "text only"};
	public String statusDisplay = statusDisplayOpts[1];
	private final String displayHPosOpts[] = {"left", "center", "right"};
	public String displayHPos = displayHPosOpts[0];
	private final String displayVPosOpts[] = {"top", "middle", "bottom"};
	public String displayVPos = displayVPosOpts[1];
	private KeyBinding sneakBinding;
	private KeyBinding sprintBinding;
	private List<KeyBinding> kbList;
	private final Minecraft mc = Minecraft.getMinecraft();
	private final MovementInputModded mim = new MovementInputModded(mc.gameSettings, this);
	public final GuiDrawer guiDrawer = new GuiDrawer(this, mim);

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {

		config = new Configuration(event.getSuggestedConfigurationFile(), Integer.toString(configVersionMod));
		config.setCategoryComment(Configuration.CATEGORY_GENERAL, "ATTENTION: Editing this file manually is no longer necessary. \n" +
				"Use the Mods button on Minecraft's home screen to modify these settings.");
		try { configVersionFile = Integer.parseInt(config.getLoadedConfigVersion()); } catch (NumberFormatException e) { };
		while (configVersionFile < configVersionMod) upgradeConfigFrom(configVersionFile++);
		syncConfig();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
        kbList = getKeyBindings();
        for(KeyBinding kb: kbList) ClientRegistry.registerKeyBinding(kb);

		MinecraftForge.EVENT_BUS.register(this);
	}

	@EventHandler
	public void deactivate(FMLModDisabledEvent event) {
		// this class instance is already unregistered from the event bus by Forge itself
		if (displayStatus() > 0) MinecraftForge.EVENT_BUS.unregister(guiDrawer);		
		if (mc.thePlayer != null)
			mc.thePlayer.movementInput = new MovementInputFromOptions(mc.gameSettings);
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {

		if (eventArgs.getModID().equals("@MOD_ID@")) syncConfig();
	}

	public void syncConfig() {

		toggleSneak = config.getBoolean("toggleSneakEnabled", Configuration.CATEGORY_GENERAL, toggleSneak, "Will the sneak toggle function be enabled on startup?", "zebrastogglesneak.config.panel.sneak");
		toggleSprint = config.getBoolean("toggleSprintEnabled", Configuration.CATEGORY_GENERAL, toggleSprint, "Will the sprint toggle function be enabled on startup?", "zebrastogglesneak.config.panel.sprint");
		flyBoost = config.getBoolean("flyBoostEnabled", Configuration.CATEGORY_GENERAL, flyBoost, "Fly boost activated by sprint key in creative mode", "zebrastogglesneak.config.panel.flyboost");
		flyBoostFactor = config.getFloat("flyBoostFactor", Configuration.CATEGORY_GENERAL, flyBoostFactor, 1.0F, 8.0F, "Speed multiplier for fly boost", "zebrastogglesneak.config.panel.flyboostfactor");
		keyHoldTicks = config.getInt("keyHoldTicks", Configuration.CATEGORY_GENERAL, keyHoldTicks, 0, 200, "Minimum key hold time in ticks to prevent toggle", "zebrastogglesneak.config.panel.keyholdticks");
		statusDisplay = config.getString("statusDisplay", Configuration.CATEGORY_GENERAL, statusDisplay, "Status display style", statusDisplayOpts, "zebrastogglesneak.config.panel.display");
		displayHPos = config.getString("displayHPosition", Configuration.CATEGORY_GENERAL, displayHPos, "Horizontal position of onscreen display", displayHPosOpts, "zebrastogglesneak.config.panel.hpos");
		displayVPos = config.getString("displayVPosition", Configuration.CATEGORY_GENERAL, displayVPos, "Vertical position of onscreen display", displayVPosOpts, "zebrastogglesneak.config.panel.vpos");
		guiDrawer.setDrawPosition(displayHPos, displayVPos, displayHPosOpts, displayVPosOpts);
		config.save();
	}
	
	private void upgradeConfigFrom(int version) {
		switch (version) {
		case 0:   // upgrade to version 1: convert displayStatus to string option
			if (config.hasKey(Configuration.CATEGORY_GENERAL,"displayEnabled")) {
				if (!config.hasKey(Configuration.CATEGORY_GENERAL,"statusDisplay")) 
					statusDisplay = config.getBoolean("displayEnabled", Configuration.CATEGORY_GENERAL, true, "dummy")
						? statusDisplayOpts[1] : statusDisplayOpts[0];
				config.getCategory(Configuration.CATEGORY_GENERAL).remove("displayEnabled");
			}
			break;
		}
	}

	public List<KeyBinding> getKeyBindings() {
		
		List<KeyBinding> list = new ArrayList<KeyBinding>();		
		list.add(sneakBinding = new KeyBinding("zebrastogglesneak.key.toggle.sneak", Keyboard.KEY_G, "zebrastogglesneak.key.categories"));
		list.add(sprintBinding = new KeyBinding("zebrastogglesneak.key.toggle.sprint", Keyboard.KEY_V, "zebrastogglesneak.key.categories"));
		return list;
	}

	@EventHandler
	public void postLoad(FMLPostInitializationEvent event) {
	
		if (displayStatus() > 0) MinecraftForge.EVENT_BUS.register(guiDrawer);
	}

	@SubscribeEvent
	public void clientTick(ClientTickEvent event) {
		
		clientTick();
	}

	public void clientTick() {
		
		if ((mc.thePlayer != null) && (!(mc.thePlayer.movementInput instanceof MovementInputModded))) {
			mc.thePlayer.movementInput = mim;
		}
	}
	
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {

		for(KeyBinding kb: kbList) {
			if (kb.isKeyDown()) onKeyInput(kb);
		}
	}

	public void onKeyInput(KeyBinding kb) {
		
		if ((mc.currentScreen instanceof GuiChat)) return;
		
		if (kb == sneakBinding) toggleSneak = !toggleSneak;
		if (kb == sprintBinding) toggleSprint = !toggleSprint;
	}
	
	public int displayStatus() {
		for (int i=0; i < statusDisplayOpts.length; i++) 
			if (statusDisplayOpts[i].equals(statusDisplay)) return i;
		return 0;
	}

}
