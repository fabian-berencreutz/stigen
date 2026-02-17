package se.iths.fabian;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectLauncher {

    // Vi definierar två lägen för vår applikation
    private enum Mode {
        NORMAL,
        SEARCH
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        File devDir = new File("/home/fabian/dev/java");
        final List<File> allProjects = Arrays.stream(devDir.listFiles())
                .filter(File::isDirectory)
                .filter(f -> !f.getName().equals("project-launcher"))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        if (allProjects.isEmpty()) {
            System.out.println("No projects found in /home/fabian/dev/java");
            return;
        }

        // --- State Variables ---
        Mode currentMode = Mode.NORMAL;
        String searchQuery = "";
        List<File> displayedProjects = allProjects;
        int selectedIndex = 0;
        int longestName = displayedProjects.stream().map(p -> p.getName().length()).max(Integer::compareTo).orElse(0);

        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.clear();

        try {
            while (true) {
                screen.clear(); // Rensa för varje ny rendering
                TextGraphics tg = screen.newTextGraphics();

                // --- Rit-logik ---
                tg.setForegroundColor(TextColor.ANSI.CYAN);
                tg.putString(2, 1, "=== STIGEN ===");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.putString(2, 2, "(/ to search, ESC to cancel, Q to exit, ENTER to open)");

                        for (int i = 0; i < displayedProjects.size(); i++) {
                            String name = displayedProjects.get(i).getName();
                            String format = "%-" + longestName + "s";
                            String paddedName = String.format(format, name);
                
                            if (i == selectedIndex) {
                                tg.setForegroundColor(TextColor.ANSI.CYAN);
                                tg.setBackgroundColor(TextColor.ANSI.DEFAULT); // Se till att bakgrunden är standard
                                tg.putString(2, 4 + i, ">> " + paddedName);
                            } else {
                                tg.setForegroundColor(TextColor.ANSI.WHITE); // Sätt explicit till vit för omarkerade
                                tg.setBackgroundColor(TextColor.ANSI.DEFAULT); // Se till att bakgrunden är standard
                                tg.putString(2, 4 + i, "   " + paddedName);
                            }
                        }
                // Visa sökrutan om vi är i SEARCH-läge
                if (currentMode == Mode.SEARCH) {
                    tg.putString(2, screen.getTerminalSize().getRows() - 2, "/" + searchQuery);
                    screen.setCursorPosition(new TerminalPosition(2 + 1 + searchQuery.length(), screen.getTerminalSize().getRows() - 2));
                } else {
                    screen.setCursorPosition(null); // Göm markören i NORMAL-läge
                }

                screen.refresh();

                // --- Input-hantering ---
                KeyStroke keyStroke = screen.readInput();
                KeyType keyType = keyStroke.getKeyType();

                if (currentMode == Mode.SEARCH) {
                    if (keyType == KeyType.Escape) {
                        currentMode = Mode.NORMAL;
                        searchQuery = "";
                        displayedProjects = allProjects; // Återställ listan
                    } else if (keyType == KeyType.Enter) {
                        currentMode = Mode.NORMAL;
                    } else if (keyType == KeyType.Backspace && !searchQuery.isEmpty()) {
                        searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                        final String currentQuery = searchQuery.toLowerCase(); // <-- FIX
                        displayedProjects = allProjects.stream()
                                .filter(p -> p.getName().toLowerCase().contains(currentQuery))
                                .collect(Collectors.toList());
                        selectedIndex = 0;
                    } else if (keyType == KeyType.Character) {
                        searchQuery += keyStroke.getCharacter();
                        final String currentQuery = searchQuery.toLowerCase(); // <-- FIX
                        displayedProjects = allProjects.stream()
                                .filter(p -> p.getName().toLowerCase().contains(currentQuery))
                                .collect(Collectors.toList());
                        selectedIndex = 0;
                    }
                } else { // NORMAL Mode
                    if (keyType == KeyType.Character && keyStroke.getCharacter() == '/') {
                        currentMode = Mode.SEARCH;
                        searchQuery = "";
                    } else if (keyType == KeyType.ArrowDown) {
                        if (!displayedProjects.isEmpty()) {
                            selectedIndex = (selectedIndex + 1) % displayedProjects.size();
                        }
                    } else if (keyType == KeyType.ArrowUp) {
                        if (!displayedProjects.isEmpty()) {
                            selectedIndex = (selectedIndex - 1 + displayedProjects.size()) % displayedProjects.size();
                        }
                    } else if (keyType == KeyType.Enter && !displayedProjects.isEmpty()) {
                        String projectPath = displayedProjects.get(selectedIndex).getAbsolutePath();
                        screen.clear();
                        tg.putString(2, 2, "Starting IntelliJ for: " + displayedProjects.get(selectedIndex).getName());
                        screen.refresh();
                        new ProcessBuilder("intellij-idea-ultimate-edition", projectPath).start();
                        Thread.sleep(1500);
                        break;
                    } else if (keyType == KeyType.Escape) { // Handle Escape differently
                        if (!searchQuery.isEmpty()) { // If there's an active filter
                            searchQuery = "";
                            displayedProjects = allProjects; // Clear filter
                            selectedIndex = 0; // Reset selection
                        } else { // No active filter, so exit app
                            // Do nothing
                        }
                    } else if (keyType == KeyType.Character && keyStroke.getCharacter() == 'q') {
                        break; // 'q' always exits
                    }
                }
            }
        } finally {
            screen.stopScreen();
        }
    }
}
