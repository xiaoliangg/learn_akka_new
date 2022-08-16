package sample.distributeddata.yltest;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.serialization.jackson.JacksonObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import sample.distributeddata.VotingService;
import sample.distributeddata.yltest.entity.HttpRequest;
import sample.distributeddata.yltest.entity.HttpResponseData;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.longSegment;
import static akka.http.javadsl.server.PathMatchers.segment;

public class HttpRoutes {

    private final ActorSystem<?> system;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final Unmarshaller<HttpEntity, HttpRequest> dataUnmarshaller;
    private final ActorRef<RequestBehavior.Command> requestActor;
    private final ActorRef<VotingService.Command> votingActor;
    private final ActorRef<QueryAllActorsBehavior.Command> queryAllActorsBehavior;

    public HttpRoutes(ActorSystem<?> system,
                      ActorRef<VotingService.Command> votingActor,
                      ActorRef<RequestBehavior.Command> requestActor,
                      ActorRef<QueryAllActorsBehavior.Command> queryAllActorsBehavior) {
        this.system = system;
        timeout = Duration.ofSeconds(3);
        objectMapper = JacksonObjectMapperProvider.get(system).getOrCreate("jackson-json", Optional.empty());
        dataUnmarshaller = Jackson.unmarshaller(objectMapper, HttpRequest.class);
        this.votingActor = votingActor;
        this.requestActor = requestActor;
        this.queryAllActorsBehavior = queryAllActorsBehavior;
    }

    /**
     * 测试报文:
     * distributed data 增删改查:
     * curl -XPOST http://localhost:12561/test1/0 -H "Content-Type: application/json" --data '{"taskId": "supervisorNull"}'
     * curl -XPOST http://localhost:12561/test1/1 -H "Content-Type: application/json" --data '{"taskId": "supervisorNull"}'
     * curl -XPOST http://localhost:12561/test1/2 -H "Content-Type: application/json" --data '{"participant": "lawadeal"}'
     * curl -XPOST http://localhost:12561/test1/3 -H "Content-Type: application/json" --data '{"participant": "lawadeal"}'
     * 删除
     * curl -XPOST http://localhost:12561/test1/4 -H "Content-Type: application/json" --data '{"participant": "lawadeal"}'
     *
     * 查询当前actor树:
     * curl -XPOST http://localhost:12561/test1/5 -H "Content-Type: application/json" --data '{"a": "1"}'
     * @param wsid
     * @param data
     * @return
     */
    private CompletionStage<HttpResponseData> recordData(int wsid, HttpRequest data) {
        switch (wsid) {
            case 0: // 直接发送 open 到 votingActor
                return AskPattern.ask(
                        votingActor,
                        replyTo -> VotingService.Open.INSTANCE,
                        Duration.ofSeconds(3),
                        system.scheduler());

            case 1: // open
                return AskPattern.ask(
                        requestActor,
                        replyTo -> new RequestBehavior.Request(VotingService.Open.INSTANCE, replyTo),
                        Duration.ofSeconds(3),
                        system.scheduler());
            case 2: // 投票
                return AskPattern.ask(
                        requestActor,
                        replyTo -> new RequestBehavior.Request(new VotingService.Vote(data.participant), replyTo),
                        Duration.ofSeconds(3),
                        system.scheduler());
            case 3: // 查询投票
                return AskPattern.ask(
                        requestActor,
                        replyTo -> new RequestBehavior.Request(new VotingService.GetVotes(null), replyTo),
                        Duration.ofSeconds(3),
                        system.scheduler());
            case 4: // 删除某人所有投票
                return AskPattern.ask(
                        requestActor,
                        replyTo -> new RequestBehavior.Request(new VotingService.DeleteVote(data.participant), replyTo),
                        Duration.ofSeconds(3),
                        system.scheduler());
            case 5: // 查询当前actor树
                return AskPattern.ask(
                        queryAllActorsBehavior,
                        replyTo -> new QueryAllActorsBehavior.Request(replyTo),
                        Duration.ofSeconds(3),
                        system.scheduler());

            default:
                return null;
        }

    }


    public Route test1() {
        return path(segment("test1").slash().concat(longSegment()), wsid ->
                        concat(
//        get(() -> completeOKWithFuture(query(wsid, dataType, function), Jackson.marshaller()),
                                post(() ->
                                        entity(dataUnmarshaller, data ->
                                                onSuccess(recordData(wsid.intValue(), data), performed ->
                                                        complete(StatusCodes.ACCEPTED, performed + " from event time: " + data.eventTime)
                                                )
                                        )
                                )
                        )
        );
    }

}
