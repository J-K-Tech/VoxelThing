package io.bluestaggo.voxelthing.world.block;

import io.bluestaggo.voxelthing.Identifier;

public class BlockBillBoard extends Block{
    public BlockBillBoard(String id) {
        super(id);
    }

    public BlockBillBoard(String namespace, String name) {
        super(namespace, name);
    }

    public BlockBillBoard(Identifier id) {
        super(id);
    }
}
