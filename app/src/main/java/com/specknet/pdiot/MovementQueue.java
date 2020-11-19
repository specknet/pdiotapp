package com.specknet.pdiot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MovementQueue {

    private Queue<MovePoint> moves;
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
            moves.remove();

    }

}
