package com.yl;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.state.javadsl.CommandHandler;
import akka.persistence.typed.state.javadsl.DurableStateBehavior;

/**
 * https://doc.akka.io/docs/akka/current/typed/durable-state/persistence.html#example-and-core-api
 */
public class MyPersistentCounter
        extends DurableStateBehavior<MyPersistentCounter.Command<?>, MyPersistentCounter.State> {

    // -------------- command -------------- //
    interface Command<ReplyMessage> {}

    public enum Increment implements Command<Void> {
        INSTANCE
    }

    public static class IncrementBy implements Command<Void> {
        public final int value;

        public IncrementBy(int value) {
            this.value = value;
        }
    }

    public static class GetValue implements Command<State> {
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

    public static Behavior<Command<?>> create(PersistenceId persistenceId) {
        return new MyPersistentCounter(persistenceId);
    }

    private MyPersistentCounter(PersistenceId persistenceId) {
        super(persistenceId);
    }

    @Override
    public State emptyState() {
        return new State(0);
    }

//    @Override
//    public CommandHandler<Command<?>, State> commandHandler() {
//        return (state, command) -> {
//            throw new RuntimeException("TODO: process the command & return an Effect");
//        };
//    }

    @Override
    public CommandHandler<Command<?>, State> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(
                        Increment.class, (state, command) -> Effect().persist(new State(state.get() + 1)))
                .onCommand(
                        IncrementBy.class,
                        (state, command) -> Effect().persist(new State(state.get() + command.value)))
                .onCommand(
                        GetValue.class, (state, command) -> Effect().reply(command.replyTo, state.get()))
                .build();
    }

}