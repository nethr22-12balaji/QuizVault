# QuizVault Web Application

A web-based multiplayer quiz application. Built entirely with Java SE (built-in `com.sun.net.httpserver`) and Vanilla HTML/JS/CSS.

## Compilation and Execution

Run these commands from the project root directory (`QuizVault/QuizVault`):

```bash
# Compile all Java files into the 'out' directory
javac -d out -sourcepath src src/com/quizvault/Main.java

# Run the server
java -cp out com.quizvault.Main
```

Then open your browser to `http://localhost:8080`.

## Features
- **Quizmaster & Participants**: Quizmasters can manage the question bank and start quizzes. Participants join teams and answer questions.
- **Concurrent Answering**: Teams are asked individually generated random questions concurrently.
- **Scoring**: +10 points for the correct answer, -1 for a wrong answer.
- **Persistent State**: The question bank, teams, and active quizzes are auto-saved.
- **Certificates**: Automatically generates HTML certificates for all participants when a quiz finishes.

## Folder Layout
- `src/` - Java source code
- `webroot/` - Static HTML/JS/CSS assets
- `archives/` - (Auto-generated) Completed quizzes and leaderboards
- `exports/` / `certs/` - (Auto-generated) HTML certificates
- `users.csv`, `*.dat` - (Auto-generated) Persistent data files
