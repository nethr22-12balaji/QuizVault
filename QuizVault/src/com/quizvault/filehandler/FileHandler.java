package com.quizvault.filehandler;

import com.quizvault.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileHandler {

    private static final String ARCHIVE_DIR = "archives/";
    private static final String USERS_FILE = "users.csv";
    private static final String BANK_FILE = "question_bank.dat";
    private static final String TEAMS_FILE = "teams.dat";
    private static final String CERTS_FILE = "certificates.dat";
    private static final String AUTOSAVE_FILE = "autosave.dat";

    private static final Object lock = new Object();

    public static void initialize() {
        System.out.println("Working Directory: " + new File(".").getAbsolutePath());
        new File(ARCHIVE_DIR).mkdirs();
        new File("webroot/certs").mkdirs();
        try { new File(USERS_FILE).createNewFile(); } catch (IOException e) {}
    }

    private static void writeAtomic(String filename, Object data) throws IOException {
        synchronized (lock) {
            File temp = new File(filename + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(temp))) {
                oos.writeObject(data);
            }
            Files.move(temp.toPath(), new File(filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Object readSafe(String filename) throws IOException, ClassNotFoundException {
        synchronized (lock) {
            File f = new File(filename);
            if (!f.exists() || f.length() == 0) return null;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                return ois.readObject();
            } catch (EOFException | InvalidClassException e) {
                System.err.println("Warning: Could not load " + filename + " (" + e.getClass().getSimpleName() + "). Starting fresh.");
                return null;
            }
        }
    }

    public static void saveQuestionBank() throws IOException {
        writeAtomic(BANK_FILE, QuestionBank.getInstance().getAllQuestions());
    }

    @SuppressWarnings("unchecked")
    public static void loadQuestionBank() {
        try {
            Object data = readSafe(BANK_FILE);
            if (data instanceof List) QuestionBank.getInstance().setQuestions((List<Question>) data);
        } catch (Exception e) {}
    }

    public static void saveRegisteredTeams() throws IOException {
        writeAtomic(TEAMS_FILE, TeamManager.getInstance().getRegisteredTeams());
    }

    @SuppressWarnings("unchecked")
    public static void loadRegisteredTeams() {
        try {
            Object data = readSafe(TEAMS_FILE);
            if (data instanceof List) TeamManager.getInstance().setTeams((List<Team>) data);
        } catch (Exception e) {}
    }
    
    public static void saveCertificates(List<Map<String, String>> certs) throws IOException {
        writeAtomic(CERTS_FILE, new ArrayList<>(certs));
    }
    
    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> loadCertificates() {
        try {
            Object data = readSafe(CERTS_FILE);
            if (data instanceof List) return (List<Map<String, String>>) data;
        } catch (Exception e) {}
        return new ArrayList<>();
    }
    
    public static void saveAutoSave(Quiz q) {
        try { if (q != null) writeAtomic(AUTOSAVE_FILE, q); else new File(AUTOSAVE_FILE).delete(); } catch (Exception e) {}
    }
    
    public static Quiz loadAutoSave() {
        try {
            Object data = readSafe(AUTOSAVE_FILE);
            if (data instanceof Quiz) return (Quiz) data;
        } catch (Exception e) {}
        return null;
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public static void saveQuiz(Quiz quiz) throws IOException {
        synchronized (lock) {
            String safeName = sanitizeFileName(quiz.getQuizName());
            String date = quiz.getQuizDate().replaceAll("[^a-zA-Z0-9-]", "");
            int counter = 1;
            File f = new File(ARCHIVE_DIR + safeName + "_" + date + ".dat");
            while (f.exists()) {
                f = new File(ARCHIVE_DIR + safeName + "_" + date + "_" + (counter++) + ".dat");
            }
            writeAtomic(f.getPath(), quiz);
        }
    }

    public static List<String> getArchivedQuizFiles() {
        synchronized (lock) {
            List<String> list = new ArrayList<>();
            File dir = new File(ARCHIVE_DIR);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".dat"));
            if (files != null) {
                for (File f : files) list.add(f.getName());
            }
            return list;
        }
    }

    public static Quiz loadQuiz(String filename) throws IOException, ClassNotFoundException {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) throw new IOException("Invalid filename");
        synchronized (lock) {
            Object data = readSafe(ARCHIVE_DIR + filename);
            if (data instanceof Quiz) return (Quiz) data;
            return null;
        }
    }

    public static String authenticateUser(String username, String password) throws IOException {
        synchronized (lock) {
            try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 3);
                    if (parts.length == 3 && parts[0].equals(username)) {
                        String storedHash = parts[1];
                        String role = parts[2];
                        if (PasswordUtils.verifyPassword(password, storedHash)) {
                            // Upgrade old hash if necessary
                            if (!storedHash.contains(":")) updatePasswordHash(username, password);
                            return role;
                        }
                    }
                }
            }
            return null;
        }
    }

    private static void updatePasswordHash(String username, String plainPassword) {
        // Rewrite file with new hash
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(USERS_FILE))) {
                String l;
                while ((l = r.readLine()) != null) {
                    String[] p = l.split(",", 3);
                    if (p.length == 3 && p[0].equals(username)) {
                        lines.add(p[0] + "," + PasswordUtils.hashPassword(plainPassword) + "," + p[2]);
                    } else {
                        lines.add(l);
                    }
                }
            }
            File temp = new File(USERS_FILE + ".tmp");
            try (BufferedWriter w = new BufferedWriter(new FileWriter(temp))) {
                for (String l : lines) w.write(l + "\n");
            }
            Files.move(temp.toPath(), new File(USERS_FILE).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {}
    }

    public static boolean userExists(String username) throws IOException {
        synchronized (lock) {
            try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 3);
                    if (parts.length > 0 && parts[0].equals(username)) return true;
                }
            }
            return false;
        }
    }

    public static void saveUser(String username, String hashedPassword, String role) throws IOException {
        synchronized (lock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
                writer.write(username + "," + hashedPassword + "," + role + "\n");
            }
        }
    }
}