package com.fusionflux.portalcubed.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class GeneralUtil {
    /**
     * @author maximum
     */
    public static VoxelShape rotate(VoxelShape shape, Direction dir) {
        List<VoxelShape> shapes = new ArrayList<>();

        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> shapes.add(switch (dir) {
            case WEST -> Shapes.box(z1, y1, x1, z2, y2, x2);
            case SOUTH -> Shapes.box(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
            case EAST -> Shapes.box(1 - z2, y1, 1 - x2, 1 - z1, y2, 1 - x1);
            default -> Shapes.box(x1, y1, z1, x2, y2, z2);
        }));

        return Shapes.or(Shapes.empty(), shapes.toArray(new VoxelShape[0]));
    }

    public static double calculateVelocity(double x, double y, double a, double g) {
        a = Math.toRadians(a);
        return sqrt(x * x * g / (2 * cos(-a) * (x * sin(-a) + y * cos(-a))));
    }

    // Code based off of code from ChatGPT
    public static Vec3 calculatePerpendicularVector(Vec3 lineStart, Vec3 lineEnd, Vec3 point) {
        // Calculate direction vector of line
        final Vec3 d = lineEnd.subtract(lineStart);

        // Calculate vector from point to line
        final Vec3 p = point.subtract(lineStart);

        // Calculate projection of point vector onto line vector
        final double t = p.dot(d) / d.dot(d);

        // Calculate perpendicular vector
        return p.subtract(d.scale(t));
    }

    public static boolean targetsEqual(HitResult a, HitResult b) {
        return a.getType() == b.getType() && switch (a.getType()) {
            case MISS -> true;
            case BLOCK -> ((BlockHitResult)a).getBlockPos().equals(((BlockHitResult)b).getBlockPos());
            case ENTITY -> ((EntityHitResult)a).getEntity() == ((EntityHitResult)b).getEntity();
        };
    }

    // Based on https://forum.unity.com/threads/how-do-i-find-the-closest-point-on-a-line.340058/
    public static Vec3 nearestPointOnLine(Vec3 linePnt, Vec3 lineDir, Vec3 pnt) {
        lineDir = lineDir.normalize();
        final Vec3 v = pnt.subtract(linePnt);
        final double d = v.dot(lineDir);
        return linePnt.add(lineDir.scale(d));
    }

    public static AABB rotate(AABB box, float angle, Direction.Axis axis) {
        angle = Mth.wrapDegrees(angle);
        if ((angle > -45 && angle <= 45) || angle > 135 || angle <= -135) {
            return box;
        } else {
            return switch (axis) {
                case X -> new AABB(box.minX, box.minZ, box.minY, box.maxX, box.maxZ, box.maxY);
                case Y -> new AABB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
                case Z -> new AABB(box.minY, box.minX, box.minZ, box.maxY, box.maxX, box.maxZ);
            };
        }
    }

    public static Vec2 normalToRotation(Vec3 normal) {
        return new Vec2(
            (float)toDegrees(-Mth.atan2(normal.y, sqrt(normal.x * normal.x + normal.z * normal.z))),
            (float)toDegrees(Mth.atan2(normal.z, normal.x)) - 90
        );
    }
}
