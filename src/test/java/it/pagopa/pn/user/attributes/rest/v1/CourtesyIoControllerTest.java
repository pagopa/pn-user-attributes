package it.pagopa.pn.user.attributes.rest.v1;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.user.attributes.services.AddressBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;


@WebFluxTest(controllers = {CourtesyIoController.class})
class CourtesyIoControllerTest {

    @MockBean
    AddressBookService svc;

    @Autowired
    WebTestClient webTestClient;


    @MockBean
    PnAuditLogBuilder pnAuditLogBuilder;

    PnAuditLogEvent logEvent;

    @BeforeEach
    public void init(){
        logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(pnAuditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(pnAuditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(pnAuditLogBuilder);
        Mockito.when(logEvent.generateSuccess(Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);
        Mockito.when(logEvent.log()).thenReturn(logEvent);
    }

    @Test
    void getCourtesyAddressIo() {
    }

    @Test
    void setCourtesyAddressIo() {
    }
}