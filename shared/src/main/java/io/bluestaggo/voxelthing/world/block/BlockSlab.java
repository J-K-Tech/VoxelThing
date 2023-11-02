package io.bluestaggo.voxelthing.world.block;

import io.bluestaggo.voxelthing.Identifier;
import io.bluestaggo.voxelthing.math.AABB;

public class BlockSlab extends Block{
    public BlockSlab(String id) {
        super(new Identifier(id));
    }

    public BlockSlab(String namespace, String name) {
        super(new Identifier(namespace, name));
    }

    public AABB getCollisionBox(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + .5f, z + 1);
    }
}
