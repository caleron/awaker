package com.awaker.control;

import com.awaker.global.Command;

enum ControlCommand implements Command {
    //Serverkontrolle
    SHUTDOWN_SERVER("shutdownServer"),
    SHUTDOWN_RASPI("shutdownRaspi"),
    REBOOT_RASPI("rebootRaspi"),
    REBOOT_SERVER("rebootServer");

    private final String action;

    ControlCommand(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
