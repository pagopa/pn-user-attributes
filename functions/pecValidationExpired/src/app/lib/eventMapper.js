const { TABLES } = require("./repository");

async function mapPayload(event) {
  let timelineObj = parseKinesisObjToJsonObj(filteredEvents[i].dynamodb.NewImage);

      let date = new Date();

      let action = {
        iun: timelineObj.iun,
        paId: timelineObj.paId,
        timelineId: timelineObj.timelineElementId,
        eventId: `${date.toISOString()}_${timelineObj.timelineElementId}`,
        type: 'REGISTER_EVENT'
      };

      let messageAttributes = {
        publisher: {
          DataType: 'String',
          StringValue: 'deliveryPush'
        },
        iun: {
          DataType: 'String',
          StringValue: action.iun
        },
        eventId: {
          DataType: 'String',
          StringValue: crypto.randomUUID()
        },

        createdAt: {
          DataType: 'String',
          StringValue: date.toISOString()
        },
        eventType:  {
          DataType: 'String',
          StringValue:'WEBHOOK_ACTION_GENERIC'
        },
      };

      /*
      let webhookEvent = {
        header: header,
        payload: action
      };
      */

      let resultElement = {
        Id: filteredEvents[i].kinesisSeqNumber,
        MessageAttributes: messageAttributes,
        MessageBody: JSON.stringify(action)
      };

}

exports.mapEvents = async (events) => {
  // mi interessa filtrare gli eventi di eliminazione
  // provenienti da timeout TTL (lo si capisce da userIdentity)
  // che siano verificationcode, con codice validato dall'utente ma pec non validata
  const filteredEvents = events.filter((e) => {
    return (
      e.eventName == "REMOVE" &&
      e.userIdentity.type == "Service" &&
      e.userIdentity.principalId == "dynamodb.amazonaws.com" &&
      e.tableName == TABLES.USERATTRIBUTES &&
      e.dynamodb.OldImage.pk.startWith("VC#") &&
      e.dynamodb.OldImage.codeValid == true &&
      e.dynamodb.OldImage.pecValid == false
    );
  });
  let ops = [];

  for (let filteredEvent of filteredEvents) {
    const dynamoDbOps = await mapPayload(filteredEvent);
    ops = ops.concat(dynamoDbOps); // concatenate the arrays
  }
  return ops;
};
