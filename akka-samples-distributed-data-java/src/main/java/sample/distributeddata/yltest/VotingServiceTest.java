package sample.distributeddata.yltest;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sample.distributeddata.VotingService;

import java.util.Collections;

/**
 * @description: TODO
 * @author: xiaoliang
 * @date: 2022/8/11 15:55
 **/
public class VotingServiceTest {

    public static void main(String[] args) {
        int ackPort = Integer.parseInt(args[0]);
        int httpPort = Integer.parseInt(args[1]);
        Config config = configWithPort(ackPort);
        Behavior<Void> root =
                Behaviors.setup(
                        context -> {
//                      context.spawn(VotingService.create(), "votingService");
//                      context.spawn(VotingService.create(), "votingService").tell(new VotingService.Vote( "b"));
//                            ActorRef<RequestBehavior.Command> requestActor =
//                                    context.spawn(RequestBehavior.create(), "requestBehavior");
                            ActorRef<RequestBehavior.Command> requestActor =
                                    context.spawn(Behaviors.supervise(RequestBehavior.create()).onFailure(SupervisorStrategy.restart()), "requestBehavior");

                            ActorRef<VotingService.Command> votingActor =
                                    context.spawn(VotingService.create(), "votingService2");

                            ActorRef<QueryAllActorsBehavior.Command> queryAllActorsBehavior =
                                    context.spawn(QueryAllActorsBehavior.create(), "queryAllActorsBehavior");


                            HttpRoutes routes = new HttpRoutes(context.getSystem(),votingActor,requestActor,queryAllActorsBehavior);
                            HttpServer.start(routes.test1(), httpPort, context.getSystem());

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
