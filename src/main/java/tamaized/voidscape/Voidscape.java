package tamaized.voidscape;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.voidscape.network.NetworkMessages;
import tamaized.voidscape.turmoil.Insanity;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.Turmoil;
import tamaized.voidscape.world.VoidBiome;
import tamaized.voidscape.world.VoidChunkGenerator;
import tamaized.voidscape.world.VoidTeleporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Mod(Voidscape.MODID)
public class Voidscape {

	public static final String MODID = "voidscape";

	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder.
			named(new ResourceLocation(MODID, MODID)).
			clientAcceptedVersions(s -> true).
			serverAcceptedVersions(s -> true).
			networkProtocolVersion(() -> "1").
			simpleChannel();

	private static final List<DeferredRegister> REGISTERS = new ArrayList<>();

	private static final DeferredRegister<Biome> REGISTRY_BIOME = create(DeferredRegister.create(ForgeRegistries.BIOMES, MODID));
	public static final RegistryObject<Biome> BIOME = REGISTRY_BIOME.register("void", VoidBiome::new);
	public static final RegistryKey<DimensionType> DIMENSION_TYPE = RegistryKey.func_240903_a_(Registry.DIMENSION_TYPE_KEY, new ResourceLocation(MODID, "void"));
	public static final RegistryKey<World> WORLD_KEY = RegistryKey.func_240903_a_(Registry.WORLD_KEY, new ResourceLocation(MODID, "void"));
	public static final SubCapability.ISubCap.SubCapKey<Turmoil> subCapTurmoilData = SubCapability.AttachedSubCap.register(Turmoil.class, Turmoil::new);
	public static final SubCapability.ISubCap.SubCapKey<Insanity> subCapInsanity = SubCapability.AttachedSubCap.register(Insanity.class, Insanity::new);

	public Voidscape() {
		IEventBus busMod = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus busForge = MinecraftForge.EVENT_BUS;
		for (DeferredRegister register : REGISTERS)
			register.register(busMod);
		busMod.addListener((Consumer<FMLCommonSetupEvent>) event -> {
			NetworkMessages.register(NETWORK);
			Registry.register(Registry.field_239690_aB_, new ResourceLocation(MODID, "void"), VoidChunkGenerator.codec);
			CapabilityManager.INSTANCE.register(SubCapability.ISubCap.class, new SubCapability.ISubCap.Storage() {
			}, SubCapability.AttachedSubCap::new);
		});
		busMod.addListener((Consumer<FMLClientSetupEvent>) event -> {
			DimensionRenderInfo.field_239208_a_.put(getDimensionType(), new DimensionRenderInfo(Float.NaN, false, DimensionRenderInfo.FogType.NONE, false, false) {
				@Override
				public Vector3d func_230494_a_(Vector3d p_230494_1_, float p_230494_2_) {
					return Vector3d.ZERO;
				}

				@Override
				public boolean func_230493_a_(int p_230493_1_, int p_230493_2_) {
					return true;
				}

				@Override
				@Nullable
				public float[] func_230492_a_(float p_230492_1_, float p_230492_2_) {
					return null;
				}
			});
		});
		busForge.addListener((Consumer<EntityViewRenderEvent.FogColors>) event -> {
			if (Minecraft.getInstance().world != null && checkForVoidDimension(Minecraft.getInstance().world)) {
				event.setRed(0.04F);
				event.setGreen(0.03F);
				event.setBlue(0.05F);
			}
		});
		busForge.addListener((Consumer<LivingDeathEvent>) event -> {
			if (event.getEntity() instanceof PlayerEntity && checkForVoidDimension(event.getEntity().world)) {
				event.setCanceled(true);
				((PlayerEntity) event.getEntity()).setHealth(((PlayerEntity) event.getEntity()).getMaxHealth());
				event.getEntity().changeDimension(getWorld(event.getEntity().world, World.field_234918_g_), VoidTeleporter.INSTANCE);
			}
		});
	}

	private static <R extends IForgeRegistryEntry<R>> DeferredRegister<R> create(DeferredRegister<R> register) {
		REGISTERS.add(register);
		return register;
	}

	public static RegistryKey<DimensionType> getDimensionType() {
		return DIMENSION_TYPE;
	}

	public static boolean checkForVoidDimension(World world) {
		return checkForVoidDimension(world.func_234922_V_());
	}

	public static boolean checkForVoidDimension(RegistryKey<DimensionType> type) {
		return type.func_240901_a_().equals(getDimensionType().func_240901_a_());
	}

	public static ServerWorld getWorld(World world, RegistryKey<World> dest) {
		return Objects.requireNonNull(Objects.requireNonNull(world.getServer()).getWorld(dest));
	}

	@Nonnull
	@SuppressWarnings("ConstantConditions")
	public static <T> T getNull() {
		return null;
	}

}
