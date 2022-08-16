package com.yl.bubble;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DeathPactException;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * 源码地址:https://doc.akka.io/docs/akka/current/typed/fault-tolerance.html#bubble-failures-up-through-the-hierarchy
 */
public interface Protocol {
    public interface Command {
    }

    public static class Fail implements Command {
        public final String text;

        public Fail(String text) {
            this.text = text;
        }
    }

    public static class Hello implements Command {
        public final String text;
        public final ActorRef<String> replyTo;

        public Hello(String text, ActorRef<String> replyTo) {
            this.text = text;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return "Hello{" +
                    "text='" + text + '\'' +
                    ", replyTo=" + replyTo +
                    '}';
        }
    }

    public static class Worker extends AbstractBehavior<Protocol.Command> {

        public static Behavior<Protocol.Command> create() {
            return Behaviors.setup(Worker::new);
        }

        private Worker(ActorContext<Protocol.Command> context) {
            super(context);
        }

        @Override
        public Receive<Protocol.Command> createReceive() {
            return newReceiveBuilder()
                    .onMessage(Protocol.Fail.class, this::onFail)
                    .onMessage(Protocol.Hello.class, this::onHello)
                    .build();
        }

        private Behavior<Protocol.Command> onFail(Protocol.Fail message) {
            throw new RuntimeException(message.text);
        }

        private Behavior<Protocol.Command> onHello(Protocol.Hello message) {
            message.replyTo.tell(message.text);
            return this;
        }
    }

    public static class MiddleManagement extends AbstractBehavior<Protocol.Command> {

        public static Behavior<Protocol.Command> create() {
            return Behaviors.setup(MiddleManagement::new);
        }

        private final ActorRef<Protocol.Command> child;

        private MiddleManagement(ActorContext<Protocol.Command> context) {
            super(context);

            context.getLog().info("Middle management starting up");
            // default supervision of child, meaning that it will stop on failure
            child = context.spawn(Worker.create(), "child");

            // we want to know when the child terminates, but since we do not handle
            // the Terminated signal, we will in turn fail on child termination
            context.watch(child);
        }

        @Override
        public Receive<Protocol.Command> createReceive() {
            // here we don't handle Terminated at all which means that
            // when the child fails or stops gracefully this actor will
            // fail with a DeathPactException
            return newReceiveBuilder().onMessage(Protocol.Command.class, this::onCommand).build();
        }

        private Behavior<Protocol.Command> onCommand(Protocol.Command message) {
            // just pass messages on to the child
            child.tell(message);
            return this;
        }
    }

    public static class Boss extends AbstractBehavior<Protocol.Command> {

        public static Behavior<Protocol.Command> create() {
            return Behaviors.supervise(Behaviors.setup(Boss::new))
                    .onFailure(DeathPactException.class, SupervisorStrategy.restart());
        }

        private final ActorRef<Protocol.Command> middleManagement;

        private Boss(ActorContext<Protocol.Command> context) {
            super(context);
            context.getLog().info("Boss starting up");
            // default supervision of child, meaning that it will stop on failure
            middleManagement = context.spawn(MiddleManagement.create(), "middle-management");
            context.watch(middleManagement);
        }

        @Override
        public Receive<Protocol.Command> createReceive() {
            // here we don't handle Terminated at all which means that
            // when middle management fails with a DeathPactException
            // this actor will also fail
            return newReceiveBuilder().onMessage(Protocol.Command.class, this::onCommand).build();
        }

        private Behavior<Protocol.Command> onCommand(Protocol.Command message) {
            // just pass messages on to the child
            middleManagement.tell(message);
            return this;
        }
    }
}
