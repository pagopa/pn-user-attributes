const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

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
      input.Entries.forEach((i) => {
          let body = JSON.parse(i.MessageBody);

          const em = body.address.indexOf('@');
          const startIndex = em * .2 | 0;
          const endIndex   = em * .9 | 0;
          const anonymEm = body.address.slice(0, startIndex) +
                 body.address.slice(startIndex, endIndex).replace(/./g, '*') +
                 body.address.slice(endIndex);
          body.address = anonymEm;

          console.log("sending message id:" +  i.Id + " eventId:" + i.MessageAttributes.eventId.StringValue + " message:" + JSON.stringify(body));
        })

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
