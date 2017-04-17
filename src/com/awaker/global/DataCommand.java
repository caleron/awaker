package com.awaker.global;

import com.awaker.global.router.Command;

public enum DataCommand implements Command {

    //datenaustausch
    GET_STATUS("getStatus"),
    GET_LIBRARY("getLibrary"),
    SEND_STRING("sendString");

    private final String action;

    DataCommand(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
