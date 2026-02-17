package se.iths.fabian;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectLauncher {
    public static void main(String[] args) throws IOException, InterruptedException {
        File devDir = new File("/home/fabian/dev/java");
        List<File> projects = Arrays.stream(devDir.listFiles())
                .filter(File::isDirectory)
                .filter(f -> !f.getName().equals("project-launcher"))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            System.out.println("Inga projekt hittades i /home/fabian/dev/java");
            return;
        }

        int longestName = projects.stream()
                .map(p -> p.getName().length())
                .max(Integer::compareTo)
                .orElse(0);

        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.clear();

        int selectedIndex = 0;

        try {
            while (true) {
                TextGraphics tg = screen.newTextGraphics();

                tg.setForegroundColor(TextColor.ANSI.CYAN);
                tg.putString(2, 1, "=== STIGEN ===");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.putString(2, 2, "Choose a project to open (ENTER to open, ESC to exit)");

                for (int i = 0; i < projects.size(); i++) {
                    String name = projects.get(i).getName();
                    String format = "%-" + longestName + "s";
                    String paddedName = String.format(format, name);

                    if (i == selectedIndex) {
                        tg.setForegroundColor(TextColor.ANSI.CYAN);
                        tg.putString(2, 4 + i, "> " + paddedName);
                        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                    } else {
                        tg.putString(2, 4 + i, "   " + paddedName);
                    }
                }

                screen.refresh();

                KeyStroke keyStroke = screen.readInput();
                if (keyStroke.getKeyType() == KeyType.Escape || (keyStroke.getCharacter() != null && keyStroke.getCharacter() == 'q')) {
                    break;
                }

                if (keyStroke.getKeyType() == KeyType.ArrowDown) {
                    selectedIndex = (selectedIndex + 1) % projects.size();
                } else if (keyStroke.getKeyType() == KeyType.ArrowUp) {
                    selectedIndex = (selectedIndex - 1 + projects.size()) % projects.size();
                } else if (keyStroke.getKeyType() == KeyType.Enter) {
                    String projectPath = projects.get(selectedIndex).getAbsolutePath();
                    
                    screen.clear();
                    tg.putString(2, 2, "Startar IntelliJ för: " + projects.get(selectedIndex).getName());
                    screen.refresh();
                    
                    new ProcessBuilder("intellij-idea-ultimate-edition", projectPath).start();
                    
                    Thread.sleep(1500);
                    break;
                }
            }
        } finally {
            screen.stopScreen();
        }
    }
}
