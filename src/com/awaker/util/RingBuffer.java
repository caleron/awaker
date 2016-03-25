package com.awaker.util;

/**
 * Stellt eine zirkularen Buffer mit maximaler Größe dar.
 *
 * @param <E> Elementtyp der Queue
 */
public class RingBuffer<E> {

    private E[] arr;
    private int capacity;

    private int elementCount = 0;
    private int writePos = 0;


    /**
     * Erstellt einen neuen Buffer.
     *
     * @param size Die Größe des Buffers
     */
    public RingBuffer(int size) {
        capacity = size;
        arr = (E[]) new Object[size];
    }

    /**
     * Fügt ein Element zur Queue hinzu
     *
     * @param obj Das neue Element
     */
    public void put(E obj) {
        if (writePos >= capacity) {
            writePos = 0;
        }
        arr[writePos] = obj;
        writePos++;

        if (elementCount < capacity) {
            elementCount++;
        }
        System.out.println("put: " + obj);
    }

    /**
     * Entfernt das erste Element des Buffers und gibt es zurück
     *
     * @return das erste Element der Queue
     */
    public E take() {
        if (elementCount == 0) {
            return null;
        }
        int front = writePos - elementCount;
        if (front < 0) {
            front += capacity;
        }

        E element = arr[front];
        arr[front] = null;
        elementCount--;
        System.out.println("take: " + element);
        return element;
    }

    public void reset() {
        writePos = 0;
        elementCount = 0;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Gibt zurück, ob die Queue voll ist.
     *
     * @return True, wenn die Queue voll ist
     */
    public boolean isFull() {
        return capacity == elementCount;
    }

    /**
     * Gibt zurück, ob die Queue keine Elemente enthält
     *
     * @return True, wenn die Queue leer ist
     */
    public boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Gibt die Elementzahl zurück
     *
     * @return Anzahl der Elemente in der Queue
     */
    public int count() {
        return elementCount;
    }
}
