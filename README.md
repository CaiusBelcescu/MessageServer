# MessageServer
A message server where all the activities must be done simultaneously

Functional Requirements:
- a user is a sender or a reciver
- a user can send a message 
- a user can recieve a message 
- a user can choose between sending a private message and sending a message to all online users


Non-Functional Requirements:
- delete messages after a period of time
- each period of time the application checks the online users
- if a message has no reciever the message will not be sent
- the message queues can hold a maximum number of messages

Entities:
- Sender 
- Reciver
- Topic 
- Queue
- Message
