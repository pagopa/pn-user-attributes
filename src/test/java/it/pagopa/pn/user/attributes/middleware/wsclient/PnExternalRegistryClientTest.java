package it.pagopa.pn.user.attributes.middleware.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.internal.v1.dto.PrivacyNoticeVersionResponse;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.ActivationPayload;
import it.pagopa.pn.user.attributes.user.attributes.generated.openapi.msclient.externalregistry.io.v1.dto.ActivationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.Delay;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@WebFluxTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.user-attributes.client_externalregistry_basepath=http://localhost:9999",
        "pn.env.runtime=PROD",
        "pn.commons.read-timeout-millis=300"
})
@ContextConfiguration(classes = {
        PnExternalRegistryClient.class,
        MsClientExtRegistryConfig.class,
        PnUserattributesConfig.class,
        MsClientConfig.class
})
class PnExternalRegistryClientTest {

    @Autowired
    private PnExternalRegistryClient client;

    @Autowired
    private PnUserattributesConfig pnUserattributesConfig;

    @Autowired
    private MsClientExtRegistryConfig msClientConfig;
    @Autowired
    private MsClientConfig msClientConfig1;

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
                        .withStatusCode( 200 )
                        .withDelay(Delay.milliseconds(400)) // < 150% di pn.commons.read-timeout-millis
                        .withConnectionOptions(ConnectionOptions.connectionOptions().withCloseSocketDelay(Delay.milliseconds(400)))
                        );



        //When
        String res = client.findPrivacyNoticeVersion( "TOS", "PF" ).block();

        //Then
        Assertions.assertEquals( "1", res );
    }



    @Test
    void findPrivacyNoticeVersiontimeout() {
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
                        .withStatusCode( 200 )
                        .withDelay(Delay.milliseconds(500))); // > 150% di pn.commons.read-timeout-millis



        //When
        Mono<String> mono = client.findPrivacyNoticeVersion( "TOS", "PF" );
        Assertions.assertThrows(Exception.class, () -> mono.block());
    }
}