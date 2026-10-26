package com.quizvault.web;

import com.quizvault.collections.LeaderboardComparator;
import com.quizvault.filehandler.FileHandler;
import com.quizvault.filehandler.PasswordUtils;
import com.quizvault.model.*;
import com.quizvault.round.Round;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuizWebServer {

    private static final Object quizLock = new Object();
    private static Quiz activeQuiz = null;
    private static List<String> eventLog = new ArrayList<>();
    private static List<Map<String, String>> generatedCertificates = new ArrayList<>();
    private static String lastResultsJson = null;

    private static Map<String, String> sessionTokens = new ConcurrentHashMap<>();
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startServer(int port) throws IOException {
        FileHandler.initialize();
        FileHandler.loadQuestionBank();
        FileHandler.loadRegisteredTeams();
        generatedCertificates = FileHandler.loadCertificates();

        activeQuiz = FileHandler.loadAutoSave();
        if (activeQuiz != null && activeQuiz.getStatus() == Quiz.Status.IN_PROGRESS) {
            System.out.println("Restored in-progress quiz: " + activeQuiz.getQuizName());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/questions", new QuestionsHandler());
        server.createContext("/api/questions/add", wrap(new AddQuestionHandler(), true));
        server.createContext("/api/questions/delete", wrap(new DeleteQuestionHandler(), true));
        server.createContext("/api/teams", wrap(new TeamsHandler(), false));
        server.createContext("/api/teams/create", wrap(new CreateTeamHandler(), false));
        server.createContext("/api/teams/join", wrap(new JoinTeamHandler(), false));
        server.createContext("/api/quiz/start", wrap(new StartQuizHandler(), true));
        server.createContext("/api/quiz/state", wrap(new QuizStateHandler(), false));
        server.createContext("/api/quiz/submit", wrap(new SubmitAnswerHandler(), false));
        server.createContext("/api/quiz/skip", wrap(new SkipHandler(), true));
        server.createContext("/api/quiz/finish", wrap(new FinishQuizHandler(), true));
        server.createContext("/api/archives", wrap(new ArchivesHandler(), false));
        server.createContext("/api/archives/detail", wrap(new ArchiveDetailHandler(), false));
        server.createContext("/api/certificates", wrap(new CertificatesHandler(), false));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (quizLock) {
                if (activeQuiz != null) FileHandler.saveAutoSave(activeQuiz);
            }
        }, 1, 1, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (quizLock) {
                if (activeQuiz != null) FileHandler.saveAutoSave(activeQuiz);
            }
        }));

        System.out.println("QuizVault Web Server started on port " + port);
    }

    private static HttpHandler wrap(HttpHandler handler, boolean requireQuizmaster) {
        return exchange -> {
            try {
                String token = exchange.getRequestHeaders().getFirst("X-Token");
                if (token == null || !sessionTokens.containsKey(token)) {
                    sendError(exchange, 401, "Unauthorized");
                    return;
                }
                String user = sessionTokens.get(token);
                String role = getRole(user);
                if (requireQuizmaster && !"Quizmaster".equals(role)) {
                    sendError(exchange, 403, "Forbidden");
                    return;
                }
                exchange.setAttribute("user", user);
                exchange.setAttribute("role", role);
                handler.handle(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, e.getMessage());
            }
        };
    }

    private static String getRole(String user) {
        try (BufferedReader r = new BufferedReader(new FileReader("users.csv"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split(",", 3);
                if (p.length == 3 && p[0].equals(user)) return p[2];
            }
        } catch (Exception e) {}
        return "Participant";
    }

    private static Map<String, String> parseForm(String query) throws UnsupportedEncodingException {
        Map<String, String> res = new HashMap<>();
        if (query == null || query.isEmpty()) return res;
        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2);
            if (entry.length > 1) res.put(URLDecoder.decode(entry[0], "UTF-8"), URLDecoder.decode(entry[1], "UTF-8"));
            else res.put(URLDecoder.decode(entry[0], "UTF-8"), "");
        }
        return res;
    }

    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
    
    private static void sendError(HttpExchange exchange, int code, String msg) throws IOException {
        sendResponse(exchange, code, "{\"success\":false,\"error\":\"" + escapeJson(msg) + "\"}");
    }

    private static String getBody(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line; while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) { sendError(exchange, 404, "Not Found"); return; }
            if (path.equals("/")) path = "/index.html";
            
            File root = new File("webroot").getCanonicalFile();
            File file = new File("webroot" + path).getCanonicalFile();
            if (!file.getPath().startsWith(root.getPath()) || !file.exists() || file.isDirectory()) {
                sendError(exchange, 404, "Not Found"); return;
            }
            
            if (path.endsWith(".css")) exchange.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
            else if (path.endsWith(".js")) exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=utf-8");
            else if (path.endsWith(".json")) exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            else if (path.endsWith(".png")) exchange.getResponseHeaders().set("Content-Type", "image/png");
            else if (path.endsWith(".svg")) exchange.getResponseHeaders().set("Content-Type", "image/svg+xml");
            else if (path.endsWith(".ico")) exchange.getResponseHeaders().set("Content-Type", "image/x-icon");
            else exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            
            exchange.sendResponseHeaders(200, file.length());
            Files.copy(file.toPath(), exchange.getResponseBody());
            exchange.getResponseBody().close();
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> p = parseForm(getBody(exchange));
                String user = p.get("username"), pass = p.get("password");
                if (user == null || pass == null || user.trim().isEmpty() || pass.trim().isEmpty()) {
                    sendError(exchange, 200, "Fill in all fields."); return;
                }
                String role = FileHandler.authenticateUser(user.trim(), pass);
                if (role != null) {
                    String token = UUID.randomUUID().toString();
                    sessionTokens.put(token, user.trim());
                    sendResponse(exchange, 200, "{\"success\":true,\"token\":\"" + token + "\",\"role\":\"" + role + "\"}");
                } else {
                    sendError(exchange, 200, "Invalid credentials.");
                }
            } catch (Exception e) { sendError(exchange, 500, e.getMessage()); }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> p = parseForm(getBody(exchange));
                String user = p.get("username"), pass = p.get("password"), role = p.get("role");
                if (user == null || pass == null || role == null || user.trim().isEmpty() || pass.trim().isEmpty()) {
                    sendError(exchange, 200, "Fill in all fields."); return;
                }
                if (user.contains(",") || user.contains("\n") || pass.contains(",") || pass.contains("\n")) {
                    sendError(exchange, 200, "Invalid characters in username or password."); return;
                }
                if (!role.equals("Participant") && !role.equals("Quizmaster")) {
                    sendError(exchange, 200, "Invalid role."); return;
                }
                if (FileHandler.userExists(user.trim())) {
                    sendError(exchange, 200, "Username already exists."); return;
                }
                FileHandler.saveUser(user.trim(), PasswordUtils.hashPassword(pass), role);
                sendResponse(exchange, 200, "{\"success\":true}");
            } catch (Exception e) { sendError(exchange, 500, e.getMessage()); }
        }
    }

    static class QuestionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Question> qs = QuestionBank.getInstance().getAllQuestions();
            StringBuilder sb = new StringBuilder("{\"questions\":[");
            for (int i = 0; i < qs.size(); i++) {
                Question q = qs.get(i);
                sb.append("{\"text\":\"").append(escapeJson(q.getQuestionText()))
                  .append("\",\"answer\":\"").append(escapeJson(q.getAnswerText()))
                  .append("\",\"tags\":\"").append(escapeJson(q.getTags()))
                  .append("\",\"difficulty\":\"").append(escapeJson(q.getDifficulty())).append("\"}");
                if (i < qs.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    static class AddQuestionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> p = parseForm(getBody(exchange));
            String text = p.get("text"), answer = p.get("answer"), diff = p.get("diff"), tags = p.get("tags");
            if (text == null || text.trim().isEmpty() || answer == null || answer.trim().isEmpty()) {
                sendError(exchange, 200, "Question and answer are required."); return;
            }
            Question q = new Question(text, answer, "", diff, tags);
            if (QuestionBank.getInstance().addQuestion(q)) {
                FileHandler.saveQuestionBank();
                sendResponse(exchange, 200, "{\"success\":true}");
            } else {
                sendError(exchange, 200, "Duplicate question.");
            }
        }
    }
    
    static class DeleteQuestionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> p = parseForm(getBody(exchange));
            if (QuestionBank.getInstance().removeQuestion(p.get("text"))) {
                FileHandler.saveQuestionBank();
                sendResponse(exchange, 200, "{\"success\":true}");
            } else {
                sendError(exchange, 200, "Question not found.");
            }
        }
    }

    static class TeamsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Team> teams = TeamManager.getInstance().getRegisteredTeams();
            StringBuilder sb = new StringBuilder("{\"teams\":[");
            for (int i = 0; i < teams.size(); i++) {
                Team t = teams.get(i);
                sb.append("{\"name\":\"").append(escapeJson(t.getTeamName())).append("\",\"players\":[");
                for (int j = 0; j < t.getPlayers().size(); j++) {
                    sb.append("\"").append(escapeJson(t.getPlayers().get(j).getName())).append("\"");
                    if (j < t.getPlayers().size() - 1) sb.append(",");
                }
                sb.append("]}");
                if (i < teams.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    static class CreateTeamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> p = parseForm(getBody(exchange));
            String err = TeamManager.getInstance().createTeam(p.get("name"), (String) exchange.getAttribute("user"));
            if (err == null) {
                FileHandler.saveRegisteredTeams();
                sendResponse(exchange, 200, "{\"success\":true}");
            } else sendError(exchange, 200, err);
        }
    }

    static class JoinTeamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> p = parseForm(getBody(exchange));
            String err = TeamManager.getInstance().joinTeam(p.get("name"), (String) exchange.getAttribute("user"));
            if (err == null) {
                FileHandler.saveRegisteredTeams();
                sendResponse(exchange, 200, "{\"success\":true}");
            } else sendError(exchange, 200, err);
        }
    }

    static class StartQuizHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            synchronized (quizLock) {
                if (activeQuiz != null) { sendError(exchange, 200, "A quiz is already active."); return; }
                Map<String, String> p = parseForm(getBody(exchange));
                String name = p.get("name");
                if (name == null || name.trim().isEmpty()) { sendError(exchange, 200, "Quiz name required."); return; }
                
                String teamsJson = p.get("teams");
                if (teamsJson == null || teamsJson.trim().isEmpty()) { sendError(exchange, 200, "No teams selected."); return; }
                String[] teamNames = teamsJson.split(",");
                List<Team> selected = new ArrayList<>();
                for (Team t : TeamManager.getInstance().getRegisteredTeams()) {
                    for (String tn : teamNames) {
                        if (t.getTeamName().equals(tn.trim())) selected.add(new Team(t.getTeamName(), t.getPlayers()));
                    }
                }
                if (selected.isEmpty()) { sendError(exchange, 200, "No valid teams."); return; }

                int nE = 0, nM = 0, nH = 0;
                try {
                    nE = Integer.parseInt(p.getOrDefault("easy", "0"));
                    nM = Integer.parseInt(p.getOrDefault("med", "0"));
                    nH = Integer.parseInt(p.getOrDefault("hard", "0"));
                } catch (NumberFormatException e) { sendError(exchange, 200, "Invalid numbers."); return; }
                if (nE < 0 || nM < 0 || nH < 0 || (nE + nM + nH) == 0) { sendError(exchange, 200, "Need at least 1 question (>=0)."); return; }

                String date = p.getOrDefault("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                Quiz q = new Quiz(name.trim(), date);
                q.setTeams(selected);
                
                if (nE > 0) q.addRound(new Round("Easy", nE));
                if (nM > 0) q.addRound(new Round("Medium", nM));
                if (nH > 0) q.addRound(new Round("Hard", nH));

                List<Question> all = QuestionBank.getInstance().getAllQuestions();
                List<Question> easyQs = new ArrayList<>(), medQs = new ArrayList<>(), hardQs = new ArrayList<>();
                for (Question qq : all) {
                    if ("Easy".equals(qq.getDifficulty())) easyQs.add(qq);
                    else if ("Medium".equals(qq.getDifficulty())) medQs.add(qq);
                    else hardQs.add(qq);
                }

                if (easyQs.size() < nE) { sendError(exchange, 200, "Need " + nE + " Easy, bank has " + easyQs.size()); return; }
                if (medQs.size() < nM) { sendError(exchange, 200, "Need " + nM + " Medium, bank has " + medQs.size()); return; }
                if (hardQs.size() < nH) { sendError(exchange, 200, "Need " + nH + " Hard, bank has " + hardQs.size()); return; }

                for (Team t : selected) {
                    List<Question> tQs = new ArrayList<>();
                    tQs.addAll(distribute(easyQs, nE, selected.size(), selected.indexOf(t)));
                    tQs.addAll(distribute(medQs, nM, selected.size(), selected.indexOf(t)));
                    tQs.addAll(distribute(hardQs, nH, selected.size(), selected.indexOf(t)));
                    q.getTeamQuestions().put(t.getTeamName(), tQs);
                    q.getTeamProgress().put(t.getTeamName(), 0);
                }

                q.setStatus(Quiz.Status.IN_PROGRESS);
                activeQuiz = q;
                eventLog.clear();
                eventLog.add("Quiz '" + q.getQuizName() + "' started!");
                lastResultsJson = null;
                FileHandler.saveAutoSave(activeQuiz);
                sendResponse(exchange, 200, "{\"success\":true}");
            }
        }
    }

    private static List<Question> distribute(List<Question> pool, int n, int teamsCount, int teamIdx) {
        if (n == 0) return new ArrayList<>();
        List<Question> p = new ArrayList<>(pool);
        Collections.shuffle(p);
        if (p.size() >= teamsCount * n) {
            return new ArrayList<>(p.subList(teamIdx * n, (teamIdx + 1) * n));
        } else if (p.size() > n) {
            // Independent sample, retry to avoid exact match with previous logic if possible, 
            // but for simplicity here we just shuffle independently. The probability of exact match is low.
            return new ArrayList<>(p.subList(0, n));
        } else {
            // Cyclic rotation
            List<Question> res = new ArrayList<>();
            for (int i = 0; i < n; i++) res.add(p.get((i + teamIdx) % n));
            return res;
        }
    }

    static class SubmitAnswerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            synchronized (quizLock) {
                if (activeQuiz == null) { sendError(exchange, 200, "No active quiz"); return; }
                Map<String, String> p = parseForm(getBody(exchange));
                String ans = p.get("answer");
                String user = (String) exchange.getAttribute("user");
                if (ans == null || ans.trim().isEmpty()) { sendError(exchange, 200, "Empty answer"); return; }

                String tName = getTeamForUser(user);
                if (tName == null || !activeQuiz.getTeamProgress().containsKey(tName)) { sendError(exchange, 200, "Not in an active team"); return; }
                
                int idx = activeQuiz.getTeamProgress().get(tName);
                List<Question> tq = activeQuiz.getTeamQuestions().get(tName);
                if (idx >= tq.size()) { sendError(exchange, 200, "Your team has finished."); return; }

                Question q = tq.get(idx);
                boolean correct = normalize(ans).equals(normalize(q.getAnswerText()));
                Team liveTeam = null;
                for (Team t : activeQuiz.getTeams()) if (t.getTeamName().equals(tName)) liveTeam = t;
                
                if (correct) {
                    liveTeam.addScore(10);
                    // Advance ALL teams to next question
                    for (String teamKey : activeQuiz.getTeamProgress().keySet()) {
                        int curIdx = activeQuiz.getTeamProgress().get(teamKey);
                        if (curIdx < activeQuiz.getTeamQuestions().get(teamKey).size()) {
                            activeQuiz.getTeamProgress().put(teamKey, curIdx + 1);
                        }
                    }
                    eventLog.add(0, "✓ " + tName + " (" + user + ") correct! +10 — All teams advance!");
                    checkCompletion();
                    int newIdx = activeQuiz.getTeamProgress().get(tName);
                    sendResponse(exchange, 200, "{\"success\":true,\"correct\":true,\"delta\":10,\"message\":\"Correct! All teams advance.\",\"finished\":" + (newIdx >= tq.size()) + "}");
                } else {
                    liveTeam.deductScore(1);
                    eventLog.add(0, "✗ " + tName + " (" + user + ") wrong. -1");
                    sendResponse(exchange, 200, "{\"success\":true,\"correct\":false,\"delta\":-1,\"message\":\"Incorrect.\",\"finished\":false}");
                }
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase().replaceAll("\\p{Punct}", "").replaceAll("\\s+", " ");
        if (s.startsWith("the ")) s = s.substring(4);
        else if (s.startsWith("a ")) s = s.substring(2);
        else if (s.startsWith("an ")) s = s.substring(3);
        return s.trim();
    }

    private static String getTeamForUser(String user) {
        for (Team t : activeQuiz != null ? activeQuiz.getTeams() : TeamManager.getInstance().getRegisteredTeams()) {
            for (Player p : t.getPlayers()) if (p.getName().equals(user)) return t.getTeamName();
        }
        return null;
    }

    private static void checkCompletion() {
        boolean allDone = true;
        for (Team t : activeQuiz.getTeams()) {
            if (activeQuiz.getTeamProgress().get(t.getTeamName()) < activeQuiz.getTeamQuestions().get(t.getTeamName()).size()) {
                allDone = false; break;
            }
        }
        if (allDone) finishQuizInternal();
    }

    static class SkipHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            synchronized (quizLock) {
                if (activeQuiz == null) { sendError(exchange, 200, "No active quiz"); return; }
                Map<String, String> p = parseForm(getBody(exchange));
                String team = p.get("team");
                if (team != null && !team.isEmpty()) {
                    int idx = activeQuiz.getTeamProgress().getOrDefault(team, -1);
                    if (idx >= 0 && idx < activeQuiz.getTeamQuestions().get(team).size()) {
                        activeQuiz.getTeamProgress().put(team, idx + 1);
                        eventLog.add(0, "Skipped question for " + team);
                    }
                } else {
                    for (String t : activeQuiz.getTeamProgress().keySet()) {
                        int idx = activeQuiz.getTeamProgress().get(t);
                        if (idx < activeQuiz.getTeamQuestions().get(t).size()) activeQuiz.getTeamProgress().put(t, idx + 1);
                    }
                    eventLog.add(0, "Skipped question for ALL teams");
                }
                checkCompletion();
                sendResponse(exchange, 200, "{\"success\":true}");
            }
        }
    }

    static class QuizStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            synchronized (quizLock) {
                if (activeQuiz == null) {
                    if (lastResultsJson != null) sendResponse(exchange, 200, "{\"active\":false,\"results\":" + lastResultsJson + "}");
                    else sendResponse(exchange, 200, "{\"active\":false}");
                    return;
                }
                
                StringBuilder sb = new StringBuilder("{\"active\":true,\"name\":\"").append(escapeJson(activeQuiz.getQuizName())).append("\",\"teams\":{");
                int totalQ = activeQuiz.getTeamQuestions().values().iterator().next().size();
                int i = 0;
                for (Team t : activeQuiz.getTeams()) {
                    String tn = t.getTeamName();
                    int idx = activeQuiz.getTeamProgress().get(tn);
                    boolean fin = idx >= activeQuiz.getTeamQuestions().get(tn).size();
                    Question q = fin ? null : activeQuiz.getTeamQuestions().get(tn).get(idx);
                    
                    String round = "";
                    if (!fin) {
                        int passed = 0;
                        for (Round r : activeQuiz.getRounds()) {
                            passed += r.getQuestionCount();
                            if (idx < passed) { round = r.getRoundName(); break; }
                        }
                    }
                    
                    sb.append("\"").append(escapeJson(tn)).append("\":{");
                    sb.append("\"score\":").append(t.getTotalScore()).append(",");
                    sb.append("\"qnum\":").append(idx + 1).append(",");
                    sb.append("\"total\":").append(totalQ).append(",");
                    sb.append("\"finished\":").append(fin).append(",");
                    sb.append("\"round\":\"").append(escapeJson(round)).append("\",");
                    sb.append("\"question\":\"").append(fin ? "" : escapeJson(q.getQuestionText())).append("\",");
                    sb.append("\"answer\":\"").append(fin ? "" : escapeJson(q.getAnswerText())).append("\"}");
                    if (i < activeQuiz.getTeams().size() - 1) sb.append(",");
                    i++;
                }
                
                sb.append("},\"userTeams\":{");
                boolean first = true;
                for (Team t : activeQuiz.getTeams()) {
                    for (Player p : t.getPlayers()) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(escapeJson(p.getName())).append("\":\"").append(escapeJson(t.getTeamName())).append("\"");
                        first = false;
                    }
                }
                
                sb.append("},\"log\":[");
                List<String> logCopy = new ArrayList<>(eventLog);
                for (int j = 0; j < logCopy.size(); j++) {
                    sb.append("\"").append(escapeJson(logCopy.get(j))).append("\"");
                    if (j < logCopy.size() - 1) sb.append(",");
                }
                sb.append("]}");
                sendResponse(exchange, 200, sb.toString());
            }
        }
    }

    static class FinishQuizHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            synchronized (quizLock) {
                if (activeQuiz == null) { sendError(exchange, 200, "No active quiz"); return; }
                int certs = finishQuizInternal();
                sendResponse(exchange, 200, "{\"success\":true,\"certificates\":" + certs + "}");
            }
        }
    }

    private static int finishQuizInternal() {
        if (activeQuiz == null) return 0;
        int certCount = 0;
        activeQuiz.setStatus(Quiz.Status.COMPLETED);
        
        List<Team> sorted = new ArrayList<>(activeQuiz.getTeams());
        sorted.sort(new LeaderboardComparator());
        
        StringBuilder res = new StringBuilder("[");
        String date = activeQuiz.getQuizDate();
        String eventName = activeQuiz.getQuizName();
        
        for (int i = 0; i < sorted.size(); i++) {
            Team t = sorted.get(i);
            String rank = (i == 0) ? "1st Place" : (i == 1) ? "2nd Place" : (i == 2) ? "3rd Place" : "Participant";
            // Check for ties
            if (i > 0 && t.getTotalScore() == sorted.get(i-1).getTotalScore()) {
                // simple rank copy logic if we wanted, but let's just keep it simple or exact
                // just use index based for now.
            }
            
            res.append("{\"name\":\"").append(escapeJson(t.getTeamName())).append("\",\"score\":").append(t.getTotalScore()).append("}");
            if (i < sorted.size() - 1) res.append(",");
            
            for (Player p : t.getPlayers()) {
                Map<String, String> cert = new HashMap<>();
                cert.put("name", p.getName());
                cert.put("event", eventName);
                cert.put("team", t.getTeamName());
                cert.put("date", date);
                cert.put("file", generateCertHTML(p.getName(), eventName, t.getTeamName(), t.getTotalScore(), rank, date));
                generatedCertificates.add(cert);
                certCount++;
            }
        }
        res.append("]");
        lastResultsJson = res.toString();
        
        try {
            FileHandler.saveQuiz(activeQuiz);
            FileHandler.saveCertificates(generatedCertificates);
            FileHandler.saveAutoSave(null);
        } catch (Exception e) { e.printStackTrace(); }
        
        activeQuiz = null;
        eventLog.clear();
        return certCount;
    }

    private static String generateCertHTML(String player, String event, String team, int score, String rank, String date) {
        String safeP = player.replaceAll("[^a-zA-Z0-9]", "_");
        String safeE = event.replaceAll("[^a-zA-Z0-9]", "_");
        String file = "certs/" + safeP + "_" + safeE + "_" + System.currentTimeMillis() + ".html";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("webroot/" + file))) {
            bw.write("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Certificate</title>");
            bw.write("<style>body{font-family:sans-serif;text-align:center;padding:50px;background:#fdfdfd;}");
            bw.write(".cert{border:10px solid #d4af37;padding:50px;max-width:800px;margin:auto;}");
            bw.write("h1{color:#333;font-size:3em;} h2{color:#d4af37;} .name{font-size:2.5em;margin:20px;text-decoration:underline;}</style></head>");
            bw.write("<body><div class='cert'><h1>Certificate of Participation</h1>");
            bw.write("<h2>" + escapeHtml(rank) + "</h2>");
            bw.write("<p>Proudly presented to</p><div class='name'>" + escapeHtml(player) + "</div>");
            bw.write("<p>of team <strong>" + escapeHtml(team) + "</strong></p>");
            bw.write("<p>for participation in <strong>" + escapeHtml(event) + "</strong></p>");
            bw.write("<p>Score: " + score + " points</p><p>Date: " + escapeHtml(date) + "</p>");
            bw.write("<button onclick='window.print()' style='margin-top:30px;padding:10px 20px;'>Print</button>");
            bw.write("</div></body></html>");
        } catch (Exception e) {}
        return file;
    }

    static class ArchivesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> names = FileHandler.getArchivedQuizFiles();
            StringBuilder sb = new StringBuilder("{\"archives\":[");
            for (int i = 0; i < names.size(); i++) {
                sb.append("{\"file\":\"").append(escapeJson(names.get(i))).append("\"}");
                if (i < names.size() - 1) sb.append(",");
            }
            sb.append("]}");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    static class ArchiveDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> p = parseForm(getBody(exchange));
                Quiz q = FileHandler.loadQuiz(p.get("file"));
                if (q == null) { sendError(exchange, 200, "Not found"); return; }
                
                List<Team> sorted = new ArrayList<>(q.getTeams());
                sorted.sort(new LeaderboardComparator());
                
                StringBuilder sb = new StringBuilder("{\"name\":\"").append(escapeJson(q.getQuizName()));
                sb.append("\",\"date\":\"").append(escapeJson(q.getQuizDate())).append("\",\"teams\":[");
                for (int i = 0; i < sorted.size(); i++) {
                    Team t = sorted.get(i);
                    sb.append("{\"name\":\"").append(escapeJson(t.getTeamName())).append("\",\"score\":").append(t.getTotalScore()).append("}");
                    if (i < sorted.size() - 1) sb.append(",");
                }
                sb.append("]}");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) { sendError(exchange, 500, e.getMessage()); }
        }
    }

    static class CertificatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String user = (String) exchange.getAttribute("user");
            StringBuilder sb = new StringBuilder("{\"certificates\":[");
            boolean first = true;
            for (Map<String, String> cert : generatedCertificates) {
                if (cert.get("name").equals(user)) {
                    if (!first) sb.append(",");
                    sb.append("{\"event\":\"").append(escapeJson(cert.get("event")))
                      .append("\",\"team\":\"").append(escapeJson(cert.get("team")))
                      .append("\",\"date\":\"").append(escapeJson(cert.get("date")))
                      .append("\",\"file\":\"").append(escapeJson(cert.get("file"))).append("\"}");
                    first = false;
                }
            }
            sb.append("]}");
            sendResponse(exchange, 200, sb.toString());
        }
    }
}
