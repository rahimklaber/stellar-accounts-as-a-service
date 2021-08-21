# Stellar accounts as a service
## about
This repo contains a basic implementation of a custodial Stellar wallet api.
Users of the api can: 
- View their muxed addresses and balances.
- Send payments to other stellar addresses.
- Receive payments on their muxed addresses.

When first starting up, the server creates 5 stellar accounts to allow for concurrent payments.
Todo: use more than one op in a tx.

The full api spec can be found here: [ApiSpec](https://editor.swagger.io/?url=https://raw.githubusercontent.com/rahimklaber/stellar-accounts-as-a-service/main/openapi.spec)

## Testing
run `./gradlew test` to run the tests.
Currently there are only System tests and no unit tests.

## Running
First configure the required settings; the custodial secret key, sqlite database path and Jwt settings.
Then run `./gradlew run`.