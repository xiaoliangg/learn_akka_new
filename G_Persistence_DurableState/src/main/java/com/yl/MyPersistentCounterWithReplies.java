package com.yl;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.state.javadsl.CommandHandler;
import akka.persistence.typed.state.javadsl.DurableStateBehavior;

/**
 * https://doc.akka.io/docs/akka/current/typed/durable-state/persistence.html#effects-and-side-effects
 */
public class MyPersistentCounterWithReplies
        extends DurableStateBehavior<MyPersistentCounterWithReplies.Command<?>, MyPersistentCounterWithReplies.State> {

    interface Command<ReplyMessage> {}

    public static class IncrementWithConfirmation implements Command<Void> {
        public final ActorRef<Done> replyTo;

        public IncrementWithConfirmation(ActorRef<Done> replyTo) {
            this.replyTo = replyTo;
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
        return new MyPersistentCounterWithReplies(persistenceId);
    }

    private MyPersistentCounterWithReplies(PersistenceId persistenceId) {
        super(persistenceId);
    }

    @Override
    public State emptyState() {
        return new State(0);
    }

    @Override
    public CommandHandler<Command<?>, State> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(
                        IncrementWithConfirmation.class,
                        (state, command) ->
                                Effect()
                                        .persist(new State(state.get() + 1))
                                        .thenReply(command.replyTo, (st) -> Done.getInstance()))
                .onCommand(
                        GetValue.class, (state, command) -> Effect().reply(command.replyTo, state.get()))
                .build();
    }

    // Example factoring out a chained effect to use in several places with `thenRun`
    // 复用thenRun的方法
//    static final Procedure<ExampleState> commonChainedEffect =
//            state -> System.out.println("Command handled!");
//    @Override
//    public CommandHandler<MyCommand, MyEvent, ExampleState> commandHandler() {
//        return newCommandHandlerBuilder()
//                .forStateType(ExampleState.class)
//                .onCommand(
//                        Cmd.class,
//                        (state, cmd) ->
//                                Effect()
//                                        .persist(new Evt(cmd.data))
//                                        .thenRun(() -> cmd.replyTo.tell(new Ack()))
//                                        .thenRun(commonChainedEffect))
//                .build();
//    }
}