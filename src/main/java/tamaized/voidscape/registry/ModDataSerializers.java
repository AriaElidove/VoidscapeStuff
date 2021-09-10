package tamaized.voidscape.registry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.DataSerializerEntry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModDataSerializers {

	private static final DeferredRegister<DataSerializerEntry> REGISTRY = RegUtil.create(ForgeRegistries.DATA_SERIALIZERS);

	public static final EntityDataSerializer<Long> LONG = new EntityDataSerializer<>() {
		@Override
		public void write(FriendlyByteBuf buf, Long value) {
			buf.writeLong(value);
		}

		@Override
		public Long read(FriendlyByteBuf buf) {
			return buf.readLong();
		}

		@Override
		public Long copy(Long value) {
			return value;
		}
	};

	private ModDataSerializers() {

	}

	static void classload() {
		REGISTRY.register("long", () -> new DataSerializerEntry(LONG));
	}

}
