package com.awaker.mesh;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Library-Interface für die awaker_master-Library (bzw. AwakerMesh).
 * Die Funktionsdeklarationen hier müssen exakt mit denen in der Library übereinstimmen.
 * In C++ müssen entsprechende Methoden dafür mit extern "C" deklariert werden.
 * Dabei beachten, dass in Java keine Unsigned-Integertypen vorhanden sind.
 */
public interface MeshLibrary extends Library {
    MeshLibrary INSTANCE = (MeshLibrary) Native.loadLibrary("awakermesh", MeshLibrary.class);

    void init(MeshMaster.UnsignedByte channel);

    void setChannel(MeshMaster.UnsignedByte channel);

    void updateAndDhcp();

    boolean available();

    short peekType();

    short readNext(Pointer type, Pointer sender, Pointer returnArray, short maxSize);
}
