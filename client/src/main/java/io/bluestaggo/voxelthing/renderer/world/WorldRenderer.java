package io.bluestaggo.voxelthing.renderer.world;

import io.bluestaggo.voxelthing.renderer.GLState;
import io.bluestaggo.voxelthing.renderer.MainRenderer;
import io.bluestaggo.voxelthing.renderer.util.Primitives;
import io.bluestaggo.voxelthing.renderer.vertices.FloatBindings;
import io.bluestaggo.voxelthing.renderer.vertices.MixedBindings;
import io.bluestaggo.voxelthing.window.Window;
import io.bluestaggo.voxelthing.world.Direction;
import io.bluestaggo.voxelthing.world.World;
import io.bluestaggo.voxelthing.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL33C.*;

public class WorldRenderer {
	private final MainRenderer renderer;
	private final MixedBindings background;
	private final FloatBindings clouds;

	private World world;
	private ChunkRenderer[] chunkRenderers;
	private Set<ChunkRenderer> sortedChunkRenderers;
	private Set<ChunkRenderer> sortedCulledChunkRenderers;
	private int minX, minY, minZ;
	private int maxX, maxY, maxZ;
	private int lastCullX, lastCullY, lastCullZ;
	private boolean forceCulling = true;

	public int renderDistanceHor = 16;
	public int renderDistanceVer = 8;
	private int renderRangeHor;
	private int renderRangeVer;

	private final Matrix4f viewProj = new Matrix4f();
	private final Matrix4f mvp = new Matrix4f();
	private final Vector3f camPos = new Vector3f();

	public WorldRenderer(MainRenderer renderer) {
		this.renderer = renderer;

		background = Primitives.inWorld().generateSphere(null, 1.0f, 16, 16);
		clouds = Primitives.ofVector3f().generatePlane(null);
	}

	public int chunkRendererCoord(int x, int y, int z) {
		x = Math.floorMod(x + renderDistanceHor, renderRangeHor);
		y = Math.floorMod(y + renderDistanceVer, renderRangeVer);
		z = Math.floorMod(z + renderDistanceHor, renderRangeHor);
		return (x * renderRangeHor + z) * renderRangeVer + y;
	}

	public void setWorld(World world) {
		this.world = world;
		this.loadRenderers();
	}

	public void loadRenderers() {
		minX = minZ = -renderDistanceHor;
		maxX = maxZ = renderDistanceHor;
		minY = -renderDistanceVer;
		maxY = renderDistanceVer;
		renderRangeHor = renderDistanceHor * 2 + 1;
		renderRangeVer = renderDistanceVer * 2 + 1;

		if (chunkRenderers != null) {
			for (ChunkRenderer chunkRenderer : chunkRenderers) {
				chunkRenderer.unload();
			}
		}

		chunkRenderers = new ChunkRenderer[renderRangeHor * renderRangeVer * renderRangeHor];
		for (int x = -renderDistanceHor; x <= renderDistanceHor; x++) {
			for (int z = -renderDistanceHor; z <= renderDistanceHor; z++) {
				for (int y = -renderDistanceVer; y <= renderDistanceVer; y++) {
					int i = chunkRendererCoord(x, y, z);
					chunkRenderers[i] = new ChunkRenderer(renderer, world, x, y, z);
				}
			}
		}

		moveRenderers();
	}

