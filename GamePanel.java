import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;

public class GamePanel extends JPanel implements ActionListener {
    // Game Phases
    private enum GamePhase { INTRO, FIGHTING, GAME_OVER }
    private GamePhase currentPhase = GamePhase.INTRO;

    Player p1, p2;
    Timer timer;
    HashSet<Integer> pressedKeys = new HashSet<>();

    // Intro Phase Variables (60 frames per second * 3 seconds = 180 total frames)
    private int introTimer = 180; 

    public GamePanel() {
        setPreferredSize(new Dimension(800, 500));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        
        p1 = new Player(150, 1); 
        p2 = new Player(550, 2); 

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressedKeys.add(e.getKeyCode());
                // Only allow combat inputs if the match has officially started
                if (currentPhase == GamePhase.FIGHTING) {
                    handleCombatInputs(e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }

    private void handleCombatInputs(int keyCode) {
        if (keyCode == KeyEvent.VK_W) p1.jump();
        if (keyCode == KeyEvent.VK_F) p1.punch();
        if (keyCode == KeyEvent.VK_G) p1.kick();

        if (keyCode == KeyEvent.VK_UP) p2.jump();
        if (keyCode == KeyEvent.VK_NUMPAD1 || keyCode == KeyEvent.VK_I) p2.punch();
        if (keyCode == KeyEvent.VK_NUMPAD2 || keyCode == KeyEvent.VK_O) p2.kick();
    }

    private void processMovement() {
        // Freeze movement during intro phase
        if (currentPhase != GamePhase.FIGHTING) {
            p1.xSpeed = 0;
            p2.xSpeed = 0;
            return;
        }

        // Player 1 Movement
        if (pressedKeys.contains(KeyEvent.VK_A)) p1.xSpeed = -5;
        else if (pressedKeys.contains(KeyEvent.VK_D)) p1.xSpeed = 5;
        else p1.xSpeed = 0;

        // Player 2 Movement
        if (pressedKeys.contains(KeyEvent.VK_LEFT)) p2.xSpeed = -5;
        else if (pressedKeys.contains(KeyEvent.VK_RIGHT)) p2.xSpeed = 5;
        else p2.xSpeed = 0;
    }

    private void checkHit(Player attacker, Player defender) {
        if (attacker.hasHitTarget) return; 

        Rectangle attackBox = attacker.getHitbox();
        if (attackBox != null && attackBox.intersects(defender.getBounds())) {
            attacker.hasHitTarget = true; 
            int damage = (attackBox.height < 40) ? 5 : 10; 
            
            defender.health -= damage;
            if (defender.health < 0) defender.health = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentPhase == GamePhase.INTRO) {
            introTimer--;
            if (introTimer <= 0) {
                currentPhase = GamePhase.FIGHTING;
            }
            // Still update players so idling animations play during intro
            p1.update();
            p2.update();
        } 
        else if (currentPhase == GamePhase.FIGHTING) {
            processMovement();
            p1.update();
            p2.update();
            checkHit(p1, p2);
            checkHit(p2, p1);

            // Check Win Condition
            if (p1.health <= 0 || p2.health <= 0) {
                currentPhase = GamePhase.GAME_OVER;
            }
        }

        repaint(); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw Stage Floor
        g.setColor(Color.GRAY);
        g.fillRect(0, 400, 800, 100);

        // Draw Characters
        p1.draw(g);
        p2.draw(g);

        // Draw HUD (UI Health Bars)
        drawHUD(g);
        
        // Render Overlays based on active phase
        if (currentPhase == GamePhase.INTRO) {
            drawIntroOverlay(g);
        } 
        else if (currentPhase == GamePhase.GAME_OVER) {
            drawGameOverOverlay(g);
        }
    }

    private void drawIntroOverlay(Graphics g) {
        g.setFont(new Font("Impact", Font.BOLD, 70));
        
        // Frames 180 to 60 (First 2 seconds): Show "READY"
        if (introTimer > 60) {
            g.setColor(Color.ORANGE);
            g.drawString("READY...", 285, 240);
        } 
        // Frames 60 to 0 (Last 1 second): Show "FIGHT!"
        else {
            g.setColor(Color.RED);
            g.drawString("FIGHT!", 310, 240);
        }
    }

    private void drawGameOverOverlay(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, 800, 500);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Impact", Font.BOLD, 50));
        String winner = p1.health <= 0 ? "PLAYER 2 WINS" : "PLAYER 1 WINS";
        g.drawString(winner, 260, 250);
        timer.stop();
    }

    private void drawHUD(Graphics g) {
        // Player 1 Health Bar
        g.setColor(Color.BLACK);
        g.fillRect(50, 30, 250, 25);
        g.setColor(Color.GREEN);
        g.fillRect(50, 30, (int)(p1.health * 2.5), 25); 
        g.setColor(Color.WHITE);
        g.drawString("P1 Power: " + p1.health + "%", 55, 47);

        // Player 2 Health Bar
        g.setColor(Color.BLACK);
        g.fillRect(500, 30, 250, 25);
        g.setColor(Color.GREEN);
        g.fillRect(500, 30, (int)(p2.health * 2.5), 25); 
        g.setColor(Color.WHITE);
        g.drawString("P2 Power: " + p2.health + "%", 505, 47);
    }
}
