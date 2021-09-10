package tamaized.voidscape.turmoil;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.network.client.ClientPacketSubCapSync;
import tamaized.voidscape.turmoil.caps.IAggroTable;
import tamaized.voidscape.turmoil.caps.IEffectContext;
import tamaized.voidscape.turmoil.caps.IVoidicArrow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SubCapability {

	public static class Registry {

		private static class Data<C, T extends Tag> {

			private final ResourceLocation id;
			private final NonNullSupplier<C> defaultInstance;
			private final ISubCap.IStorage<C, T> storage;
			private final T defaultTag;

			public Data(ResourceLocation id, NonNullSupplier<C> defaultInstance, ISubCap.IStorage<C, T> storage, T defaultTag) {
				this.id = id;
				this.defaultInstance = defaultInstance;
				this.storage = storage;
				this.defaultTag = defaultTag;
			}
		}

		private static final Map<Class<?>, Data<?, ?>> REGISTRY = new HashMap<>();

		public static <C, T extends Tag> void register(Class<C> cap, ResourceLocation id, NonNullSupplier<C> defaultInstance, ISubCap.IStorage<C, T> storage, T defaultTag) {
			CapabilityManager.INSTANCE.register(cap);
			REGISTRY.put(cap, new Data<>(id, defaultInstance, storage, defaultTag));
		}

		public static Data<?, ?> lookup(Capability<?> cap) {
			return REGISTRY.get(cap.getClass());
		}

	}

	@CapabilityInject(ISubCap.class)
	public static final Capability<ISubCap> CAPABILITY = Voidscape.getNull();

	@CapabilityInject(IVoidicArrow.class)
	public static final Capability<IVoidicArrow> CAPABILITY_VOIDICARROW = Voidscape.getNull();

	@CapabilityInject(IEffectContext.class)
	public static final Capability<IEffectContext> CAPABILITY_EFFECTCONTEXT = Voidscape.getNull();

	@CapabilityInject(IAggroTable.class)
	public static final Capability<IAggroTable> CAPABILITY_AGGRO = Voidscape.getNull();

	static {
		MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (Consumer<AttachCapabilitiesEvent<Entity>>) event -> {
			if (event.getObject() instanceof LivingEntity) {
				apply(event, CAPABILITY);
				apply(event, CAPABILITY_EFFECTCONTEXT);
			}
			if (event.getObject() instanceof AbstractArrow)
				apply(event, CAPABILITY_VOIDICARROW);
			if (event.getObject() instanceof Mob)
				apply(event, CAPABILITY_AGGRO);
		});
		MinecraftForge.EVENT_BUS.addListener((Consumer<LivingEvent.LivingUpdateEvent>) event -> {
			if (!event.getEntity().canUpdate())
				return;
			event.getEntity().getCapability(SubCapability.CAPABILITY).ifPresent(cap -> {
				Arrays.stream(cap.tickers()).forEach(t -> t.tick(event.getEntity()));
				if (event.getEntity() instanceof ServerPlayer) {
					if (cap.getLastWorld() != event.getEntity().level.dimension().location()) {
						Arrays.stream(cap.network()).forEach(n -> n.sendToClient((ServerPlayer) event.getEntity()));
						cap.setLastWorld(event.getEntity().level.dimension().location());
					}
				}
			});
			if (event.getEntity() instanceof Mob)
				event.getEntity().getCapability(CAPABILITY_AGGRO).ifPresent(cap -> cap.tick((Mob) event.getEntity()));
		});
		MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.Clone>) event -> event.getPlayer().getCapability(CAPABILITY).ifPresent(cap -> event.getOriginal().getCapability(CAPABILITY).ifPresent(o -> cap.clone(o, event.isWasDeath()))));
	}

	private static <C, T extends Tag> void apply(AttachCapabilitiesEvent<?> event, Capability<C> cap) {
		@SuppressWarnings("unchecked") Registry.Data<C, T> data = (Registry.Data<C, T>) Registry.lookup(cap);
		final LazyOptional<C> instance = LazyOptional.of(data.defaultInstance);
		event.addCapability(data.id, new ICapabilitySerializable<T>() {
			@Nonnull
			@Override
			public <R> LazyOptional<R> getCapability(@Nonnull Capability<R> check, @Nullable Direction side) {
				return cap.orEmpty(check, instance.cast());
			}

			@Override
			public T serializeNBT() {
				T tag = data.storage.writeNBT(cap, instance.orElseThrow(NullPointerException::new), null);
				return tag == null ? data.defaultTag : tag;
			}

			@Override
			public void deserializeNBT(T nbt) {
				data.storage.readNBT(cap, instance.orElseThrow(NullPointerException::new), null, nbt);
			}
		});
	}

	public interface ISubCap {

		@Nullable
		ResourceLocation getLastWorld();

		void setLastWorld(ResourceLocation level);

		<C extends ISubCapData> Optional<C> get(SubCapKey<C> key);

		ISubCapData.ITickHandler[] tickers();

		ISubCapData.IStorageHandler[] storage();

		ISubCapData.INetworkHandler[] network();

		void clone(ISubCap old, boolean death);

		Optional<ISubCapData.INetworkHandler> network(ResourceLocation id);

		interface ISubCapData {

			void clone(ISubCapData old, boolean death);

			interface ITickHandler extends ISubCapData {

				void tick(Entity parent);

			}

			interface IHasID extends ISubCapData {

				ResourceLocation id();

			}

			interface IStorageHandler extends IHasID {

				CompoundTag write(CompoundTag nbt, @Nullable Direction side);

				void read(CompoundTag nbt, @Nullable Direction side);

			}

			interface INetworkHandler extends IHasID {

				void write(FriendlyByteBuf buffer);

				void read(FriendlyByteBuf buffer);

				default boolean handle(LogicalSide side) {
					return side == LogicalSide.CLIENT;
				}

				default void sendToClient(ServerPlayer parent) {
					Voidscape.NETWORK.send(PacketDistributor.PLAYER.with(() -> parent), new ClientPacketSubCapSync(this));
				}

				default void sendToClients(Entity parent) {
					Voidscape.NETWORK.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> parent), new ClientPacketSubCapSync(this, parent.getId()));
				}

			}

			interface All extends ITickHandler, IStorageHandler, INetworkHandler {

			}

		}

		interface IStorage<C, T extends Tag> {
			T writeNBT(Capability<C> capability, C instance, @Nullable Direction side);

			void readNBT(Capability<C> capability, C instance, @Nullable Direction side, T nbt);
		}

		class DummyStorage<C> implements IStorage<C, CompoundTag> {
			@Nullable
			@Override
			public CompoundTag writeNBT(Capability<C> capability, C instance, @Nullable Direction side) {
				return new CompoundTag();
			}

			@Override
			public void readNBT(Capability<C> capability, C instance, @Nullable Direction side, CompoundTag nbt) {

			}
		}

		interface Storage extends IStorage<ISubCap, CompoundTag> {

			@Nullable
			@Override
			default CompoundTag writeNBT(Capability<ISubCap> capability, ISubCap instance, @Nullable Direction side) {
				CompoundTag nbt = new CompoundTag();
				Arrays.stream(instance.storage()).forEach(h -> nbt.put(h.id().toString(), h.write(new CompoundTag(), side)));
				return nbt;
			}

			@Override
			default void readNBT(Capability<ISubCap> capability, ISubCap instance, @Nullable Direction side, CompoundTag nbt) {
				Arrays.stream(instance.storage()).forEach(h -> h.read(nbt.getCompound(h.id().toString()), side));
			}
		}

		class SubCapKey<C extends ISubCapData> {
			private final Class<C> cast;
			private final Supplier<C> factory;

			public SubCapKey(Class<C> cast, Supplier<C> factory) {
				this.cast = cast;
				this.factory = factory;
			}
		}

	}

	public static final class AttachedSubCap implements ISubCap {

		public static final ResourceLocation ID = new ResourceLocation(Voidscape.MODID, "data");

		private static final List<SubCapKey<?>> REGISTRY = new ArrayList<>();
		private final Map<SubCapKey<?>, ISubCapData> instances = new HashMap<>();

		private ResourceLocation level;

		{
			REGISTRY.forEach(key -> instances.putIfAbsent(key, key.factory.get()));
		}

		public static <C extends ISubCapData> SubCapKey<C> register(Class<C> cast, Supplier<C> factory) {
			SubCapKey<C> key = new SubCapKey<>(cast, factory);
			REGISTRY.add(key);
			return key;
		}

		@Nullable
		@Override
		public ResourceLocation getLastWorld() {
			return level;
		}

		@Override
		public void setLastWorld(ResourceLocation level) {
			this.level = level;
		}

		@Override
		public <C extends ISubCapData> Optional<C> get(SubCapKey<C> key) {
			ISubCapData o = instances.get(key);
			return key.cast.equals(o.getClass()) ? Optional.of(key.cast.cast(o)) : Optional.empty();
		}

		@Override
		public ISubCapData.ITickHandler[] tickers() {
			return instances.values().stream().filter(ISubCapData.ITickHandler.class::isInstance).map(ISubCapData.ITickHandler.class::cast).toArray(ISubCapData.ITickHandler[]::new);
		}

		@Override
		public ISubCapData.IStorageHandler[] storage() {
			return instances.values().stream().filter(ISubCapData.IStorageHandler.class::isInstance).map(ISubCapData.IStorageHandler.class::cast).toArray(ISubCapData.IStorageHandler[]::new);
		}

		@Override
		public ISubCapData.INetworkHandler[] network() {
			return instances.values().stream().filter(ISubCapData.INetworkHandler.class::isInstance).map(ISubCapData.INetworkHandler.class::cast).toArray(ISubCapData.INetworkHandler[]::new);
		}

		@Override
		public void clone(ISubCap old, boolean death) {
			instances.forEach((k, v) -> old.get(k).ifPresent(o -> v.clone(o, death)));
		}

		@Override
		public Optional<ISubCapData.INetworkHandler> network(ResourceLocation id) {
			return instances.values().stream().
					filter(data -> data instanceof ISubCapData.INetworkHandler).
					map(ISubCapData.INetworkHandler.class::cast).
					filter(data -> data.id().equals(id)).
					findAny();
		}
	}

}
