package com.yl.bubble;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import java.util.List;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/8/16 14:33
 **/
public class ReceiveBehavior {
    public Behavior<String> create() {
        return Behaviors.receive(String.class)
                .onMessage(String.class, o -> handleHello(o))
                .build();
    }

    private Behavior<String> handleHello(String o) {
        System.out.println("接收到反馈:" + o);
        return Behaviors.same();
    }
}
