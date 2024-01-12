package io.bluestaggo.voxelthing.math;

public class AABBCCi {
    int x1;
    int x2;
    int y1;
    int y2;
    int z1;
    int z2;

    public AABBCCi(int x1, int x2, int y1, int y2, int z1, int z2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.z1 = z1;
        this.z2 = z2;
    }
    public AABBCCi(int[] ints) {

        this.x1 = ints[0];
        this.x2 = ints[1];
        this.y1 = ints[2];
        this.y2 = ints[3];
        this.z1 = ints[4];
        this.z2 = ints[5];
    }

    public AABBCCi() {
        this.x1 = 0;
        this.x2 = 0;
        this.y1 = 0;
        this.y2 = 0;
        this.z1 = 0;
        this.z2 = 0;
    }
    public int[] asArray(){
        return new int[]{this.x1,this.y1,this.z1,this.x2,this.y2,this.z2};
    }
}
