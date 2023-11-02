package io.bluestaggo.voxelthing.world.block;

import io.bluestaggo.voxelthing.Identifier;
import io.bluestaggo.voxelthing.math.AABB;

public class BlockStair extends Block {

    public BlockStair(String id) {
        super(id);
    }

    public BlockStair(String namespace, String name) {
        super(namespace, name);
    }

    public BlockStair(Identifier id) {
        super(id);
    }

    public AABB getCollisionBox(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + .5f, z + 1);
    }

    public AABB getCollisionBoxSecondary(int x, int y, int z) {
        return new AABB(x+.0f, y, z, x + 1.f, y + 1.f, z + .5f);

    }
}