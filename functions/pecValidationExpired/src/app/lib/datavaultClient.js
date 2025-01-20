const http = require(process.env.PN_DATAVAULT_PROTOCOL);
const HOSTNAME = process.env.PN_DATAVAULT_HOSTNAME;
const PATHGET = process.env.PN_DATAVAULT_GET_RECIPIENT_ADDRESSES_PATH;

function getRecipientAddressesByInternalId(internalId) {
  console.log(`Invoking external service pn-datavault - getRecipientAddressesByInternalId with args : [${internalId}]`);
  const options = {
    method: "GET",
    hostname: HOSTNAME,
    path: PATHGET.replace("{internalId}", internalId),
    headers: {
      "Content-Type": "application/json",
    },
  };
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseBody = "";

      res.on("data", (chunk) => {
        responseBody += chunk;
      });

      res.on("end", () => {
        resolve(JSON.parse(responseBody));
      });
    });

    req.on("error", (err) => {
      reject(err);
    });
    req.end();
  });
}

exports.getVerificationCodeAddressByInternalId = async function (internalId, hashedAddress) {
  try {
    console.log(`Invoking method getVerificationCodeAddressByInternalId with args : [${internalId}, ${hashedAddress}]`);
    var response = await getRecipientAddressesByInternalId(internalId);
    return response.addresses["VC#" + internalId];
  }
  catch (error) {
    console.error("Exception in method getVerificationCodeAddressByInternalId: " + error);
    throw error;
  }
}