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

      console.log('Sending batch message: %j', input);

      const command = new SendMessageBatchCommand(input);
      const response = await sqs.send(command);

      if (response.Failed && response.Failed.length > 0)
      {
        console.log("error sending some message totalErrors:" + response.Failed.length);

        error = error.concat(response.Failed.map((i) => {
          return { kinesisSeqNumber : i.Id };
        }));

      }

  }catch(exc){
      console.log("error sending message", exc)
      throw exc;
  }
  return error;

};
