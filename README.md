# MessageServer
A message server where all the activities must be done simultaneously

Functional Requirements:
- Consists of users which can send messages to a server
- All messages should be sent simultaneously
- All messages should be received simultaneously (if everyone has internet )
- Servers can be created and deleted
- People can be added
- People can be ejected

Non-Functional Requirements:
- If a message is sent without access to internet, the message should be stored in a queue until the internet access is restored
- If a user has no connection to the internet he should see all the messages stored before the interuption 

Entities:
- Sender 
- Reciver
- Server Admin
- Topic 
- Queue
- Message
