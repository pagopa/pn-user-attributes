package it.pagopa.pn.user.attributes.middleware.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.handler.ExternalChannelResponseHandler;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.ActivationPayload;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.ActivationStatus;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.v1.dto.PrivacyNoticeVersionResponse;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ExternalChannelHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_externalregistry_basepath=http://localhost:9999",
        "pn.env.runtime=PROD"
})
class PnExternalRegistryClientTest {

    @Autowired
    private PnExternalRegistryClient client;

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
    void findPrivacyNoticeVersion() {
        //Given

        PrivacyNoticeVersionResponse response = new PrivacyNoticeVersionResponse();
        response.setVersion(1);
        byte[] responseBodyBites = new byte[0];

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerFor( PrivacyNoticeVersionResponse.class );
        try {
            responseBodyBites = mapper.writeValueAsBytes( response );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        ActivationPayload fiscalCodePayload = new ActivationPayload();
        fiscalCodePayload.setFiscalCode( "EEEEEE00E00E000A" );
        fiscalCodePayload.setStatus(ActivationStatus.ACTIVE);


        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "GET" )
                        .withPath( "/ext-registry-private/privacynotice/{consentsType}/{portalType}"
                                .replace("{consentsType}","TOS")
                                .replace("{portalType}", "PF")))
                .respond( response()
                        .withBody( responseBodyBites )
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 200 ));



        //When
        String res = client.findPrivacyNoticeVersion( "TOS", "PF" ).block();

        //Then
        Assertions.assertEquals( "1", res );
    }
}