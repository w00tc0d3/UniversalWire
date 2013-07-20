package samoht2401.universalwire;

import java.util.EnumSet;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileSourceEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import samoht2401.universalwire.blocks.BlockCable;
import samoht2401.universalwire.blocks.BlockRubberSmelter;
import samoht2401.universalwire.blocks.BlockRubberStill;
import samoht2401.universalwire.gui.GuiHandler;
import samoht2401.universalwire.network.UWPacketHandler;
import samoht2401.universalwire.system.SystemManager;
import samoht2401.universalwire.tileentity.TileEntityCable;
import samoht2401.universalwire.tileentity.TileEntityRubberSmelter;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "UniversalWire", name = "Universal Wire", version = "0.0.1", dependencies = "required-after:Forge@[7.8.0.725,);required-after:IC2;required-after:BuildCraft|Core")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class UniversalWire implements ITickHandler {

	// The instance of your mod that Forge uses.
	@Instance("UniversalWire")
	public static UniversalWire instance;
	public static SystemManager systemManager;

	public static int blockRenderId;

	public static Block blockRubberSmelter;
	public static Block rubberStill;
	public static BlockCable blockCable;

	public static LiquidStack rubberLiquid;

	public static int rubberModel;

	// Says where the client and server 'proxy' code is loaded.
	@SidedProxy(clientSide = "samoht2401.universalwire.client.ClientProxy", serverSide = "samoht2401.univeralwire.CommonProxy")
	public static CommonProxy proxy;

	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
		systemManager = new SystemManager();
		// Stub Method
		blockRubberSmelter = new BlockRubberSmelter(1337);
		rubberStill = new BlockRubberStill(2400, Material.water).setUnlocalizedName("rubberStill");
		blockCable = (BlockCable) new BlockCable(2401, Material.circuits).setUnlocalizedName("cable");

		GameRegistry.registerBlock(blockRubberSmelter, ItemBlock.class, "blockRubberSmelter");
		GameRegistry.registerBlock(rubberStill, ItemBlock.class, "rubberStill");
		GameRegistry.registerBlock(blockCable, ItemBlock.class, "cable");
		GameRegistry.registerTileEntity(TileEntityRubberSmelter.class, "containerRubberSmelter");
		GameRegistry.registerTileEntity(TileEntityCable.class, "containerCable");
		NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
		NetworkRegistry.instance().registerChannel(new UWPacketHandler(), UWPacketHandler.CHANNEL);
		LanguageRegistry.addName(blockRubberSmelter, "Rubber Smelter");
		LanguageRegistry.addName(rubberStill, "Rubber Liquid");
		LanguageRegistry.addName(blockCable, "Cable");

		GameRegistry.addShapedRecipe(new ItemStack(blockCable, 16), "GDG", "GRG", "GDG", 'G', Block.glass, 'D', Item.diamond, 'R',
				Block.blockRedstone);

		rubberLiquid = LiquidDictionary.getOrCreateLiquid("Rubber", new LiquidStack(rubberStill, 1));

		MinecraftForge.EVENT_BUS.register(this);
		TickRegistry.registerTickHandler(this, Side.SERVER);
	}

	@ForgeSubscribe
	@SideOnly(Side.CLIENT)
	public void textureHook(TextureStitchEvent.Post event) {
		LiquidDictionary.getCanonicalLiquid("Rubber").setRenderingIcon(rubberStill.getBlockTextureFromSide(1))
				.setTextureSheet("/terrain.png");
	}

	@ForgeSubscribe
	public void EnergyLoadEvent(EnergyTileLoadEvent event) {
		if (event.energyTile instanceof TileEntity)
			systemManager.addItem(event.world, (TileEntity) event.energyTile);
	}

	@ForgeSubscribe
	public void EnergyUnloadEvent(EnergyTileUnloadEvent event) {
		if (event.energyTile instanceof TileEntity)
			systemManager.removeItem(event.world, (TileEntity) event.energyTile);
	}

	@ForgeSubscribe
	public void EnergySourceEvent(EnergyTileSourceEvent event) {
		if (event.energyTile instanceof TileEntity)
			event.amount = systemManager.sourceEvent(event.world, (TileEntity) event.energyTile, event.amount);
	}

	@ForgeSubscribe
	public void worldSaveEvent(WorldEvent.Unload event) {
		if (systemManager != null)
			systemManager.unloadWorld(event.world);
	}

	@ForgeSubscribe
	public void playerJoinEvent(EntityJoinWorldEvent event) {
		if (!(event.entity instanceof EntityPlayer))
			return;
		if (systemManager != null)
			systemManager.playerJoin(event.world, (EntityPlayer) event.entity);
	}

	@Init
	public void load(FMLInitializationEvent event) {
		proxy.registerRenderers();

	}

	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
		// Stub Method
	}

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		systemManager.update();
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {

	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.WORLD);
	}

	@Override
	public String getLabel() {
		return "UniversalWireGeneralTickHandler";
	}

}