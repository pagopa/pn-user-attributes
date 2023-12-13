const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("test sqs functions", () => {

  it("should sendMessages", async () => {

    const events = [buildMessage()];

    const mockSQSClient = {
      send: async () => ({}) // Mock per un successo
    };

    const lambda = proxyquire.noCallThru().load("../app/lib/sqs.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      }
    });


    const res = await lambda.sendMessages(events);
    expect(res).deep.equals([]);
  });

  it("should sendMessages wit failures", async () => {
      const events = [buildMessage()];

      const mockSQSClient = {
        send: async () => ({Failed:[{Id:"123"}]}) // Mock per un failed
      };

      const lambda = proxyquire.noCallThru().load("../app/lib/sqs.js", {
        "@aws-sdk/client-sqs": {
          SQSClient: class {
            constructor() {
              return mockSQSClient;
            }
          },
          SendMessageBatchCommand: class {},
        }
      });


      const res = await lambda.sendMessages(events);
      expect(res).deep.equals([{kinesisSeqNumber: "123"}]);
    });

  it("should sendMessages wit exception", async () => {
        const events = [{}];

        const mockSQSClient = {
          send: async () => {
            throw new TypeError('Illegal data!');
          }
        };

        const lambda = proxyquire.noCallThru().load("../app/lib/sqs.js", {
          "@aws-sdk/client-sqs": {
            SQSClient: class {
              constructor() {
                return mockSQSClient;
              }
            },
            SendMessageBatchCommand: class {},
          }
        });

        await expectThrowsAsync(() => lambda.sendMessages(events))

        //await expect().to.be.rejectedWith(Error)

      });


  const expectThrowsAsync = async (method, errorMessage) => {
    let error = null
    try {
      await method()
    }
    catch (err) {
      error = err
    }
    expect(error).to.be.an('Error')
    if (errorMessage) {
      expect(error.message).to.equal(errorMessage)
    }
  }

  function buildMessage() {
          let date = new Date();

          let action = {
            actionId: '1234567890',
            internalId: 'PF-123456789',
            address: 'test@test.it',
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
            Id: '1234567890',
            MessageAttributes: messageAttributes,
            MessageBody: JSON.stringify(action)
          };
          return resultElement;
  }

});
