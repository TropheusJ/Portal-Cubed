package com.fusionflux.portalcubed.mixin;

import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.RotationUtil;
import com.fusionflux.portalcubed.accessor.*;
import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.client.packet.PortalCubedClientPackets;
import com.fusionflux.portalcubed.compat.rayon.RayonIntegration;
import com.fusionflux.portalcubed.entity.CorePhysicsEntity;
import com.fusionflux.portalcubed.entity.EntityAttachments;
import com.fusionflux.portalcubed.entity.ExperimentalPortal;
import com.fusionflux.portalcubed.entity.GelBlobEntity;
import com.fusionflux.portalcubed.mechanics.CrossPortalInteraction;
import com.fusionflux.portalcubed.util.IPQuaternion;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAttachments, EntityPortalsAccess, ClientTeleportCheck {
    @Shadow
    public World world;

    @Unique
    private double maxFallSpeed = 0;

    @Unique
    private boolean inFunnel = false;

    @Unique
    private double maxFallHeight = -99999999;

    private Direction prevGravDirec = Direction.DOWN;

    @Unique
    private Vec3d lastVel = Vec3d.ZERO;

    @Unique
    private int gelTransferTimer = 0;

    @Unique
    private int gelTransferChangeTimer = 0;

    @Unique
    private boolean isBounced = false;

    @Unique
    private int funnelTimer = 0;

    @Override
    public double getMaxFallSpeed() {
        return maxFallSpeed;
    }

    @Override
    public void setMaxFallSpeed(double maxFallSpeed) {
        this.maxFallSpeed = maxFallSpeed;
    }

    @Shadow
    public abstract BlockPos getBlockPos();

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public abstract boolean equals(Object o);

    @Shadow
    public abstract boolean hasNoGravity();

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    private Vec3d pos;

    @Shadow
    public abstract boolean canUsePortals();

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Shadow
    public abstract float getYaw();

    @Shadow public abstract float getPitch(float tickDelta);

    @Shadow public abstract double getY();

    @Shadow public abstract int getId();

    @Shadow public abstract double getX();

    @Shadow public abstract double getZ();

    @Shadow public abstract float getYaw(float tickDelta);

    @Shadow public abstract float getPitch();

    private static final Box NULL_BOX = new Box(0, 0, 0, 0, 0, 0);

    @Unique
    private final Map<BlockState, BlockPos> collidingBlocks = new HashMap<>();
    @Unique
    private final Map<BlockState, BlockPos> leftBlocks = new HashMap<>();


    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {

        Entity thisentity = ((Entity) (Object) this);

        Vec3d entityVelocity = this.getVelocity();


        if (!(thisentity instanceof PlayerEntity)) {
            Box portalCheckBox = getBoundingBox();

            portalCheckBox = portalCheckBox.stretch(entityVelocity.add(0, .08, 0));

            List<ExperimentalPortal> list = ((Entity) (Object) this).world.getNonSpectatingEntities(ExperimentalPortal.class, portalCheckBox);
            VoxelShape omittedDirections = VoxelShapes.empty();

            for (ExperimentalPortal portal : list) {
                if (portal.calculateCuttoutBox() != NULL_BOX && portal.calculateBoundsCheckBox() != NULL_BOX) {
                    if (portal.getActive())
                        omittedDirections = VoxelShapes.union(omittedDirections, VoxelShapes.cuboid(portal.getCutoutBoundingBox()));
                }
            }
            CalledValues.setPortalCutout(((Entity) (Object) this), omittedDirections);
        }

        if (this.isInFunnel() && this.getFunnelTimer() != 0) {
            this.setFunnelTimer(this.getFunnelTimer() - 1);
        }
        if (this.isInFunnel() && this.getFunnelTimer() == 0 && this.hasNoGravity()) {
            RayonIntegration.INSTANCE.setNoGravity((Entity)(Object)this, false);
            setInFunnel(false);
        }


        if (this.gelTransferTimer != 0) {
            this.gelTransferTimer -= 1;
        }
        if (this.gelTransferChangeTimer != 0) {
            this.gelTransferChangeTimer -= 1;
        }

        if (maxFallSpeed == 10 && world.getBlockState(this.getBlockPos()).getBlock() == PortalCubedBlocks.PROPULSION_GEL) {
            maxFallSpeed = 10;
        } else {
            if (maxFallSpeed > 0) {
                maxFallSpeed = maxFallSpeed - 1;
            }
        }


        Vec3d rotatedPos;
        rotatedPos = RotationUtil.vecWorldToPlayer(this.pos, GravityChangerAPI.getGravityDirection((Entity) (Object) this));
        if (prevGravDirec != GravityChangerAPI.getGravityDirection(((Entity) (Object) this))) {
            this.maxFallHeight = rotatedPos.y;
        }

        if (!this.isOnGround()) {
            if (rotatedPos.y > this.maxFallHeight) {
                this.maxFallHeight = rotatedPos.y;
            }
        } else {
            this.maxFallHeight = rotatedPos.y;
        }

        this.lastVel = this.getVelocity();

        if (world.getBlockState(this.getBlockPos()).getBlock() != PortalCubedBlocks.REPULSION_GEL && this.isBounced()) {
            this.setBounced(false);
        }

        prevGravDirec = GravityChangerAPI.getGravityDirection(((Entity) (Object) this));
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void tickTail(CallbackInfo ci) {
        Entity thisEntity = ((Entity) (Object) this);

        if (!thisEntity.world.isClient() && !(thisEntity instanceof PlayerEntity) && !(thisEntity instanceof ExperimentalPortal)) {
            Vec3d entityVelocity = this.getVelocity();


            Box portalCheckBox = getBoundingBox();

            portalCheckBox = portalCheckBox.stretch(entityVelocity.add(0, .08, 0));


            List<ExperimentalPortal> list = ((Entity) (Object) this).world.getNonSpectatingEntities(ExperimentalPortal.class, portalCheckBox);
            ExperimentalPortal portal;
            for (ExperimentalPortal portalCheck : list) {
                portal = portalCheck;
                if (this.canUsePortals() && portal.getActive() && !CalledValues.getHasTeleportationHappened(thisEntity) && !CalledValues.getIsTeleporting(thisEntity)) {
                    Direction portalFacing = portal.getFacingDirection();
                    Direction otherDirec = Direction.fromVector((int) portal.getOtherFacing().getX(), (int) portal.getOtherFacing().getY(), (int) portal.getOtherFacing().getZ());

                    if (otherDirec != null) {


                        entityVelocity = entityVelocity.add(0, .08, 0);

                        Vec3d entityEyePos = thisEntity.getEyePos();

                        if (portalFacing.getUnitVector().getX() < 0) {
                            if (entityEyePos.getX() + entityVelocity.x >= portal.getPos().getX() && entityVelocity.getX() > 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }
                        if (portalFacing.getUnitVector().getY() < 0) {
                            if (entityEyePos.getY() + entityVelocity.y >= portal.getPos().getY() && entityVelocity.getY() > 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }
                        if (portalFacing.getUnitVector().getZ() < 0) {
                            if (entityEyePos.getZ() + entityVelocity.z >= portal.getPos().getZ() && entityVelocity.getZ() > 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }
                        if (portalFacing.getUnitVector().getX() > 0) {
                            if (entityEyePos.getX() + entityVelocity.x <= portal.getPos().getX() && entityVelocity.getX() < 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }
                        if (portalFacing.getUnitVector().getY() > 0) {
                            if (entityEyePos.getY() + entityVelocity.y <= portal.getPos().getY() && entityVelocity.getY() < 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }
                        if (portalFacing.getUnitVector().getZ() > 0) {
                            if (entityEyePos.getZ() + entityVelocity.z <= portal.getPos().getZ() && entityVelocity.getZ() < 0 && portal.calculateBoundsCheckBox().intersects(thisEntity.getBoundingBox())) {
                                performTeleport(thisEntity, portal, entityVelocity);
                                break;
                            }
                        }

                    }
                }
            }
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    public void pushAwayFrom(Entity entity, CallbackInfo ci) {
        if (entity instanceof CorePhysicsEntity || entity instanceof GelBlobEntity) {
            ci.cancel();
        }
    }

    private void performTeleport(
            Entity thisEntity,
            ExperimentalPortal portal,
            Vec3d entityVelocity
    ) {
        double teleportXOffset = (thisEntity.getEyePos().getX()) - portal.getPos().getX();
        double teleportYOffset = (thisEntity.getEyePos().getY()) - portal.getPos().getY();
        double teleportZOffset = (thisEntity.getEyePos().getZ()) - portal.getPos().getZ();
        Direction portalFacing = portal.getFacingDirection();
        Direction otherDirec = Direction.fromVector((int) portal.getOtherFacing().getX(), (int) portal.getOtherFacing().getY(), (int) portal.getOtherFacing().getZ());

        IPQuaternion rotationW = IPQuaternion.getRotationBetween(portal.getAxisW().orElseThrow().multiply(-1), portal.getOtherAxisW(), portal.getAxisH().orElseThrow());
        IPQuaternion rotationH = IPQuaternion.getRotationBetween((portal.getAxisH().orElseThrow()), (portal.getOtherAxisH()), portal.getAxisW().orElseThrow());

        if (portalFacing == Direction.UP || portalFacing == Direction.DOWN) {
            if (otherDirec == portalFacing) {
                rotationW = IPQuaternion.getRotationBetween(portal.getNormal().multiply(-1), portal.getOtherNormal(), (portal.getAxisH().orElseThrow()));
                rotationH = IPQuaternion.getRotationBetween((portal.getAxisH().orElseThrow()), (portal.getOtherAxisH()), portal.getNormal().multiply(-1));
            }
        }

        float modPitch = thisEntity.getPitch();
        if (modPitch == 90) {
            modPitch = 0;
        }

        Vec3d rotatedYaw = Vec3d.fromPolar(modPitch, thisEntity.getYaw());
        Vec3d rotatedPitch = Vec3d.fromPolar(thisEntity.getPitch(), thisEntity.getYaw());
        Vec3d rotatedVel = entityVelocity;
        Vec3d rotatedOffsets = new Vec3d(teleportXOffset, teleportYOffset, teleportZOffset);

        rotatedYaw = (rotationH.rotate(rotationW.rotate(rotatedYaw)));
        rotatedPitch = (rotationH.rotate(rotationW.rotate(rotatedPitch)));
        rotatedVel = (rotationH.rotate(rotationW.rotate(rotatedVel)));
        rotatedOffsets = (rotationH.rotate(rotationW.rotate(rotatedOffsets)));

        if (otherDirec == Direction.UP && rotatedVel.y < 0.48) {
            rotatedVel = new Vec3d(rotatedVel.x, 0.48, rotatedVel.z);
        }

        rotatedOffsets = rotatedOffsets.subtract(0, thisEntity.getEyeY() - thisEntity.getY(), 0);
        if (otherDirec != Direction.UP && otherDirec != Direction.DOWN) {
            if (rotatedOffsets.y < -0.95) {
                rotatedOffsets = new Vec3d(rotatedOffsets.x, -0.95, rotatedOffsets.z);
            } else if (rotatedOffsets.y > (-0.95 + (1.9 - thisEntity.getHeight()))) {
                rotatedOffsets = new Vec3d(rotatedOffsets.x, (-0.95 + (1.9 - thisEntity.getHeight())), rotatedOffsets.z);
            }
        }

        Vec2f lookAnglePitch = new Vec2f(
                (float)Math.toDegrees(-MathHelper.atan2(rotatedPitch.y, Math.sqrt(rotatedPitch.x * rotatedPitch.x + rotatedPitch.z * rotatedPitch.z))),
                (float)Math.toDegrees(MathHelper.atan2(rotatedPitch.z, rotatedPitch.x))
        );

        Vec2f lookAngleYaw = new Vec2f(
                (float)Math.toDegrees(-MathHelper.atan2(rotatedYaw.y, Math.sqrt(rotatedYaw.x * rotatedYaw.x + rotatedYaw.z * rotatedYaw.z))),
                (float)Math.toDegrees(MathHelper.atan2(rotatedYaw.z, rotatedYaw.x))
        );
        final Vec3d destPos = portal.getDestination().orElseThrow(ExperimentalPortal.NOT_INIT).add(rotatedOffsets);
        thisEntity.refreshPositionAndAngles(destPos.x, destPos.y, destPos.z, lookAngleYaw.y - 90, lookAnglePitch.x);
        thisEntity.setVelocity(rotatedVel);
        GravityChangerAPI.clearGravity(thisEntity);
        if (world instanceof ServerWorld serverWorld) {
            final PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(getId());
            buf.writeDouble(getX());
            buf.writeDouble(getY());
            buf.writeDouble(getZ());
            buf.writeFloat(getYaw());
            buf.writeFloat(getPitch());
            final Packet<?> packet = ServerPlayNetworking.createS2CPacket(PortalCubedClientPackets.REFRESH_POS, buf);
            for (final ServerPlayerEntity player : serverWorld.getPlayers()) {
                serverWorld.sendToPlayerIfNearby(player, true, destPos.x, destPos.y, destPos.z, packet);
            }
        }
    }

    @Override
    public boolean isInFunnel() {
        return this.inFunnel;
    }

    @Override
    public void setInFunnel(boolean inFunnel) {
        this.inFunnel = inFunnel;
    }

    @Override
    public boolean isBounced() {
        return this.isBounced;
    }

    @Override
    public void setBounced(boolean bounced) {
        this.isBounced = bounced;
    }

    @Override
    public int getFunnelTimer() {

        return this.funnelTimer;
    }

    @Override
    public double getMaxFallHeight() {
        return this.maxFallHeight;
    }


    @Override
    public void setMaxFallHeight(double fall) {
        this.maxFallHeight = fall;
    }

    @Override
    public Vec3d getLastVel() {
        return this.lastVel;
    }


    @Override
    public void setFunnelTimer(int funnelTimer) {
        this.funnelTimer = funnelTimer;
    }

    @Override
    public void setGelTimer(int funnelTimer) {
        this.gelTransferTimer = funnelTimer;
    }

    @Override
    public int getGelTimer() {
        return this.gelTransferTimer;
    }


    @ModifyArgs(
            method = "adjustSingleAxisMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At(target = "Lcom/google/common/collect/ImmutableList$Builder;addAll(Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableList$Builder;", value = "INVOKE", ordinal = 1, remap = false)
    )
    private static void addAllModifyArg(Args args, @Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions) {
        VoxelShape portalBox = CalledValues.getPortalCutout(entity);
        if (portalBox != VoxelShapes.empty())
            args.set(0, ((CustomCollisionView) world).getPortalBlockCollisions(entity, entityBoundingBox.stretch(movement), portalBox));
    }

    @Inject(method = "doesNotCollide(Lnet/minecraft/util/math/Box;)Z", at = @At("RETURN"), cancellable = true)
    private void doesNotCollide(Box box, CallbackInfoReturnable<Boolean> cir) {
        VoxelShape portalBox = CalledValues.getPortalCutout(((Entity) (Object) this));
        if (portalBox != VoxelShapes.empty())
            cir.setReturnValue(true);
    }

    @Inject(method = "wouldPoseNotCollide", at = @At("RETURN"), cancellable = true)
    public void wouldPoseNotCollide(EntityPose pose, CallbackInfoReturnable<Boolean> cir) {
        VoxelShape portalBox = CalledValues.getPortalCutout(((Entity) (Object) this));
        if (portalBox != VoxelShapes.empty())
            cir.setReturnValue(true);
    }

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    public void isInsideWall(CallbackInfoReturnable<Boolean> cir) {
        VoxelShape portalBox = CalledValues.getPortalCutout(((Entity) (Object) this));
        if (portalBox != VoxelShapes.empty()) cir.setReturnValue(false);
    }

    @Inject(method = "collidesWithStateAtPos", at = @At("HEAD"), cancellable = true)
    public void collidesWithStateAtPos(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        VoxelShape portalBox = CalledValues.getPortalCutout(((Entity) (Object) this));
        if (portalBox != VoxelShapes.empty())
            cir.setReturnValue(false);
    }

    @Inject(method = "checkBlockCollision", at = @At("HEAD"))
    private void beginBlockCheck(CallbackInfo ci) {
        leftBlocks.putAll(collidingBlocks);
    }

    @Redirect(
            method = "checkBlockCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;onEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void midBlockCheck(BlockState instance, World world, BlockPos pos, Entity entity) {
        instance.onEntityCollision(world, pos, entity);
        if (
                instance.getBlock() instanceof BlockCollisionTrigger trigger &&
                        intersects(
                                entity.getBoundingBox().offset(pos.multiply(-1)),
                                trigger.getTriggerShape(instance, world, pos, ShapeContext.of(entity))
                        )
        ) {
            final BlockPos immutable = pos.toImmutable();
            if (collidingBlocks.put(instance, immutable) == null) {
                trigger.onEntityEnter(instance, world, immutable, entity);
            }
            leftBlocks.remove(instance);
        }
    }

    @Inject(method = "checkBlockCollision", at = @At("TAIL"))
    private void endBlockCheck(CallbackInfo ci) {
        for (final var entry : leftBlocks.entrySet()) {
            if (entry.getKey().getBlock() instanceof BlockCollisionTrigger trigger) {
                trigger.onEntityLeave(entry.getKey(), world, entry.getValue(), (Entity) (Object) this);
            }
            collidingBlocks.remove(entry.getKey());
        }
        leftBlocks.clear();
    }

    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;raycast(Lnet/minecraft/world/RaycastContext;)Lnet/minecraft/util/hit/BlockHitResult;"))
    private BlockHitResult portalCubed$portalCompatibleRaycast(World world, RaycastContext context) {
        return CrossPortalInteraction.blockInteractionRaycast(world, context);
    }

    private boolean intersects(Box box, VoxelShape shape) {
        return shape.getBoundingBoxes().stream().anyMatch(box::intersects);
    }

    @Override
    public boolean cfg() {
        return false;
    }

    @Override
    public void setCFG() {
    }
}
