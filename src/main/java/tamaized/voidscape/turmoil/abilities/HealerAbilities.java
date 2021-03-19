package tamaized.voidscape.turmoil.abilities;

import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.entity.abilities.EntitySpellBolt;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.skills.TurmoilSkills;

public class HealerAbilities {

	public static final TurmoilAbility REZ = new TurmoilAbility(unloc("resurrection"), TurmoilAbility.Type.Voidic, 800, 30 * 20, (spell, caster) -> {
		RayTraceResult ray = Voidscape.getHitResultFromEyes(caster, e -> e.getCapability(SubCapability.CAPABILITY).map(cap -> cap.get(Voidscape.subCapTurmoilTracked).map(data -> data.incapacitated).orElse(false)).orElse(false), 32, 1.5D, 0D);
		if (caster.level instanceof ServerWorld && ray instanceof EntityRayTraceResult) {
			Entity entity = ((EntityRayTraceResult) ray).getEntity();
			entity.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapTurmoilTracked).ifPresent(data -> data.incapacitated = false));
			for (int i = 0; i < 10; i++) {
				Vector3d pos = new Vector3d(0.25F + entity.level.random.nextFloat() * 0.75F, 0, 0).yRot((float) Math.toRadians(entity.level.random.nextInt(360))).add(entity.getX(), entity.getY() + entity.level.random.nextFloat(), entity.getZ());
				((ServerWorld) caster.level).sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 0, 0, 0, 0, 1);
			}
			caster.level.playSound(null, entity, SoundEvents.BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1F, 0.75F + entity.level.random.nextFloat() * 0.5F);
			return true;
		}
		return false;
	});
	public static final TurmoilAbility HEALING_BOLT = new TurmoilAbility(unloc("healing_bolt"), TurmoilAbility.Type.Voidic, 100, 3 * 20, (spell, caster) -> {
		EntitySpellBolt bolt = new EntitySpellBolt(caster, HealerAbilities.HEALING_BOLT).healing().color(0xFFFF00);
		bolt.setDamage(spell.damage(caster, caster.getCapability(SubCapability.CAPABILITY).map(cap -> cap.get(Voidscape.subCapTurmoilData).map(data ->

				data.hasSkill(TurmoilSkills.MAGE_SKILLS.VOIDMANCY_5) ? 4 :

						data.hasSkill(TurmoilSkills.MAGE_SKILLS.VOIDMANCY_4) ? 3 :

								data.hasSkill(TurmoilSkills.MAGE_SKILLS.VOIDMANCY_3) ? 2 :

										data.hasSkill(TurmoilSkills.MAGE_SKILLS.VOIDMANCY_2) ? 1 : 0).orElse(0)).orElse(0)));
		caster.level.addFreshEntity(bolt);
		return true;
	}).damage(1F);

	private static String unloc(String loc) {
		return Voidscape.MODID.concat(".abilities.healer.".concat(loc));
	}

}
