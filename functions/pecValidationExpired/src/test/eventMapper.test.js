const { expect } = require("chai");
const fs = require("fs");

const { mapEvents } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {
  // correct mapping
  it("test PEC_REJECTED_ACTION", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    const events = [event];

    const res = await mapEvents(events);
    const entity = JSON.parse(res[0].MessageBody);
    expect(res[0].MessageAttributes.eventType.StringValue).equal("PEC_REJECTED_ACTION");

    expect(entity.address).equal("email@email.it");
    expect(entity.hashedAddress).equal("2345678");
    expect(entity.internalId).equal("PF-12345678");
    expect(entity.type).equal("PEC_REJECTED_ACTION");
    expect(entity.actionId).equal(
      "VC#PF-12345678026e8c72-7944-4dcd-8668-f596447fec6d"
    );
  });

  // correct mapping with missing address field
  it("test PEC_REJECTED_ACTION missing address", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    event.dynamodb.OldImage.address = null;
    const events = [event];

    const res = await mapEvents(events);
    const entity = JSON.parse(res[0].MessageBody);
    expect(res[0].MessageAttributes.eventType.StringValue).equal("PEC_REJECTED_ACTION");

    expect(entity.address).to.be.null;
    expect(entity.hashedAddress).equal("2345678");
    expect(entity.internalId).equal("PF-12345678");
    expect(entity.type).equal("PEC_REJECTED_ACTION");
    expect(entity.actionId).equal(
      "VC#PF-12345678026e8c72-7944-4dcd-8668-f596447fec6d"
    );
  }); 

  // check that the event is not mapped, by changing the event
  it("test PEC_REJECTED_ACTION wrong event", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    event.eventName = "INSERT";
    const events = [event];

    const res = await mapEvents(events);

    expect(res.length).equal(0);
  });

  // check that the event is not mapped, by changing the table
  it("test PEC_REJECTED_ACTION wrong tableName", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    event.tableName = "altratabella";
    const events = [event];

    const res = await mapEvents(events);

    expect(res.length).equal(0);
  });

  // check that the event is not mapped, by changing the userIdentity.type
  it("test PEC_REJECTED_ACTION wrong userIdentity.type", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    event.userIdentity.type = "altro";
    const events = [event];

    const res = await mapEvents(events);

    expect(res.length).equal(0);
  });

  // check that the event is not mapped, by changing the principalId
  it("test PEC_REJECTED_ACTION wrong userIdentity.principalId", async () => {
    const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
    const event = JSON.parse(eventJSON);
    event.userIdentity.principalId = "altro";
    const events = [event];

    const res = await mapEvents(events);

    expect(res.length).equal(0);
  });

  // check that the event is not mapped, by changing the codeValid
    it("test PEC_REJECTED_ACTION wrong codeValid", async () => {
      const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
      const event = JSON.parse(eventJSON);
      event.dynamodb.OldImage.codeValid.BOOL = false;
      const events = [event];

      const res = await mapEvents(events);

      expect(res.length).equal(0);
    });

  // check that the event is not mapped, by changing the pecValid
      it("test PEC_REJECTED_ACTION wrong pecValid", async () => {
        const eventJSON = fs.readFileSync("./src/test/entity.verificationcode_ok.json");
        const event = JSON.parse(eventJSON);
        event.dynamodb.OldImage.pecValid.BOOL = true;
        const events = [event];

        const res = await mapEvents(events);

        expect(res.length).equal(0);
      });
});
