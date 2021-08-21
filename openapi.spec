openapi: 3.0.1
info:
  title: Stellar accounts as a service
  description: 'This is the api specification for the Stellar accounts as a service bounty.'
  version: 1.0.0
tags:
- name: auth
  description: Authenticate yourself
- name: info
  description: Info
- name: pay
  description: Different ways to pay
paths:
  /register:
    post:
      tags:
      - auth
      summary: Create an account.
      requestBody:
        description: Credentials to register with.
        content:
          application/json:
            schema:
              properties:
                username:
                  example: coolUsername
                  type: string
                password:
                  example: "!coolPassword42"
                  type: string
        required: true
      responses:
        204:
          description: Succesfully created an account.
        409:
          description: Account already exists.
        400:
          description: Invalid body.
        500:
          description: Catch all response for other errors aside from the ones listed.
          content:
            application/json:
              schema:
                properties:
                  error:
                    type: string
                    description: Text explaining the error.

  /login:
    post:
      tags:
      - auth
      summary: Get an auth token.
      requestBody:
        description: Credentials to login with.
        content:
         application/json:
          schema:
            properties:
              username:
                example: coolUsername
                type: string
              password:
                example: "!coolPassword42"
                type: string
        required: true
      responses:
        200:
          description: Successful login.
          content:
            application/json:
              schema:
                properties:
                  apikey:
                    type: string
        400:
          description: Invalid Body.
        401:
          description: Invalid credentials. Either the user name or password is incorrect.
  /info:
    get:
      tags:
      - info
      security:
        - bearerAuth : []
      summary: Get info about the user's address and XLM balance.
      responses:
        200:
          description: Sucessful operation.
          content:
            application/json:
              schema:
                properties:
                  address:
                    example: MA6FZSS32ISLP3TDDOVUP3OR3V7L4WHKH5EEDKY2HWEP23FEYU3NMAAAAAAAAAAAAF6VQ
                    type: string
                    description: The user's M-address.
                  balance:
                    example: "200.20"
                    type: string
                    description: The user's balance in XLM.
        401:
          description: Invalid api key.
  /pay:
    post:
      tags:
      - pay
      security:
       - bearerAuth: []
      summary: Pay Stellar accounts with XLM.
      requestBody:
        required: true
        description: Destination address and amount in XLM
        content:
          application/json:
            schema:
              properties:
                destination:
                  example: GBLKRATZODTSJNU7XTB5HY5VAAN63CPRT77UYZT2VLCNXE7F3YHSW22M
                  type: string
                  description: Address to send XLM to.
                amount:
                  example: 200
                  type: string
                  description: Amount of XLM to send.
      responses:
        204:
          description: Successful payment.
        401:
          description: Invalid api key.
        404:
          description: Destination address does not exist.
        400:
          description: Invalid body.
        409:
          description: Insufficient account balance.
        500:
          description: Catch all response for other errors aside from the ones listed.
          content:
            application/json:
              schema:
                properties:
                  error:
                    type: string
                    description: Text explaining the error.


components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
