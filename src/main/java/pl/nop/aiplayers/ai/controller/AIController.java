package pl.nop.aiplayers.ai.controller;

import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.concurrent.CompletableFuture;

public interface AIController {
    CompletableFuture<Action> decide(AIPlayerSession session, Perception perception);
}
