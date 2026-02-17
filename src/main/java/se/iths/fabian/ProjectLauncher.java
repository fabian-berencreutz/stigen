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
import java.util.Collections;
import java.util.stream.Collectors;

public class ProjectLauncher {

    private static StigenConfig config;

    // Vi definierar två lägen för vår applikation
    private enum Mode {
        NORMAL,
        SEARCH,
        SETTINGS
    }

    private static List<File> loadProjects(File devDir) {
        if (!devDir.exists() || !devDir.isDirectory()) {
            return Collections.emptyList();
        }
        return Arrays.stream(devDir.listFiles())
                .filter(File::isDirectory)
                .filter(f -> !f.getName().equals("project-launcher"))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        config = new StigenConfig();

        String projectsRootPath;
        if (args.length > 0) {
            projectsRootPath = args[0];
        } else {
            projectsRootPath = config.getDefaultDirectory();
            if (projectsRootPath == null) {
                projectsRootPath = System.getProperty("user.home");
            }
        }

        File devDir = new File(projectsRootPath);
        if (!devDir.exists() || !devDir.isDirectory()) {
            System.err.println("Error: The specified path '" + projectsRootPath + "' is not a valid directory.");
            return;
        }

        List<File> allProjects = loadProjects(devDir);

        // --- State Variables ---
        Mode currentMode = Mode.NORMAL;
        String searchQuery = "";
        String settingsPathInput = devDir.getAbsolutePath(); // Initialize with current path
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
                    tg.setForegroundColor(TextColor.ANSI.CYAN);
                    tg.putString(2, 1, "=== STIGEN ===");
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.putString(2, 2, "(s for settings, / to search, ESC to cancel, q to exit, ENTER to open)");

                    // Clear main content area
                    int listStartRow = 4;
                    int searchLineRow = screen.getTerminalSize().getRows() - 2;
                    for (int row = listStartRow; row < searchLineRow; row++) {
                        tg.putString(0, row, " ".repeat(screen.getTerminalSize().getColumns()));
                    }

                    if (currentMode == Mode.SETTINGS) {
                        // Settings Screen Drawing (initial version)
                        tg.putString(2, listStartRow, "--- SETTINGS ---");
                        tg.putString(2, listStartRow + 2, "Default Directory:");
                        
                        File currentPathCheck = new File(settingsPathInput);
                        if (currentPathCheck.exists() && currentPathCheck.isDirectory()) {
                            tg.setForegroundColor(TextColor.ANSI.GREEN);
                        } else {
                            tg.setForegroundColor(TextColor.ANSI.RED);
                        }
                        tg.putString(2, listStartRow + 3, " " + settingsPathInput + " ");
                        
                        tg.setForegroundColor(TextColor.ANSI.WHITE); // Reset color
                        tg.putString(2, listStartRow + 5, "(Type to change, ENTER to save, ESC to cancel)");


                        screen.setCursorPosition(new TerminalPosition(3 + settingsPathInput.length(), listStartRow + 3));

                    } else {
                        // --- Projektlista ---
                        if (allProjects.isEmpty()){
                            tg.setForegroundColor(TextColor.ANSI.RED);
                            tg.putString(2, listStartRow, "No projects found in " + devDir.getAbsolutePath());
                            tg.setForegroundColor(TextColor.ANSI.WHITE);
                        }
                        else if (displayedProjects.isEmpty() && currentMode == Mode.SEARCH && !searchQuery.isEmpty()) {
                            tg.setForegroundColor(TextColor.ANSI.RED);
                            tg.putString(2, listStartRow, "No projects found matching '" + searchQuery + "'");
                            tg.setForegroundColor(TextColor.ANSI.WHITE);
                        } else {
                            for (int i = 0; i < displayedProjects.size(); i++) {
                                if (listStartRow + i >= searchLineRow) {
                                    break;
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
                    }

                    // --- Sök/Status-rad ---
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.putString(0, searchLineRow, " ".repeat(screen.getTerminalSize().getColumns()));
                    if (currentMode == Mode.SEARCH) {
                        tg.putString(2, searchLineRow, "/" + searchQuery);
                        screen.setCursorPosition(new TerminalPosition(2 + 1 + searchQuery.length(), searchLineRow));
                    } else if (currentMode != Mode.SETTINGS){
                        screen.setCursorPosition(null);
                    }

                    // --- Statusrad ---
                    int statusBarRow = screen.getTerminalSize().getRows() - 1;
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.CYAN);
                    String statusBarText = String.format("PATH: %s | PROJECTS: %d / %d", devDir.getAbsolutePath(), displayedProjects.size(), allProjects.size());
                    tg.putString(0, statusBarRow, " ".repeat(screen.getTerminalSize().getColumns()));
                    tg.putString(1, statusBarRow, statusBarText);

                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);

                    screen.refresh();
                    needsRedraw = false;
                }

                // --- Input-hantering ---
                KeyStroke keyStroke = screen.readInput();
                KeyType keyType = keyStroke.getKeyType();

                if (currentMode == Mode.SETTINGS) {
                    if (keyType == KeyType.Escape) {
                        currentMode = Mode.NORMAL;
                        settingsPathInput = devDir.getAbsolutePath(); // Reset changes
                        needsRedraw = true;
                    } else if (keyType == KeyType.Enter) {
                        File newDir = new File(settingsPathInput);
                        if (newDir.exists() && newDir.isDirectory()) {
                            config.setDefaultDirectory(settingsPathInput);
                            config.save();
                            devDir = newDir; // Update our main directory file
                            
                            // Reload projects
                            allProjects = loadProjects(devDir);
                            displayedProjects = allProjects;
                            selectedIndex = 0;
                            longestName = displayedProjects.stream().map(p -> p.getName().length()).max(Integer::compareTo).orElse(0);

                            currentMode = Mode.NORMAL; // Go back to normal mode
                            needsRedraw = true;
                        } else {
                            // Optional: Add a visual cue that the path is invalid, e.g., flash the screen
                            terminal.bell();
                        }
                    } else if (keyType == KeyType.Backspace && !settingsPathInput.isEmpty()) {
                        settingsPathInput = settingsPathInput.substring(0, settingsPathInput.length() - 1);
                        needsRedraw = true;
                    } else if (keyType == KeyType.Character) {
                        settingsPathInput += keyStroke.getCharacter();
                        needsRedraw = true;
                    }
                } else if (currentMode == Mode.SEARCH) {
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
                    if (keyType == KeyType.Character && (keyStroke.getCharacter() == 's' || keyStroke.getCharacter() == 'S')) {
                        currentMode = Mode.SETTINGS;
                        settingsPathInput = devDir.getAbsolutePath(); // Start with current path
                        needsRedraw = true;
                    } else if (keyType == KeyType.Character && keyStroke.getCharacter() == '/') {
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
                    } else if (keyType == KeyType.Escape) {
                        if (!searchQuery.isEmpty()) {
                            searchQuery = "";
                            displayedProjects = allProjects;
                            selectedIndex = 0;
                            needsRedraw = true;
                        } else {
                            // Do nothing
                        }
                    } else if (keyType == KeyType.Character && (keyStroke.getCharacter() == 'q' || keyStroke.getCharacter() == 'Q')) {
                        break;
                    }
                }
            }
        } finally {
            screen.stopScreen();
        }
    }
}
