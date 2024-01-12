package io.bluestaggo.voxelthing.world;

import io.bluestaggo.pds.CompoundItem;

import java.util.Random;

public class WorldInfo {
	public String name = "world";
	public long seed = new Random().nextLong();

	public Integer gamemode=1;

	public void deserialize(CompoundItem data) {
		name = data.getString("name");
		seed = data.getLong("seed");
		gamemode =data.getInteger("gamemode");
		if (gamemode==null)gamemode=1;
	}

	public CompoundItem serialize() {
		var data = new CompoundItem();
		data.setString("name", name);
		data.setLong("seed", seed);
		return data;
	}
}
