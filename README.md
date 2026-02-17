# Stigen 🌲

A blazing-fast TUI (Terminal User Interface) project launcher to navigate and open your Java projects directly from the terminal. Built with Java and Lanterna.

## Screenshot

![Stigen Screenshot](stigen-screenshot.png)

## About The Project

`Stigen` (Swedish for "The Path") is a simple yet powerful terminal utility designed to streamline the workflow for developers with many local projects. Instead of `cd`'ing and `ls`'ing through directories, Stigen provides an interactive list of all your Java projects, allowing you to open any of them in IntelliJ IDEA with a single keystroke.

## Built With

*   [Java](https://www.java.com/)
*   [Maven](https://maven.apache.org/)
*   [Lanterna](https://github.com/mabe02/lanterna)

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

*   Java (JDK 17 or later)
*   Maven
*   IntelliJ IDEA (with the command-line launcher installed)

### Installation & Running

1.  Clone the repo (replace with your repo URL):
    ```sh
    git clone https://github.com/your-username/stigen.git
    ```
2.  Navigate to the project directory:
    ```sh
    cd stigen
    ```
3.  Compile and run the application:
    ```sh
    mvn compile exec:java -Dexec.mainClass="se.iths.fabian.ProjectLauncher"
    ```
    Or use your `stigen` alias if you have set it up!

## Usage

*   Use the **Up/Down arrow keys** to navigate the project list.
*   Press **Enter** to open the selected project in IntelliJ IDEA.
*   Press **Escape** or **'q'** to exit the application.
