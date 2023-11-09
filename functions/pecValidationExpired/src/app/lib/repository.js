const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  TransactWriteCommand,
} = require("@aws-sdk/lib-dynamodb");
const { nDaysFromNowAsUNIXTimestamp } = require("./utils");

const TABLES = {
  USERATTRIBUTES: "pn-UserAttributes"
};

const client = new DynamoDBClient({
  region: process.env.REGION,
});

exports.TABLES = TABLES;
