package com.specknet.pdiot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MovementQueue {

    private List<MovePoint> moves;
    private int windowsize;
    public MovementQueue(int windowsize)
    {
        this.windowsize=windowsize;
        moves=new LinkedList<MovePoint>();
    }
    public void AddMove(MovePoint move)
    {
        moves.add(move);
        if(moves.size()>windowsize)
            moves.remove(0);

    }
    public ByteBuffer ConvertDataToBuffer()
    {
        int cellsize = java.lang.Float.SIZE/ Byte.SIZE;
        ByteBuffer inputdata=ByteBuffer.allocateDirect(cellsize*3*windowsize).order(ByteOrder.nativeOrder());
        if(!isFull())
        {
            AddMove(new MovePoint(0,0,0));
            return ConvertDataToBuffer();
        }
        for(int i=0; i<windowsize;i++)
        {
            MovePoint nextMove=moves.get(i);
            inputdata.putFloat(i* cellsize,nextMove.x);
            inputdata.putFloat((i+windowsize)* cellsize,nextMove.y);
            inputdata.putFloat((i+windowsize*2)* cellsize,nextMove.z);
        }
        return inputdata;
    }
    public boolean isFull()
    {
        return moves.size()==windowsize;
    }

}
