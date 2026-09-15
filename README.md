# BloodBridge — Full Stack Java

BloodBridge is a full-stack web application designed to connect voluntary blood donors with people who are looking for blood donors.

The complete application runs on Java 17 with Spring Boot, Spring Security, Spring Data JPA, Hibernate and MySQL. React provides the frontend interface using HTML, CSS and JavaScript. The application is deployed on Render and uses Aiven MySQL as the production database.

BloodBridge helps organize donor information, donor discovery, blood requests, approvals, completed donations, rewards and administration in one platform.

BloodBridge does not replace hospitals or blood banks. Medical screening, blood compatibility testing, blood collection, storage and transfusion must always be handled by qualified medical professionals and authorized medical facilities.

## Live Website

https://bloodbridge-java.onrender.com

## Technology Stack

- Java 17
- Spring Boot
- Spring Security
- React
- HTML
- CSS
- JavaScript
- REST API
- JSON
- MySQL
- Spring Data JPA
- Hibernate
- Flyway
- Maven
- Docker
- Git
- GitHub
- Render
- Aiven MySQL

## Main Features

- Member sign-in and session management
- Donor profile creation and management
- Find donors by blood group and location
- Donor availability control
- Protected donor contact information
- Blood request management
- Request approval workflow
- Blood Received confirmation
- Recognition rewards for completed donations
- Saviour and Family Assistance functionality
- My Profile section
- Protected Admin Portal
- Internal admin and member communication

## How BloodBridge Works

A user interacts with the BloodBridge frontend through a web browser.

The frontend is built using React, HTML, CSS and JavaScript. It provides the pages, forms, buttons, donor cards and other parts of the website that users can see and use.

When a user performs an action, the frontend sends a request to the Java Spring Boot backend through REST APIs.

The backend processes the request, applies the required application and security rules and communicates with the MySQL database using Spring Data JPA and Hibernate.

The database stores the required application records. The backend then returns the result to the frontend, where it is displayed to the user.

Simple application flow:

User
↓
React / HTML / CSS / JavaScript
↓
REST API
↓
Java + Spring Boot
↓
Spring Data JPA / Hibernate
↓
MySQL Database

## Donor and Blood Request Flow

A member can create a donor profile containing the required donor information.

The donor can control whether they are currently available for contact.

A blood seeker can search for donors using blood group and location filters.

When a blood request is created, BloodBridge manages the request and approval process. Private contact information is protected and is made available according to the request approval rules.

The actual blood donation and medical process takes place through an appropriate hospital or blood centre.

After blood is received, the receiver can confirm it in BloodBridge. The system then updates the applicable completed-donation record and recognition reward.

## Rewards and Family Assistance

BloodBridge provides recognition points for completed voluntary donations.

An eligible completed donation gives the donor 500 recognition points after Blood Received is confirmed. Duplicate confirmation does not award the same reward again.

A donor reaches Saviour status after 10 completed donations. Saviour members receive two lifetime Family Assistance uses according to the current application rules.

## Admin Portal

BloodBridge contains a separate protected Admin Portal.

The Admin Portal is used for private administrative functions such as donor record management, internal communication and Family Assistance request management.

Admin access is protected separately from normal member access.

## Frontend

The frontend is the part of BloodBridge that users see and interact with.

HTML provides the basic structure of the pages.

CSS controls the design, layout, spacing, cards and responsive appearance.

JavaScript and React manage user interactions, forms, button actions, page state and communication with the backend.

## Backend

The backend is developed using Java 17 and Spring Boot.

It is responsible for:

- Receiving frontend requests
- Processing application logic
- Managing donor information
- Managing blood requests
- Applying request and reward rules
- Managing member and admin sessions
- Applying security rules
- Reading and writing database records
- Returning responses to the frontend

## REST API

BloodBridge uses REST APIs for communication between the frontend and backend.

For example:

User searches for donors
↓
Frontend sends an HTTP request
↓
Spring Boot receives the request
↓
Backend searches the database
↓
Matching donor records are returned
↓
Backend sends a JSON response
↓
Frontend displays the donors

## Database

BloodBridge uses MySQL to store persistent application data.

The database stores the application records required for features such as member accounts, donor profiles, blood requests, approvals, completed donations, rewards, Family Assistance and internal communication.

Spring Data JPA provides an easier way for Java code to work with database records.

Hibernate maps Java objects to database tables and helps the backend read and write information without manually writing SQL for every database operation.

Flyway manages versioned changes to the database structure.

## Security

BloodBridge uses Spring Security and server-side security controls.

Important security features include:

- Member sessions
- Separate administrator authentication
- Protected routes
- Authorization
- CSRF protection
- Protected donor contact information
- Secure administrator password hashing
- Environment variables for production configuration

The administrator password is not stored as plain text in the source code.

Production passwords, database credentials and other secrets must not be committed to the public GitHub repository.

## Environment Variables

Production configuration and secret values are supplied through environment variables.

Examples of environment variable names include:

ADMIN_USERNAME
ADMIN_PASSWORD_HASH
DB_URL
DB_USER
DB_PASSWORD

Actual secret values are not stored in the public source code.

## Maven

Maven manages the Java dependencies and build process.

The pom.xml file contains the required dependencies and build configuration for the Spring Boot application.

Maven compiles, tests and packages the Java application.

## Docker

Docker packages BloodBridge so that the application can run consistently on the hosting server.

Build flow:

Source Code
↓
Maven Build
↓
Spring Boot JAR
↓
Docker Image
↓
Docker Container
↓
Render

## Open and Run in VS Code

1. Extract the project ZIP and open the BloodBridge-Java folder using File → Open Folder.

2. Install JDK 17 or newer and Maven.

Check the installations using:

```bash
java -version
mvn -version
```

3. On Windows, run:

```bash
run-local.bat
```

On macOS/Linux, run:

```bash
bash run-local.sh
```

4. On the first local run, use the included admin setup process to configure the local administrator account securely.

5. Open:

http://localhost:8080

The compiled frontend is already included. Node.js is mainly required when editing or rebuilding the React frontend source.

## Use MySQL with Docker

With Docker Desktop installed, complete the local administrator setup and then run:

```bash
docker compose up --build
```

This starts the application with the local MySQL setup.

Open:

http://localhost:8080

To stop the containers:

```bash
docker compose down
```

Docker volumes can be used to keep local database data when containers are stopped.

## Production Deployment

BloodBridge is deployed using Render.

The production MySQL database is hosted on Aiven.

Deployment flow:

GitHub Repository
↓
Render
↓
Docker Build
↓
Spring Boot Application
↓
Aiven MySQL Database
↓
BloodBridge Live Website

Render runs the Java Spring Boot application and provides the public website.

Aiven hosts the production MySQL database.

The application connects to the production database using secure environment configuration.

## Project Architecture

User / Browser
↓
React + HTML + CSS + JavaScript
↓
REST API
↓
Java + Spring Boot + Spring Security
↓
Spring Data JPA + Hibernate
↓
MySQL Database

## Project Details

Developed by: Shreyansh Singh

College: K. C. College of Engineering & Management Studies & Research, Thane

Course: B.E. Computer Engineering

Year: Second Year

Project: FSJP Mini Project

Academic Year: 2026–27

Project Guide: Dr. Nita Patil
