package com.yl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.state.javadsl.CommandHandler;
import akka.persistence.typed.state.javadsl.DurableStateBehavior;

import java.time.Duration;

/**
 * https://doc.akka.io/docs/akka/current/typed/durable-state/persistence.html#accessing-the-actorcontext
 */

public class MyPersistentBehavior
        extends DurableStateBehavior<MyPersistentBehavior.Command, MyPersistentBehavior.State> {


    @Override
    public State emptyState() {
        return null;
    }

    @Override
    public CommandHandler<Command, State> commandHandler() {
        return (state, command) -> {
            context.getLog().info("In command handler");
            return Effect().none();
        };
    }

    // -------------- command -------------- //
    interface Command<ReplyMessage> {}

    public enum Increment implements MyPersistentCounter.Command<Void> {
        INSTANCE
    }

    public static class IncrementBy implements MyPersistentCounter.Command<Void> {
        public final int value;

        public IncrementBy(int value) {
            this.value = value;
        }
    }

    public static class GetValue implements MyPersistentCounter.Command<MyPersistentCounter.State> {
        private final ActorRef<Integer> replyTo;

        public GetValue(ActorRef<Integer> replyTo) {
            this.replyTo = replyTo;
        }
    }


    public static class State {
        private final int value;

        public State(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }

    public static Behavior<Command> create(PersistenceId persistenceId) {
        return Behaviors.setup(ctx -> new MyPersistentBehavior(persistenceId, ctx));
    }

    // this makes the context available to the command handler etc.
    private final ActorContext<Command> context;

    // optionally if you only need `ActorContext.getSelf()`
    private final ActorRef<Command> self;

    public MyPersistentBehavior(PersistenceId persistenceId, ActorContext<Command> ctx) {
        super(
                persistenceId,
                SupervisorStrategy.restartWithBackoff(
                        Duration.ofSeconds(10), Duration.ofSeconds(30), 0.2));
        this.context = ctx;
        this.self = ctx.getSelf();
    }

}