	public void draw() {
		if (renderDistanceHor != renderer.game.settings.renderDistanceHor.getValue()
				|| renderDistanceVer != renderer.game.settings.renderDistanceVer.getValue()) {
			renderDistanceHor = renderer.game.settings.renderDistanceHor.getValue();
			renderDistanceVer = renderer.game.settings.renderDistanceVer.getValue();
			loadRenderers();
		}

		sortedCulledChunkRenderers = sortedChunkRenderers;

		renderer.camera.getViewProj(viewProj);
		Vector3d offset = renderer.camera.getOffset();

		int updates = 0;
		int maxUpdates = 1;
		double currentTime = Window.getTimeElapsed();

		for (ChunkRenderer chunkRenderer : sortedCulledChunkRenderers) {
			if (chunkRenderer.isEmpty() && !chunkRenderer.needsUpdate() || !chunkRenderer.inCamera(renderer.camera)) continue;


			if (updates < maxUpdates) {
				boolean neededUpdate = chunkRenderer.needsUpdate();
				chunkRenderer.render();
				if (neededUpdate) {
					updates++;
				}
				else{
					if (world.blocksWannaRender.get(new Vector3i(
							chunkRenderer.getX(),chunkRenderer.getY(),chunkRenderer.getZ()))!=null){
						chunkRenderer.renderTicking((Vector3i[]) world.blocksWannaRender.get(new Vector3i(
								chunkRenderer.getX(),chunkRenderer.getY(),chunkRenderer.getZ())).toArray());
					}
				}
			}

			float offX = (float) (chunkRenderer.getX() * Chunk.LENGTH - offset.x);
			float offY = (float) (chunkRenderer.getY() * Chunk.LENGTH - offset.y);
			float offZ = (float) (chunkRenderer.getZ() * Chunk.LENGTH - offset.z);

			viewProj.translate(offX, offY, offZ, mvp);
			renderer.camera.getPosition().sub(offX, offY, offZ, camPos);

			renderer.worldShader.mvp.set(mvp);
			renderer.worldShader.fogInfo.camPos.set(camPos);
			renderer.worldShader.fade.set((float)chunkRenderer.getFadeAmount(currentTime));
			chunkRenderer.draw();
		}
		world.blocksWannaRender=new Hashtable<>();
		renderer.worldShader.fade.set(0.0f);
	}

	private ChunkRenderer getRendererAt(int x, int y, int z) {
		return chunkRenderers[chunkRendererCoord(x, y, z)];
	}

