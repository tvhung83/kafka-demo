## Kafka demo of producer/consumer
This source code includes the most basic features of Kafka Producer/Consumer API:
- An HTTP servlet to receive JSON payload.
- A consumer CLI app to print out message.

Note: dependency injection is done with Guice.

### How to run
- Run `producer` servlet:
```sh
$ ./gradlew clean runProducer
```
- Run `consumer` CLI app:
```sh
$ ./gradlew clean runConsumer
```
- Send request:
```http
POST http://localhost:8888/
Content-Type: application/json

{
  "email": "john@example.com",
  "shippingAddress": {
    "firstName": "John",
    "lastName": "Doe"
  },
  "lineItems": [
    {
      "sku": "FOO-BAR-123",
      "productType": "Electronic",
      "salePrice": 10000,
      "warehouse": "DUMMY",
      "quantity": 10,
      "vendor": "Alibaba"
    }
  ],
  "orderNumber": "ORD-111111"
}
```