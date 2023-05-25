package it.pagopa.pn.user.attributes.middleware.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ExternalChannelHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.selfcare.v1.dto.PaSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_externalregistry_basepath=http://localhost:9999",
        "pn.env.runtime=PROD"
})
public class PnSelfcareClientTest {

    @Autowired
    private PnSelfcareClient client;

    @MockBean
    ActionHandler actionHandler;

    @MockBean
    SqsActionProducer sqsActionProducer;

    @MockBean
    ExternalChannelResponseHandler externalChannelResponseHandler;

    @MockBean
    ExternalChannelHandler externalChannelHandler;

    private static ClientAndServer mockServer;

    @BeforeEach
    public void init(){
        mockServer = startClientAndServer(9999);
    }

    @AfterEach
    public void end(){
        mockServer.stop();
    }

    @Test
    void getManyPaByIds() throws JsonProcessingException {
        //Given
        ArrayList<String> paIds = new ArrayList<>();
        paIds.add("abc");
        paIds.add("def");

        List<PaSummary> paSummaries = new ArrayList<>();
        PaSummary paSummary = new PaSummary();
        paSummary.setId(paIds.get(0));
        paSummary.setName("Fake first pa");
        paSummaries.add(paSummary);
        paSummary.setId(paIds.get(1));
        paSummary.setName("Fake second pa");
        paSummaries.add(paSummary);

        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(paSummaries);

        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "GET" )
                        .withPath( "/ext-registry-private/pa/v1/activated-on-pn")
                        .withQueryStringParameters(Parameter.param("id", paIds.get(0)), Parameter.param("id", paIds.get(1))))
                .respond( response()
                        .withBody( respjson )
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 200 ));

        //When
        List<PaSummary> res = client.getManyPaByIds(paIds).collectList().block();

        //Then
        Assertions.assertEquals( paSummaries, res );
    }
}
