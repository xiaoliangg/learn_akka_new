package com.yl.blocking.solution;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @description: https://doc.akka.io/docs/akka/current/typed/dispatchers.html#solution-dedicated-dispatcher-for-blocking-operations
 * @author: xiaoliang
 * @date: 2022/7/28 11:30
 **/
public class SeparateDispatcherFutureActor extends AbstractBehavior<Integer> {
    private final Executor ec;

    public static Behavior<Integer> create() {
        return Behaviors.setup(SeparateDispatcherFutureActor::new);
    }

    private SeparateDispatcherFutureActor(ActorContext<Integer> context) {
        super(context);
        ec =
                context
                        .getSystem()
                        .dispatchers()
                        .lookup(DispatcherSelector.fromConfig("my-blocking-dispatcher"));
    }

    @Override
    public Receive<Integer> createReceive() {
        return newReceiveBuilder()
                .onMessage(
                        Integer.class,
                        i -> {
                            triggerFutureBlockingOperation(i, ec);
                            return Behaviors.same();
                        })
                .build();
    }

    private static void triggerFutureBlockingOperation(Integer i, Executor ec) {
        System.out.println("Calling blocking Future on separate dispatcher: " + i);
        CompletableFuture<Integer> f =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                Thread.sleep(5000);
                                System.out.println("Blocking future finished: " + i);
                                return i;
                            } catch (InterruptedException e) {
                                return -1;
                            }
                        },
                        ec);
    }
}
