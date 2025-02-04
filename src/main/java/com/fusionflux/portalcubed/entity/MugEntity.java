package com.fusionflux.portalcubed.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.Random;

public class MugEntity extends CorePhysicsEntity  {

    public MugEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }
    final Random rand = new Random();

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
    }

    public int getMugType() {
        return getEntityData().get(MUG_TYPE);
    }

    public void genMugType() {
        setMugType(rand.nextInt(4));
    }

    public void setMugType(Integer type) {
        this.getEntityData().set(MUG_TYPE, type);
    }

    public static final EntityDataAccessor<Integer> MUG_TYPE = SynchedEntityData.defineId(MugEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(MUG_TYPE, 20);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        if (this.getMugType() == 20) {
            setMugType(rand.nextInt(4));
        }
        super.recreateFromPacket(packet);
    }
}
