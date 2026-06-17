
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Player {
    // --- 1. Sprite Configuration ---
    private static final int TILE_SIZE = 80; // Standardized cell dimensions (80x80)
    private static final int ANIM_SPEED = 8;  

    // --- 2. Positioning & Physics ---
    int x, y;
    int renderWidth = 128, renderHeight = 128; 
    int xSpeed = 0, ySpeed = 0;
    int health = 100, id;

    private final int GRAVITY = 1;
    private final int GROUND_Y = 400;

    // --- 3. Animation & State Tracking ---
    private BufferedImage masterSheet;
    private BufferedImage currentSprite;
    private PlayerState currentState = PlayerState.IDLE;
    private int animationFrame = 0;
    private int animationTick = 0; 
    
    // Combat flag to ensure attacks only land damage once per swing sequence
    public boolean hasHitTarget = false;
    
    // Rigid pixel lookups for the 4 frame columns based on 80px cell size
    private final int[] frameCoordinatesX = {0, 80, 160, 240};

    public Player(int x, int id) {
        this.x = x;
        this.y = GROUND_Y - this.renderHeight;
        this.id = id;
        loadSpritesheet();
    }

    private void loadSpritesheet() {
        try {
            masterSheet = ImageIO.read(new File("player_spritesheet.png"));
            
            if (this.id == 2) {
                float[] scales = {2.0f, 0.5f, 0.5f, 1.0f}; 
                float[] offsets = new float[4];
                RescaleOp rop = new RescaleOp(scales, offsets, null);
                
                BufferedImage filtered = new BufferedImage(
                    masterSheet.getWidth(), 
                    masterSheet.getHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                
                Graphics2D g2d = filtered.createGraphics();
                g2d.drawImage(masterSheet, 0, 0, null);
                g2d.dispose();
                
                masterSheet = rop.filter(filtered, null);
            }
            
            updateAnimationFrame();
            
        } catch (IOException e) {
            System.out.println("Error: Could not load player_spritesheet.png");
            System.out.println("Java is looking for the image here: " + new File("player_spritesheet.png").getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void update() {
        // --- 1. Physics & Movement Calculations ---
        x += xSpeed;
        if (x < 0) x = 0;
        if (x > 800 - renderWidth) x = 800 - renderWidth; 

        y += ySpeed;
        boolean isGrounded = true;

        if (y < GROUND_Y - renderHeight) {
            ySpeed += GRAVITY;
            isGrounded = false;
        } else {
            y = GROUND_Y - renderHeight;
            ySpeed = 0;
        }

        // --- 2. State Machine System ---
        if (isAttacking()) {
            // Let the animation sequence complete naturally; do not overwrite state here
        } else if (!isGrounded) {
            currentState = PlayerState.JUMPING;
        } else if (xSpeed != 0) {
            currentState = PlayerState.WALKING;
        } else {
            currentState = PlayerState.IDLE;
        }

        // --- 3. Animation Tick Engine ---
        animationTick++;
        if (animationTick >= ANIM_SPEED) {
            animationTick = 0;
            animationFrame++;
            updateAnimationFrame();
        }
        

        
    }

    /**
     * Slices the correct 80x80 frame based on clean state validation.
     */
    private void updateAnimationFrame() {
        if (masterSheet == null) return;

        // One-shot action overrides validation checked BEFORE slice bounds calculation
        if (!isLoopingState()) {
            if (animationFrame >= 4) {
                currentState = PlayerState.IDLE;
                animationFrame = 0;
                hasHitTarget = false; // Reset fighting damage allowance for next strike
            }
        } else {
            animationFrame %= 4;
        }

        // Safe array boundary check fallback
        if (animationFrame < 0 || animationFrame >= frameCoordinatesX.length) {
            animationFrame = 0;
        }

        // Row mapping configuration matching layout rows
        int row = 0; 
        switch (currentState) {
            case WALKING:  row = 1; break;
            case JUMPING:  row = 2; break;
            case PUNCHING: row = 3; break;
            case KICKING:  row = 4; break;
            case IDLE:     row = 0; break;
        }

        int srcX = frameCoordinatesX[animationFrame];
        int srcY = row * 64; // FIXED: Changed 64 to TILE_SIZE (80) to perfectly align with grid rows

        // Crop matching dimensions (80x80)
        currentSprite = masterSheet.getSubimage(srcX, srcY, TILE_SIZE, 64);
    }

    public void draw(Graphics g) {
        if (currentSprite != null) {
            // Flip P2 horizontally so they face left towards Player 1
            if (this.id == 2) {
                g.drawImage(currentSprite, x + renderWidth, y, -renderWidth, renderHeight, null);
            } else {
                g.drawImage(currentSprite, x, y, renderWidth, renderHeight, null);
            }
        } else {
            g.setColor(id == 1 ? Color.BLUE : Color.RED);
            g.fillRect(x, y, renderWidth, renderHeight);
        }
        
        // --- Hitbox Debug Render Overlay ---
        if (isAttacking()) {
            g.setColor(new Color(255, 255, 0, 100)); 
            Rectangle hitbox = getHitbox();
            if (hitbox != null) {
                g.fillRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
            }
        }
    }

    // --- State Helpers ---
    public boolean isAttacking() {
        return currentState == PlayerState.PUNCHING || currentState == PlayerState.KICKING;
    }

    private boolean isLoopingState() {
        return currentState == PlayerState.IDLE || currentState == PlayerState.WALKING || currentState == PlayerState.JUMPING;
    }

    public void punch() {
        if (!isAttacking() && y >= GROUND_Y - renderHeight) { 
            currentState = PlayerState.PUNCHING;
            animationFrame = 0; 
            animationTick = 0;
            hasHitTarget = false; 
            updateAnimationFrame(); 
        }
    }

    public void kick() {
        if (!isAttacking() && y >= GROUND_Y - renderHeight) { 
            currentState = PlayerState.KICKING;
            animationFrame = 0; 
            animationTick = 0;
            hasHitTarget = false; 
            updateAnimationFrame(); 
        }
    }

    public void jump() {
        if (y >= GROUND_Y - renderHeight) { 
            ySpeed = -20; 
            currentState = PlayerState.JUMPING;
            animationFrame = 0;
            animationTick = 0;
        }
    }

    // --- Collisions Scaled to 128x128 ---
    public Rectangle getBounds() {
        return new Rectangle(x + (renderWidth / 4), y, renderWidth / 2, renderHeight);
    }

    public Rectangle getHitbox() {
        if (!isAttacking()) return null;
        
        int hbWidth = (currentState == PlayerState.PUNCHING) ? 60 : 70;
        int hbHeight = (currentState == PlayerState.PUNCHING) ? 30 : 40;
        int hbY = (currentState == PlayerState.PUNCHING) ? (y + 30) : (y + 80);

        int hbX;
        if (this.id == 1) {
            hbX = x + renderWidth - 20; 
        } else {
            hbX = x - hbWidth + 20;     
        }

        return new Rectangle(hbX, hbY, hbWidth, hbHeight);
    }
}
