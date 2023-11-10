const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("test sqs functions", () => {

  it("should sendMessages", async () => {
    const events = [{}];

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
      const events = [{}];

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

});
