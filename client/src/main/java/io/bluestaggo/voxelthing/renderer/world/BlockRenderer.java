package io.bluestaggo.voxelthing.renderer.world;

import io.bluestaggo.voxelthing.renderer.vertices.MixedBindings;
import io.bluestaggo.voxelthing.world.Direction;
import io.bluestaggo.voxelthing.world.IBlockAccess;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.block.BlockSlab;
import io.bluestaggo.voxelthing.world.block.BlockStair;
import io.bluestaggo.voxelthing.world.chunk.Chunk;
import org.joml.Vector2i;

public class BlockRenderer {
	private static final float SHADE_FACTOR = 0.15f;
	private final SideRenderer[] SIDE_RENDERERS = {
			this::renderNorthFace,
			this::renderSouthFace,
			this::renderWestFace,
			this::renderEastFace,
			this::renderBottomFace,
			this::renderTopFace,
	};

	@FunctionalInterface
	private interface SideRenderer {
		void render(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z);
	}

	private byte getShade(int amount) {
		return (byte) ((1.0f - SHADE_FACTOR * amount) * 255.0f);
	}

	public boolean render(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, int x, int y, int z) {
		Block block = chunk.getBlock(x, y, z);
		if (block == null) {
			return false;
		}

		int xx = x + Chunk.LENGTH * chunk.x;
		int yy = y + Chunk.LENGTH * chunk.y;
		int zz = z + Chunk.LENGTH * chunk.z;
		for (Direction dir : Direction.ALL) {
			if (block instanceof BlockStair){
				SIDE_RENDERERS[dir.ordinal()].render(bindings, blockAccess, chunk, block, xx, yy, zz);
			}
			else{
			if (block.isFaceDrawn(blockAccess, xx + dir.X, yy + dir.Y, zz + dir.Z, dir)) {
				SIDE_RENDERERS[dir.ordinal()].render(bindings, blockAccess, chunk, block, xx, yy, zz);
			}}

		}

		return true;
	}

	private void renderNorthFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.NORTH, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(1);

		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;
		float top = 1.f;
		if (block instanceof BlockSlab||block instanceof BlockStair){top=.5f;
			if (block instanceof BlockStair){
				addVertices(bindings,   x + 1.f,  y + 1.f,  z,  shade,  shade,  shade,  texX,   texY    );
				addVertices(bindings,   x + 1.f,  y+ .5f,      z,  shade,  shade,  shade,  texX,   texYp   );
				addVertices(bindings,   x,      y+ .5f,      z,  shade,  shade,  shade,  texXp,  texYp   );
				addVertices(bindings,   x,      y + 1.f,  z,  shade,  shade,  shade,  texXp,  texY    );
				bindings.addIndices(0, 1, 2, 2, 3, 0);

			}
		}
		texYp = texY + (Block.TEXTURE_WIDTH*top);

