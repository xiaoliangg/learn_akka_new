/*
 * Copyright (C) 2017-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package com.yl;

// #imports
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

// #imports

import akka.actor.typed.Terminated;
import akka.actor.typed.Props;
import akka.actor.typed.DispatcherSelector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// 文档: https://doc.akka.io/docs/akka/current/typed/actors.html
public interface IntroTest {

  // #hello-world-actor
  public class HelloWorld extends AbstractBehavior<HelloWorld.Greet> {

    public static final class Greet {
      public final String whom;
      public final ActorRef<Greeted> replyTo;

      public Greet(String whom, ActorRef<Greeted> replyTo) {
        this.whom = whom;
        this.replyTo = replyTo;
      }
    }

    public static final class Greeted {
      public final String whom;
      public final ActorRef<Greet> from;

      public Greeted(String whom, ActorRef<Greet> from) {
        this.whom = whom;
        this.from = from;
      }
    }

    public static Behavior<Greet> create() {
      return Behaviors.setup(HelloWorld::new);
    }

    private HelloWorld(ActorContext<Greet> context) {
      super(context);
    }

    @Override
    public Receive<Greet> createReceive() {
      return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
    }

    private Behavior<Greet> onGreet(Greet command) {
      getContext().getLog().info("Hello {}!", command.whom);
      command.replyTo.tell(new Greeted(command.whom, getContext().getSelf()));
      return this;
    }
  }
  // #hello-world-actor

  // #hello-world-bot
  public class HelloWorldBot extends AbstractBehavior<HelloWorld.Greeted> {

    public static Behavior<HelloWorld.Greeted> create(int max) {
      return Behaviors.setup(context -> new HelloWorldBot(context, max));
    }

    private final int max;
    private int greetingCounter;

    private HelloWorldBot(ActorContext<HelloWorld.Greeted> context, int max) {
      super(context);
      this.max = max;
    }

    @Override
    public Receive<HelloWorld.Greeted> createReceive() {
      return newReceiveBuilder()
              .onMessage(HelloWorld.Greeted.class, this::onGreeted)
              .build();
    }

    private Behavior<HelloWorld.Greeted> onGreeted(HelloWorld.Greeted message) {
      greetingCounter++;
      getContext().getLog().info("Greeting {} for {}", greetingCounter, message.whom);
      if (greetingCounter == max) {
        return Behaviors.stopped();
      } else {
        message.from.tell(new HelloWorld.Greet(message.whom, getContext().getSelf()));
        return this;
      }
    }
  }
  // #hello-world-bot

  // #hello-world-main
  // #hello-world-main-setup
  public class HelloWorldMain extends AbstractBehavior<HelloWorldMain.SayHello> {
    // #hello-world-main-setup

    public static class SayHello {
      public final String name;

      public SayHello(String name) {
        this.name = name;
      }
    }

    // #hello-world-main-setup
    public static Behavior<SayHello> create() {
      return Behaviors.setup(HelloWorldMain::new);
    }

    private final ActorRef<HelloWorld.Greet> greeter;

    private HelloWorldMain(ActorContext<SayHello> context) {
      super(context);
      // yl context是SayHello类型, HelloWorld 及里面的 contenxt 是 Greet 类型
      greeter = context.spawn(HelloWorld.create(), "greeter");
    }
    // #hello-world-main-setup

    @Override
    public Receive<SayHello> createReceive() {
      return newReceiveBuilder().onMessage(SayHello.class, this::onStart).build();
    }

    private Behavior<SayHello> onStart(SayHello command) {
      ActorRef<HelloWorld.Greeted> replyTo =
          getContext().spawn(HelloWorldBot.create(3), command.name);
      greeter.tell(new HelloWorld.Greet(command.name, replyTo));
      return this;
    }
    // #hello-world-main-setup
  }
  // #hello-world-main-setup
  // #hello-world-main

  interface CustomDispatchersExample {
    // #hello-world-main-with-dispatchers
    public class HelloWorldMain extends AbstractBehavior<HelloWorldMain.SayHello> {

      // Start message...
      // #hello-world-main-with-dispatchers
      public static class SayHello {
        public final String name;

        public SayHello(String name) {
          this.name = name;
        }
      }
      // #hello-world-main-with-dispatchers

      public static Behavior<SayHello> create() {
        return Behaviors.setup(HelloWorldMain::new);
      }

      private final ActorRef<HelloWorld.Greet> greeter;

      private HelloWorldMain(ActorContext<SayHello> context) {
        super(context);

        final String dispatcherPath = "akka.actor.default-blocking-io-dispatcher";
        Props greeterProps = DispatcherSelector.fromConfig(dispatcherPath);
        greeter = getContext().spawn(HelloWorld.create(), "greeter", greeterProps);
      }

      // createReceive ...
      // #hello-world-main-with-dispatchers
      @Override
      public Receive<SayHello> createReceive() {
        return null;
      }
      // #hello-world-main-with-dispatchers
    }
    // #hello-world-main-with-dispatchers
  }

  public static void main(String[] args) throws Exception {
    // #hello-world
    final ActorSystem<HelloWorldMain.SayHello> system =
        ActorSystem.create(HelloWorldMain.create(), "hello");

    system.tell(new HelloWorldMain.SayHello("World"));
    system.tell(new HelloWorldMain.SayHello("Akka"));
    // #hello-world

    Thread.sleep(3000);
    system.terminate();
  }
}
