package it.pagopa.pn.user.attributes.middleware.wsclient.common;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class OcpBaseClient extends CommonBaseClient {
    private static final String HEADER_API_KEY = "Ocp-Apim-Subscription-Key";

    protected WebClient.Builder initWebClient(WebClient.Builder builder, String apiKey){

        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        return super.enrichBuilder(builder)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HEADER_API_KEY, apiKey);
    }


    protected String elabExceptionMessage(Throwable x)
    {
        try {
            String message = x.getMessage()==null?"":x.getMessage();
            if (x instanceof WebClientResponseException)
            {
                message += ";" + ((WebClientResponseException)x).getResponseBodyAsString();
            }
            return  message;
        } catch (Exception e) {
            log.error("exception reading body", e);
            return x.getMessage();
        }
    }
}
