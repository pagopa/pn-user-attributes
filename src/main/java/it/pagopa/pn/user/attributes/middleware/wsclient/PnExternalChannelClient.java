package it.pagopa.pn.user.attributes.middleware.wsclient;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.ApiClient;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.api.AddressBookApi;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.AddressDtoDto;
import it.pagopa.pn.user.attributes.user.attributes.microservice.msclient.generated.datavault.v1.dto.RecipientAddressesDtoDto;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
public class PnExternalChannelClient {

    // FIXME: stub di mock per l'invio di notifica
    private final PnUserattributesConfig pnUserattributesConfig;

    public PnExternalChannelClient(PnUserattributesConfig pnUserattributesConfig) {
        this.pnUserattributesConfig = pnUserattributesConfig;
    }



    public Mono<Void> sendVerificationCode(String address, String channelType, String verificationCode)
    {
         return Mono.empty();
            
    }

}
