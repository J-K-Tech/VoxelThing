package io.bluestaggo.voxelthing.world;

import io.bluestaggo.pds.CompoundItem;
import io.bluestaggo.pds.IntArrayItem;
import io.bluestaggo.voxelthing.math.AABB;
import io.bluestaggo.voxelthing.math.AABBCCi;
import io.bluestaggo.voxelthing.math.MathUtil;
import io.bluestaggo.voxelthing.util.IntList;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.block.BlockStair;
import io.bluestaggo.voxelthing.world.chunk.Chunk;
import io.bluestaggo.voxelthing.world.generation.GenCache;
import io.bluestaggo.voxelthing.world.generation.GenerationInfo;
import io.bluestaggo.voxelthing.world.storage.ChunkStorage;
import io.bluestaggo.voxelthing.world.storage.EmptySaveHandler;
import io.bluestaggo.voxelthing.world.storage.ISaveHandler;
import org.joml.Matrix3x2d;
import org.joml.Matrix3x2dc;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

public class World implements IBlockAccess {
	protected final ChunkStorage chunkStorage;
	private final GenCache genCache;
	public final ISaveHandler saveHandler;

	public final Random random = new Random();
	public final WorldInfo info;

	public Dictionary<Vector3i,List<Vector3i>> blocksWannaRender;

	public double partialTick;
	public final List<AABBCCi> tickingBlocks=new ArrayList<>();
	private int ticking=0;

	public World() {
		this(null, null);
	}

	public World(ISaveHandler saveHandler) {
		this(saveHandler, null);
	}

	public World(ISaveHandler saveHandler, WorldInfo info) {
		if (info == null) {
			this.info = new WorldInfo();
			CompoundItem data = saveHandler.loadData("world");
			if (data != null) {
				this.info.deserialize(data);
			}
		} else {
			this.info = info;
		}

		if (saveHandler == null) {
			saveHandler = new EmptySaveHandler();
		}

		chunkStorage = new ChunkStorage(this);
		genCache = new GenCache(this);
		this.saveHandler = saveHandler;
	}

	public int getTicking(){
		return this.ticking;
	}

	public void tick(){
		for (int i=0;i<this.tickingBlocks.size();i++){
			int[] blockPoss=this.tickingBlocks.get(i).asArray();
			Vector3i chp=new Vector3i(blockPoss[0],blockPoss[1],blockPoss[2]);
			Vector3i blp=new Vector3i(blockPoss[3],blockPoss[4],blockPoss[5]);
			if (chunkStorage.getRealChunkAt(chp.x,chp.y,chp.z)!=null){
				if(chunkStorage.getRealChunkAt(chp.x,chp.y,chp.z).getBlock(blp.x,blp.y,blp.z)==null){
				System.out.println(blp.x+" "+blp.y+" "+blp.z);
				System.out.println(chp.x+" "+chp.y+" "+chp.z);
				this.tickingBlocks.remove(i);
			}

				chunkStorage.getRealChunkAt(chp.x,chp.y,chp.z)
						.getBlock(blp.x,blp.y,blp.z).tick();
				if (chunkStorage.getRealChunkAt(chp.x,chp.y,chp.z)
						.getBlock(blp.x,blp.y,blp.z).renderAtTick){
					if (this.blocksWannaRender.get(chp)==null){
						List<Vector3i> blocks=new ArrayList<>();
						blocks.add(blp);
						this.blocksWannaRender.put(chp, blocks);
					} else {
						this.blocksWannaRender.get(chp).add(blp);

					}
				}
			}
			else {this.tickingBlocks.remove(i);}
		}
	}

	public Chunk getChunkAt(int x, int y, int z) {
		return chunkStorage.getChunkAt(x, y, z);
	}

	public boolean chunkExists(int x, int y, int z) {
		return getChunkAt(x, y, z) != null;
	}

	public Chunk getChunkAtBlock(int x, int y, int z) {
		return getChunkAt(
				Math.floorDiv(x, Chunk.LENGTH),
				Math.floorDiv(y, Chunk.LENGTH),
				Math.floorDiv(z, Chunk.LENGTH)
		);
	}

	public boolean chunkExistsAtBlock(int x, int y, int z) {
		return getChunkAtBlock(x, y, z) != null;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		Chunk chunk = getChunkAtBlock(x, y, z);
		if (chunk == null) {
			return null;
		}
		return chunk.getBlock(
				Math.floorMod(x, Chunk.LENGTH),
				Math.floorMod(y, Chunk.LENGTH),
				Math.floorMod(z, Chunk.LENGTH)
		);
	}

