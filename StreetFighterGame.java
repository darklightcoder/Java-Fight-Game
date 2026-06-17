import javax.swing.JFrame;

public class StreetFighterGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Java 2D Street Fighter Arc");
        GamePanel gamePanel = new GamePanel();
        
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center window
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
