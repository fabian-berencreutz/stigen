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

        boolean needsRedraw = true;
        try {
            while (true) {
                if (needsRedraw) {
                    TextGraphics tg = screen.newTextGraphics();

                    // --- Rit-logik ---
                    tg.setForegroundColor(TextColor.ANSI.CYAN);
                    tg.putString(2, 1, "=== STIGEN ===");
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.putString(2, 2, "(/ to search, ESC to cancel, Q to exit, ENTER to open)");

                    // --- Projektlista ---
                    int listStartRow = 4;
                    int searchLineRow = screen.getTerminalSize().getRows() - 2;
                    // Rensa listområdet
                    for (int row = listStartRow; row < searchLineRow; row++) {
                        tg.putString(0, row, " ".repeat(screen.getTerminalSize().getColumns()));
                    }
                    // Rita listan
                    if (displayedProjects.isEmpty() && currentMode == Mode.SEARCH && !searchQuery.isEmpty()) {
                        tg.setForegroundColor(TextColor.ANSI.RED);
                        tg.putString(2, listStartRow, "No projects found matching '" + searchQuery + "'");
                        tg.setForegroundColor(TextColor.ANSI.WHITE); // Återställ färgen
                    } else if (displayedProjects.isEmpty() && !allProjects.isEmpty()){ // If allProjects is not empty, but displayed is empty (e.g., initial state with no projects)
                         tg.setForegroundColor(TextColor.ANSI.RED);
                        tg.putString(2, listStartRow, "No projects found in /home/fabian/dev/java");
                        tg.setForegroundColor(TextColor.ANSI.WHITE); // Återställ färgen
                    } else if (allProjects.isEmpty()){
                         tg.setForegroundColor(TextColor.ANSI.RED);
                        tg.putString(2, listStartRow, "No projects found in /home/fabian/dev/java");
                        tg.setForegroundColor(TextColor.ANSI.WHITE); // Återställ färgen
                    } else {
                        for (int i = 0; i < displayedProjects.size(); i++) {
                            if (listStartRow + i >= searchLineRow) {
                                break; // Rita inte utanför listområdet
                            }
                            String name = displayedProjects.get(i).getName();
                            String format = "%-" + longestName + "s";
                            String paddedName = String.format(format, name);

                            if (i == selectedIndex) {
                                tg.setForegroundColor(TextColor.ANSI.CYAN);
                                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                                tg.putString(2, listStartRow + i, ">  " + paddedName);
                            } else {
                                tg.setForegroundColor(TextColor.ANSI.WHITE);
                                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                                tg.putString(2, listStartRow + i, "   " + paddedName);
                            }
                        }
                    }

                    // --- Sökfält ---
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.putString(0, searchLineRow, " ".repeat(screen.getTerminalSize().getColumns()));
                    if (currentMode == Mode.SEARCH) {
                        tg.putString(2, searchLineRow, "/" + searchQuery);
                        screen.setCursorPosition(new TerminalPosition(2 + 1 + searchQuery.length(), searchLineRow));
                    } else {
                        screen.setCursorPosition(null);
                    }

                    // --- Statusrad ---
                    int statusBarRow = screen.getTerminalSize().getRows() - 1;
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.CYAN);
                    String statusBarText = String.format(" MODE: %-7s | PROJECTS: %d / %d", currentMode, displayedProjects.size(), allProjects.size());
                    tg.putString(0, statusBarRow, " ".repeat(screen.getTerminalSize().getColumns()));
                    tg.putString(1, statusBarRow, statusBarText);
                    
                    // Återställ färger för säkerhets skull
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);

                    screen.refresh();
                    needsRedraw = false;
                }

                // --- Input-hantering ---
                KeyStroke keyStroke = screen.readInput();
                KeyType keyType = keyStroke.getKeyType();

                if (currentMode == Mode.SEARCH) {
                    if (keyType == KeyType.Escape) {
                        currentMode = Mode.NORMAL;
                        searchQuery = "";
                        displayedProjects = allProjects; // Återställ listan
                        needsRedraw = true;
                    } else if (keyType == KeyType.Enter) {
                        currentMode = Mode.NORMAL;
                        needsRedraw = true; // To hide the search bar
                    } else if (keyType == KeyType.Backspace && !searchQuery.isEmpty()) {
                        searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                        final String currentQuery = searchQuery.toLowerCase();
                        displayedProjects = allProjects.stream()
                                .filter(p -> p.getName().toLowerCase().contains(currentQuery))
                                .collect(Collectors.toList());
                        selectedIndex = 0;
                        needsRedraw = true;
                    } else if (keyType == KeyType.Character) {
                        searchQuery += keyStroke.getCharacter();
                        final String currentQuery = searchQuery.toLowerCase();
                        displayedProjects = allProjects.stream()
                                .filter(p -> p.getName().toLowerCase().contains(currentQuery))
                                .collect(Collectors.toList());
                        selectedIndex = 0;
                        needsRedraw = true;
                    }
                } else { // NORMAL Mode
                    if (keyType == KeyType.Character && keyStroke.getCharacter() == '/') {
                        currentMode = Mode.SEARCH;
                        searchQuery = "";
                        needsRedraw = true;
                    } else if (keyType == KeyType.ArrowDown) {
                        if (!displayedProjects.isEmpty()) {
                            selectedIndex = (selectedIndex + 1) % displayedProjects.size();
                            needsRedraw = true;
                        }
                    } else if (keyType == KeyType.ArrowUp) {
                        if (!displayedProjects.isEmpty()) {
                            selectedIndex = (selectedIndex - 1 + displayedProjects.size()) % displayedProjects.size();
                            needsRedraw = true;
                        }
                    } else if (keyType == KeyType.Enter && !displayedProjects.isEmpty()) {
                        String projectPath = displayedProjects.get(selectedIndex).getAbsolutePath();
                        screen.clear();
                        TextGraphics tg = screen.newTextGraphics();
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
                            needsRedraw = true;
                        } else { // No active filter, so do nothing
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
