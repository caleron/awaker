package com.awaker.global.router;

import com.awaker.server.json.Answer;
import com.awaker.server.json.CommandData;

public interface CommandHandler {
    Answer handleCommand(Command command, CommandData data, boolean buildAnswer);
}
