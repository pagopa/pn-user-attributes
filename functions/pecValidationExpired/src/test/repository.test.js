const { expect } = require("chai");
const { mockClient } = require("aws-sdk-client-mock");
const {
  DynamoDBDocumentClient,
  TransactWriteCommand,
} = require("@aws-sdk/lib-dynamodb");

const ddbMock = mockClient(DynamoDBDocumentClient);

describe("DynamoDB tests", function () {
  this.beforeEach(() => {
    ddbMock.reset();
  });

  const events = [
    {
      actionId: "notification_cancellation_iun_XLDW-MQYJ-WUKA-202302-A-1",
      timeslot: "2021-09-23T10:00",
      iun: "XLDW-MQYJ-WUKA-202302-A-1",
      type: "NOTIFICATION_CANCELLATION",
      notBefore: "2021-09-23T10:00:00.000Z",
      timelineId:
        "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1",
      opType: "INSERT_ACTION_FUTUREACTION",
      kinesisSeqNumber: "4950",
    },
  ];
});
