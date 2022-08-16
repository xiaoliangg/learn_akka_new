package sample.distributeddata.yltest;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import sample.distributeddata.VotingService;
import sample.distributeddata.yltest.entity.CborSerializable;
import sample.distributeddata.yltest.entity.HttpResponseData;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/8/15 15:32
 **/
public class QueryAllActorsBehavior extends AbstractBehavior<QueryAllActorsBehavior.Command> {

    public interface Command {}
    public static final class Request implements Command {
        public final ActorRef<HttpResponseData> replyTo;
        public Request(ActorRef<HttpResponseData> replyTo) {
            this.replyTo = replyTo;
        }
    }

    private final ActorSystem<Void> system;
    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new QueryAllActorsBehavior(context));
    }

    public QueryAllActorsBehavior(ActorContext<Command> context) {
        super(context);
        system = context.getSystem();
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, this::onRequest)
                .build();
    }

    private Behavior<Command> onRequest(Request request) {
        String actorTree = system.printTree();
        System.out.println("当前actor树:" + actorTree);
        request.replyTo.tell(new HttpResponseData("200", actorTree));
        return Behaviors.same();
    }
}
