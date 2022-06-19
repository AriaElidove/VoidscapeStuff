package tamaized.voidscape.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tamaized.voidscape.Voidscape;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Voidscape.MODID)
public final class InstanceManager {

	private static final List<Instance> instances = new ArrayList<>();

	private InstanceManager() {

	}

	public static void setup(MinecraftServer server) {
		instances.clear();
		server.getWorldData().worldGenSettings().dimensions().entrySet().stream().
				filter(entry -> entry.getValue().generator() instanceof InstanceChunkGenerator).
				forEach(entry -> instances.add(new Instance(entry.getKey(), entry.getValue())));
	}

	public static Optional<Instance> findByPlayer(Player player) {
		return instances.stream().filter(instance -> instance.players().contains(player)).findFirst();
	}

	public static Optional<Instance> find(ResourceLocation loc) {
		return instances.stream().filter(instance -> instance.location().location().equals(loc)).findFirst();
	}

	public static List<Instance> findByGroup(ResourceLocation group) {
		return instances.stream().filter(instance -> instance.generator().group().equals(group)).collect(Collectors.toList());
	}

	public static Optional<Instance> findByLevel(Level level) {
		return instances.stream().filter(instance -> instance.getLevel() == level).findFirst();
	}

	public static Optional<Instance> findFreeInstanceByGroup(ResourceLocation group) {
		return instances.stream().filter(instance -> instance.generator().group().equals(group) && !instance.active() && !instance.unloading()).findAny();
	}

	private static void load(ServerLevel level) {
		if (level.dimension().location().equals(Level.OVERWORLD.location())) {
			setup(level.getServer());
			return;
		}
		instances.stream().filter(instance -> instance.location().location().equals(level.dimension().location())).
				forEach(instance -> instance.init(level));
	}

	@SubscribeEvent
	public static void onLevelLoad(WorldEvent.Load event) {
		if (event.getWorld().isClientSide())
			return;
		if (event.getWorld() instanceof ServerLevel)
			load((ServerLevel) event.getWorld());
	}

	@SubscribeEvent
	public static void onTick(TickEvent.WorldTickEvent event) {
		if (event.phase == TickEvent.Phase.END || event.world.isClientSide())
			return;
		instances.stream().filter(instance -> instance.getLevel() == event.world).forEach(Instance::tick);
	}

}
