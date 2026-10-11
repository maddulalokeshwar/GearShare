# GearShare

GearShare is a full-stack web application that allows users to list, discover, and rent equipment from other users. It provides a centralized platform for sharing gear, managing rental requests, and building trust between users.

## Features

- **User Authentication:** Register and log in securely.
- **Gear Listings:** Create, view, and manage equipment listings.
- **Rental Requests:** Request to rent equipment and manage rental requests.
- **Notifications:** Receive notifications for relevant activities.
- **Reviews:** Share feedback and reviews to build trust.
- **Profile Management:** View and update user profile information.
- **Database Persistence:** Store application data in MySQL.
- **Secure Password Storage:** Hash passwords using BCrypt.

## Tech Stack

### Frontend
- React
- Vite
- JavaScript
- CSS
- Axios

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Maven
- BCrypt password hashing

### Database
- MySQL

## Project Structure

```text
GearShare/
├── backend/          # Spring Boot REST API
├── frontend/         # React frontend
├── README.md
└── .gitignore
```

## Prerequisites

Install the following before running the project:

- Java JDK
- Node.js and npm
- MySQL Server
- Maven, or use the Maven Wrapper if included
- IntelliJ IDEA or another Java IDE
- Visual Studio Code or another frontend editor

## Database Setup

1. Start your MySQL Server.
2. Open MySQL Workbench or another MySQL client.
3. Create the project database:

```sql
CREATE DATABASE gearshare;
```

4. Configure the database connection in the Spring Boot application using your local MySQL username, password, and database URL.
5. Ensure your local database credentials are not committed to GitHub.

The database URL commonly follows this format:

```text
jdbc:mysql://localhost:3306/gearshare
```

Use the database settings expected by your project's existing configuration.

## Running the Backend

Open a terminal in the `backend` directory:

```powershell
cd D:\GearShare\backend
```

If the Maven Wrapper is present, run:

```powershell
.\mvnw.cmd spring-boot:run
```

Otherwise, if Maven is installed, run:

```powershell
mvn spring-boot:run
```

The backend will typically run at:

```text
http://localhost:8080
```

## Running the Frontend

Open another terminal:

```powershell
cd D:\GearShare\frontend
```

Install the dependencies:

```powershell
npm install
```

Start the development server:

```powershell
npm run dev
```

Open the local URL printed by Vite, typically:

```text
http://localhost:5173
```

Keep the backend, MySQL Server, and frontend running while using the application.

## Security Notes

- Passwords are hashed before storage.
- Keep database passwords, API keys, and other secrets private.
- Do not commit `.env` files or local configuration containing credentials.
- Configure production security settings before deploying the application publicly.

## Future Improvements

Possible future enhancements include deployment, automated testing, improved search and filtering, and additional rental management features.

## Author

Developed as a full-stack project to demonstrate frontend development, REST API design, authentication, database integration, and equipment-sharing workflows.
