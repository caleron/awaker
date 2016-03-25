package com.awaker.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RingBufferTest {

    private RingBuffer<Integer> buffer;

    @Before
    public void setUp() throws Exception {
        buffer = new RingBuffer<>(5);
    }

    @Test
    public void put() throws Exception {
        assertEquals(buffer.count(), 0);
        assertEquals(buffer.getCapacity(), 5);

        buffer.put(10);
        buffer.put(3);
        buffer.put(6);
        assertEquals(buffer.count(), 3);

        assertEquals(buffer.take().toString(), "10");
        assertEquals(buffer.count(), 2);
        assertEquals(buffer.take().toString(), "3");
        assertEquals(buffer.take().toString(), "6");
        assertEquals(buffer.count(), 0);

        buffer.put(10);
        buffer.put(7);
        buffer.put(9);
        buffer.put(12223);
        buffer.put(34);
        buffer.put(14);
        buffer.put(0);
        assertTrue(buffer.isFull());

        assertEquals(buffer.count(), 5);
        assertEquals(buffer.take().toString(), "9");
        buffer.put(100);
        assertEquals(buffer.count(), 5);
        assertTrue(buffer.isFull());

        assertEquals(buffer.take().toString(), "12223");
        assertEquals(buffer.take().toString(), "34");
        assertEquals(buffer.take().toString(), "14");
        assertEquals(buffer.take().toString(), "0");
        assertEquals(buffer.take().toString(), "100");
        assertTrue(buffer.isEmpty());
    }
}