	private void calculateCulledRenderers() {
		Vector3f pos = renderer.camera.getPosition();
		int ix = ((int) pos.x) >> Chunk.SIZE_POW2;
		int iy = ((int) pos.y) >> Chunk.SIZE_POW2;
		int iz = ((int) pos.z) >> Chunk.SIZE_POW2;

		if (!forceCulling && ix == lastCullX && iy == lastCullY && iz == lastCullZ) {
			return;
		}
		forceCulling = false;

		Queue<CulledChunkStep> queue = new ArrayDeque<>();
		CulledChunkStep step = new CulledChunkStep(ix, iy, iz, null, 0);
		Set<ChunkRenderer> culledChunkRenderers = new HashSet<>();
		queue.add(step);
		culledChunkRenderers.add(getRendererAt(step.x, step.y, step.z));

		while (!queue.isEmpty()) {
			CulledChunkStep next = queue.remove();
			ChunkRenderer chunkRenderer = getRendererAt(next.x, next.y, next.z);
			Chunk chunk = chunkRenderer != null ? chunkRenderer.getChunk() : null;

			if (chunk != null && chunk.needsCullingUpdate()) {
				chunk.calculateCulling();
			}

			for (Direction dir : Direction.ALL) {
				int nx = next.x + dir.X;
				int ny = next.y + dir.Y;
				int nz = next.z + dir.Z;

				if ((next.steppedDirs & dir.getOpposite().bitMask) != 0) continue;
				if (nx < ix - renderDistanceHor || nx > ix + renderDistanceHor
						|| ny < iy - renderDistanceVer || ny > iy + renderDistanceVer
						|| nz < iz - renderDistanceHor || nz > iz + renderDistanceHor) continue;
				ChunkRenderer nextChunkRenderer = getRendererAt(nx, ny, nz);
				if (nextChunkRenderer != null && culledChunkRenderers.contains(nextChunkRenderer)) continue;
				if (next.fromDir != null && chunk != null && !chunk.canBeSeen(next.fromDir, dir)) continue;
//				if (nextChunkRenderer != null && !nextChunkRenderer.inFrustum(frustum)) continue;

				culledChunkRenderers.add(nextChunkRenderer);
				queue.add(new CulledChunkStep(nx, ny, nz, dir.getOpposite(), next.steppedDirs | dir.bitMask));
			}
		}

		sortedCulledChunkRenderers = sortedChunkRenderers.stream()
				.filter(culledChunkRenderers::contains)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public void loadChunks(long max) {
		var stream = sortedChunkRenderers.stream()
				.filter(Predicate.not(ChunkRenderer::areChunksLoaded));
		if (max > 0) {
			stream = stream.limit(max);
		}
		stream.parallel().forEach(ChunkRenderer::loadNeighborChunks);
	}

	public void drawSky() {
		try (GLState state = new GLState()) {
			renderer.skyShader.use();
			state.disable(GL_DEPTH_TEST);
			glCullFace(GL_FRONT);
			background.draw();
			glCullFace(GL_BACK);
		}
	}

	public void drawClouds() {
		try (GLState state = new GLState()) {
			renderer.cloudShader.use();
			state.enable(GL_BLEND);
			state.disable(GL_CULL_FACE);
			clouds.draw();
		}
	}

	public void moveRenderers() {
		Vector3d cameraPos = renderer.camera.getOffset();
		int x = (int)Math.floor(cameraPos.x / Chunk.LENGTH);
		int y = (int)Math.floor(cameraPos.y / Chunk.LENGTH);
		int z = (int)Math.floor(cameraPos.z / Chunk.LENGTH);

		minX = x - renderDistanceHor;
		minY = y - renderDistanceVer;
		minZ = z - renderDistanceHor;
		maxX = x + renderDistanceHor;
		maxY = y + renderDistanceVer;
		maxZ = z + renderDistanceHor;

		for (int ax = 0; ax < renderRangeHor; ax++) {
			for (int ay = 0; ay < renderRangeVer; ay++) {
				for (int az = 0; az < renderRangeHor; az++) {
					int cx = ax + x - renderDistanceHor;
					int cy = ay + y - renderDistanceVer;
					int cz = az + z - renderDistanceHor;

					ChunkRenderer renderer = chunkRenderers[chunkRendererCoord(cx, cy, cz)];
					if (renderer.getX() != cx || renderer.getY() != cy || renderer.getZ() != cz) {
						renderer.setPosition(cx, cy, cz);
					}
				}
			}
		}

		sortedChunkRenderers = Arrays.stream(chunkRenderers)
				.sorted(this::compareChunks)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private int compareChunks(ChunkRenderer a, ChunkRenderer b) {
		Vector3d cameraPos = renderer.camera.getOffset();

		int ax = (a.getX() - (int)(cameraPos.x / Chunk.LENGTH));
		int ay = (a.getY() - (int)(cameraPos.y / Chunk.LENGTH));
		int az = (a.getZ() - (int)(cameraPos.z / Chunk.LENGTH));

		int bx = (b.getX() - (int)(cameraPos.x / Chunk.LENGTH));
		int by = (b.getY() - (int)(cameraPos.y / Chunk.LENGTH));
		int bz = (b.getZ() - (int)(cameraPos.z / Chunk.LENGTH));

		int aDist = ax * ax + ay * ay + az * az;
		int bDist = bx * bx + by * by + bz * bz;
		return Integer.compare(aDist, bDist);
	}

	public void markNeighbourUpdateAt(int x, int y, int z) {
		markUpdateAt(x, y, z);
		markUpdateAt(x - 1, y, z);
		markUpdateAt(x + 1, y, z);
		markUpdateAt(x, y - 1, z);
		markUpdateAt(x, y + 1, z);
		markUpdateAt(x, y, z - 1);
		markUpdateAt(x, y, z + 1);
	}

	private void markUpdateAt(int x, int y, int z) {
		int cx = Math.floorDiv(x, Chunk.LENGTH);
		int cy = Math.floorDiv(y, Chunk.LENGTH);
		int cz = Math.floorDiv(z, Chunk.LENGTH);
		markChunkUpdateAt(cx, cy, cz);
	}

	public void markNeighbourChunkUpdateAt(int x, int y, int z) {
		markChunkUpdateAt(x, y, z);
		markChunkUpdateAt(x - 1, y, z);
		markChunkUpdateAt(x + 1, y, z);
		markChunkUpdateAt(x, y - 1, z);
		markChunkUpdateAt(x, y + 1, z);
		markChunkUpdateAt(x, y, z - 1);
		markChunkUpdateAt(x, y, z + 1);
	}

	private void markChunkUpdateAt(int x, int y, int z) {
		if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
			return;
		}

		ChunkRenderer renderer = chunkRenderers[chunkRendererCoord(x, y, z)];
		if (renderer.getX() == x && renderer.getY() == y && renderer.getZ() == z) {
			forceCulling = true;
			renderer.queueUpdate();
		}
	}

	public void unload() {
		background.unload();

		if (chunkRenderers != null) {
			for (ChunkRenderer chunkRenderer : chunkRenderers) {
				chunkRenderer.unload();
			}
		}
	}

	private record CulledChunkStep(int x, int y, int z, Direction fromDir, int steppedDirs) {}
}
