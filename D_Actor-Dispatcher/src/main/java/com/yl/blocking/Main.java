package com.yl.blocking;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import com.yl.blocking.solution.SeparateDispatcherFutureActor;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/7/28 10:44
 **/
public class Main {
    public static void main(String[] args) {
        Behavior<Void> root =
                Behaviors.setup(
                        context -> {
                            for (int i = 0; i < 100; i++) {
                                //  阻塞Behavior复现
//                                context.spawn(BlockingActor.create(), "BlockingActor-" + i).tell(i);
                                // 阻塞Behavior使用转由的调度器
                                context.spawn(SeparateDispatcherFutureActor.create(), "BlockingActor-" + i).tell(i);
                                context.spawn(PrintActor.create(), "PrintActor-" + i).tell(i);
                            }
                            return Behaviors.ignore();
                        });
        ActorSystem<Void> system = ActorSystem.create(root, "blocking");
    }
}
