Event Ledger is a Spring Boot microservices project that processes account transactions. It consists of two services that work together to handle events and maintain account balances. The Event Gateway receives requests from clients, validates the incoming events, stores them, and forwards valid transactions to the Account Service. The Account Service processes CREDIT and DEBIT transactions, updates account balances, and maintains transaction history. The two services communicate synchronously using REST APIs, and each service has its own H2 database to keep them independent.
Event Gateway — Receives client requests, validates events, stores them, and forwards valid transactions to the Account Service.
Account Service — Processes CREDIT and DEBIT transactions, updates account balances, and maintains transaction history.
**Setup and Running the Project**

**Prerequisites**

**Make sure the following software is installed before running the project:**

- Java 21
- Maven 3.9 or later
- Docker Desktop
- Docker Compose
- Git

**Clone the Repository**
git clone https://github.com/swethachowdary888/event-ledger.git
cd event-ledger



**Build the Project**

Build both services before running them.

**Account Service**


cd account-service
mvn clean package


**Event Gateway**


cd ../event-gateway
mvn clean package




**Running the Project Using Docker**

From the project root directory, run:


docker compose up --build


This command builds the images (if needed) and starts both microservices.

To run the services in the background:


docker compose up -d


To stop the services:


docker compose down



**
Running the Project Without Docker**

Step 1 – Start the Account Service

cd account-service
mvn spring-boot:run

Step 2 – Start the Event Gateway

Open a new terminal window and run:

cd event-gateway
mvn spring-boot:run




**Verify the Application**

Event Gateway

Health Endpoint


http://localhost:8080/health


Available Metrics

http://localhost:8080/actuator/metrics


Account Service

Health Endpoint

http://localhost:8081/health



Running the Tests

**Account Service**


cd account-service
mvn test


Event Gateway


cd event-gateway
mvn test




**Project Notes**

- Event Gateway runs on http://localhost:8080
- Account Service runs on http://localhost:8081
- Each service uses its own H2 in-memory database.
- Docker Compose starts both services together and creates the required Docker network automatically.
