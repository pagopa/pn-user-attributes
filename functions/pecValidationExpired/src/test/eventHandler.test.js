const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

// event handler tests
describe("event handler tests", function () {
  it("test Ok", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
      "./lib/sqs.js": {
        sendMessages: async () => {
          return  [];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no Kinesis data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [];
        },
      },
      "./lib/sqs.js": {
        sendMessages: async () => {
          return  [];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no mapped data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [];
        },
      },
      "./lib/sqs.js": {
        sendMessages: async () => {
          return  [];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test with persist errors", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
      "./lib/sqs.js": {
        sendMessages: async () => {
          return  [{ kinesisSeqNumber: "4950" }] ;
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "4950" }],
    });
  });
});
