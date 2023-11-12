package io.bluestaggo.voxelthing.world.chunk;

import io.bluestaggo.pds.CompoundItem;
import io.bluestaggo.voxelthing.math.MathUtil;
import io.bluestaggo.voxelthing.util.IntList;
import io.bluestaggo.voxelthing.world.Direction;
import io.bluestaggo.voxelthing.world.IBlockAccess;
import io.bluestaggo.voxelthing.world.World;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.chunk.layer.BlockStorage;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.IntConsumer;

public class Chunk implements IBlockAccess {
	public static final int SIZE_POW2 = 5;
	public static final int LENGTH = 1 << SIZE_POW2;
	public static final int LENGTH_MASK = (1 << SIZE_POW2) - 1;
	public static final int AREA = 1 << SIZE_POW2 * 2;
	public static final int VOLUME = 1 << SIZE_POW2 * 3;

	public final World world;
	public final int x, y, z;

	private final BlockStorage blockStorage;
	private boolean hasChanged;
	private boolean needsCullingUpdate;

	private short cullInfo = Short.MAX_VALUE;

	public static final List<Vector3i[]>tickingBlocks=new ArrayList<>();


	public Chunk(World world, int x, int y, int z) {
		this(world, x, y, z, new BlockStorage());
	}

	public Chunk(World world, int x, int y, int z, BlockStorage blockStorage) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockStorage = blockStorage;
		needsCullingUpdate = !blockStorage.isEmpty();
	}

	public int toGlobalX(int x) {
		return x + (this.x << Chunk.SIZE_POW2);
	}

	public int toGlobalY(int y) {
		return y + (this.y << Chunk.SIZE_POW2);
	}

	public int toGlobalZ(int z) {
		return z + (this.z << Chunk.SIZE_POW2);
	}

	public List<Vector3i[]> getTicking(){
		return this.tickingBlocks;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		if (!containsLocal(x, y, z)) {
			return this.world.getBlock(toGlobalX(x), toGlobalY(y), toGlobalZ(z));
		}
		return blockStorage.getBlock(x, y, z);
	}

	public void setBlock(int x, int y, int z, Block block) {
		blockStorage.setBlock(x, y, z, block);
		Vector3i[] blockpos={new Vector3i(this.x,this.y,this.z),new Vector3i(x,y,z)};
		boolean[] shouldchange={block.doesTick,tickingBlocks.contains(blockpos)};

		if (shouldchange[0]!=shouldchange[1]){
			if (shouldchange[0])tickingBlocks.add(blockpos);
			else tickingBlocks.remove(blockpos);
		}
		hasChanged = true;
		needsCullingUpdate = true;
	}

	public boolean isEmpty() {
		return blockStorage.isEmpty();
	}

	public boolean contains(int x, int y, int z) {
		x -= this.x * Chunk.LENGTH;
		y -= this.y * Chunk.LENGTH;
		z -= this.z * Chunk.LENGTH;
		return containsLocal(x, y, z);
	}

	public boolean containsLocal(int x, int y, int z) {
		return x >= 0 && x < Chunk.LENGTH
				&& y >= 0 && y < Chunk.LENGTH
				&& z >= 0 && z < Chunk.LENGTH;
	}

	public void calculateCulling() {
		needsCullingUpdate = false;

		if (isEmpty()) {
			cullInfo = Short.MAX_VALUE;
			return;
		}

		cullInfo = 0;
		Set<IntList> posFillLocations = new HashSet<>();
		Set<IntList> negFillLocations = new HashSet<>();
		int posDirs = 0;
		int negDirs = 0;

		for (int y = 0; y < Chunk.LENGTH; y++) {
			for (int z = 0; z < Chunk.LENGTH; z++) {
				posDirs |= startFloodFill(posFillLocations, Chunk.LENGTH - 1, y, z, Direction.EAST);
				negDirs |= startFloodFill(negFillLocations, 0, y, z, Direction.WEST);
			}
		}
		processCullDirs(posDirs, Direction.EAST);
		processCullDirs(negDirs, Direction.WEST);

		posFillLocations.clear();
		negFillLocations.clear();
		posDirs = 0;
		negDirs = 0;
		for (int x = 0; x < Chunk.LENGTH; x++) {
			for (int z = 0; z < Chunk.LENGTH; z++) {
				posDirs |= startFloodFill(posFillLocations, x, Chunk.LENGTH - 1, z, Direction.TOP);
				negDirs |= startFloodFill(negFillLocations, x, 0, z, Direction.BOTTOM);
			}
		}
		processCullDirs(posDirs, Direction.TOP);
		processCullDirs(negDirs, Direction.BOTTOM);

		posFillLocations.clear();
		negFillLocations.clear();
		posDirs = 0;
		negDirs = 0;
		for (int x = 0; x < Chunk.LENGTH; x++) {
			for (int y = 0; y < Chunk.LENGTH; y++) {
				posDirs |= startFloodFill(posFillLocations, x, y, Chunk.LENGTH - 1, Direction.SOUTH);
				negDirs |= startFloodFill(negFillLocations, x, y, 0, Direction.NORTH);
			}
		}
		processCullDirs(posDirs, Direction.SOUTH);
		processCullDirs(negDirs, Direction.NORTH);
	}

	public boolean needsCullingUpdate() {
		return needsCullingUpdate;
	}

	private int startFloodFill(Set<IntList> fillLocations, int x, int y, int z, Direction startDir) {
		int packedCoord = MathUtil.pack8bit(x, Chunk.LENGTH - 1, z);
		for (IntList fillPoints : fillLocations) {
			if (fillPoints.contains(packedCoord)) {
				return 0;
			}
		}

		IntList floodPoints = new IntList();
		fillLocations.add(floodPoints);
		return floodFill(floodPoints, x, y, z, startDir);
	}

	private void processCullDirs(int dirs, Direction startDir) {
		for (Direction dir : Direction.ALL) {
			if (dir != startDir && (dirs & dir.bitMask) > 0) {
				cullInfo |= (short) Objects.requireNonNull(ChunkCullDirection.get(startDir, dir)).bitMask;
			}
		}
	}

	private int floodFill(IntList floodPoints, int x, int y, int z, Direction startDir) {
		Block block = getBlock(x, y, z);
		if (block != null && block.isSolidOpaque()) {
			floodPoints.add(MathUtil.pack8bit(x, y, z));
			return 0;
		}

		int dirs = 0;

		IntList floodQueue = new IntList();
		int packedCoord = MathUtil.pack8bit(x, y, z);
		floodPoints.add(packedCoord);
		floodQueue.add(packedCoord);

		IntConsumer addToQueue = (int i) -> {
			if (!floodPoints.contains(i)) {
				floodPoints.add(i);
				floodQueue.add(i);
			}
		};

		while (floodQueue.size() > 0) {
			int nextCoord = floodQueue.remove(0);
			int xx = MathUtil.unpack8bitX(nextCoord);
			int yy = MathUtil.unpack8bitY(nextCoord);
			int zz = MathUtil.unpack8bitZ(nextCoord);
			block = getBlock(xx, yy, zz);

			if (block != null && block.isSolidOpaque()) {
				continue;
			}

			if (xx < Chunk.LENGTH - 1) addToQueue.accept(MathUtil.pack8bit(xx + 1, yy, zz));
			if (xx > 0) addToQueue.accept(MathUtil.pack8bit(xx - 1, yy, zz));
			if (yy < Chunk.LENGTH - 1) addToQueue.accept(MathUtil.pack8bit(xx, yy + 1, zz));
			if (yy > 0) addToQueue.accept(MathUtil.pack8bit(xx, yy - 1, zz));
			if (zz < Chunk.LENGTH - 1) addToQueue.accept(MathUtil.pack8bit(xx, yy, zz + 1));
			if (zz > 0) addToQueue.accept(MathUtil.pack8bit(xx, yy, zz - 1));

			if (xx == Chunk.LENGTH - 1 && startDir != Direction.WEST) {
				dirs |= Direction.WEST.bitMask;
			} else if (xx == 0 && startDir != Direction.EAST) {
				dirs |= Direction.EAST.bitMask;
			}

			if (yy == Chunk.LENGTH - 1 && startDir != Direction.TOP) {
				dirs |= Direction.TOP.bitMask;
			} else if (yy == 0 && startDir != Direction.BOTTOM) {
				dirs |= Direction.BOTTOM.bitMask;
			}

			if (zz == Chunk.LENGTH - 1 && startDir != Direction.SOUTH) {
				dirs |= Direction.SOUTH.bitMask;
			} else if (zz == 0 && startDir != Direction.NORTH) {
				dirs |= Direction.NORTH.bitMask;
			}
		}

		return dirs;
	}

	public short getCullInfo() {
		return cullInfo;
	}

	public boolean canBeSeen(Direction faceA, Direction faceB) {
		ChunkCullDirection cullDirection = ChunkCullDirection.get(faceA, faceB);
		return cullDirection == null || (cullInfo & (1 << cullDirection.ordinal())) > 0;
	}

	public CompoundItem serialize() {
		var item = new CompoundItem();
		item.setItem("blocks", blockStorage.serialize());
		return item;
	}

	public static Chunk deserialize(World world, int x, int y, int z, CompoundItem item) {
		BlockStorage storage = BlockStorage.deserialize(item.getItem("blocks"));
		return new Chunk(world, x, y, z, storage);
	}

	public void dontSave() {
		hasChanged = false;
	}

	public void onUnload() {
		save();
	}

	public void save() {
		if (hasChanged) {
			world.saveHandler.saveChunkData(x, y, z, serialize());
		}
	}
}
