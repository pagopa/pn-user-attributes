const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");
const { getVerificationCodeAddressByInternalId } = require("./datavaultClient.js")
const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL

exports.sendMessages = async function sendMessages(messages) {
  let error = [];
  try{

      console.log('Proceeding to send ' + messages.length + ' messages to ' + QUEUE_URL);
      const input = {
        Entries: messages,
        QueueUrl: QUEUE_URL
      }

      // i messaggi contengono info sensibili, non posso stampare l'input liscio
    await Promise.all(input.Entries.map(async (i) => {
          let body = JSON.parse(i.MessageBody);

          // Recuperiamo l'indirizzo da datavault, in quanto non viene piu' valorizzato nel record VC su pn-UserAttributes.
          // Se non dovesse essere presente su datavault, è ancora presente nella tabella e lo recuperiamo da lì.
          let address = await getVerificationCodeAddressByInternalId(body.internalId, body.hashedAddress);
          address = address == null ? body.address : address;

          if (address == null) {
            throw new Error(`Address is null for recipientId "${body.internalId}" and hashedAddress "${body.hashedAddress}"`);
          }

          // Sovrascriviamo il campo address del MessageBody originale.
          body.address = address;
          i.MessageBody = JSON.stringify(body);

          // Anonimizziamo l'indirizzo per poterlo stampare nel log
          const em = address.indexOf('@');
          const startIndex = em * .2 | 0;
          const endIndex   = em * .9 | 0;
          const anonymEm = address.slice(0, startIndex) +
                 address.slice(startIndex, endIndex).replace(/./g, '*') +
                 address.slice(endIndex);
          body.address = anonymEm;

          console.log("sending message id:" +  i.Id + " eventId:" + i.MessageAttributes.eventId.StringValue + " message:" + JSON.stringify(body));
        }));

      const command = new SendMessageBatchCommand(input);
      const response = await sqs.send(command);

      if (response.Failed && response.Failed.length > 0)
      {
        console.log("error sending some message totalErrors:" + response.Failed.length);
        response.Failed.map((i) => {
          console.log("failed message error " +  i.Id + ":", i);
          return { kinesisSeqNumber : i.Id };
        })

        error = error.concat(response.Failed.map((i) => {
          return { kinesisSeqNumber : i.Id };
        }));

      }

  }catch(exc){
      console.log("error sending message");
      console.log(exc);
      throw exc;
  }
  return error;

};
