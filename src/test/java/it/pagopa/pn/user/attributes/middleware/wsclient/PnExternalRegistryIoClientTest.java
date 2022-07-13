package it.pagopa.pn.user.attributes.middleware.wsclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.Activation;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.ActivationPayload;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.ActivationStatus;
import it.pagopa.pn.user.attributes.microservice.msclient.generated.externalregistry.io.v1.dto.FiscalCodePayload;
import it.pagopa.pn.user.attributes.middleware.queue.consumer.ActionHandler;
import it.pagopa.pn.user.attributes.middleware.queue.sqs.SqsActionProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

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
class PnExternalRegistryIoClientTest {


    @Autowired
    private PnExternalRegistryIoClient client;

    @MockBean
    PnDataVaultClient pnDataVaultClient;

    @MockBean
    ActionHandler actionHandler;

    @MockBean
    SqsActionProducer sqsActionProducer;


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
    void upsertServiceActivation() {
        //Given
        Activation responseDto = new Activation();
        responseDto.setFiscalCode("EEEEEE00E00E000A");
        responseDto.setStatus(ActivationStatus.ACTIVE);
        responseDto.setVersion(1);

        byte[] responseBodyBites = new byte[0];

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerFor( Activation.class );
        try {
            responseBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        ActivationPayload fiscalCodePayload = new ActivationPayload();
        fiscalCodePayload.setFiscalCode( "EEEEEE00E00E000A" );
        fiscalCodePayload.setStatus(ActivationStatus.ACTIVE);
        byte[] reqBodyBites = new byte[0];

        mapper.writerFor( ActivationPayload.class );
        try {
            reqBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "PUT" )
                        .withPath( "/ext-registry-private/io/v1/activations" ))
                .respond( response()
                        .withBody( responseBodyBites )
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 200 ));

        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId("PF-abcd");
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));


        //When
        Boolean limitedProfile = client.upsertServiceActivation( fiscalCodePayload.getFiscalCode(), true ).block();

        //Then
        Assertions.assertEquals( true, limitedProfile );
    }


    @Test
    void upsertServiceActivation_FAIL() {
        //Given
        Activation responseDto = new Activation();
        responseDto.setFiscalCode("EEEEEE00E00E000A");
        responseDto.setStatus(ActivationStatus.INACTIVE);
        responseDto.setVersion(1);

        byte[] responseBodyBites = new byte[0];

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerFor( Activation.class );
        try {
            responseBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        ActivationPayload fiscalCodePayload = new ActivationPayload();
        fiscalCodePayload.setFiscalCode( "EEEEEE00E00E000A" );
        fiscalCodePayload.setStatus(ActivationStatus.ACTIVE);
        byte[] reqBodyBites = new byte[0];

        mapper.writerFor( ActivationPayload.class );
        try {
            reqBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "PUT" )
                        .withHeader("Ocp-Apim-Subscription-Key", "fake_api_key")
                        .withPath( "/ext-registry-private/io/v1/activations" ))
                .respond( response()
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 500 ));


        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "POST" )
                        .withPath( "/ext-registry-private/io/v1/activations" ))
                .respond( response()
                        .withBody( responseBodyBites )
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 200 ));
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId("PF-abcd");
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));


        //When
        Boolean limitedProfile = client.upsertServiceActivation( fiscalCodePayload.getFiscalCode(), true ).block();

        //Then
        Assertions.assertEquals( false, limitedProfile);
    }

    @Test
    void getServiceActivation() {
        //Given
        Activation responseDto = new Activation();
        responseDto.setFiscalCode("EEEEEE00E00E000A");
        responseDto.setStatus(ActivationStatus.ACTIVE);
        responseDto.setVersion(1);

        byte[] responseBodyBites = new byte[0];

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerFor( Activation.class );
        try {
            responseBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        FiscalCodePayload fiscalCodePayload = new FiscalCodePayload();
        fiscalCodePayload.setFiscalCode( "EEEEEE00E00E000A" );
        byte[] reqBodyBites = new byte[0];

        mapper.writerFor( FiscalCodePayload.class );
        try {
            reqBodyBites = mapper.writeValueAsBytes( responseDto );
        } catch ( JsonProcessingException e ){
            e.printStackTrace();
        }


        new MockServerClient( "localhost", 9999 )
                .when( request()
                        .withMethod( "POST" )
                        .withPath( "/ext-registry-private/io/v1/activations" ))
                .respond( response()
                        .withBody( responseBodyBites )
                        .withContentType( MediaType.APPLICATION_JSON )
                        .withStatusCode( 200 ));
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setInternalId("PF-abcd");
        baseRecipientDtoDto.setTaxId("EEEEEE00E00E000A");
        baseRecipientDtoDto.setDenomination("mario rossi");
        List<BaseRecipientDtoDto> list = new ArrayList<>();
        list.add(baseRecipientDtoDto);
        Mockito.when(pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(list));


        //When
        Activation limitedProfile = client.getServiceActivation( fiscalCodePayload.getFiscalCode() ).block();

        //Then
        Assertions.assertNotNull( limitedProfile );
    }
}