package com.awaker.config;

import com.awaker.global.Command;

public enum ConfigCommand implements Command {
    //Config
    GET_CONFIG("getConfig"),
    SET_CONFIG("setConfig"),
    GET_CONFIG_LIST("getConfigList"),
    GET_CONFIG_OPTIONS("getConfigOptions");

    private final String action;

    ConfigCommand(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
