const { TABLES } = require("./repository");
const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require('crypto');

async function mapPayload(event) {
      let verificationCodeObj = parseKinesisObjToJsonObj(event);
      let entity = verificationCodeObj.dynamodb.OldImage;
      let date = new Date();

      let action = {
        actionId: entity.pk+entity.requestId,
        internalId: entity.pk.replace("VC#",""),
        address: entity.address,
        timestamp: date.toISOString(),
        type: 'PEC_REJECTED_ACTION'
      };

      const evId = crypto.randomUUID();
      let messageAttributes = {
        publisher: {
          DataType: 'String',
          StringValue: 'userAttributes'
        },
        iun: {
          DataType: 'String',
          StringValue: evId
        },
        eventId: {
          DataType: 'String',
          StringValue: evId
        },
        createdAt: {
          DataType: 'String',
          StringValue: date.toISOString()
        },
        eventType:  {
          DataType: 'String',
          StringValue:'PEC_REJECTED_ACTION'
        },
      };


      let resultElement = {
        Id: event.kinesisSeqNumber,
        MessageAttributes: messageAttributes,
        MessageBody: JSON.stringify(action)
      };

      return resultElement;
}

exports.mapEvents = async (events) => {
  // mi interessa filtrare gli eventi di eliminazione
  // provenienti da timeout TTL (lo si capisce da userIdentity)
  // che siano verificationcode, con codice validato dall'utente ma pec non validata
  const filteredEvents = events.filter((e) => {
    return (
      e.eventName == "REMOVE" &&
      e.userIdentity != null &&
      e.userIdentity.type == "Service" &&
      e.userIdentity.principalId == "dynamodb.amazonaws.com" &&
      e.tableName == TABLES.USERATTRIBUTES &&
      e.dynamodb.OldImage.pk.S.startsWith("VC#") &&
      e.dynamodb.OldImage.codeValid.BOOL == true &&
      e.dynamodb.OldImage.pecValid.BOOL == false
    );
  });
  let ops = [];

  for (let filteredEvent of filteredEvents) {
    const dynamoDbOps = await mapPayload(filteredEvent);
    ops = ops.concat(dynamoDbOps); // concatenate the arrays
  }
  return ops;
};
