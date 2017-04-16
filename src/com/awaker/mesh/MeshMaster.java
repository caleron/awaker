package com.awaker.mesh;

import com.awaker.Awaker;
import com.awaker.util.Log;
import com.sun.jna.IntegerType;
import com.sun.jna.Memory;

public class MeshMaster {
    private static Thread meshThread;


    public static void start() {
        if (Awaker.isMSWindows) {
            return;
        }

        if (meshThread != null && meshThread.isAlive()) {
            meshThread.interrupt();
        }
        meshThread = new Thread(MeshMaster::runMesh);
        meshThread.start();
    }

    private static void runMesh() {
        MeshLibrary library = MeshLibrary.INSTANCE;
        library.init(new UnsignedByte(97));

        Log.message("MeshMaster initialized");

        while (!meshThread.isInterrupted()) {
            library.updateAndDhcp();

            UnsignedShort maxSize = new UnsignedShort(32);
            MyMemory type = new MyMemory(2);
            MyMemory sender = new MyMemory(2);
            MyMemory returnArray = new MyMemory(maxSize.intValue());

            while (library.available()) {
                short readBytes = library.readNext(type, sender, returnArray, (short) 32);

                handleMessage(type.getShort(0), sender.getShort(0), readBytes, returnArray);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleMessage(int type, int senderNodeId, int readBytes, MyMemory data) {
        senderNodeId = 10; //TODO irgendwie bekomme ich immer nur -1 für sender, fixen
        MeshNode sender = MeshNode.getNodeForId(senderNodeId);
        if (sender == null) {
            Log.message("unknown node " + senderNodeId);
            return;
        }

        switch (type) {
            case MessageType.ANALOG_CHANGE:
                int index = data.getUnsignedByte(0);
                int value = data.getUnsignedByte(1);

                sender.handleAnalogChange(index, value);
                break;

            case MessageType.BUTTON_PUSHED:
                index = data.getUnsignedByte(0);
                sender.handleButtonPressed(index);
                break;
            default:
                Log.message("received unknown message type " + type + " from node " + senderNodeId);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "serial"})
    public static class UnsignedInt extends IntegerType {

        public UnsignedInt(int value) {
            super(4, value, true);
        }

        public UnsignedInt() {
            super(4, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "serial"})
    public static class UnsignedShort extends IntegerType {

        public UnsignedShort(int value) {
            super(2, value, true);
        }

        public UnsignedShort() {
            super(2, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "serial"})
    public static class UnsignedByte extends IntegerType {

        public UnsignedByte(int value) {
            super(1, value, true);
        }

        public UnsignedByte() {
            super(1, true);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class MyMemory extends Memory {
        public MyMemory(long size) {
            super(size);
        }

        public int getUnsignedShort(int offset) {
            return getShort(offset) & 0xffff;
        }

        public int getUnsignedByte(int offset) {
            return (getByte(offset) & 0xff);
        }
    }

}