		addVertices(bindings,   x + 1,  y + top,  z,  shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x + 1,  y,      z,  shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x,      y,      z,  shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x,      y + top,  z,  shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private void renderSouthFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.SOUTH, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(3);

		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;
		float top = 1.f;
		if (block instanceof BlockSlab||block instanceof BlockStair){top=.5f;
			if (block instanceof BlockStair){
				addVertices(bindings,   x,      y + 1.f,  z + .5f,  shade,  shade,  shade,  texX,   texY    );
				addVertices(bindings,   x,      y+ .5f,      z + .5f,  shade,  shade,  shade,  texX,   texYp   );
				addVertices(bindings,   x + 1,  y+.5f,      z + .5f,  shade,  shade,  shade,  texXp,  texYp   );
				addVertices(bindings,   x + 1,  y + 1.f,  z + .5f,  shade,  shade,  shade,  texXp,  texY    );
				bindings.addIndices(0, 1, 2, 2, 3, 0);

			}
		}
		texYp = texY + (Block.TEXTURE_WIDTH*top);

		addVertices(bindings,   x,      y + top,  z + 1,  shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x,      y,      z + 1,  shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x + 1,  y,      z + 1,  shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x + 1,  y + top,  z + 1,  shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private void renderWestFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.WEST, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(2);
		float top = 1.f;

		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;
		if (block instanceof BlockSlab||block instanceof BlockStair){top=.5f;
			if (block instanceof BlockStair){
				addVertices(bindings,   x,  y + 1,  z,      shade,  shade,  shade,  texX,   texY    );
				addVertices(bindings,   x,  y+.5f,      z,      shade,  shade,  shade,  texX,   texYp   );
				addVertices(bindings,   x,  y+.5f,      z + .5f,  shade,  shade,  shade,  texXp,  texYp   );
				addVertices(bindings,   x,  y + 1,  z + .5f,  shade,  shade,  shade,  texXp,  texY    );
				bindings.addIndices(0, 1, 2, 2, 3, 0);

			}
		}
		texYp = texY + (Block.TEXTURE_WIDTH*top);

		addVertices(bindings,   x,  y + top,  z,      shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x,  y,      z,      shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x,  y,      z + 1,  shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x,  y + top,  z + 1,  shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private void renderEastFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.EAST, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(2);
		float top = 1.f;
		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;
		if (block instanceof BlockSlab||block instanceof BlockStair){top=.5f;
			if (block instanceof BlockStair){
				addVertices(bindings,   x + 1,  y + 1,  z + .5f,  shade,  shade,  shade,  texX,   texY    );
				addVertices(bindings,   x + 1,  y+.5f,      z + .5f,  shade,  shade,  shade,  texX,   texYp   );
				addVertices(bindings,   x + 1,  y+.5f,      z,      shade,  shade,  shade,  texXp,  texYp   );
				addVertices(bindings,   x + 1,  y + 1,  z,      shade,  shade,  shade,  texXp,  texY    );

			}
		}
		texYp = texY + (Block.TEXTURE_WIDTH*top);

		addVertices(bindings,   x + 1,  y + top,  z + 1,  shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x + 1,  y,      z + 1,  shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x + 1,  y,      z,      shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x + 1,  y + top,  z,      shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private void renderBottomFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.BOTTOM, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(4);

		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;

		addVertices(bindings,   x + 1,  y,  z,      shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x + 1,  y,  z + 1,  shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x,      y,  z + 1,  shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x,      y,  z,      shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private void renderTopFace(MixedBindings bindings, IBlockAccess blockAccess, Chunk chunk, Block block, int x, int y, int z) {
		Vector2i texture = block.getTexture().get(Direction.TOP, blockAccess, x, y, z);
		float texX = texture.x * Block.TEXTURE_WIDTH;
		float texY = texture.y * Block.TEXTURE_WIDTH;
		float texXp = texX + Block.TEXTURE_WIDTH;
		float texYp = texY + Block.TEXTURE_WIDTH;
		byte shade = getShade(0);

		x -= chunk.x << Chunk.SIZE_POW2;
		y -= chunk.y << Chunk.SIZE_POW2;
		z -= chunk.z << Chunk.SIZE_POW2;
		float top = 1.f;
		if (block instanceof BlockSlab||block instanceof BlockStair){top=.5f;
		if (block instanceof BlockStair){
			addVertices(bindings,   x + 1,  y + 1,  z + .5f,  shade,  shade,  shade,  texX,   texY    );
			addVertices(bindings,   x + 1,  y + 1,  z,      shade,  shade,  shade,  texX,   texYp   );
			addVertices(bindings,   x,      y + 1,  z,      shade,  shade,  shade,  texXp,  texYp   );
			addVertices(bindings,   x,      y + 1,  z + .5f,  shade,  shade,  shade,  texXp,  texY    );

		}
		}

		addVertices(bindings,   x + 1,  y + top,  z + 1,  shade,  shade,  shade,  texX,   texY    );
		addVertices(bindings,   x + 1,  y + top,  z,      shade,  shade,  shade,  texX,   texYp   );
		addVertices(bindings,   x,      y + top,  z,      shade,  shade,  shade,  texXp,  texYp   );
		addVertices(bindings,   x,      y + top,  z + 1,  shade,  shade,  shade,  texXp,  texY    );
		bindings.addIndices(0, 1, 2, 2, 3, 0);
	}

	private static void addVertices(MixedBindings bindings, float x, float y, float z, byte r, byte g, byte b, float u, float v) {
		bindings.put(x).put(y).put(z).put(r).put(g).put(b).put(u).put(v);
	}
}
