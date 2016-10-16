package com.awaker.global;

import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;

public interface CommandHandler {
    Answer handleCommand(Command command, CommandData data);
}
