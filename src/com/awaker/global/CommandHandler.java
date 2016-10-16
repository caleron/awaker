package com.awaker.global;

import com.awaker.server.json.Answer;
import com.awaker.server.json.JsonCommand;

public interface CommandHandler {
    Answer handleCommand(Command command, JsonCommand data);
}