	public void setBlock(int x, int y, int z, Block block) {
		Chunk chunk = getChunkAtBlock(x, y, z);
		if (chunk == null) {
			return;
		}
		Vector3i bl=new Vector3i(
				Math.floorMod(x, Chunk.LENGTH),
				Math.floorMod(y, Chunk.LENGTH),
				Math.floorMod(z, Chunk.LENGTH));

		boolean doesTick=false;
		int[] blockpos={chunk.x,chunk.y,chunk.z,bl.x,bl.y,bl.z};
		if (block!=null){doesTick=block.doesTick;}

		int index=0;

		if(this.tickingBlocks.size()!=0){index=-1;
			for (int i=0;i<this.tickingBlocks.size();i++){
				index++;
				if (Arrays.equals(blockpos,this.tickingBlocks.get(i).asArray())){
					System.out.println(this.tickingBlocks.get(index));
					System.out.println(index);
					break;
				}
			}
		}


		boolean[] shouldchange= new boolean[]{doesTick, this.tickingBlocks.size()!=0?this.tickingBlocks.get(index)!=null:false};

		ArrayList<Vector3i> a= new ArrayList<>();


		if (block==null){
			System.out.println(bl.x+" "+bl.y+" "+bl.z);
			System.out.println(chunk.x+" "+chunk.y+" "+chunk.z);
			System.out.println(shouldchange[0]+" "+shouldchange[1]);
		}


		if (shouldchange[0]!=shouldchange[1]){
			if (shouldchange[0]){
				this.tickingBlocks.add(new AABBCCi(blockpos));
			}
		}
		chunk.setBlock(
				bl.x,bl.y,bl.z,
				block
		);
		System.out.println(this.tickingBlocks.size());
		ticking=tickingBlocks.size();
		onBlockUpdate(x, y, z);
	}

	public void loadChunkAt(int cx, int cy, int cz) {
		if (chunkStorage.getChunkAt(cx, cy, cz) != null) {
			return;
		}

		CompoundItem data = saveHandler.loadChunkData(cx, cy, cz);
		if (data != null) {
			chunkStorage.deserializeChunkAt(cx, cy, cz, data);
			return;
		}

		Chunk chunk = chunkStorage.newChunkAt(cx, cy, cz);
		GenerationInfo genInfo = genCache.getGenerationAt(cx, cz);

		genInfo.generate();

		for (int x = 0; x < Chunk.LENGTH; x++) {
			for (int z = 0; z < Chunk.LENGTH; z++) {
				float height = genInfo.getHeight(x, z);

				for (int y = 0; y < Chunk.LENGTH; y++) {
					int yy = cy * Chunk.LENGTH + y;
					boolean cave = yy < height && genInfo.getCave(x, yy, z) && false;
					Block block = null;

					if (!cave) {
						if (yy < height - 4) {
							block = Block.STONE;
						} else if (yy < height - 1) {
							block = Block.DIRT;
						} else if (yy < height) {
							block = Block.GRASS;
						}
					}

					if (block != null) {
						chunk.setBlock(x, y, z, block);
						if (block.doesTick){
							this.tickingBlocks.add(new AABBCCi( cx, cy, cz,x, y, z));
							ticking++;
						}

					}
				}
			}
		}

		chunk.dontSave();
		onChunkAdded(cx, cy, cz);
	}

	public synchronized Chunk getOrLoadChunkAt(int x, int y, int z) {
		Chunk chunk = getChunkAt(x, y, z);
		if (chunk == null) {
			loadChunkAt(x, y, z);
			chunk = getChunkAt(x, y, z);
		}
		return chunk;
	}

	public List<AABB> getSurroundingCollision(AABB box) {
		List<AABB> boxes = new ArrayList<>();

		int minX = (int) Math.floor(box.minX);
		int minY = (int) Math.floor(box.minY);
		int minZ = (int) Math.floor(box.minZ);
		int maxX = (int) Math.floor(box.maxX + 1.0);
		int maxY = (int) Math.floor(box.maxY + 1.0);
		int maxZ = (int) Math.floor(box.maxZ + 1.0);

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					Block block = getBlock(x, y, z);
					if (block != null) {
						if (block instanceof BlockStair){
							System.out.println(block.getClass());
							boxes.add(((BlockStair) block).getCollisionBoxSecondary(x, y, z));
						}
						boxes.add(block.getCollisionBox(x, y, z));
					}
				}
			}
		}

		return boxes;
	}

	public boolean doRaycast(BlockRaycast raycast) {
		Vector3d pos = new Vector3d(raycast.position);
		Vector3d dir = raycast.direction;
		float dist = 0.0f;

		while (dist < raycast.length) {
			int x = (int) Math.floor(pos.x());
			int y = (int) Math.floor(pos.y());
			int z = (int) Math.floor(pos.z());

			Block block = getBlock(x, y, z);
			if (block != null) {
				AABB collision = block.getCollisionBox(x, y, z);
				if (collision.contains(pos.x, pos.y, pos.z)) {
					raycast.setResult(x, y, z, collision.getClosestFace(pos, dir));
					return true;
				}
				if (block instanceof BlockStair){
					collision = ((BlockStair) block).getCollisionBoxSecondary(x, y, z);
					if (collision.contains(pos.x, pos.y, pos.z)) {
						raycast.setResult(x, y, z, collision.getClosestFace(pos, dir));
						return true;
					}

				}
			}

			pos.add(dir);
			dist += BlockRaycast.STEP_DISTANCE;
		}

		return false;
	}

	public double scaleToTick(double a, double b) {
		return MathUtil.lerp(a, b, partialTick);
	}

	public void onBlockUpdate(int x, int y, int z) {
	}

	public void onChunkAdded(int x, int y, int z) {
	}

	public void close() {
		saveHandler.saveData("world", info.serialize());
		chunkStorage.unloadAllChunks();
	}
}
