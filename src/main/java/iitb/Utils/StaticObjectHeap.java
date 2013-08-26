/*
 * Created on May 21, 2005
 *
 */
package iitb.Utils;

import java.util.ArrayList;

/**
 * @author imran
 * @since 1.2
 * @version 1.3
 */
public abstract class StaticObjectHeap extends ObjectHeap {

    protected ArrayList<Object> objects;
    protected int curIndex = 0;
    
    public StaticObjectHeap(int initCapacity) {
        super(initCapacity);
        objects = new ArrayList<Object>();
        reset();
        for(int i = 0; i < initCapacity; i++)
            addObject();
        
    }
        
    public Object getFreeObject(){
        if(curIndex >=objects.size()){
            addObject();
            curIndex = objects.size() - 1;
        }        
        return objects.get(curIndex++);
    }
        
    
    protected boolean addObject(){
        return objects.add(newObject());
    }
    
    protected abstract Object newObject();
    protected abstract Object getObject();
    
    public void reset(){
        curIndex = 0;
    }
    
    public void clear(){
        objects.clear();
        reset();
    }
    
    public void setObjectHeap(int capacity){
        if(objects.size() < capacity)
            for(int i = capacity - objects.size(); i >=0; i--)
                addObject();
        else if(objects.size() > capacity)
            for(int i = objects.size() - capacity; i >= 0; i--)
                objects.remove(capacity + i);        
    }
}