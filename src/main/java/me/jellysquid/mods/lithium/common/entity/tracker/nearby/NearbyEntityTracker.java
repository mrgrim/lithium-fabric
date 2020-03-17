package me.jellysquid.mods.lithium.common.entity.tracker.nearby;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Box;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.Set;

/**
 * Maintains a collection of all entities within the range of this listener. This allows AI goals to quickly
 * assess nearby entities which match the provided class.
 */
public class NearbyEntityTracker<T extends Entity> implements NearbyEntityListener {
    private final Class<T> clazz;
    private final Entity self;

    private final int rangeC;
    private final float rangeSq;

    private final Set<T> nearby = new ReferenceOpenHashSet<>();

    public NearbyEntityTracker(Class<T> clazz, Entity self, float range) {
        this.clazz = clazz;
        this.self = self;
        this.rangeSq = range * range;
        this.rangeC = Math.max((MathHelper.ceil(range) + 15) >> 4, 1);
    }

    @Override
    public int getChunkRange() {
        return this.rangeC;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEntityEnteredRange(Entity entity) {
        if (!this.clazz.isInstance(entity)) {
            return;
        }

        this.nearby.add((T) entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEntityLeftRange(Entity entity) {
        if (this.nearby.isEmpty() || !this.clazz.isInstance(entity)) {
            return;
        }

        this.nearby.remove((T) entity);
    }

    /**
     * Gets the closest T (extends LivingEntity) to the center of this tracker that also intersects with the given box and meets the
     * requirements of the targetPredicate.
     * The result may be different from vanilla if there are multiple closest entities.
     * @param box the box the entities have to intersect
     * @param targetPredicate predicate the entity has to meet
     * @return the closest Entity that meets all requirements (distance, box intersection, predicate, type T)
     */
    public T getClosestEntity(Box box, TargetPredicate targetPredicate) {
        if (this.self instanceof LivingEntity) {
            double x = this.self.getX();
            double y = this.self.getY();
            double z = this.self.getZ();

            T nearest = null;
            double nearestDistance = Double.POSITIVE_INFINITY;

            for (T entity : this.nearby) {
                if (entity instanceof LivingEntity) {
                    double distance = entity.squaredDistanceTo(x, y, z);

                    if (distance < nearestDistance && (box == null || box.intersects(entity.getBoundingBox())) && targetPredicate.test((LivingEntity) this.self, (LivingEntity) entity)) {
                        nearest = entity;
                        nearestDistance = distance;
                    }
                }
            }

            if (nearestDistance <= this.rangeSq) {
                return nearest;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return super.toString() + " for entity class: " + clazz.getName() + ", in rangeSq: " + rangeSq + ", around entity: " + self.toString() + " with NBT: " + self.toTag(new CompoundTag());
    }
}
