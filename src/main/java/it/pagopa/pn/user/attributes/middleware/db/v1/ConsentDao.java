package it.pagopa.pn.user.attributes.middleware.db.v1;

import it.pagopa.pn.user.attributes.config.PnUserattributesConfig;
import it.pagopa.pn.user.attributes.middleware.db.v1.entities.ConsentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

@Repository
@Slf4j
public class ConsentDao extends BaseDao implements IConsentDao {

    DynamoDbAsyncTable<ConsentEntity> userAttributesTable;

    public ConsentDao(DynamoDbEnhancedAsyncClient dynamoDbAsyncClient,
                      PnUserattributesConfig pnUserattributesConfig) {
        this.userAttributesTable = dynamoDbAsyncClient.table(pnUserattributesConfig.getDynamodbTableName(), TableSchema.fromBean(ConsentEntity.class));
    }

    // Crea o modifica l'entity ConsentEntity

    /**
     * Inserice o aggiorna un item di tipo ConsentEntity
     * setta i campi accepted, created, lastModified
     *
     * ATTENZIONE: il metodo esegue in sequenza un'operazione di lettura e una di scrittura in database.
     * Non essendoci una transazione che le comprenda entrambe c'è il rischio che il consenso letto sia già stato modificato da un'altra istanza
     * prima di essere salvato in database. Questa sezione di codice richiede un'ulteriore analisi e una revisione.
     * N.B: La race condition si verifica solo se l'utente utilizza due finestre del browser dfferenti. Eventualità poco probabile.
     *
     * @param userAttributes
     * @return none
     */
    @Override
    public Mono<Object> consentAction(ConsentEntity userAttributes){
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(userAttributes.getRecipientId(), userAttributes.getConsentType()))
                .build();

        return  Mono.fromFuture(userAttributesTable.getItem(getReq)
                        .thenCompose(r -> {
                            if (r != null) {
                                // update -> don't modify created
                                userAttributes.setCreated(null);
                                if (r.isAccepted() == userAttributes.isAccepted())
                                    // se il consenso non cambia non modifico lastModified
                                    userAttributes.setLastModified(null);
                            }
                            else
                                // create -> don't set lastModified
                                userAttributes.setLastModified(null);

                            UpdateItemEnhancedRequest<ConsentEntity> updRequest = UpdateItemEnhancedRequest.builder(ConsentEntity.class)
                                    .item(userAttributes)
                                    .ignoreNulls(true)
                                    .build();
                            return userAttributesTable.updateItem(updRequest);
                        }));
    }

    /**
     * Legge l'entity ConsentEntity associata a recipientId e ConsentType (TOS/DATAPRIVACY)
     *
     * @param recipientId
     * @param consentType
     * @return ConsentEntity
     */
    @Override
     public Mono<ConsentEntity> getConsentByType(String recipientId, String consentType) {
        ConsentEntity ce = new ConsentEntity(recipientId, consentType);
        GetItemEnhancedRequest getReq = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(ce.getPk(), ce.getSk()))
                .build();

        return Mono.fromFuture(userAttributesTable.getItem(getReq));
    }


    /**
     * Legge la lista di entity ConsentEntity associata a recipientId
     * Per ogni recipientId esistono tanti consensi quante sono le tipologie di consenso (2): ConsentTypeDto.TOS e ConsentTypeDto.DATAPRIVACY
     *
     * @param recipientId
     * @return Flux<ConsentEntity>  lista di ConsentEntity
     */
    @Override
    public Flux<ConsentEntity> getConsents(String recipientId) {
        ConsentEntity ce = new ConsentEntity(recipientId, "");

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(ce.getPk())))
                .scanIndexForward(true)
                .build();

        return Flux.from(userAttributesTable.query(qeRequest))
                .flatMapIterable(Page::items);

    }

}
