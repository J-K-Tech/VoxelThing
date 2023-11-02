package io.bluestaggo.voxelthing.renderer.world;

import io.bluestaggo.voxelthing.window.Window;
import io.bluestaggo.voxelthing.world.BlockRaycast;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Camera {
	private final Window window;

	private final Vector3f position = new Vector3f();
	private final Vector3d offset = new Vector3d();
	private final Vector3d offsetPosition = new Vector3d();
	private final FrustumIntersection frustum = new FrustumIntersection();
	private final Vector3f front;
	private final Vector3f up = new Vector3f();
	private final Vector3f right = new Vector3f();
	private final Vector3f target = new Vector3f();

	private float yaw = -90.0f;
	private float pitch = 0.0f;

	private final float fov;
	private float near;
	private float far;

	private final Matrix4f view = new Matrix4f();
	private final Matrix4f proj = new Matrix4f();
	private final Matrix4f viewProj = new Matrix4f();

	public Camera(Window window) {
		this(window, 60.0f, 0.1f, 256.0f);
	}

	public Camera(Window window, float fov, float near, float far) {
		this.window = window;

		front = new Vector3f(0.0f, 0.0f, -1.0f);

		this.fov = (float) Math.toRadians(fov);
		this.near = near;
		this.far = far;

		updateVectors();
	}

	public void setPosition(float x, float y, float z) {
		Vector3d f= getRaycast(5).getHitd();
		position.x = x;
		position.y = y;
		position.z = z;
		offset.add(position, offsetPosition);
		offset.ceil(f);
		updateVectors();
	}

	public void addPosition(float x, float y, float z) {
		Vector3d f= getRaycast(5).getHitd();
		position.x += x;
		position.y += y;
		position.z += z;
		offset.add(position, offsetPosition);
		offset.ceil(f);
		updateVectors();
	}

	public void setOffset(double x, double y, double z) {
		offset.x = x;
		offset.y = y;
		offset.z = z;
		offset.add(position, offsetPosition);
	}

	public void setRotation(float yaw, float pitch) {
		this.yaw = yaw % 360.0f;
		if (Math.abs(Math.abs(pitch) - 90.0f) < 0.1f) {
			pitch = 90.1f * Math.signum(pitch);
		}
		this.pitch = pitch % 360.0f;
		updateVectors();
	}

	public BlockRaycast getRaycast(double length) {
		return new BlockRaycast(
				offset,
				new Vector3d(front),
				length
		);
	}

	public void moveForward(float x) {
		position.add(front.mul(x, new Vector3f()));
		updateVectors();
	}

	public void moveRight(float x) {
		position.add(right.mul(x, new Vector3f()));
		updateVectors();
	}

	private void updateVectors() {
		boolean flip = Math.abs(pitch) > 90.0f;

		front.set(0.0f, 0.0f, -1.0f);
		front.rotateX((float) Math.toRadians(pitch));
		front.rotateY((float) Math.toRadians(-yaw - 90.0f));

		front.cross(0.0f, 1.0f, 0.0f, right);
		if (flip) {
			right.rotateY((float) Math.toRadians(180.0));
		}
		right.cross(front, up);

		position.add(front, target);
		frustum.set(getViewProj(viewProj));
	}

	public Vector3f getPosition() {
		return position;
	}

	public Vector3f getPosition(Vector3f vector) {
		return vector.set(position);
	}

	public Vector3d getOffset() {
		return offset;
	}

	public Vector3d getOffsetPosition() {
		return offsetPosition;
	}

	public float getNear() {
		return near;
	}

	public float getFar() {
		return far;
	}

	public void setNear(float near) {
		this.near = near;
	}

	public void setFar(float far) {
		this.far = far;
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public Matrix4f getView() {
		return view.identity().lookAt(position, target, up);
	}

	public Matrix4f getProj() {
		return proj.identity().perspective(fov, (float) window.getWidth() / (float) window.getHeight(), near, far);
	}

	public Matrix4f getViewProj(Matrix4f viewProj) {
		return proj.mul(view, viewProj);
	}

	public FrustumIntersection getFrustum() {
		return frustum;
	}
}
