
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;



import java.sql.*;

public class UserProfileProgram {
    private Connection conn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UserProfileProgram::new);
    }

    public UserProfileProgram() {
        try {
            // Connect to the existing database
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydbmovies", "root", "1234");

            // Create and display the login frame
            SwingUtilities.invokeLater(this::createAndShowLoginFrame);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: Unable to connect to the database.", "Database Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void createAndShowLoginFrame() {
        JFrame frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(20);
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateUser(email, password)) {
                showUserProfileFrame(email);
                frame.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid email or password. Please try again.", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(loginButton);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dropUserMoviesTable(); // Drop the user_movies table on application close
            }
        });
    }

    private void showUserProfileFrame(String email) {
        JFrame frame = new JFrame("Watchlist");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());

        JPanel userInfoPanel = new JPanel(new GridLayout(3, 1)); // Update to accommodate additional user info
        userInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Fetch and display user information
        try {
            int userId = getUserId(email);
            PreparedStatement stmt = conn.prepareStatement("SELECT u.email, pd.full_name, pd.age " +
                    "FROM users u " +
                    "JOIN personal_details pd ON u.user_id = pd.user_id " +
                    "WHERE u.email = ?");
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String fullName = rs.getString("full_name");
                int age = rs.getInt("age");

                JLabel fullNameLabel = new JLabel("Full Name: " + fullName);
                JLabel ageLabel = new JLabel("Age: " + age);

                userInfoPanel.add(fullNameLabel);
                userInfoPanel.add(ageLabel);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JButton addMoviesButton = new JButton("Add Movies");
        JTextArea newMoviesArea = new JTextArea(10, 30); // Empty area for newly added movies
        newMoviesArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(newMoviesArea);
        userInfoPanel.add(addMoviesButton);

        //  ActionListener to the Add Movies button
        addMoviesButton.addActionListener(e -> showMovieList(email, newMoviesArea));

        panel.add(userInfoPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    private void dropUserMoviesTable() {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE IF EXISTS user_movies");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showMovieList(String email, JTextArea newMoviesArea) {
        JFrame movieListFrame = new JFrame("Available Movies");
        movieListFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());

        DefaultListModel<String> movieListModel = new DefaultListModel<>();
        JList<String> movieJList = new JList<>(movieListModel);
        JScrollPane scrollPane = new JScrollPane(movieJList);

        // Fetch available movies from the database
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT title FROM movies");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                movieListModel.addElement(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JButton addButton = new JButton("Add Movie");
        addButton.addActionListener(e -> {
            // Add selected movie to user's profile
            String selectedMovie = movieJList.getSelectedValue();
            if (selectedMovie != null) {
                addMovieToProfile(selectedMovie, email, newMoviesArea); // Pass newMoviesArea
                JOptionPane.showMessageDialog(movieListFrame, "Movie added to your profile!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(movieListFrame, "Please select a movie.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Add MouseListener to handle double-click event
        movieJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedMovie = movieJList.getSelectedValue();
                    if (selectedMovie != null) {
                        showMovieInformation(selectedMovie);
                    }
                }
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(addButton, BorderLayout.SOUTH);

        movieListFrame.add(panel);
        movieListFrame.pack();
        movieListFrame.setLocationRelativeTo(null);
        movieListFrame.setVisible(true);
    }

    private void showMovieInformation(String selectedMovie) {
        // Fetch and display detailed information about the selected movie
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM movie_information WHERE movie_id = (SELECT movie_id FROM movies WHERE title = ?)");
            stmt.setString(1, selectedMovie);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String plot = rs.getString("plot");
                String actors = rs.getString("actors");
                String awards = rs.getString("awards");

                JOptionPane.showMessageDialog(null, "Title: " + selectedMovie + "\nPlot: " + plot + "\nActors: " + actors + "\nAwards: " + awards, "Movie Information", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }




    private boolean authenticateUser(String email, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ? AND password = ?");
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    private void updateMoviesArea(String email, JTextArea newMoviesArea) {
        try {
            int userId = getUserId(email);

            // Clear the current content of the newMoviesArea
            newMoviesArea.setText("");

            // Fetch movies associated with the user from the user_movies table
            String query = "SELECT m.title FROM movies m " +
                    "JOIN user_movies um ON m.movie_id = um.movie_id " +
                    "WHERE um.user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            // Append each movie title to the newMoviesArea
            while (rs.next()) {
                String movieTitle = rs.getString("title");
                newMoviesArea.append(movieTitle + "\n");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void addMovieToProfile(String movieTitle, String email, JTextArea newMoviesArea) {
        try {
            int userId = getUserId(email);
            int movieId = getMovieId(movieTitle);

            // Create the user_movies table
            createTableUserMovies();

            // Insert the movie into the user_movies table
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO user_movies (user_id, movie_id) VALUES (?, ?)");
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();

            // Update the newMoviesArea with all movies associated with the user
            updateMoviesArea(email, newMoviesArea);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void createTableUserMovies() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS user_movies (" +
                    "user_movies_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT," +
                    "movie_id INT," +
                    "UNIQUE (user_id, movie_id)," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(movie_id)" +
                    ")";
            stmt.executeUpdate(sql);
        }
    }





    private int getUserId(String email) throws SQLException {
        int userId = -1;
        PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM users WHERE email = ?");
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            userId = rs.getInt("user_id");
        }
        return userId;
    }

    private int getMovieId(String movieTitle) throws SQLException {
        int movieId = -1;
        PreparedStatement stmt = conn.prepareStatement("SELECT movie_id FROM movies WHERE title = ?");
        stmt.setString(1, movieTitle);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            movieId = rs.getInt("movie_id");
        }
        return movieId;
    }
}
