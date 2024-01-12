package io.bluestaggo.voxelthing.math;

import io.bluestaggo.voxelthing.world.Direction;
import org.joml.Matrix3x2d;
import org.joml.Vector3d;

public class AABB {
	private static final Vector3d[] FACES = new Vector3d[6];

	public double minX;

	public double minY;
	public double minZ;
	public double maxX;
	public double maxY;
	public double maxZ;

	static {
		for (int i = 0; i < 6; i++) {
			FACES[i] = new Vector3d();
		}
	}

	public AABB() {
		this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
	}

	public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		setBounds(minX, minY, minZ, maxX, maxY, maxZ, this);
	}

	public AABB setBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, AABB result) {
		if (result == null) result = this;
		result.minX = minX;
		result.minY = minY;
		result.minZ = minZ;
		result.maxX = maxX;
		result.maxY = maxY;
		result.maxZ = maxZ;
		return result;
	}

	public AABB offset(double x, double y, double z, AABB result) {
		if (result == null) result = this;
		result.minX = minX + x;
		result.minY = minY + y;
		result.minZ = minZ + z;
		result.maxX = maxX + x;
		result.maxY = maxY + y;
		result.maxZ = maxZ + z;
		return result;
	}

	public AABB expandToPoint(double x, double y, double z, AABB result) {
		if (result == null) result = this;
		result.minX = x < 0.0 ? minX + x : minX;
		result.minY = y < 0.0 ? minY + y : minY;
		result.minZ = z < 0.0 ? minZ + z : minZ;
		result.maxX = x > 0.0 ? maxX + x : maxX;
		result.maxY = y > 0.0 ? maxY + y : maxY;
		result.maxZ = z > 0.0 ? maxZ + z : maxZ;
		return result;
	}

	public double calcXOffset(AABB other, double off) {
		if (other.maxY <= minY || other.minY >= maxY
				|| other.maxZ <= minZ || other.minZ >= maxZ) {
			return off;
		}

		if (off > 0.0 && other.maxX <= minX && minX - other.maxX < off) {
			off = minX - other.maxX;
		}

		if (off < 0.0 && other.minX >= maxX && maxX - other.minX > off) {
			off = maxX - other.minX;
		}

		return off;
	}

	public double calcYOffset(AABB other, double off) {
		if (other.maxX <= minX || other.minX >= maxX
				|| other.maxZ <= minZ || other.minZ >= maxZ) {
			return off;
		}

		if (off > 0.0 && other.maxY <= minY && minY - other.maxY < off) {
			off = minY - other.maxY;
		}

		if (off < 0.0 && other.minY >= maxY && maxY - other.minY > off) {
			off = maxY - other.minY;
		}

		return off;
	}

	public double calcZOffset(AABB other, double off) {
		if (other.maxX <= minX || other.minX >= maxX
				|| other.maxY <= minY || other.minY >= maxY) {
			return off;
		}

		if (off > 0.0 && other.maxZ <= minZ && minZ - other.maxZ < off) {
			off = minZ - other.maxZ;
		}

		if (off < 0.0 && other.minZ >= maxZ && maxZ - other.minZ > off) {
			off = maxZ - other.minZ;
		}

		return off;
	}

	public double getMidX() {
		return MathUtil.lerp(minX, maxX, 0.5);
	}

	public double getMidY() {
		return MathUtil.lerp(minY, maxY, 0.5);
	}

	public double getMidZ() {
		return MathUtil.lerp(minZ, maxZ, 0.5);
	}

	public boolean intersects(AABB other) {
		return other.maxX > minX && other.minX < maxX
				&& other.maxY > minY && other.minY < maxY
				&& other.maxZ > minZ && other.minZ < maxZ;
	}

	public boolean contains(double x, double y, double z) {
		return x > minX && x < maxX
				&& y > minY && y < maxY
				&& z > minZ && z < maxZ;
	}

	public Direction getClosestFace(Vector3d pos, Vector3d dir) {
		dir.normalize().mul(0.01);

		while (contains(pos.x, pos.y, pos.z)) {
			pos.sub(dir);
		}

		double midX = getMidX();
		double midY = getMidY();
		double midZ = getMidZ();

		FACES[Direction.NORTH.ordinal()].set(midX, midY, minZ);
		FACES[Direction.SOUTH.ordinal()].set(midX, midY, maxZ);
		FACES[Direction.WEST.ordinal()].set(minX, midY, midZ);
		FACES[Direction.EAST.ordinal()].set(maxX, midY, midZ);
		FACES[Direction.BOTTOM.ordinal()].set(midX, minY, midZ);
		FACES[Direction.TOP.ordinal()].set(midX, maxY, midZ);

		int closestFace = 0;
		double closestDist = FACES[0].distanceSquared(pos);

		for (int i = 1; i < 6; i++) {
			double dist = FACES[i].distanceSquared(pos);
			if (dist < closestDist) {
				closestDist = dist;
				closestFace = i;
			}
		}

		return Direction.values()[closestFace];
	}
}
