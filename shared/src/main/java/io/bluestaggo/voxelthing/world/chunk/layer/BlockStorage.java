package io.bluestaggo.voxelthing.world.chunk.layer;

import io.bluestaggo.pds.CompoundItem;
import io.bluestaggo.pds.ListItem;
import io.bluestaggo.pds.StructureItem;
import io.bluestaggo.voxelthing.Identifier;
import io.bluestaggo.voxelthing.util.IntList;
import io.bluestaggo.voxelthing.world.block.Block;
import io.bluestaggo.voxelthing.world.chunk.Chunk;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockStorage {
	public final List<Block> palette;
	private final List<Block> mutablePalette;
	private final BlockLayer[] layers;
	private final IntList blockCounts = new IntList();
	private final List<IntList> blockCountsPerLayer = new ArrayList<>();





	public BlockStorage() {
		this(new ArrayList<>());
	}

	public BlockStorage(List<Block> palette) {
		this(new BlockLayer[Chunk.LENGTH], palette);
	}

	public BlockStorage(BlockLayer[] layers, List<Block> palette) {
		this.layers = layers;
		for (int i = 0; i < layers.length; i++) {
			blockCountsPerLayer.add(new IntList());
			if (layers[i] == null) {
				layers[i] = new EmptyBlockLayer();
			}
		}

		mutablePalette = palette;
		this.palette = Collections.unmodifiableList(mutablePalette);

		if (palette.isEmpty()) {
			palette.add(null);
		}

		for (int i = 0; i < palette.size(); i++) {
			blockCounts.set(i, 0);
		}

		updateBlockCounts();
	}

	public Block getBlock(int x, int y, int z) {
		return palette.get(layers[y].getBlockId(x, z));
	}

	public void setBlock(int x, int y, int z, Block block) {
		BlockLayer layer = layers[y];
		int index = mutablePalette.indexOf(block);
		if (index == -1) {
			index = mutablePalette.lastIndexOf(null);
			if (index <= 0) {
				index = mutablePalette.size();
				mutablePalette.add(block);
				blockCounts.add(0);
			} else {
				mutablePalette.set(index, block);
				blockCounts.set(index, 0);
			}
		}

		while (layer.needsExpansion(index)) {
			layer = layers[y] = layer.expand();
		}

		int oldId = layer.getBlockId(x, z);
		if (index != oldId) {
			IntList layerBlockCounts = blockCountsPerLayer.get(y);
			layer.setBlockId(x, z, index);
			blockCounts.set(index, blockCounts.get(index) + 1);
			if (layerBlockCounts.size() <= index) {
				layerBlockCounts.set(index, 0);
			}
			layerBlockCounts.set(index, layerBlockCounts.get(index) + 1);

			if (oldId > 0) {
				blockCounts.set(oldId, blockCounts.get(oldId) - 1);
				layerBlockCounts.set(oldId, layerBlockCounts.get(oldId) - 1);
				if (blockCounts.get(oldId) <= 0) {
					mutablePalette.set(oldId, null);
				}

				boolean emptyLayer = true;
				for (int i = 0; i < layerBlockCounts.size(); i++) {
					int count = layerBlockCounts.get(i);
					if (count > 0) {
						emptyLayer = false;
					}

					if (count >= Chunk.AREA) {
						layers[y] = new SingleBlockLayer(i);
						break;
					}
				}

				if (emptyLayer) {
					layerBlockCounts.clear();
					layers[y] = new EmptyBlockLayer();
				}
			}
		}
	}

	protected void updateBlockCounts() {
		for (int i = 0; i < palette.size(); i++) {
			blockCounts.set(i, 0);
		}

		int y = 0;
		for (BlockLayer layer : layers) {
			IntList layerBlockCounts = blockCountsPerLayer.get(y);
			for (int x = 0; x < Chunk.LENGTH; x++) {
				for (int z = 0; z < Chunk.LENGTH; z++) {
					int id = layer.getBlockId(x, z);
					if (id > 0) {
						if (layerBlockCounts.size() <= id) {
							layerBlockCounts.set(id, 0);
						}

						blockCounts.set(id, blockCounts.get(id) + 1);
						layerBlockCounts.set(id, layerBlockCounts.get(id) + 1);
					}
				}
			}
			y++;
		}
	}

	public boolean isEmpty() {
		for (int i = 1; i < blockCounts.size(); i++) {
			if (blockCounts.get(i) != 0) {
				return false;
			}
		}

		return true;
	}

	public StructureItem serialize() {
		var item = new CompoundItem();
		var paletteItem = new ListItem(palette.stream()
				.map(b -> (b == null ? Block.ID_AIR : b.id).serialize())
				.collect(Collectors.toList()));
		item.setItem("palette", paletteItem);

		var layersItem = new ListItem();
		for (BlockLayer layer : layers) {
			var layerItem = new CompoundItem();
			layerItem.setByte("layerType", layer.getType());
			layerItem.setByteArray("data", layer.getRawData());
			layersItem.list.add(layerItem);
		}
		item.setItem("layers", layersItem);

		return item;
	}

	public static BlockStorage deserialize(StructureItem item) {
		if (!(item instanceof CompoundItem compoundItem)) {
			return new BlockStorage();
		}

		BlockLayer[] layers = new BlockLayer[Chunk.LENGTH];
		Arrays.fill(layers, new EmptyBlockLayer());

		List<StructureItem> layerList = compoundItem.getItem("layers").getList();
		for (int i = 0; i < Math.min(layerList.size(), Chunk.LENGTH); i++) {
			StructureItem layerItem = layerList.get(i);
			if (layerItem instanceof CompoundItem layerCompound) {
				try {
					layers[i] = BlockLayer.decode(layerCompound.getByte("layerType"), layerCompound.getByteArray("data"));
				} catch (UnsupportedOperationException ignored) {
				}
			}
		}

		var paletteItem = compoundItem.getItem("palette");
		List<Block> palette = paletteItem.getList().stream()
				.map(Identifier::deserialize)
				.map(Block::fromId)
				.collect(Collectors.toCollection(ArrayList::new));

		return new BlockStorage(layers, palette);
	}
}
