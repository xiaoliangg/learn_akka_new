package com.yl.bubble;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Collections;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/8/16 14:22
 **/
public class Main {
    public static void main(String[] args) {
        int ackPort = 15023;
        Config config = configWithPort(ackPort);
        Behavior<Void> root =
                Behaviors.setup(
                        context -> {
                            ActorRef<Protocol.Command> queryAllActorsBehavior =
                                    context.spawn(Protocol.Boss.create(), "ylBoss");
                            ActorRef<String> receive =
                                    context.spawn((new ReceiveBehavior()).create(), "ylReceive");

                            // 测试发送普通消息和发送异常消息
                            queryAllActorsBehavior.tell(new Protocol.Hello("helloYLYLYL",receive));
//                            queryAllActorsBehavior.tell(new Protocol.Fail("failYLYLYL"));

                            return Behaviors.ignore();

                        });
        ActorSystem<Void> system = ActorSystem.create(root, "ClusterSystem",config);


    }

    private static Config configWithPort(int port) {
        return ConfigFactory.parseMap(
                Collections.singletonMap("akka.remote.artery.canonical.port", Integer.toString(port))
        ).withFallback(ConfigFactory.load());
    }
}
