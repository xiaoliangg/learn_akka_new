package sample.distributeddata.yltest;

import akka.actor.typed.ActorRef;
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
public class RequestBehavior extends AbstractBehavior<RequestBehavior.Command> {

    public interface Command extends CborSerializable {}

    public static final class Request implements Command {
        public final VotingService.Command command;
        public final ActorRef<HttpResponseData> replyTo;
        public Request(VotingService.Command command, ActorRef<HttpResponseData> replyTo) {
            this.command = command;
            this.replyTo = replyTo;
        }
    }

    private final ActorRef<VotingService.Command> votingActor;

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new RequestBehavior(context));
    }

    public RequestBehavior(ActorContext<Command> context) {
        super(context);
        votingActor = context.spawn(VotingService.create(), "votingService");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, this::onRequest)
                .onMessage(VotingService.Votes.class, this::handleVotes)
                .build();
    }

    private Behavior<Command> handleVotes(VotingService.Votes a) {
        System.out.println("查询结果:" + a.toString());
        return Behaviors.same();
    }

    private Behavior<Command> onRequest(Request request) {
        System.out.println("接收到命令:" + request.command.toString());
        VotingService.Command command = request.command;
        if(command instanceof VotingService.GetVotes) {
            ((VotingService.GetVotes)command).replyTo = getContext().getSelf();
        }
        votingActor.tell(command);
        request.replyTo.tell(new HttpResponseData(System.currentTimeMillis() + "","ok"));
        return Behaviors.same();
    }
}
