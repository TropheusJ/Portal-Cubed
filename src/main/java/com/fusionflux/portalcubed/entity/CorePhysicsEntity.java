package com.fusionflux.portalcubed.entity;

import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.Gravity;
import com.fusionflux.gravity_api.util.RotationUtil;
import com.fusionflux.portalcubed.accessor.Accessors;
import com.fusionflux.portalcubed.accessor.CalledValues;
import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.client.packet.PortalCubedClientPackets;
import com.fusionflux.portalcubed.items.PortalCubedItems;
import com.fusionflux.portalcubed.packet.NetworkingSafetyWrapper;
import com.fusionflux.portalcubed.sound.PortalCubedSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CorePhysicsEntity extends PathAwareEntity  {

    private float fizzleProgress = 0f;
    private boolean fizzling = false;
    private final Set<ServerPlayerEntity> tracking = new HashSet<>();

    public CorePhysicsEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    private boolean canUsePortals = true;
    private boolean hasCollided;
    private int timeSinceLastSound;

    public Vec3d lastPos = this.getPos();

    private float rotation_yaw = this.bodyYaw;

    private final Vec3d offset = new Vec3d(0,this.getWidth()/2,0);
    private final Vec3d offsetHeight = new Vec3d(0,this.getHeight()/2,0);

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        tracking.add(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        tracking.remove(player);
    }

    @Override
    public boolean collides() {
        return !this.isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return canUsePortals;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return  damageSource != DamageSource.OUT_OF_WORLD && !damageSource.isSourceCreativePlayer() ;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.world.isClient && !this.isRemoved()) {
            boolean bl = source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).getAbilities().creativeMode;
            if (source.getAttacker() instanceof PlayerEntity || source == DamageSource.OUT_OF_WORLD) {
                if(source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).getAbilities().allowModifyWorld){
                    if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && !bl) {
                        this.dropItem(PortalCubedItems.STORAGE_CUBE);
                    }
                    this.discard();
                }
                if(!(source.getAttacker() instanceof PlayerEntity)) {
                    if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && !bl) {
                        this.dropItem(PortalCubedItems.STORAGE_CUBE);
                    }
                    this.discard();
                }
            }

        }
        return false;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }
    @Override
    public boolean isCustomNameVisible() {
        return false;
    }


    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        float g = packet.method_11168();
        float h = packet.method_11171();
        this.syncPacketPositionCodec(d, e, f);
        this.bodyYaw = 0;
        this.headYaw = 0;
        this.prevBodyYaw = this.bodyYaw;
        this.prevHeadYaw = this.headYaw;
        this.setNoDrag(true);
        this.setId(packet.getId());
        this.setUuid(packet.getUuid());
        this.updatePositionAndAngles(d, e, f, g, h);
        this.setVelocity((double)((float)packet.getVelocityX() ), (double)((float)packet.getVelocityY() ), (double)((float)packet.getVelocityZ() ));
    }


    public static final TrackedData<Optional<UUID>> HOLDERUUID = DataTracker.registerData(CorePhysicsEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    protected void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(HOLDERUUID, Optional.empty());
    }

    public Boolean getUUIDPresent() {
        return getDataTracker().get(HOLDERUUID).isPresent();
    }

    public UUID getHolderUUID() {
        if (getDataTracker().get(HOLDERUUID).isPresent()) {
            return getDataTracker().get(HOLDERUUID).get();
        }
        return null;
    }
    public void setRotYaw(float yaw) {
        rotation_yaw = yaw;
    }

    public float getRotYaw(){
        return  rotation_yaw;
    }

    public void setHolderUUID(UUID uuid) {
        if(uuid != null) {
            if (getDataTracker().get(HOLDERUUID).isPresent()) {
                PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
                if(player != null)
                CalledValues.setCubeUUID(player,null);
            }
            this.getDataTracker().set(HOLDERUUID, Optional.of(uuid));
        }else {
            this.getDataTracker().set(HOLDERUUID, Optional.empty());
        }
    }

    @Override
    public boolean canUsePortals() {
        return canUsePortals;
    }

    public void dropCube(){
        setHolderUUID(null);
        var bytebuf = PacketByteBufs.create();
        bytebuf.writeDouble(this.getPos().x);
        bytebuf.writeDouble(this.getPos().y);
        bytebuf.writeDouble(this.getPos().z);
        bytebuf.writeDouble(this.lastPos.x);
        bytebuf.writeDouble(this.lastPos.y);
        bytebuf.writeDouble(this.lastPos.z);
        bytebuf.writeFloat(this.rotation_yaw);
        bytebuf.writeUuid(this.getUuid());
        NetworkingSafetyWrapper.sendFromClient("cubeposupdate", bytebuf);
    }

    public void tick() {
        super.tick();
        timeSinceLastSound++;
        this.bodyYaw = rotation_yaw;
        this.headYaw = rotation_yaw;
        canUsePortals = !getUUIDPresent();
        Vec3d rotatedOffset = RotationUtil.vecPlayerToWorld(offsetHeight, GravityChangerAPI.getGravityDirection(this));
        this.lastPos = this.getPos();
        if(!world.isClient) {
            this.setNoDrag(!this.isOnGround() && !this.world.getBlockState(this.getBlockPos()).getBlock().equals(PortalCubedBlocks.EXCURSION_FUNNEL));
            if (getUUIDPresent()) {
                PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
                if (player != null && player.isAlive()) {
                    Vec3d vec3d = player.getCameraPosVec(0);
                    double d = 2;
                    //HitResult hitResult = customRaycast(player,3, 0.0F, false);
                    //if (hitResult.getType() == HitResult.Type.BLOCK) {
                    //    Vec3d resultPos = player.getEyePos().subtract(hitResult.getPos());
                    //    d = Math.sqrt((resultPos.x * resultPos.x) +(resultPos.y * resultPos.y) +(resultPos.z * resultPos.z));
                    //    d -= this.getWidth();
                    //}
                    //if(d>2){
                    //    d=2;
                    //}
                    canUsePortals = false;
                    Vec3d vec3d2 = this.getPlayerRotationVector(player.getPitch(),player.getYaw());
                    Vec3d vec3d3 = vec3d.add((vec3d2.x * d) - rotatedOffset.x, (vec3d2.y * d) - rotatedOffset.y, (vec3d2.z * d) - rotatedOffset.z);
                    GravityChangerAPI.addGravity( this, new Gravity(GravityChangerAPI.getGravityDirection(player),10,1,"player_interaction"));
                    this.fallDistance = 0;
                    //if(player.getHorizontalFacing().equals(Direction.SOUTH)){
                    //    rotation_yaw=0;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.WEST)){
                    //    rotation_yaw=90;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.NORTH)){
                    //    rotation_yaw=180;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.EAST)){
                    //    rotation_yaw=270;
                    //}
                    rotation_yaw = player.headYaw;
                    //if(this instanceof RadioEntity){
                    //    rotation_yaw -= 90;
                    //}

                    move(
                        MovementType.PLAYER,
                        RotationUtil.vecWorldToPlayer(vec3d3.subtract(getPos()), GravityChangerAPI.getGravityDirection(player))
                    );
                    //this.setVelocity(RotationUtil.vecWorldToPlayer(this.getPos().subtract(lastPos), GravityChangerAPI.getGravityDirection(this)).multiply(.5));
                    //this.velocityModified = true;
                }else{
                    if(player != null ){
                        setHolderUUID(null);
                    }
                    canUsePortals = true;
                }
            }
        }else{
            if (getUUIDPresent()) {
                PlayerEntity player = (PlayerEntity)((Accessors) world).getEntity(getHolderUUID());
                if (player != null && player.isAlive()) {
                    Vec3d vec3d = player.getCameraPosVec(0);
                    double d = 2;
                   // HitResult hitResult = customRaycast(player,3, 0.0F, false);
                   // if (hitResult.getType() == HitResult.Type.BLOCK) {
                   //     Vec3d resultPos = player.getEyePos().subtract(hitResult.getPos());
                   //     d = Math.sqrt((resultPos.x * resultPos.x) +(resultPos.y * resultPos.y) +(resultPos.z * resultPos.z));
                   //     d -= this.getWidth();
                   // }
                   // if(d>2){
                   //     d=2;
                   // }
                    Vec3d vec3d2 = player.getRotationVec(1.0F);
                    //if(player.getHorizontalFacing().equals(Direction.SOUTH)){
                    //    rotation_yaw=0;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.WEST)){
                    //    rotation_yaw=90;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.NORTH)){
                    //    rotation_yaw=180;
                    //}
                    //if(player.getHorizontalFacing().equals(Direction.EAST)){
                    //    rotation_yaw=270;
                    //}
                    rotation_yaw = player.headYaw;
                    //if(this instanceof RadioEntity){
                    //    rotation_yaw -= 90;
                    //}
                    Vec3d vec3d3 = vec3d.add((vec3d2.x * d) - rotatedOffset.x, (vec3d2.y * d) - rotatedOffset.y, (vec3d2.z * d) - rotatedOffset.z);
                    move(
                        MovementType.PLAYER,
                        RotationUtil.vecWorldToPlayer(vec3d3.subtract(getPos()), GravityChangerAPI.getGravityDirection(player))
                    );
                }
            }
        }
        if(this.getVelocity().y < -3.92){
            this.setVelocity(this.getVelocity().add(0,.81d,0));
        }
        if (fizzling) {
            if (world.isClient) {
                fizzleProgress += MinecraftClient.getInstance().getTickDelta();
            } else {
                fizzleProgress += 0.05f;
                if (fizzleProgress >= 1f) {
                    remove(RemovalReason.KILLED);
                }
            }
        }
    }

    public void startFizzlingProgress() {
        fizzling = true;
    }

    public void fizzle() {
        world.playSound(null, getX(), getY(), getZ(), PortalCubedSounds.MATERIAL_EMANCIPATION_EVENT, SoundCategory.NEUTRAL, 0.1f, 1f);
        setNoGravity(true);
        fizzling = true;
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(getId());
        final Packet<?> packet = ServerPlayNetworking.createS2CPacket(PortalCubedClientPackets.FIZZLE_PACKET, buf);
        for (final ServerPlayerEntity player : tracking) {
            player.networkHandler.sendPacket(packet);
        }
    }

    public float getFizzleProgress() {
        return fizzleProgress;
    }

    protected final Vec3d getPlayerRotationVector(float pitch, float yaw) {
        float f = pitch * (float) (Math.PI / 180.0);
        float g = -yaw * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return RotationUtil.vecPlayerToWorld(new Vec3d((double)(i * j), (double)(-k), (double)(h * j)), GravityChangerAPI.getGravityDirection(this));
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        super.move(movementType, movement);
        if (horizontalCollision) {
            if (!hasCollided) {
                hasCollided = true;
                if (!world.isClient && timeSinceLastSound >= 20) {
                    world.playSoundFromEntity(null, this, getCollisionSound(), SoundCategory.NEUTRAL, 1f, 1f);
                    timeSinceLastSound = 0;
                }
            }
        } else {
            hasCollided = false;
        }
    }

    protected SoundEvent getCollisionSound() {
        return PortalCubedSounds.CUBE_LOW_HIT_EVENT; // TODO: implement for other physics objects (this requires a lot of assets)
    }

    public void onRemoved() {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
    }

   // public void onKilledOther(ServerWorld world, LivingEntity other) {
   //     if(!world.isClient) {
   //         PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
   //         if (player != null) {
   //             CalledValues.setCubeUUID(player,null);
   //         }
   //     }
   // }

    protected void updatePostDeath() {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
        super.updatePostDeath();
    }

    public void onDeath(DamageSource source) {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
        super.onDeath(source);
    }

    public HitResult customRaycast(Entity user, double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3d vec3d = user.getCameraPosVec(tickDelta);
        Vec3d vec3d2 = user.getRotationVec(tickDelta);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
        return user.world
                .raycast(
                        new RaycastContext(
                                vec3d,
                                vec3d3,
                                RaycastContext.ShapeType.COLLIDER,
                                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                                user
                        )
                );
    }

}
