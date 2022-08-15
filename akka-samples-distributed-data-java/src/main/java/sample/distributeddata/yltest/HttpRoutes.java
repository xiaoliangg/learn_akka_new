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
    private final ActorRef<VotingService.Command> votingActor;

    public HttpRoutes(ActorSystem<?> system, ActorRef<VotingService.Command> votingActor) {
        this.system = system;
        timeout = Duration.ofSeconds(3);
        objectMapper = JacksonObjectMapperProvider.get(system).getOrCreate("jackson-json", Optional.empty());
        dataUnmarshaller = Jackson.unmarshaller(objectMapper, HttpRequest.class);
        this.votingActor = votingActor;
    }

    private CompletionStage<HttpResponseData> recordData(long wsid, HttpRequest data) {
        return AskPattern.ask(
                votingActor,
                replyTo -> VotingService.Open.INSTANCE,
                Duration.ofSeconds(3),
                system.scheduler());
    }


    public Route test1() {
        return path(segment("test1").slash().concat(longSegment()), wsid ->
                        concat(
//        get(() -> completeOKWithFuture(query(wsid, dataType, function), Jackson.marshaller()),
                                post(() ->
                                        entity(dataUnmarshaller, data ->
                                                onSuccess(recordData(wsid, data), performed ->
                                                        complete(StatusCodes.ACCEPTED, performed + " from event time: " + data.eventTime)
                                                )
                                        )
                                )
                        )
        );
    }

}
