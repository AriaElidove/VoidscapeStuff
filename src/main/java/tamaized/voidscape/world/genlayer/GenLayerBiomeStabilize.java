package tamaized.voidscape.world.genlayer;

import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.context.BigContext;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer1;

public enum GenLayerBiomeStabilize implements AreaTransformer1 {

	INSTANCE;

	GenLayerBiomeStabilize() {
	}

	@Override
	public int getParentX(int x) {
		return x & 3;
	}

	@Override
	public int getParentY(int z) {
		return z & 3;
	}

	@Override
	public int applyPixel(BigContext<?> iExtendedNoiseRandom, Area iArea, int x, int z) {
		int offX = getParentX(x << 4);
		int offZ = getParentY(z << 4);
		int centerX = ((x + offX + 1) & -4) - offX;
		int centerZ = ((z + offZ + 1) & -4) - offZ;
		return x <= centerX + 1 && x >= centerX - 1 && z <= centerZ + 1 && z >= centerZ - 1 ? iArea.get(centerX, centerZ) : iArea.get(x, z);
	}
}