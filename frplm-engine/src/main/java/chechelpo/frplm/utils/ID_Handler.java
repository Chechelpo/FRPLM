package chechelpo.frplm.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

/**
 * Utility class for handling IDs. Uses a backing array. Contains a mapping ID -> object
 */
public class ID_Handler<T extends Identifiable> {
    private int pointer = 0;
    private final Class<T> clazz;
    private T[] objects;
    private final IntStack freeIDs = new IntArrayList();

    public ID_Handler(Class<T> type, int initialCapacity) {
        this.clazz = type;
        this.objects = (T[]) Array.newInstance(type, initialCapacity);
    }
    public synchronized int registerObject(T object) {
        return registerObject(object, getNextFreeID());
    }

    public int registerObject(T object, int ID) {
        if (object == null) return -1;

        if (object.getID() == ID) {
            if (isValidIndex(ID) && objects[ID] == null) objects[ID] = object;
            else throw new IndexOutOfBoundsException("ID out of bounds or overwriting existing entity");

            return ID;
        } else throw new IllegalStateException("Object ID and input don't match");
    }

    public T[] getObjects() {
        return objects;
    }

    public T get(int ID) {
        if (isValidIndex(ID)) return objects[ID];
        else throw new IndexOutOfBoundsException("Tried to access array at index " + ID);
    }

    public int getNextFreeID() {
        int ID;
        if (freeIDs.isEmpty()){
            ID = pointer;
            if(pointer == objects.length) objects = Arrays.copyOf(objects, objects.length * 2);
            pointer++;
        }else ID = freeIDs.popInt();

        return ID;
    }
    public void cancelNextFreeID(int ID) {
        if ((ID - 1) == pointer){
            pointer --;
        }else{
            freeIDs.push(ID);
        }
    }
    public void advancePast(int ID){
        if (ID >= pointer){
            pointer = ID + 1;
            if (pointer >= objects.length) objects = Arrays.copyOf(objects, objects.length * 2);
        }
    }

    public void remove(int ID) {
        if (!isValidIndex(ID)) throw new IndexOutOfBoundsException("Tried to access array at index " + ID);

        freeIDs.push(ID);
        objects[ID] = null;
    }

    public boolean containsID(int ID) {
        return isValidIndex(ID) && objects[ID] != null;
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < objects.length;
    }

    public void putAll(@Nullable Map<Integer, T> locations) {
        if (locations == null) return;
        for(Map.Entry<Integer, T> entry : locations.entrySet()){
            advancePast(entry.getKey());
            locations.put(entry.getKey(), entry.getValue());
        }
    }
}
