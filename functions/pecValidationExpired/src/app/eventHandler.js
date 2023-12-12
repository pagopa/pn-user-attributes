const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { sendMessages } = require("./lib/sqs.js");

exports.handleEvent = async (event) => {
  const defaultPayload = {
    batchItemFailures: [],
  };

  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return defaultPayload;
  }



  const processedItems = await mapEvents(cdcEvents);
  if (processedItems.length == 0) {
    console.log("No events to send");
    return defaultPayload;
  }

  console.log(`Items to send size:`, processedItems.length);

  let batchItemFailures = [];
  while(processedItems.length > 0){
    let currentCdcEvents = processedItems.splice(0,10);
    try{
      if (currentCdcEvents.length > 0){
        let responseError = await sendMessages(currentCdcEvents);

        if(responseError.length > 0){
          // gli eventi contengono info sensibili (Email) e non possono essere loggati
          batchItemFailures = batchItemFailures.concat(responseError.map((i) => {
          return { itemIdentifier: i.kinesisSeqNumber };
          }));
          console.log('Error in send current cdcEvents: ', batchItemFailures);
        }
      }else{
        console.log('No events to send in current cdcEvents: ',currentCdcEvents);
      }
    }catch(exc){
      console.log(exc);
      // gli eventi contengono info sensibili (Email) e non possono essere loggati
      batchItemFailures = batchItemFailures.concat(currentCdcEvents.map((i) => {
        return { itemIdentifier: i.kinesisSeqNumber };
      }));
      console.log('Error in send current cdcEvents: ', batchItemFailures);
    }
  }
  if(batchItemFailures.length > 0){
    console.log('process finished with error!');
  }
  return {
    batchItemFailures: batchItemFailures,
  };

};

