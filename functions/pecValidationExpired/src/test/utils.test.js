const { expect } = require("chai");
const { parseKinesisObjToJsonObj } = require("../app/lib/utils.js");
const fs = require("fs");



describe("test utils functions", () => {

  it("should parse kinesis obj", () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const kinesisObj = JSON.parse(eventJSON);

    const parsedObj = parseKinesisObjToJsonObj(kinesisObj);
    expect(parsedObj).to.eql({
     dynamodb: {
        NewImage: {},
        OldImage: {
          address: "email@email.it",
          codeValid: true,
          created: "2023-01-20T14:48:00.000Z",
          lastModified: "2023-01-20T14:48:00.000Z",
          pecValid: false,
          pk: "VC#PF-12345678",
          requestId: "026e8c72-7944-4dcd-8668-f596447fec6d",
          sk: "2345678#123456"
        }
      },
      eventName: "REMOVE",
      tableName: "pn-UserAttributes",
      userIdentity: {
        principalId: "dynamodb.amazonaws.com",
        type: "Service"
      }
    });
  });

  it("no kinesis obj", () => {
    const parsedObj = parseKinesisObjToJsonObj(null);
    expect(parsedObj).equal(null);
  });
});
