// Import statements for GUI, graphics, events, and utilities
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Main animation class for "The Great Mosquito Adventure"
 * This class handles the complete animation sequence including:
 * - Intro sequence with text
 * - Egg hatching and mosquito evolution
 * - Flying and mating scenes
 * - Meteor impact and destruction
 * - Underwater transformation and rebirth cycle
 */
public class main extends JPanel implements ActionListener {

    // Animation timer for controlling frame rate
    private Timer timer;
    
    // Canvas dimensions
    private final int WIDTH = 600, HEIGHT = 600;
    
    // Collections for animated elements
    private ArrayList<Bubble> bubbles;
    private ArrayList<Point> clouds;
    private Random rand;

    // ===== ANIMATION STATE VARIABLES =====
    private int frameCount = 0;  // Counter for animation frames
    private int sceneState = -1; // Scene state: -1=intro, 0=egg, 1=evolving, 2=flying, 3=mating, 4=meteor, 5=falling, 6=explosion

    // ===== INTRO SEQUENCE VARIABLES =====
    private float introAlpha = 0f;        // Fade-in opacity for intro text
    private int introStep = 0;            // Current intro text step
    private long introStartTime = System.currentTimeMillis();  // Timing for intro sequence
    private String[] introTexts = {
            "In my final breath,\nall I saw was the blinding glow\nthe last light before the dark claimed me",
            "Now... the wheel turns\nIt's time for me to be reborn",
            "The last memory that clung to me\n was the simple truth that\na male mosquito lives only seven days",
            "Such a brief flicker of existence, isn't it?"
    };
    private boolean isFlashing = false;   // Controls screen flash effects

    // ===== UNDERWATER/EGG SCENE VARIABLES =====
    private int eggBaseX = 300, eggBaseY = 530;  // Base position of the mosquito egg
    private double eggSwingAngle = 0;             // Angle for egg swinging animation
    private double mosquitoY;                     // Y position of evolving mosquito
    private int shellOffset;                      // Offset for cracked egg shell animation

    // ===== AIR/FLYING SCENE VARIABLES =====
    private double mosquitoX_air, mosquitoY_air;  // Position of male mosquito in air
    private double femaleMosquitoX, femaleMosquitoY;  // Position of female mosquito
    private double meetingX, meetingY;            // Meeting point coordinates
    private Heart heart;                          // Heart object for mating scene
    double meteorX, meteorY;                      // Meteor position
    private double meteorSpeedY = 10;             // Meteor falling speed
    private boolean meteorHit = false;            // Flag for meteor impact
    ArrayList<Integer> cloudSpeeds = new ArrayList<>();  // Individual cloud movement speeds
    private ArrayList<Seaweed> seaweeds;          // Underwater seaweed objects
    private ArrayList<SeaGrass> seaGrasses;       // Underwater sea grass objects

    // ===== GROUND SCENE VARIABLES =====
    // Puddle properties for ground scene
    private int puddleX = 30;           // X position of puddle
    private int puddleY = 500;          // Y position of puddle
    private int puddleWidth = 500;      // Width of puddle
    private int puddleHeight = 60;      // Height of puddle

    // Forest scene elements
    private ArrayList<Tree> trees;           // Forest trees
    private ArrayList<Bush> bushes;          // Forest bushes
    private ArrayList<Bird> birds;           // Flying birds
    private ArrayList<FloatingLeaf> floatingLeaves;  // Floating leaves

    // ===== FALLING CORPSE PHYSICS VARIABLES =====
    // Male mosquito corpse physics
    private double corpseVy = 0;                    // Vertical velocity of male corpse
    private double corpseRotationDeg = 0;           // Rotation angle of male corpse
    private double corpseRotSpeedDeg = 0;           // Rotation speed of male corpse
    private boolean corpseOnWater = false;          // Flag if male corpse hit water
    
    // Female mosquito corpse physics
    private double femaleCorpseVy = 0;              // Vertical velocity of female corpse
    private double femaleCorpseRotationDeg = 0;     // Rotation angle of female corpse
    private double femaleCorpseRotSpeedDeg = 0;     // Rotation speed of female corpse
    private boolean femaleCorpseOnWater = false;    // Flag if female corpse hit water
    private boolean corpseSettleStarted = false;    // Flag to start settling animation

    // ===== EXPLOSION AND TRANSFORMATION VARIABLES =====
    // Particle systems for visual effects
    private ArrayList<ExplosionParticle> explosionParticles;      // Fire/explosion particles
    private ArrayList<SmokeParticle> smokeParticles;              // Smoke particles
    private ArrayList<TransformationParticle> transformationParticles;  // Transformation effect particles
    
    // Explosion animation properties
    private int explosionRadius = 0;               // Current explosion size
    private boolean explosionStarted = false;      // Flag to start explosion
    private boolean underwaterTransition = false;  // Flag for underwater scene transition
    private double underwaterMosquitoY = 100;      // Y position during underwater transition
    private double eggTransformationProgress = 0.0; // Progress of egg transformation (0.0 to 1.0)
    
    // Visual effects
    private int shockwaveRadius = 0;               // Shockwave ring radius
    private float shockwaveAlpha = 0f;             // Shockwave opacity
    private float flashAlpha = 0f;                 // Screen flash opacity
    private double shakeIntensity = 0.0;           // Camera shake intensity
    private int shakeFrames = 0;                   // Remaining shake frames

    // ===== BUFFERED IMAGE CACHES =====
    // Pre-rendered shapes for performance optimization
    private BufferedImage circleImage;      // Cached circle shape
    private BufferedImage ellipseImage;     // Cached ellipse shape
    private BufferedImage rectangleImage;   // Cached rectangle shape

    // ===== CUSTOM DRAWING ALGORITHMS =====
    /**
     * Draws a line using Polygon for better performance
     * Uses Bresenham's line algorithm concept with Polygon implementation
     * @param g Graphics context
     * @param x1 Start X coordinate
     * @param y1 Start Y coordinate
     * @param x2 End X coordinate
     * @param y2 End Y coordinate
     */
    private void drawBresenhamLine(Graphics g, int x1, int y1, int x2, int y2) {
        // Use Polygon instead of drawLine for better performance
        Polygon line = new Polygon();
        line.addPoint(x1, y1);
        line.addPoint(x2, y2);
        g.drawPolygon(line);
    }

    /**
     * Creates a circle polygon for drawing
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param r Radius
     * @return Polygon representing the circle
     */
    private Polygon createCirclePolygon(int xc, int yc, int r) {
        Polygon circle = new Polygon();
        for (int angle = 0; angle < 360; angle += 5) {
            double rad = Math.toRadians(angle);
            int x = xc + (int) (r * Math.cos(rad));
            int y = yc + (int) (r * Math.sin(rad));
            circle.addPoint(x, y);
        }
        return circle;
    }

    /**
     * Creates an ellipse polygon for drawing
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param rx X radius
     * @param ry Y radius
     * @return Polygon representing the ellipse
     */
    private Polygon createEllipsePolygon(int xc, int yc, int rx, int ry) {
        Polygon ellipse = new Polygon();
        for (int angle = 0; angle < 360; angle += 5) {
            double rad = Math.toRadians(angle);
            int x = xc + (int) (rx * Math.cos(rad));
            int y = yc + (int) (ry * Math.sin(rad));
            ellipse.addPoint(x, y);
        }
        return ellipse;
    }

    /**
     * Creates a rectangle polygon for drawing
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @return Polygon representing the rectangle
     */
    private Polygon createRectanglePolygon(int x, int y, int width, int height) {
        Polygon rect = new Polygon();
        rect.addPoint(x, y);
        rect.addPoint(x + width, y);
        rect.addPoint(x + width, y + height);
        rect.addPoint(x, y + height);
        return rect;
    }

    // ===== FILL AND DRAW METHODS =====
    /**
     * Fills a circle using Polygon
     * @param g Graphics context
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param r Radius
     */
    private void fillCircle(Graphics g, int xc, int yc, int r) {
        Polygon circle = createCirclePolygon(xc, yc, r);
        g.fillPolygon(circle);
    }

    /**
     * Draws a circle outline using Polygon
     * @param g Graphics context
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param r Radius
     */
    private void drawCircle(Graphics g, int xc, int yc, int r) {
        Polygon circle = createCirclePolygon(xc, yc, r);
        g.drawPolygon(circle);
    }

    /**
     * Fills an ellipse using Polygon
     * @param g Graphics context
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param rx X radius
     * @param ry Y radius
     */
    private void fillEllipse(Graphics g, int xc, int yc, int rx, int ry) {
        Polygon ellipse = createEllipsePolygon(xc, yc, rx, ry);
        g.fillPolygon(ellipse);
    }

    /**
     * Draws an ellipse outline using Polygon
     * @param g Graphics context
     * @param xc Center X coordinate
     * @param yc Center Y coordinate
     * @param rx X radius
     * @param ry Y radius
     */
    private void drawEllipse(Graphics g, int xc, int yc, int rx, int ry) {
        Polygon ellipse = createEllipsePolygon(xc, yc, rx, ry);
        g.drawPolygon(ellipse);
    }

    /**
     * Fills a rectangle using Polygon
     * @param g Graphics context
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     */
    private void fillRectangle(Graphics g, int x, int y, int width, int height) {
        Polygon rect = createRectanglePolygon(x, y, width, height);
        g.fillPolygon(rect);
    }

    /**
     * Draws a rectangle outline using Polygon
     * @param g Graphics context
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     */
    private void drawRectangle(Graphics g, int x, int y, int width, int height) {
        Polygon rect = createRectanglePolygon(x, y, width, height);
        g.drawPolygon(rect);
    }

    // ===== BUFFERED IMAGE CREATION METHODS =====
    /**
     * Creates a BufferedImage containing a filled circle
     * @param radius Circle radius
     * @param color Fill color
     * @return BufferedImage with the circle
     */
    private BufferedImage createCircleImage(int radius, Color color) {
        BufferedImage img = new BufferedImage(radius * 2 + 2, radius * 2 + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillOval(1, 1, radius * 2, radius * 2);
        g2d.dispose();
        return img;
    }

    /**
     * Creates a BufferedImage containing a filled ellipse
     * @param width Ellipse width
     * @param height Ellipse height
     * @param color Fill color
     * @return BufferedImage with the ellipse
     */
    private BufferedImage createEllipseImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillOval(1, 1, width, height);
        g2d.dispose();
        return img;
    }

    /**
     * Creates a BufferedImage containing a filled rectangle
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color Fill color
     * @return BufferedImage with the rectangle
     */
    private BufferedImage createRectangleImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(1, 1, width, height);
        g2d.dispose();
        return img;
    }

    // ===== BUFFERED IMAGE DRAWING METHODS =====
    /**
     * Draws a circle using pre-rendered BufferedImage
     * @param g Graphics context
     * @param x Center X coordinate
     * @param y Center Y coordinate
     * @param radius Circle radius
     * @param color Circle color
     */
    private void drawCircleImage(Graphics g, int x, int y, int radius, Color color) {
        BufferedImage img = createCircleImage(radius, color);
        g.drawImage(img, x - radius - 1, y - radius - 1, null);
    }

    /**
     * Draws an ellipse using pre-rendered BufferedImage
     * @param g Graphics context
     * @param x Center X coordinate
     * @param y Center Y coordinate
     * @param width Ellipse width
     * @param height Ellipse height
     * @param color Ellipse color
     */
    private void drawEllipseImage(Graphics g, int x, int y, int width, int height, Color color) {
        BufferedImage img = createEllipseImage(width, height, color);
        g.drawImage(img, x - width/2 - 1, y - height/2 - 1, null);
    }

    /**
     * Draws a rectangle using pre-rendered BufferedImage
     * @param g Graphics context
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color Rectangle color
     */
    private void drawRectangleImage(Graphics g, int x, int y, int width, int height, Color color) {
        BufferedImage img = createRectangleImage(width, height, color);
        g.drawImage(img, x - 1, y - 1, null);
    }

    /**
     * Starts a screen flash effect
     * Used for transitions and dramatic moments
     */
    private void startFlash() {
        isFlashing = true;
        flashAlpha = 1f;
    }

    /**
     * Constructor - initializes the animation panel
     * Sets up timer, random generator, and all scene objects
     */
    public main() {
        // Set up the panel dimensions
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        
        // Initialize animation timer (30 FPS)
        timer = new Timer(33, this);
        timer.start();
        
        // Initialize random number generator
        rand = new Random();

        // ===== PARTICLE SYSTEM INITIALIZATION =====
        explosionParticles = new ArrayList<>();
        smokeParticles = new ArrayList<>();
        transformationParticles = new ArrayList<>();

        // ===== SCENE OBJECT INITIALIZATION =====
        bubbles = new ArrayList<>();
        clouds = new ArrayList<>();
        seaweeds = new ArrayList<>();
        seaGrasses = new ArrayList<>();
        
        // Create initial clouds with random positions and speeds
        for (int i = 0; i < 5; i++) {
            clouds.add(new Point(rand.nextInt(WIDTH), rand.nextInt(200) + 50));
            cloudSpeeds.add(1 + rand.nextInt(3));
        }

        // ===== UNDERWATER VEGETATION INITIALIZATION =====
        // Create seaweed stalks along the bottom
        int seaweedCount = 15;
        for (int i = 0; i < seaweedCount; i++) {
            int baseX = 30 + i * (WIDTH - 60) / seaweedCount + rand.nextInt(20) - 10;
            int height = 60 + rand.nextInt(60);
            double amp = 6 + rand.nextDouble() * 8;
            double speed = 0.02 + rand.nextDouble() * 0.03;
            int segments = 12 + rand.nextInt(6);
            seaweeds.add(new Seaweed(baseX, height, amp, speed, segments));
        }

        // Create small sea grass clusters along the bottom
        int clusterCount = 30;
        for (int i = 0; i < clusterCount; i++) {
            int baseX = 15 + i * (WIDTH - 30) / clusterCount + rand.nextInt(10) - 5;
            int blades = 6 + rand.nextInt(6);
            int minH = 18 + rand.nextInt(8);
            int maxH = minH + 10 + rand.nextInt(12);
            int spread = 10 + rand.nextInt(8);
            double amp = 2.5 + rand.nextDouble() * 2.5;
            double speed = 0.015 + rand.nextDouble() * 0.02;
            seaGrasses.add(new SeaGrass(baseX, blades, spread, minH, maxH, amp, speed));
        }

        // ===== FOREST SCENE INITIALIZATION =====
        // Initialize forest element collections
        trees = new ArrayList<>();
        bushes = new ArrayList<>();
        birds = new ArrayList<>();
        floatingLeaves = new ArrayList<>();

        // Create trees across the bottom of the scene
        int treeCount = 8;
        for (int i = 0; i < treeCount; i++) {
            int baseX = 50 + i * (WIDTH - 100) / treeCount + rand.nextInt(40) - 20;
            int height = 80 + rand.nextInt(60);
            int trunkWidth = 8 + rand.nextInt(6);
            trees.add(new Tree(baseX, height, trunkWidth));
        }

        // Create bushes between trees
        int bushCount = 12;
        for (int i = 0; i < bushCount; i++) {
            int baseX = 30 + i * (WIDTH - 60) / bushCount + rand.nextInt(30) - 15;
            int size = 20 + rand.nextInt(25);
            bushes.add(new Bush(baseX, size));
        }

        // Create flying birds with random starting positions
        for (int i = 0; i < 5; i++) {
            int startX = rand.nextInt(WIDTH);
            int startY = 80 + rand.nextInt(100);
            birds.add(new Bird(startX, startY));
        }

        // Create floating leaves with random starting positions
        for (int i = 0; i < 15; i++) {
            int startX = rand.nextInt(WIDTH);
            int startY = 150 + rand.nextInt(100);
            floatingLeaves.add(new FloatingLeaf(startX, startY));
        }

        // ===== MOSQUITO POSITION INITIALIZATION =====
        // Set up meeting point for mosquitoes
        meetingX = WIDTH / 2.0;
        meetingY = 120;
        
        // Position female mosquito to the right of meeting point
        femaleMosquitoX = (meetingX + 60);
        femaleMosquitoY = meetingY;
        
        // Position male mosquito to the left of meeting point
        mosquitoX_air = meetingX - 60;
        mosquitoY_air = meetingY;
        
        // Create heart object for mating scene
        heart = new Heart((femaleMosquitoX + mosquitoX_air) / 2, meetingY, 30, 0);
        
        // Initialize meteor position above male mosquito
        meteorX = mosquitoX_air;
        meteorY = mosquitoY_air - 100;
    }

    /**
     * Main painting method called by Swing
     * Delegates to drawScene for the actual rendering
     * @param g Graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene(g);
    }

    /**
     * Main scene rendering method
     * Handles all drawing based on current scene state
     * @param g Graphics context
     */
    private void drawScene(Graphics g) {
        // Apply camera shake effect during explosion scene
        if (sceneState == 6 && shakeIntensity > 0) {
            int sx = (int) ((rand.nextDouble() * 2 - 1) * shakeIntensity);
            int sy = (int) ((rand.nextDouble() * 2 - 1) * shakeIntensity);
            g.translate(sx, sy);
        }

        // ===== BACKGROUND RENDERING =====
        // Underwater scenes (egg, evolving, and underwater transition)
        if (sceneState <= 1 || sceneState == 5) {
            g.setColor(new Color(70, 130, 150));  // Underwater blue color
            fillRectangle(g, 0, 0, WIDTH, HEIGHT);
            drawBottomGround(g);
            drawSeaGrass(g);
            drawSeaweed(g);
            manageBubbles(g);
            if (sceneState == 5) {
                drawSkyBackground(g, (int) mosquitoY_air);
            }
        } 
        // Air scenes (flying, mating, meteor)
        else if (sceneState >= 2 && sceneState <= 4) {
            drawSkyBackground(g, 0);
        } 
        // Scene 5 has special background handling
        else if (sceneState == 5) {
            // Sky background will be drawn in case 5
        }

        // ===== INTRO SCENE RENDERING =====
        if (sceneState == -1) {
            // Black background for intro
            g.setColor(Color.BLACK);
            fillRectangle(g, 0, 0, WIDTH, HEIGHT);

            // Draw intro text with fade-in effect
            g.setColor(new Color(255, 255, 255, (int) (introAlpha * 255)));
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            String text = introTexts[introStep];
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                int textWidth = g.getFontMetrics().stringWidth(lines[i]);
                g.drawString(lines[i], (WIDTH - textWidth) / 2, HEIGHT / 2 + i * 30);
            }
            return;
        }

        // ===== SCREEN FLASH EFFECT =====
        // Apply white flash overlay for dramatic transitions
        if (isFlashing) {
            g.setColor(new Color(255, 255, 255, (int) (flashAlpha * 255)));
            fillRectangle(g, 0, 0, getWidth(), getHeight());
        }

        // ===== MAIN SCENE RENDERING =====
        // Render different elements based on current scene state
        switch (sceneState) {
            case 0 -> drawMosquitoEgg(g);
            case 1 -> {
                double progress = ((double) eggBaseY - mosquitoY) / (eggBaseY - 100);
                progress = Math.max(0, Math.min(1.0, progress));
                drawCrackedEgg(g, shellOffset);
                drawEvolvingMosquito(g, eggBaseX, (int) mosquitoY, progress);
            }
            case 2 -> {
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX + 60, (int) femaleMosquitoY);
            }
            case 3 -> {
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX + 60, (int) femaleMosquitoY);
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 25);
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 15;
                heart.x = heartX;
                heart.y = heartY;
                heart.draw(g);
            }
            case 4 -> {
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX + 60, (int) femaleMosquitoY);
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 25);
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 15;
                heart.x = heartX;
                heart.y = heartY;
                heart.draw(g);
                drawMeteor(g, meteorX - 150, meteorY);
            }
            case 5 -> {
                if (!underwaterTransition) {
                    // Use original sky background but with destroyed elements
                    drawDestroyedSkyBackground(g);
                    // Position mosquitoes closer together in center of puddle
                    drawDeadMosquito(g, mosquitoX_air, mosquitoY_air, corpseRotationDeg);
                    drawDeadMosquito(g, femaleMosquitoX, femaleMosquitoY, femaleCorpseRotationDeg);

                } else {
                    // Underwater scene
                    drawUnderwaterScene(g);
                }
            }
            case 6 -> {
                drawExplosion(g, (int) mosquitoX_air, (int) mosquitoY_air);
                drawExplosionParticles(g);
                drawSmokeParticles(g);
            }
        }

        // Screen flash overlay on top of everything
        if (flashAlpha > 0f) {
            g.setColor(new Color(255, 255, 255, (int) (Math.min(1f, flashAlpha) * 255)));
            fillRectangle(g, 0, 0, WIDTH, HEIGHT);
        }
    }

    /**
     * Main animation loop - called by timer every 33ms (30 FPS)
     * Handles all animation logic and scene transitions
     * @param e ActionEvent from timer
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // ===== FLASH EFFECT HANDLING =====
        // Handle screen flash fade-out
        if (isFlashing) {
            flashAlpha -= 0.02f; // Gradually reduce flash intensity
            if (flashAlpha <= 0f) {
                flashAlpha = 0f;
                isFlashing = false;
                // Don't change sceneState here anymore, let the transformation logic handle it
            }
            repaint();
            return;
        }
        // ===== INTRO SCENE LOGIC =====
        if (sceneState == -1) {
            long elapsed = System.currentTimeMillis() - introStartTime;

            // Fade in text gradually
            if (introAlpha < 1f) {
                introAlpha += 0.015f; // Fade in speed
            } 
            // After 3.5 seconds, fade out and move to next text
            else if (elapsed > 3500) {
                introAlpha = 0f;
                introStep++;
                introStartTime = System.currentTimeMillis();

                // When all intro texts are shown, transition to first scene
                if (introStep >= introTexts.length) {
                    isFlashing = true;
                    flashAlpha = 1.0f;
                    introStep++;
                    sceneState = 0; // Go to first scene (egg)
                }
            }

            repaint();
            return; // Stop here, don't continue with other logic
        }
        frameCount++;  // Increment frame counter
        // ===== MAIN SCENE STATE MACHINE =====
        switch (sceneState) {
            case 0 -> { // Egg shaking scene
                // Start egg swinging after 60 frames
                if (frameCount > 60)
                    eggSwingAngle += 0.1;
                // Transition to hatching after 150 frames
                if (frameCount > 150) {
                    sceneState = 1;
                    mosquitoY = eggBaseY;
                    shellOffset = 0;
                }
            }
            case 1 -> { // Egg hatching and mosquito evolution
                // Mosquito rises from egg
                mosquitoY -= 1.5;
                // Crack egg shell gradually
                if (shellOffset < 25)
                    shellOffset++;
                // Transition to flying when mosquito reaches top
                if (mosquitoY < 80) {
                    sceneState = 2; // Fly to sky
                    mosquitoX_air = -40;
                    mosquitoY_air = meetingY;
                }
            }
            case 2 -> { // Flying to meet female mosquito
                double targetX = meetingX - 30;
                if (mosquitoX_air < targetX) {
                    mosquitoX_air += 3.0;  // Move towards meeting point

                    // Synchronized bobbing motion for both mosquitoes
                    double deltaY = Math.sin(frameCount * 0.1) * 2;
                    mosquitoY_air = meetingY + deltaY;
                    femaleMosquitoY = meetingY + deltaY;

                } else {
                    // Arrived at meeting point
                    mosquitoX_air = targetX;
                    mosquitoY_air = meetingY;
                    femaleMosquitoY = meetingY;

                    sceneState = 3;  // Transition to mating scene
                    frameCount = 0;
                    heart.visible = true;
                    heart.blinkCounter = 0;
                }
            }

            case 3 -> { // Mating scene with heart animation
                // Position heart between the two mosquitoes
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2) - 15;
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 25;
                heart.x = heartX;
                heart.y = heartY;
                heart.update();

                // After 120 frames, meteor appears
                if (frameCount > 120) {
                    sceneState = 4;  // Transition to meteor scene
                    meteorX = mosquitoX_air;
                    meteorY = -100;
                    meteorHit = false;
                    frameCount = 0;
                }
            }

            case 4 -> { // Meteor falling and impact
                meteorY += meteorSpeedY;  // Meteor falls down
                if (meteorY >= mosquitoY_air) {
                    meteorHit = true;
                    frameCount++;
                    // After 15 frames of impact, start explosion
                    if (frameCount > 15) {
                        sceneState = 6;  // Transition to explosion scene
                        frameCount = 0;
                        explosionStarted = false;
                        explosionRadius = 0;
                        shockwaveRadius = 20;
                        shockwaveAlpha = 1.0f;
                        flashAlpha = 0.9f;
                        shakeIntensity = 6.0;
                        shakeFrames = 20;
                        createExplosionParticles();
                        // Hide meteor after explosion
                        meteorX = -1000;
                        meteorY = -1000;
                    }
                }
            }
            case 5 -> { // Falling corpse physics and underwater transformation
                // Calculate center of puddle for corpse landing
                int centerPuddleX = puddleX + puddleWidth / 2;

                // ===== MALE CORPSE PHYSICS =====
                // Male mosquito falls to center of puddle
                if (!corpseOnWater) {
                    corpseVy += 1.0; // Apply gravity
                    mosquitoY_air += corpseVy;
                    // Move X position towards center of puddle
                    if (mosquitoX_air < centerPuddleX - 15) {
                        mosquitoX_air += 4.0; // Move right
                    } else if (mosquitoX_air > centerPuddleX - 15) {
                        mosquitoX_air -= 4.0; // Move left
                    }
                    corpseRotationDeg += corpseRotSpeedDeg;
                    // Check for water impact
                    if (mosquitoY_air >= puddleY + puddleHeight) {
                        mosquitoY_air = puddleY + puddleHeight;
                        mosquitoX_air = centerPuddleX - 15; // Snap to center
                        corpseOnWater = true;
                        corpseVy *= -0.25; // Small bounce effect
                        corpseRotSpeedDeg *= 0.5; // Dampen rotation
                        if (!corpseSettleStarted) {
                            frameCount = 0;
                            corpseSettleStarted = true;
                        }
                    }
                } else {
                    // Corpse is in water - apply damping
                    corpseVy *= 0.85;
                    corpseRotSpeedDeg *= 0.85;
                    mosquitoY_air += corpseVy;
                    corpseRotationDeg += corpseRotSpeedDeg;

                    // Sink deeper into puddle
                    if (mosquitoY_air < puddleY + puddleHeight + 20) {
                        mosquitoY_air += 1.0; // Sink faster
                    }
                }

                // ===== FEMALE CORPSE PHYSICS =====
                // Female mosquito falls to center of puddle
                if (!femaleCorpseOnWater) {
                    femaleCorpseVy += 1.0; // Apply gravity
                    femaleMosquitoY += femaleCorpseVy;
                    // Move X position towards center of puddle
                    if (femaleMosquitoX < centerPuddleX + 15) {
                        femaleMosquitoX += 4.0; // Move right
                    } else if (femaleMosquitoX > centerPuddleX + 15) {
                        femaleMosquitoX -= 4.0; // Move left
                    }
                    femaleCorpseRotationDeg += femaleCorpseRotSpeedDeg;
                    // Check for water impact
                    if (femaleMosquitoY >= puddleY + puddleHeight) {
                        femaleMosquitoY = puddleY + puddleHeight;
                        femaleMosquitoX = centerPuddleX + 15; // Snap to center
                        femaleCorpseOnWater = true;
                        femaleCorpseVy *= -0.25; // Small bounce effect
                        femaleCorpseRotSpeedDeg *= 0.5; // Dampen rotation
                        if (!corpseSettleStarted) {
                            frameCount = 0;
                            corpseSettleStarted = true;
                        }
                    }
                } else {
                    // Corpse is in water - apply damping
                    femaleCorpseVy *= 0.85;
                    femaleCorpseRotSpeedDeg *= 0.85;
                    femaleMosquitoY += femaleCorpseVy;
                    femaleCorpseRotationDeg += femaleCorpseRotSpeedDeg;

                    // Sink deeper into puddle
                    if (femaleMosquitoY < puddleY + puddleHeight + 20) {
                        femaleMosquitoY += 1.0; // Sink faster
                    }
                }

                // ===== UNDERWATER TRANSITION LOGIC =====
                // After both corpses are sunk in puddle and a short linger, transition to underwater
                if (corpseOnWater && femaleCorpseOnWater &&
                        mosquitoY_air >= puddleY + puddleHeight + 20 &&
                        femaleMosquitoY >= puddleY + puddleHeight + 20 &&
                        frameCount > 30 && !underwaterTransition) { // Prevent multiple transitions
                    underwaterTransition = true;
                    underwaterMosquitoY = 0; // Start from top of screen
                    eggTransformationProgress = 0.0;
                    frameCount = 0;
                    // Reset corpse positions to prevent further sinking
                    mosquitoY_air = puddleY + puddleHeight + 20;
                    femaleMosquitoY = puddleY + puddleHeight + 20;
                    System.out.println("Transitioning to underwater scene!"); // Debug
                }

                // ===== UNDERWATER SCENE LOGIC =====
                if (underwaterTransition) {
                    // Smooth sinking to egg position - no bouncing
                    if (underwaterMosquitoY < eggBaseY) {
                        underwaterMosquitoY += 15.0; // Fast, direct sinking

                        // Prevent overshooting and lock position
                        if (underwaterMosquitoY >= eggBaseY) {
                            underwaterMosquitoY = eggBaseY;
                        }
                    }

                    // ===== EGG TRANSFORMATION LOGIC =====
                    // Once at egg position, start transformation
                    if (underwaterMosquitoY >= eggBaseY) {
                        eggTransformationProgress += 0.08; // Slow transformation for visual effect

                        // Create transformation particles during middle phase
                        if (eggTransformationProgress > 0.2 && eggTransformationProgress < 0.8 && transformationParticles.size() < 50) {
                            if (rand.nextInt(3) == 0) { // Create particles occasionally
                                double angle = rand.nextDouble() * 2 * Math.PI;
                                double speed = 1 + rand.nextDouble() * 2;
                                double vx = Math.cos(angle) * speed;
                                double vy = Math.sin(angle) * speed - 1;
                                
                                // Particle color options for transformation effect
                                Color[] colors = {
                                    new Color(255, 255, 200), // Light yellow
                                    new Color(255, 215, 0),   // Gold
                                    new Color(255, 255, 255)  // White
                                };
                                Color particleColor = colors[rand.nextInt(colors.length)];
                                
                                transformationParticles.add(new TransformationParticle(
                                    eggBaseX + (rand.nextDouble() - 0.5) * 20,
                                    eggBaseY + (rand.nextDouble() - 0.5) * 10,
                                    vx, vy, 3 + rand.nextInt(4), particleColor
                                ));
                            }
                        }

                        // ===== TRANSFORMATION COMPLETION =====
                        // When transformation complete, reset to scene 0 for new cycle
                        if (eggTransformationProgress >= 1.0) {
                            // Add final flash effect for dramatic transition
                            flashAlpha = 0.8f;
                            isFlashing = true;
                            frameCount = 0;
                            sceneState = 0;
                            underwaterTransition = false;
                            eggSwingAngle = 0;
                            
                            // ===== CLEAR ALL PARTICLE SYSTEMS =====
                            bubbles.clear();
                            heart.visible = false;
                            meteorX = -100;
                            meteorY = -100;
                            explosionParticles.clear();
                            smokeParticles.clear();
                            transformationParticles.clear();
                            
                            // ===== RESET CORPSE VARIABLES =====
                            corpseOnWater = false;
                            femaleCorpseOnWater = false;
                            corpseSettleStarted = false;
                            
                            // ===== RESET UNDERWATER VARIABLES =====
                            underwaterMosquitoY = 0;
                            eggTransformationProgress = 0.0;
                            
                            // ===== RESET FLASH VARIABLES =====
                            flashAlpha = 0f;
                            isFlashing = false;
                            
                            // ===== RESET MOSQUITO POSITIONS =====
                            mosquitoX_air = -40;
                            mosquitoY_air = meetingY;
                            femaleMosquitoX = meetingX + 60;
                            femaleMosquitoY = meetingY;
                            
                            // ===== RESET HEART =====
                            heart.visible = false;
                            heart.blinkCounter = 0;
                            
                            // ===== RESET CORPSE PHYSICS =====
                            corpseVy = 0.0;
                            corpseRotationDeg = 0.0;
                            corpseRotSpeedDeg = 0.0;
                            femaleCorpseVy = 0.0;
                            femaleCorpseRotationDeg = 0.0;
                            femaleCorpseRotSpeedDeg = 0.0;
                            System.out.println("Animation complete! Returning to scene 0");
                        }
                    }
                }
            }
            case 6 -> { // Explosion scene with particles and effects
                // Initialize explosion if not started
                if (!explosionStarted) {
                    explosionStarted = true;
                    explosionRadius = 0;
                }

                // ===== EXPLOSION ANIMATION =====
                // Fireball expands quickly, then starts fading
                if (explosionRadius < 180) {
                    explosionRadius += 10; // Fast expansion
                } else if (explosionRadius < 220) {
                    explosionRadius += 4;  // Slower expansion
                } else {
                    // Start shrinking the explosion after reaching max size
                    explosionRadius = Math.max(0, explosionRadius - 8);
                }

                // ===== SHOCKWAVE EFFECT =====
                // Shockwave ring expands outward
                shockwaveRadius += 15;
                shockwaveAlpha = Math.max(0f, shockwaveAlpha - 0.08f);

                // ===== SCREEN FLASH FADE =====
                // Screen flash fades quickly
                if (flashAlpha > 0f) {
                    flashAlpha *= 0.92f;
                    if (flashAlpha < 0.02f)
                        flashAlpha = 0f;
                }

                // ===== CAMERA SHAKE DECAY =====
                // Camera shake gradually reduces
                if (shakeFrames > 0) {
                    shakeIntensity *= 0.85;
                    shakeFrames--;
                } else {
                    shakeIntensity = 0;
                }

                // ===== PARTICLE SYSTEM UPDATES =====
                // Update all explosion particles
                for (ExplosionParticle particle : explosionParticles) {
                    particle.update();
                }
                // Update all smoke particles
                for (SmokeParticle particle : smokeParticles) {
                    particle.update();
                }

                // Remove dead particles for performance
                explosionParticles.removeIf(particle -> particle.alpha <= 0);
                smokeParticles.removeIf(p -> p.isDead());

                // ===== EXPLOSION COMPLETION =====
                // Shorter explosion scene - transition when explosion fades out
                if (frameCount > 45 || explosionRadius <= 0) {
                    sceneState = 5;  // Transition to falling corpse scene
                    frameCount = 0;
                    explosionParticles.clear();
                    smokeParticles.clear();
                    flashAlpha = 0f;
                    shockwaveAlpha = 0f;
                    shakeIntensity = 0;
                    
                    // ===== INITIALIZE CORPSE FALL PHYSICS =====
                    // Male corpse physics
                    corpseVy = 0.0;
                    corpseRotationDeg = rand.nextBoolean() ? -20 : 20;
                    corpseRotSpeedDeg = (rand.nextDouble() * 6 + 2) * (rand.nextBoolean() ? 1 : -1);
                    corpseOnWater = false;
                    
                    // Female corpse physics
                    femaleCorpseVy = 0.0;
                    femaleCorpseRotationDeg = rand.nextBoolean() ? -15 : 15;
                    femaleCorpseRotSpeedDeg = (rand.nextDouble() * 5 + 2) * (rand.nextBoolean() ? 1 : -1);
                    femaleCorpseOnWater = false;
                    corpseSettleStarted = false;
                }
            }
        }

        // ===== CLOUD ANIMATION =====
        // Update cloud positions only during flying scenes (2,3,4)
        if (sceneState == 2 || sceneState == 3 || sceneState == 4) {
            for (int i = 0; i < clouds.size(); i++) {
                Point cloud = clouds.get(i);
                int speed = cloudSpeeds.get(i);
                cloud.x -= speed;  // Move clouds left
                // Reset cloud position when it goes off screen
                if (cloud.x < -150) {
                    cloud.x = WIDTH + 20;
                    cloud.y = rand.nextInt(150) + 30;
                }
            }
        }

        // In scene 5 (corpse falling), hide clouds since we're underwater
        if (sceneState == 5) {
            // Don't draw clouds in underwater scene
        }
        repaint();
    }

    /**
     * Draws the female mosquito character
     * Uses custom drawing algorithms for better performance
     * @param g Graphics context
     * @param x X coordinate of mosquito center
     * @param y Y coordinate of mosquito center
     */
    private void drawFemaleMosquito(Graphics g, double x, double y) {
        // Convert to integer coordinates for drawing
        int centerX = (int) x;
        int centerY = (int) y;

        // ===== BODY AND HEAD =====
        // Draw body using custom ellipse fill
        g.setColor(new Color(100, 20, 20));  // Dark red body
        fillEllipse(g, centerX, centerY - 10, 5, 10); // Body
        fillEllipse(g, centerX, centerY - 20, 4, 4);  // Head

        // ===== LEGS =====
        // Draw four legs using Bresenham line algorithm
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 15, centerY - 10); // Front left leg
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 15, centerY - 10); // Front right leg
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 15, centerY - 5);  // Back left leg
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 15, centerY - 5);  // Back right leg

        // ===== WINGS =====
        // Draw flapping wings with animation
        g.setColor(new Color(200, 200, 200));  // Light gray wings
        int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;  // Wing flapping animation
        fillEllipse(g, centerX - 12, centerY - 15 + wingYOffset, 12, 7); // Left wing
        fillEllipse(g, centerX + 12, centerY - 15 + wingYOffset, 12, 7); // Right wing
    }

    /**
     * Draws a meteor with tail effect and surface details
     * Creates a dramatic falling meteor with multiple layers
     * @param g Graphics context
     * @param x X coordinate of meteor center
     * @param y Y coordinate of meteor center
     */
    private void drawMeteor(Graphics g, double x, double y) {
        int centerX = (int) x;
        int centerY = (int) y;
        int size = 300;  // Meteor size

        // ===== TAIL EFFECT =====
        // Draw fiery tail behind the meteor
        g.setColor(new Color(255, 140, 0, 180));  // Orange tail with transparency
        fillEllipse(g, centerX + size / 6, centerY - size, size / 6, size);

        // ===== METEOR BODY =====
        // Draw main meteor body
        g.setColor(new Color(180, 90, 40));  // Brown meteor surface
        fillEllipse(g, centerX, centerY, size / 2, size / 2);

        // ===== HEATED CORE =====
        // Draw glowing inner core
        g.setColor(new Color(255, 100, 0, 200));  // Bright orange core
        fillEllipse(g, centerX + size / 16, centerY + size / 16, size * 3 / 8, size * 3 / 8);

        // ===== SURFACE CRATERS =====
        // Add surface detail with craters
        g.setColor(new Color(50, 25, 0));  // Dark brown craters
        fillEllipse(g, centerX + size / 6, centerY + size / 6, size / 20, size / 20);   // Large crater
        fillEllipse(g, centerX + size / 4, centerY + size / 8, size / 16, size / 16);   // Medium crater
        fillEllipse(g, centerX + size / 8, centerY + size / 4, size / 24, size / 24);   // Small crater
    }

    /**
     * Draws the sky background with clouds and ground elements
     * Creates the outdoor scene for flying and mating sequences
     * @param g Graphics context
     * @param waterLevel Water level for ground positioning (not used in current implementation)
     */
    private void drawSkyBackground(Graphics g, int waterLevel) {
        // ===== SKY BACKGROUND =====
        // Fill entire screen with sky blue color
        g.setColor(new Color(135, 206, 250));  // Sky blue
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // ===== CLOUDS =====
        // Draw moving clouds (except in underwater scene 5)
        if (sceneState != 5) {
            g.setColor(Color.WHITE);
            for (Point cloud : clouds) {
                // Draw cloud using two overlapping ellipses
                fillEllipse(g, cloud.x + 50, cloud.y + 20, 50, 20);  // Lower cloud part
                fillEllipse(g, cloud.x + 70, cloud.y, 40, 25);       // Upper cloud part
            }
        }

        // ===== GROUND ELEMENTS =====
        // Draw forest, birds, and puddle for flying scenes (2-4)
        if (sceneState >= 2 && sceneState <= 4) {
            drawForest(g);
            drawBirds(g);
            drawPuddle(g);
        }
    }

    /**
     * Draws the destroyed sky background after meteor impact
     * Shows the same sky and clouds but with destroyed ground elements
     * @param g Graphics context
     */
    private void drawDestroyedSkyBackground(Graphics g) {
        // ===== SKY BACKGROUND =====
        // Same sky background as original
        g.setColor(new Color(135, 206, 250));  // Sky blue
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // ===== CLOUDS =====
        // Keep clouds unchanged (they weren't destroyed)
        g.setColor(Color.WHITE);
        for (Point cloud : clouds) {
            fillEllipse(g, cloud.x + 50, cloud.y + 20, 50, 20);  // Lower cloud part
            fillEllipse(g, cloud.x + 70, cloud.y, 40, 25);       // Upper cloud part
        }

        // Ground with destroyed forest and changed water color
        drawDestroyedGround(g);
    }

    /**
     * Draws the destroyed ground after meteor impact
     * Shows ground and grass but with destroyed forest and changed water
     * @param g Graphics context
     */
    private void drawDestroyedGround(Graphics g) {
        int groundY = HEIGHT - 180;  // Ground level

        // ===== GROUND SURFACE =====
        // Keep the ground (it wasn't destroyed)
        g.setColor(new Color(34, 139, 34));  // Forest green
        fillRectangle(g, 0, groundY, WIDTH, HEIGHT - groundY);

        // ===== GRASS TEXTURE =====
        // Keep grass texture (it survived)
        g.setColor(new Color(0, 100, 0));  // Dark green grass
        for (int i = 0; i < WIDTH; i += 8) {
            int grassHeight = 3 + rand.nextInt(4);
            drawBresenhamLine(g, i, groundY, i, groundY + grassHeight);
        }

        // Puddle with changed color (darker, more ominous)
        drawDestroyedPuddle(g);

        // No trees, no bushes, no birds, no floating leaves (destroyed by meteor)
    }

    /**
     * Draws the destroyed puddle with darker, more ominous colors
     * Shows the impact of the meteor on the water
     * @param g Graphics context
     */
    private void drawDestroyedPuddle(Graphics g) {
        // ===== SHADOW EFFECT =====
        // Darker shadow to show destruction
        g.setColor(new Color(0, 0, 0, 60));  // Black shadow with transparency
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 + 8,
                puddleHeight / 2 + 8);

        // ===== DESTROYED WATER =====
        // Darker blue water to show contamination
        g.setColor(new Color(50, 75, 128, 200));  // Dark blue water
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2, puddleHeight / 2);

        // ===== DIMINISHED HIGHLIGHTS =====
        // Reduced highlights to show damage
        g.setColor(new Color(200, 200, 200, 80));  // Dimmed highlights
        fillEllipse(g, puddleX + 25 + puddleWidth / 8, puddleY + 12 + puddleHeight / 6, puddleWidth / 8,
                puddleHeight / 6);
        fillEllipse(g, puddleX + puddleWidth / 2 + puddleWidth / 10, puddleY + 15 + puddleHeight / 8, puddleWidth / 10,
                puddleHeight / 8);
        fillEllipse(g, puddleX + puddleWidth - 40 + puddleWidth / 12, puddleY + 10 + puddleHeight / 6, puddleWidth / 12,
                puddleHeight / 6);

        // ===== DIMINISHED RIPPLES =====
        // Reduced ripple effects (commented out for darker appearance)
        g.setColor(new Color(200, 200, 200, 60));
        // drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 30);
        // drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 50);
        // drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 70);

        // ===== SURVIVING GRASS =====
        // Grass around puddle (kept to show partial destruction)
        g.setColor(new Color(0, 100, 0));  // Dark green grass
        for (int i = 0; i < 12; i++) {
            int grassX = puddleX + (i * 18) + 15;
            int grassHeight = 4 + rand.nextInt(4);
            drawBresenhamLine(g, grassX, puddleY, grassX, puddleY - grassHeight);
        }
    }

    /**
     * Draws the underwater transformation scene
     * Shows floating mosquitoes that transform into an egg
     * @param g Graphics context
     */
    private void drawUnderwaterScene(Graphics g) {
        // ===== UNDERWATER BACKGROUND =====
        // Fill screen with underwater blue color
        g.setColor(new Color(70, 130, 150));  // Underwater blue
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // ===== UNDERWATER ELEMENTS =====
        // Draw underwater vegetation and ground
        drawBottomGround(g);
        drawSeaGrass(g);
        drawSeaweed(g);
        manageBubbles(g);
        
        // ===== TRANSFORMATION PARTICLE SYSTEM =====
        // Update and draw transformation particles
        for (TransformationParticle particle : transformationParticles) {
            particle.update();
            particle.draw(g);
        }
        // Remove dead particles for performance
        transformationParticles.removeIf(particle -> particle.isDead());
        
        // Safety check - prevent too many particles
        if (transformationParticles.size() > 100) {
            transformationParticles.clear();
        }

        // ===== MOSQUITO TO EGG TRANSFORMATION =====
        // Draw floating mosquitoes that slowly transform into egg
        if (eggTransformationProgress < 1.0) {
            // ===== FADING MOSQUITOES =====
            // Draw mosquitoes with fading effect and merging animation
            float alpha = (float) (1.0 - eggTransformationProgress);
            int mosquitoAlpha = Math.max(0, (int) (alpha * 255));

            if (mosquitoAlpha > 0) {
                g.setColor(new Color(50, 50, 50, mosquitoAlpha));
                
                // Calculate merging positions - mosquitoes move closer together as they transform
                double mergeProgress = Math.min(1.0, eggTransformationProgress * 2.0); // Start merging early
                double separation = 30.0 * (1.0 - mergeProgress); // Start 30 pixels apart, end at 0
                
                // Position mosquitoes at their current floating position with merging effect
                drawDeadMosquito(g, WIDTH / 2 - separation, (int) underwaterMosquitoY, 0);
                drawDeadMosquito(g, WIDTH / 2 + separation, (int) underwaterMosquitoY, 0);
            }

            // ===== EGG FORMATION =====
            // Draw egg forming with smoother alpha transition and glow effect
            if (eggTransformationProgress > 0.1) { // Start egg formation slightly later
                float eggAlpha = (float) ((eggTransformationProgress - 0.1) / 0.9);
                int finalEggAlpha = Math.min(255, (int) (eggAlpha * 255));
                
                // Add glow effect around the egg
                if (eggAlpha > 0.3) {
                    int glowSize = (int) (eggAlpha * 30);
                    g.setColor(new Color(255, 255, 200, (int) (eggAlpha * 100)));  // Yellow glow
                    fillEllipse(g, eggBaseX, eggBaseY, 50 + glowSize, 25 + glowSize);
                }
                
                g.setColor(new Color(139, 69, 19, finalEggAlpha));  // Brown egg with alpha
                drawMosquitoEgg(g);
            }
        } else {
            // ===== COMPLETED TRANSFORMATION =====
            // Fully transformed egg with permanent glow
            g.setColor(new Color(255, 255, 200, 100));  // Yellow glow
            fillEllipse(g, eggBaseX, eggBaseY, 80, 55);
            g.setColor(new Color(139, 69, 19, 255));  // Solid brown egg
            drawMosquitoEgg(g);
        }
    }

    /**
     * Draws a flying mosquito with animated wings
     * Shows the male mosquito during flight scenes
     * @param g Graphics context
     * @param x X coordinate of mosquito center
     * @param y Y coordinate of mosquito center
     * @param flapping Whether wings should flap
     * @param angle Rotation angle (not used in current implementation)
     */
    private void drawFlyingMosquito(Graphics g, double x, double y, boolean flapping, double angle) {
        int centerX = (int) x;
        int centerY = (int) y;

        // ===== BODY AND HEAD =====
        // Draw body using custom ellipse fill
        g.setColor(new Color(40, 40, 40));  // Dark gray body
        fillEllipse(g, centerX, centerY - 10, 5, 10);  // Body
        fillEllipse(g, centerX, centerY - 20, 4, 4);   // Head

        // ===== LEGS =====
        // Draw four legs using Bresenham line algorithm
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 15, centerY - 10); // Front left leg
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 15, centerY - 10); // Front right leg
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 15, centerY - 5);  // Back left leg
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 15, centerY - 5);  // Back right leg

        // ===== ANIMATED WINGS =====
        // Draw flapping wings if enabled
        if (flapping) {
            g.setColor(new Color(200, 200, 200, 200));  // Light gray wings with transparency
            int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;  // Wing flapping animation
            fillEllipse(g, centerX - 12, centerY - 15 + wingYOffset, 12, 7); // Left wing
            fillEllipse(g, centerX + 12, centerY - 15 + wingYOffset, 12, 7); // Right wing
        }
    }

    /**
     * Draws a dead mosquito with X-eyes and limp posture
     * Shows the corpse state after meteor impact
     * @param g Graphics context
     * @param x X coordinate of mosquito center
     * @param y Y coordinate of mosquito center
     * @param rotationDeg Rotation angle for falling animation
     */
    private void drawDeadMosquito(Graphics g, double x, double y, double rotationDeg) {
        int centerX = (int) x;
        int centerY = (int) y;

        // ===== BODY AND HEAD =====
        // Draw body using custom ellipse fill
        g.setColor(new Color(50, 50, 50));  // Dark gray body
        fillEllipse(g, centerX, centerY - 10, 5, 10);  // Body

        // Draw head
        g.setColor(new Color(60, 60, 60));  // Slightly lighter gray head
        fillEllipse(g, centerX, centerY - 20, 4, 4);   // Head

        // ===== X-EYES =====
        // Draw X-shaped eyes to show death
        g.setColor(Color.RED.darker());  // Dark red X-eyes
        drawBresenhamLine(g, centerX - 6, centerY - 26, centerX - 2, centerY - 22); // Left eye X
        drawBresenhamLine(g, centerX - 6, centerY - 22, centerX - 2, centerY - 26); // Left eye X
        drawBresenhamLine(g, centerX + 2, centerY - 26, centerX + 6, centerY - 22); // Right eye X
        drawBresenhamLine(g, centerX + 2, centerY - 22, centerX + 6, centerY - 26); // Right eye X

        // ===== LIMP LEGS =====
        // Draw drooping legs in death pose
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 12, centerY - 2);  // Front left leg
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 12, centerY - 2);  // Front right leg
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 10, centerY - 6);  // Back left leg
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 10, centerY - 6);  // Back right leg

        // ===== DROOPED WINGS =====
        // Draw limp, drooping wings
        g.setColor(new Color(180, 180, 180, 120));  // Light gray wings with transparency
        fillEllipse(g, centerX - 11, centerY - 10, 11, 6); // Left wing
        fillEllipse(g, centerX + 11, centerY - 10, 11, 6); // Right wing
    }

    /**
     * Draws a mosquito in the process of evolving from egg
     * Shows progressive development from amorphous shape to full mosquito
     * @param g Graphics context
     * @param x X coordinate of mosquito center
     * @param y Y coordinate of mosquito center
     * @param progress Evolution progress (0.0 to 1.0)
     */
    private void drawEvolvingMosquito(Graphics g, int x, int y, double progress) {
        // Adjust position for proper centering
        x = x - 25;

        // Set base color for mosquito body
        g.setColor(new Color(40, 40, 40));  // Dark gray

        // ===== EARLY EVOLUTION (0-20%) =====
        // Draw amorphous evolving shape using connected lines
        if (progress < 0.2) {
            // Create evolving shape using Bresenham lines
            int[] xPoints = { x, x + 2, x - 2, x };      // X coordinates for shape
            int[] yPoints = { y, y - 10, y - 20, y - 30 }; // Y coordinates for shape
            for (int i = 0; i < xPoints.length - 1; i++) {
                drawBresenhamLine(g, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }
        } 
        // ===== BODY AND HEAD FORMATION (20%+) =====
        else {
            // Calculate head size based on progress
            int headSize = (int) (8 * ((progress - 0.2) / 0.8));
            fillEllipse(g, x, y - 10, 5, 10);           // Body
            fillEllipse(g, x, y - 20, headSize / 2, headSize / 2); // Growing head
        }

        // ===== LEG DEVELOPMENT (40%+) =====
        // Legs gradually extend from body
        if (progress > 0.4) {
            g.setColor(Color.BLACK);
            int legLength = (int) (15 * ((progress - 0.4) / 0.6));  // Legs grow progressively
            drawBresenhamLine(g, x, y - 15, x - legLength, y - 10); // Front left leg
            drawBresenhamLine(g, x, y - 15, x + legLength, y - 10); // Front right leg
            drawBresenhamLine(g, x, y - 10, x - legLength, y - 5);  // Back left leg
            drawBresenhamLine(g, x, y - 10, x + legLength, y - 5);  // Back right leg
        }

        // ===== WING DEVELOPMENT (60%+) =====
        // Wings gradually form and start flapping
        if (progress > 0.6) {
            int wingSize = (int) (25 * ((progress - 0.6) / 0.4));   // Wings grow progressively
            int wingAlpha = (int) (150 * ((progress - 0.6) / 0.4)); // Wings fade in
            g.setColor(new Color(200, 200, 200, wingAlpha));        // Light gray wings with alpha
            boolean flap = (y % 10 < 5);                            // Simple flapping animation
            int wingYOffset = flap ? -5 : 0;                        // Wing position offset
            fillEllipse(g, x - wingSize / 2, y - 15 + wingYOffset, wingSize / 2, 7); // Left wing
            fillEllipse(g, x + wingSize / 2, y - 15 + wingYOffset, wingSize / 2, 7); // Right wing
        }
    }

    /**
     * Draws a cracked egg shell with two separated halves
     * Creates the visual effect of an egg breaking open during hatching
     * @param g Graphics context
     * @param offset Distance between the two egg shell halves
     */
    private void drawCrackedEgg(Graphics g, int offset) {
        Color eggColor = new Color(139, 69, 19, 200);  // Brown egg color with transparency
        g.setColor(eggColor);
        int x = eggBaseX - 50;  // Base X position
        int y = eggBaseY;       // Base Y position
        int width = 50;         // Egg width
        int height = 25;        // Egg height

        // Draw left half of egg (arc from 90 to 270 degrees) - FLIPPED HORIZONTALLY
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width / 2; j++) {
                // Calculate if point is inside left half ellipse - FLIPPED
                double normalizedX = (double) (width / 2 - 1 - j) / (width / 2); // Flip left half
                double normalizedY = (double) (i - height / 2) / (height / 2);
                if (normalizedX * normalizedX + normalizedY * normalizedY <= 1.0) {
                    // ใช้ Polygon สำหรับจุดเดียว
                    Polygon point = new Polygon();
                    point.addPoint(x - offset + j, y + i);
                    point.addPoint(x - offset + j, y + i);
                    g.drawPolygon(point);
                }
            }
        }

        // Draw right half of egg (arc from 270 to 90 degrees) - FLIPPED HORIZONTALLY
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width / 2; j++) {
                // Calculate if point is inside right half ellipse - FLIPPED
                double normalizedX = (double) (j) / (width / 2); // Flip right half
                double normalizedY = (double) (i - height / 2) / (height / 2);
                if (normalizedX * normalizedX + normalizedY * normalizedY <= 1.0) {
                    // ใช้ Polygon สำหรับจุดเดียว
                    Polygon point = new Polygon();
                    point.addPoint(x + offset + j, y + i);
                    point.addPoint(x + offset + j, y + i);
                    g.drawPolygon(point);
                }
            }
        }
    }

    /**
     * Draws a complete mosquito egg with swinging animation
     * Shows the egg before hatching with gentle swaying motion
     * @param g Graphics context
     */
    private void drawMosquitoEgg(Graphics g) {
        Color eggColor = new Color(139, 69, 19, 200);  // Brown egg color with transparency
        g.setColor(eggColor);
        
        // ===== SWINGING ANIMATION =====
        int swingX = 0;
        if (frameCount > 60) {  // Start swinging after 60 frames
            double sineValue = Math.sin(eggSwingAngle * 20);  // Calculate swing based on angle
            swingX = (int) (10 * Math.signum(sineValue));     // Apply swing offset
        }
        
        // ===== EGG POSITIONING =====
        int x = eggBaseX - 50 + swingX;  // Base position plus swing offset
        int y = eggBaseY;                // Base Y position
        int width = 50;                  // Egg width
        int height = 25;                 // Egg height

        // Draw egg using custom ellipse fill
        fillEllipse(g, x + width / 2, y + height / 2, width / 2, height / 2);
    }

    /**
     * Draws the underwater ground surface with scattered rocks
     * Creates the bottom terrain for underwater scenes
     * @param g Graphics context
     */
    private void drawBottomGround(Graphics g) {
        int groundHeight = 80;  // Height of ground layer
        int yStart = HEIGHT - groundHeight;  // Y position where ground starts
        
        // ===== GROUND SURFACE =====
        // Fill ground area with sand color
        g.setColor(new Color(194, 178, 128));  // Sand color
        fillRectangle(g, 0, yStart, WIDTH, groundHeight);
        
        // ===== SCATTERED ROCKS =====
        // Add decorative rocks on the ground surface
        g.setColor(new Color(120, 120, 120));  // Gray rock color
        for (int i = 0; i < 15; i++) {
            int x = 20 + i * 40;  // Space rocks evenly across ground
            int size = 10 + (i % 3) * 5;  // Vary rock sizes
            fillEllipse(g, x + size / 2, yStart + 20 + (i % 2) * 10 + size / 2, size / 2, size / 2);
        }
    }

    /**
     * Draws all seaweed plants in the underwater scene
     * Renders animated seaweed with swaying motion
     * @param g Graphics context
     */
    private void drawSeaweed(Graphics g) {
        int groundHeight = 80;  // Height of ground layer
        int yStart = HEIGHT - groundHeight;  // Y position where ground starts
        for (Seaweed s : seaweeds) {
            s.draw(g, yStart, frameCount);  // Draw each seaweed with animation
        }
    }

    /**
     * Draws all sea grass clusters in the underwater scene
     * Renders animated sea grass with gentle swaying
     * @param g Graphics context
     */
    private void drawSeaGrass(Graphics g) {
        int groundHeight = 80;  // Height of ground layer
        int yStart = HEIGHT - groundHeight;  // Y position where ground starts
        for (SeaGrass c : seaGrasses) {
            c.draw(g, yStart, frameCount);  // Draw each sea grass cluster with animation
        }
    }

    /**
     * Represents a single seaweed plant with animated swaying
     * Creates underwater vegetation with realistic movement
     */
    private class Seaweed {
        int baseX;              // X position of seaweed base
        int height;             // Total height of seaweed
        double swayAmplitude;   // How far the seaweed sways
        double swaySpeed;       // Speed of swaying animation
        int segments;           // Number of segments for smooth curves

        /**
         * Constructor for creating a seaweed plant
         * @param baseX X position of the base
         * @param height Total height of the seaweed
         * @param swayAmplitude Amplitude of swaying motion
         * @param swaySpeed Speed of swaying animation
         * @param segments Number of segments for smooth curves
         */
        Seaweed(int baseX, int height, double swayAmplitude, double swaySpeed, int segments) {
            this.baseX = baseX;
            this.height = height;
            this.swayAmplitude = swayAmplitude;
            this.swaySpeed = swaySpeed;
            this.segments = segments;
        }

        /**
         * Draws the seaweed with animated swaying motion
         * Creates a realistic underwater plant with flowing movement
         * @param g Graphics context
         * @param baseY Y position of the ground base
         * @param frame Current animation frame for timing
         */
        void draw(Graphics g, int baseY, int frame) {
            // ===== ANIMATION TIMING =====
            // Reduce movement frequency for smoother animation
            double t = frame * swaySpeed * 0.5;  // Time-based animation
            double segLen = (double) height / segments;  // Length of each segment

            // ===== SEAWEED STALK =====
            // Draw seaweed stalk using connected line segments
            g.setColor(new Color(20, 100, 60));  // Dark green stalk color
            int prevX = baseX;
            int prevY = baseY;

            for (int i = 1; i <= segments; i++) {
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress;  // Amplitude decreases toward top
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff * 0.7; // Reduced amplitude
                int nx = baseX + (int) dx;
                int ny = baseY - (int) (segLen * i);

                // Draw line segment using Bresenham algorithm
                drawBresenhamLine(g, prevX, prevY, nx, ny);
                prevX = nx;
                prevY = ny;
            }

            // ===== SEAWEED LEAVES =====
            // Add small leaves along the stalk - reduced frequency for performance
            g.setColor(new Color(30, 140, 80));  // Lighter green leaf color
            for (int i = 3; i < segments; i += 4) { // Increased spacing between leaves
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress;
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff * 0.7;
                int nx = baseX + (int) dx;
                int ny = baseY - (int) (segLen * i);
                int leafW = 8;   // Leaf width
                int leafH = 14;  // Leaf height
                fillEllipse(g, nx - leafW - 3 + leafW / 2, ny - leafH / 2 + leafH / 2, leafW / 2, leafH / 2); // Left leaf
                fillEllipse(g, nx + 3 + leafW / 2, ny - leafH / 2 + leafH / 2, leafW / 2, leafH / 2);        // Right leaf
            }
        }
    }

    /**
     * Represents a cluster of sea grass blades with animated swaying
     * Creates underwater grass with realistic flowing motion
     */
    private class SeaGrass {
        int baseX;              // X position of grass cluster base
        int bladeCount;         // Number of grass blades in cluster
        int spread;             // Width spread of the grass cluster
        int minHeight;          // Minimum height of grass blades
        int maxHeight;          // Maximum height of grass blades
        double swayAmplitude;   // How far the grass sways
        double swaySpeed;       // Speed of swaying animation

        /**
         * Constructor for creating a sea grass cluster
         * @param baseX X position of the cluster base
         * @param bladeCount Number of grass blades
         * @param spread Width spread of the cluster
         * @param minHeight Minimum blade height
         * @param maxHeight Maximum blade height
         * @param swayAmplitude Amplitude of swaying motion
         * @param swaySpeed Speed of swaying animation
         */
        SeaGrass(int baseX, int bladeCount, int spread, int minHeight, int maxHeight, double swayAmplitude,
                double swaySpeed) {
            this.baseX = baseX;
            this.bladeCount = bladeCount;
            this.spread = spread;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.swayAmplitude = swayAmplitude;
            this.swaySpeed = swaySpeed;
        }

        /**
         * Draws the sea grass cluster with animated swaying motion
         * Creates realistic underwater grass with flowing movement
         * @param g Graphics context
         * @param baseY Y position of the ground base
         * @param frame Current animation frame for timing
         */
        void draw(Graphics g, int baseY, int frame) {
            // ===== ANIMATION TIMING =====
            // Reduce movement frequency for smoother animation
            double t = frame * swaySpeed * 0.6;  // Time-based animation
            
            // ===== DRAW EACH GRASS BLADE =====
            for (int i = 0; i < bladeCount; i++) {
                // Calculate blade position within cluster spread
                int x = baseX + (int) ((i - bladeCount / 2.0) * (spread / (double) bladeCount));
                // Calculate blade height with pseudo-random variation
                int h = minHeight + (i * 37 % (maxHeight - minHeight + 1));
                double localPhase = i * 0.45;  // Individual blade phase for varied movement

                // ===== DRAW INDIVIDUAL BLADE =====
                // Draw blade using connected line segments
                int prevX = x;
                int prevY = baseY;
                int segments = 5;  // Number of segments per blade

                for (int s = 1; s <= segments; s++) {
                    double p = s / (double) segments;  // Progress along blade
                    double ampFalloff = 1.0 - 0.5 * p;  // Amplitude decreases toward blade tip
                    double dx = Math.sin(t + localPhase + p * 1.2) * swayAmplitude * ampFalloff * 0.6; // Reduced amplitude
                    int nx = x + (int) dx;
                    int ny = baseY - (int) (h * p);

                    // Draw blade segment using Bresenham algorithm
                    drawBresenhamLine(g, prevX, prevY, nx, ny);
                    prevX = nx;
                    prevY = ny;
                }
            }
        }
    }

    /**
     * Manages the bubble system for underwater scenes
     * Creates, updates, and draws rising bubbles with fade effects
     * @param g Graphics context
     */
    private void manageBubbles(Graphics g) {
        // ===== BUBBLE CREATION =====
        // Limit bubble creation rate to reduce jitter and maintain performance
        if (rand.nextInt(8) == 0 && bubbles.size() < 30) {
            int x = rand.nextInt(WIDTH - 40) + 20;  // Random X position
            int size = rand.nextInt(15) + 10;       // Random bubble size
            double speed = 0.8 + rand.nextDouble() * 1.5; // Slower, more stable speed
            bubbles.add(new Bubble(x, HEIGHT - size - 10, size, speed));
        }

        // ===== BUBBLE UPDATE AND DRAW =====
        // Update and draw all existing bubbles
        Iterator<Bubble> iter = bubbles.iterator();
        while (iter.hasNext()) {
            Bubble b = iter.next();
            b.update();  // Update bubble position and alpha
            if (b.alpha <= 0)
                iter.remove();  // Remove dead bubbles
            else
                b.draw(g);  // Draw live bubbles
        }
    }

    /**
     * Represents a single bubble in the underwater scene
     * Handles bubble movement, fading, and rendering
     */
    private class Bubble {
        int x, y, size;     // Position and size
        double speed;       // Rising speed
        float alpha;        // Transparency for fade effect

        /**
         * Constructor for creating a bubble
         * @param x X position
         * @param y Y position
         * @param size Bubble size
         * @param speed Rising speed
         */
        Bubble(int x, int y, int size, double speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.alpha = 1.0f;  // Start fully opaque
        }

        /**
         * Updates bubble position and fade effect
         * Moves bubble upward and fades it out near the top
         */
        void update() {
            y -= speed;  // Move bubble upward
            if (y <= 100) {  // Start fading when near top
                alpha -= 0.015f; // Slower fade for smoother effect
                if (alpha < 0)
                    alpha = 0;  // Ensure alpha doesn't go negative
            }
        }

        /**
         * Draws the bubble with highlight effect
         * @param g Graphics context
         */
        void draw(Graphics g) {
            // Only draw if alpha is significant to reduce flickering
            if (alpha > 0.1f) {
                g.setColor(Color.WHITE);  // White bubble outline
                drawCircle(g, x, y, size / 2);
                g.setColor(new Color(255, 255, 255, (int) (alpha * 150)));  // Highlight with alpha
                fillEllipse(g, x - size / 4 + size / 8, y - size / 3 + size / 8, size / 8, size / 8);
            }
        }
    }

    /**
     * Represents a heart object for the mating scene
     * Handles heart drawing, blinking animation, and positioning
     */
    private class Heart {
        int x, y, size;           // Position and size of heart
        boolean visible = true;   // Visibility for blinking effect
        double speed;             // Movement speed (not used in current implementation)
        int blinkCounter = 0;     // Counter for blinking animation
        int blinkRate = 15;       // Frames between blinks

        /**
         * Constructor for creating a heart object
         * @param x X coordinate
         * @param y Y coordinate
         * @param size Heart size
         * @param speed Movement speed (unused)
         */
        Heart(double x, double y, int size, double speed) {
            this.x = (int) x;
            this.y = (int) y;
            this.size = size;
            this.speed = speed;
        }

        /**
         * Updates heart blinking animation
         * Toggles visibility based on blink rate
         */
        void update() {
            blinkCounter++;
            if (blinkCounter >= blinkRate) {
                visible = !visible;  // Toggle visibility
                blinkCounter = 0;    // Reset counter
            }
        }

        /**
         * Draws the heart if it's visible
         * @param g Graphics context
         */
        void draw(Graphics g) {
            if (!visible)
                return;  // Don't draw if not visible

            g.setColor(Color.RED);  // Red heart color
            drawHeartShape(g, x, y, size);
        }

        /**
         * Draws a heart shape using mathematical equations
         * Creates a realistic heart using parametric equations
         * @param g Graphics context
         * @param cx Center X coordinate
         * @param cy Center Y coordinate
         * @param size Heart size
         */
        void drawHeartShape(Graphics g, int cx, int cy, int size) {
            // Draw heart using symmetric Polygon
            int w = size;                    // Width
            int h = (int)(size * 0.9);       // Height (slightly shorter)
            int n = 100;                     // Number of points per half heart
            int[] xp = new int[2 * n + 1];   // X coordinates array
            int[] yp = new int[2 * n + 1];   // Y coordinates array
            
            // ===== LEFT HALF OF HEART =====
            // Create left half of heart using parametric equations
            for (int i = 0; i <= n; i++) {
                double t = Math.PI - (Math.PI * i / n);  // Parameter for left half
                double x = 16 * Math.pow(Math.sin(t), 3);  // X equation
                double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);  // Y equation
                xp[i] = cx + (int)(w * x / 32.0);  // Scale and position X
                yp[i] = cy - (int)(h * y / 26.0);  // Scale and position Y
            }
            
            // ===== RIGHT HALF OF HEART =====
            // Create right half of heart using parametric equations
            for (int i = 1; i <= n; i++) {
                double t = Math.PI + (Math.PI * i / n);  // Parameter for right half
                double x = 16 * Math.pow(Math.sin(t), 3);  // X equation
                double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);  // Y equation
                xp[n + i] = cx + (int)(w * x / 32.0);  // Scale and position X
                yp[n + i] = cy - (int)(h * y / 26.0);  // Scale and position Y
            }
            
            // Create and fill the heart polygon
            Polygon heartPoly = new Polygon(xp, yp, 2 * n + 1);
            g.fillPolygon(heartPoly);
        }

        /**
         * Checks if a point is inside the heart shape
         * Uses polar coordinates for accurate heart boundary detection
         * @param x X coordinate to test
         * @param y Y coordinate to test
         * @return true if point is inside heart, false otherwise
         */
        private boolean isInsideHeart(double x, double y) {
            // Better heart equation that actually looks like a heart
            // Using polar coordinates approach
            double r = Math.sqrt(x * x + y * y);  // Distance from center
            double theta = Math.atan2(y, x);      // Angle from center

            // Heart shape in polar coordinates
            double heartR = 0.5 * (1 + Math.sin(theta)) * (1 + 0.3 * Math.cos(theta) - 0.1 * Math.cos(2 * theta));

            return r <= heartR;  // Point is inside if distance is less than heart radius
        }
    }

    /**
     * Creates explosion and smoke particle systems
     * Generates particles for dramatic meteor impact effect
     */
    private void createExplosionParticles() {
        // Clear existing particles
        explosionParticles.clear();
        smokeParticles.clear();
        Random rand = new Random();

        // ===== EXPLOSION PARTICLES =====
        // Create fire/explosion particles radiating outward
        for (int i = 0; i < 140; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;  // Random direction
            double speed = 3 + rand.nextDouble() * 6;        // Random speed
            double vx = Math.cos(angle) * speed;             // X velocity
            double vy = Math.sin(angle) * speed;             // Y velocity

            explosionParticles.add(new ExplosionParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,    // Position and velocity
                    rand.nextInt(12) + 6,                    // Random size
                    0.6f + rand.nextFloat() * 0.4f));       // Random alpha
        }

        // ===== SMOKE PARTICLES =====
        // Create smoke particles with upward bias
        for (int i = 0; i < 80; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;  // Random direction
            double speed = 0.5 + rand.nextDouble() * 1.5;   // Slower speed
            double vx = Math.cos(angle) * speed * 0.6;      // Reduced X velocity
            double vy = Math.sin(angle) * speed * 0.6 - (0.5 + rand.nextDouble() * 0.5); // Upward bias
            smokeParticles.add(new SmokeParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,    // Position and velocity
                    18 + rand.nextInt(22),                   // Random size
                    40 + rand.nextInt(50)));                // Random lifetime
        }
    }

    /**
     * Draws the main explosion effect with multiple layers
     * Creates a dramatic explosion with glow, core, and shockwave
     * @param g Graphics context
     * @param x X coordinate of explosion center
     * @param y Y coordinate of explosion center
     */
    private void drawExplosion(Graphics g, int x, int y) {
        if (explosionRadius > 0) {
            // ===== OUTER GLOW =====
            // Draw outer yellow glow effect
            g.setColor(new Color(255, 255, 0, 200));  // Yellow glow with transparency
            fillEllipse(g, x, y, explosionRadius, explosionRadius);

            // ===== INNER CORE =====
            // Draw bright white inner core
            g.setColor(new Color(255, 255, 255, 180));  // White core with transparency
            fillEllipse(g, x, y, explosionRadius * 2 / 3, explosionRadius * 2 / 3);

            // ===== SHOCKWAVE RING =====
            // Draw expanding shockwave ring
            if (shockwaveAlpha > 0f) {
                g.setColor(new Color(255, 255, 255, (int) (shockwaveAlpha * 255)));  // White shockwave
                drawCircle(g, x, y, shockwaveRadius);
            }
        }
    }

    /**
     * Draws all explosion particles
     * Renders fire/explosion particles for dramatic effect
     * @param g Graphics context
     */
    private void drawExplosionParticles(Graphics g) {
        for (ExplosionParticle particle : explosionParticles) {
            particle.draw(g);  // Draw each explosion particle
        }
    }

    /**
     * Draws all smoke particles
     * Renders smoke particles for atmospheric effect
     * @param g Graphics context
     */
    private void drawSmokeParticles(Graphics g) {
        for (SmokeParticle particle : smokeParticles) {
            particle.draw(g);  // Draw each smoke particle
        }
    }

    /**
     * Represents a single explosion particle
     * Handles fire/explosion particle movement, fading, and rendering
     */
    private class ExplosionParticle {
        double x, y, vx, vy;     // Position and velocity
        int size;                // Particle size
        float alpha;             // Transparency for fade effect
        float alphaDecay = 0.02f; // Rate of alpha decay

        /**
         * Constructor for creating an explosion particle
         * @param x Initial X position
         * @param y Initial Y position
         * @param vx X velocity
         * @param vy Y velocity
         * @param size Particle size
         * @param alpha Initial alpha value
         */
        ExplosionParticle(double x, double y, double vx, double vy, int size, float alpha) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = alpha;
        }

        /**
         * Updates particle position and fade effect
         * Applies gravity and alpha decay
         */
        void update() {
            x += vx;  // Update X position
            y += vy;  // Update Y position
            vy += 0.1;  // Apply gravity
            alpha -= alphaDecay;  // Fade out particle
            if (alpha < 0)
                alpha = 0;  // Ensure alpha doesn't go negative
        }

        /**
         * Draws the explosion particle with random fire colors
         * @param g Graphics context
         */
        void draw(Graphics g) {
            if (alpha <= 0)
                return;  // Don't draw if fully transparent

            // Fire color palette for explosion particles
            Color[] colors = {
                    new Color(255, 255, 0),   // Yellow
                    new Color(255, 165, 0),   // Orange
                    new Color(255, 69, 0),    // Red-orange
                    new Color(255, 0, 0)      // Red
            };
            g.setColor(colors[(int) (Math.random() * colors.length)]);  // Random fire color

            fillEllipse(g, (int) x, (int) y, size, size);  // Draw particle
        }
    }

    /**
     * Represents a single smoke particle
     * Handles smoke particle movement, growth, and fade effects
     */
    private class SmokeParticle {
        double x, y, vx, vy;     // Position and velocity
        double size;             // Current particle size
        double growth = 0.6;     // Rate of size growth
        float alpha = 0.0f;      // Transparency (starts at 0)
        int life;                // Total lifetime in frames
        int age = 0;             // Current age in frames

        /**
         * Constructor for creating a smoke particle
         * @param x Initial X position
         * @param y Initial Y position
         * @param vx X velocity
         * @param vy Y velocity
         * @param startSize Initial particle size
         * @param life Total lifetime in frames
         */
        SmokeParticle(double x, double y, double vx, double vy, int startSize, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = startSize;
            this.life = life;
        }

        /**
         * Updates smoke particle position, size, and alpha
         * Implements fade-in and fade-out effects
         */
        void update() {
            x += vx;  // Update X position
            y += vy;  // Update Y position
            vy *= 0.98;  // Apply air resistance (slow down)
            size += growth;  // Grow particle size
            age++;  // Increment age
            float half = life / 2f;  // Midpoint of life
            if (age < half) {
                alpha = Math.min(0.6f, alpha + 0.03f);  // Fade in during first half
            } else {
                alpha = Math.max(0f, alpha - 0.02f);    // Fade out during second half
            }
        }

        /**
         * Checks if the smoke particle should be removed
         * @return true if particle is dead, false otherwise
         */
        boolean isDead() {
            return age >= life || alpha <= 0f;  // Dead if max age reached or fully transparent
        }

        /**
         * Draws the smoke particle
         * @param g Graphics context
         */
        void draw(Graphics g) {
            if (alpha <= 0)
                return;  // Don't draw if fully transparent
            g.setColor(new Color(50, 50, 50, (int) (alpha * 255)));  // Gray smoke with alpha
            fillEllipse(g, (int) (x), (int) (y), (int) size, (int) size);  // Draw smoke particle
        }
    }

    /**
     * Draws the complete forest scene with ground, vegetation, and puddle
     * Creates the outdoor environment for flying and mating scenes
     * @param g Graphics context
     */
    private void drawForest(Graphics g) {
        int groundY = HEIGHT - 180;  // Ground level
        
        // ===== GROUND SURFACE =====
        // Fill ground area with forest green
        g.setColor(new Color(34, 139, 34));  // Forest green
        fillRectangle(g, 0, groundY, WIDTH, HEIGHT - groundY);

        // ===== GRASS TEXTURE =====
        // Add grass texture along the ground
        g.setColor(new Color(0, 100, 0));  // Dark green grass
        for (int i = 0; i < WIDTH; i += 8) {
            int grassHeight = 3 + rand.nextInt(4);  // Random grass height
            drawBresenhamLine(g, i, groundY, i, groundY + grassHeight);
        }

        // ===== SCENE ELEMENTS =====
        drawPuddle(g);  // Draw water puddle

        // Draw all trees
        for (Tree tree : trees) {
            tree.draw(g, groundY);
        }

        // Draw all bushes
        for (Bush bush : bushes) {
            bush.draw(g, groundY);
        }

        // Update and draw floating leaves
        for (FloatingLeaf leaf : floatingLeaves) {
            leaf.update();
            leaf.draw(g);
        }
    }

    /**
     * Draws a water puddle with realistic water effects
     * Creates a reflective water surface with highlights and grass
     * @param g Graphics context
     */
    private void drawPuddle(Graphics g) {
        // ===== SHADOW =====
        // Draw shadow beneath the puddle
        g.setColor(new Color(0, 0, 0, 40));  // Black shadow with transparency
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 + 8,
                puddleHeight / 2 + 8);

        // ===== WATER SURFACE =====
        // Draw main water body
        g.setColor(new Color(100, 150, 255, 180));  // Blue water with transparency
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2, puddleHeight / 2);

        // ===== WATER HIGHLIGHTS =====
        // Add reflective highlights on water surface
        g.setColor(new Color(255, 255, 255, 120));  // White highlights with transparency
        fillEllipse(g, puddleX + 25 + puddleWidth / 8, puddleY + 12 + puddleHeight / 6, puddleWidth / 8,
                puddleHeight / 6);
        fillEllipse(g, puddleX + puddleWidth / 2 + puddleWidth / 10, puddleY + 15 + puddleHeight / 8, puddleWidth / 10,
                puddleHeight / 8);
        fillEllipse(g, puddleX + puddleWidth - 40 + puddleWidth / 12, puddleY + 10 + puddleHeight / 6, puddleWidth / 12,
                puddleHeight / 6);

        // ===== GRASS AROUND PUDDLE =====
        // Add grass around the puddle edge
        g.setColor(new Color(255,255,255,100));
        g.setColor(new Color(0, 100, 0));  // Dark green grass
        for (int i = 0; i < 12; i++) {
            int grassX = puddleX + (i * 18) + 15;  // Space grass evenly
            int grassHeight = 4 + rand.nextInt(4);  // Random grass height
            drawBresenhamLine(g, grassX, puddleY, grassX, puddleY - grassHeight);
        }
    }

    /**
     * Draws all flying birds in the forest scene
     * Updates and renders animated birds with wing flapping
     * @param g Graphics context
     */
    private void drawBirds(Graphics g) {
        for (Bird bird : birds) {
            bird.update();  // Update bird position and animation
            bird.draw(g);   // Draw the bird
        }
    }

    /**
     * Represents a single tree in the forest scene
     * Handles tree drawing with trunk and foliage
     */
    private class Tree {
        int baseX, height, trunkWidth;  // Tree position and dimensions

        /**
         * Constructor for creating a tree
         * @param baseX X position of the tree base
         * @param height Total height of the tree
         * @param trunkWidth Width of the tree trunk
         */
        Tree(int baseX, int height, int trunkWidth) {
            this.baseX = baseX;
            this.height = height;
            this.trunkWidth = trunkWidth;
        }

        /**
         * Draws the tree with trunk and foliage
         * Creates a realistic tree with shadow, trunk, and layered foliage
         * @param g Graphics context
         * @param groundY Y position of the ground level
         */
        void draw(Graphics g, int groundY) {
            // ===== SHADOW =====
            // Draw shadow beneath the tree
            g.setColor(new Color(0, 0, 0, 30));  // Black shadow with transparency
            fillEllipse(g, baseX, groundY - 10, 25, 10);

            // ===== TRUNK =====
            // Draw main trunk
            g.setColor(new Color(101, 67, 33));  // Brown trunk color
            fillRectangle(g, baseX - trunkWidth / 2, groundY - height + 40, trunkWidth, height - 40);

            // ===== TRUNK SHADOW =====
            // Add shadow detail to trunk
            g.setColor(new Color(0, 0, 0, 40));  // Darker shadow on trunk
            fillRectangle(g, baseX - trunkWidth / 2 + 2, groundY - height + 40, trunkWidth, height - 40);

            // ===== FOLIAGE LAYERS =====
            // Draw multiple layers of foliage for depth
            g.setColor(new Color(0, 100, 0));  // Dark green foliage
            fillEllipse(g, baseX, groundY - height + 30, 35, 30);  // Bottom layer
            fillEllipse(g, baseX, groundY - height + 45, 25, 25);  // Middle layer
            fillEllipse(g, baseX, groundY - height + 60, 30, 20);  // Top layer

            // ===== FOLIAGE HIGHLIGHTS =====
            // Add lighter green highlights on foliage
            g.setColor(new Color(0, 128, 0));  // Lighter green highlights
            fillEllipse(g, baseX, groundY - height + 30, 30, 25);  // Bottom highlight
            fillEllipse(g, baseX, groundY - height + 52, 20, 17);  // Top highlight
        }
    }

    /**
     * Represents a single bush in the forest scene
     * Handles bush drawing with shadow and foliage
     */
    private class Bush {
        int baseX, size;  // Bush position and size

        /**
         * Constructor for creating a bush
         * @param baseX X position of the bush base
         * @param size Size of the bush
         */
        Bush(int baseX, int size) {
            this.baseX = baseX;
            this.size = size;
        }

        /**
         * Draws the bush with shadow and foliage
         * Creates a simple bush with layered foliage effect
         * @param g Graphics context
         * @param groundY Y position of the ground level
         */
        void draw(Graphics g, int groundY) {
            // ===== SHADOW =====
            // Draw shadow beneath the bush
            g.setColor(new Color(0, 0, 0, 25));  // Black shadow with transparency
            fillEllipse(g, baseX, groundY - size / 2, size / 2, size / 2);

            // ===== MAIN FOLIAGE =====
            // Draw main bush foliage
            g.setColor(new Color(0, 128, 0));  // Light green foliage
            fillEllipse(g, baseX, groundY - size, size / 2, size / 2);

            // ===== FOLIAGE DETAIL =====
            // Add darker foliage detail for depth
            g.setColor(new Color(0, 100, 0));  // Darker green detail
            fillEllipse(g, baseX, groundY - size + size / 4, size / 4, size / 4);
        }
    }

    /**
     * Represents a flying bird in the forest scene
     * Handles bird movement, wing animation, and rendering
     */
    private class Bird {
        double x, y;           // Bird position
        double vx, vy;         // Bird velocity
        int wingState = 0;     // Wing animation state

        /**
         * Constructor for creating a flying bird
         * @param startX Initial X position
         * @param startY Initial Y position
         */
        Bird(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.vx = 1 + Math.random() * 2;  // Random horizontal speed
            this.vy = Math.sin(System.currentTimeMillis() * 0.001) * 0.5;  // Gentle vertical oscillation
        }

        /**
         * Updates bird position and wing animation
         * Handles movement and screen wrapping
         */
        void update() {
            x -= vx;  // Move bird left
            y += vy;  // Apply vertical oscillation

            // ===== SCREEN WRAPPING =====
            // Reset bird position when it goes off screen
            if (x > WIDTH + 50) {
                x = -50;  // Start from left side
                y = 80 + Math.random() * 100;  // Random height
            }

            wingState = (wingState + 1) % 6;  // Cycle wing animation
        }

        /**
         * Draws the bird with body, beak, and animated wings
         * @param g Graphics context
         */
        void draw(Graphics g) {
            int centerX = (int) x;
            int centerY = (int) y;

            // ===== BODY =====
            // Draw bird body and head
            g.setColor(new Color(139, 69, 19));  // Brown body color
            fillEllipse(g, centerX, centerY, 8, 4);   // Main body
            fillEllipse(g, centerX - 6, centerY, 4, 4); // Head

            // ===== BEAK =====
            // Draw orange beak using connected lines
            g.setColor(new Color(255, 165, 0));  // Orange beak
            int[] xPoints = { centerX - 12, centerX - 16, centerX - 12 };  // Beak shape
            int[] yPoints = { centerY - 2, centerY - 2, centerY + 2 };
            for (int i = 0; i < xPoints.length - 1; i++) {
                drawBresenhamLine(g, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            // ===== ANIMATED WINGS =====
            // Draw flapping wings with animation
            g.setColor(new Color(160, 82, 45));  // Wing color
            int wingOffset = wingState < 3 ? -2 : 2;  // Wing position based on animation state
            fillEllipse(g, centerX - 15 + 6, centerY - 2 + wingOffset, 6, 3);  // Left wing
            fillEllipse(g, centerX + 3 + 6, centerY - 2 + wingOffset, 6, 3);   // Right wing
        }
    }

    /**
     * Represents a floating leaf in the forest scene
     * Handles leaf movement, rotation, and rendering with transparency
     */
    private class FloatingLeaf {
        double x, y;                    // Leaf position
        double vx, vy;                  // Leaf velocity
        double rotation;                // Current rotation angle
        double rotationSpeed;           // Rotation speed
        int size;                       // Leaf size
        float alpha;                    // Transparency level

        /**
         * Constructor for creating a floating leaf
         * @param startX Initial X position
         * @param startY Initial Y position
         */
        FloatingLeaf(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.vx = (Math.random() - 0.5) * 0.5;  // Random horizontal drift
            this.vy = Math.random() * 0.3 + 0.2;    // Downward drift with variation
            this.rotation = Math.random() * 360;     // Random starting rotation
            this.rotationSpeed = (Math.random() - 0.5) * 2;  // Random rotation speed
            this.size = 8 + (int) (Math.random() * 6);       // Random size
            this.alpha = 0.6f + (float) (Math.random() * 0.4f); // Random transparency
        }

        /**
         * Updates leaf position and rotation
         * Handles movement and screen wrapping
         */
        void update() {
            x += vx;  // Update X position
            y += vy;  // Update Y position
            rotation += rotationSpeed;  // Update rotation

            // ===== SCREEN WRAPPING =====
            // Wrap leaf around screen edges
            if (x < -20)
                x = WIDTH + 20;  // Wrap from left to right
            if (x > WIDTH + 20)
                x = -20;  // Wrap from right to left
            if (y > HEIGHT + 20) {
                y = -20;  // Reset to top
                x = Math.random() * WIDTH;  // Random X position
            }
        }

        /**
         * Draws the floating leaf with transparency
         * Creates a simple leaf shape with vein detail
         * @param g Graphics context
         */
        void draw(Graphics g) {
            // ===== LEAF BODY =====
            // Draw main leaf body with transparency
            g.setColor(new Color(0, 100, 0, (int) (alpha * 255)));  // Green leaf with alpha
            fillEllipse(g, (int) x, (int) y, size / 2, size / 4);   // Oval leaf shape

            // ===== LEAF VEIN =====
            // Draw leaf vein detail
            g.setColor(new Color(0, 80, 0, (int) (alpha * 255)));   // Darker green vein
            drawBresenhamLine(g, (int) (x - size / 3), (int) y, (int) (x + size / 3), (int) y);  // Center vein
        }
    }

    /**
     * Represents a transformation particle for the underwater scene
     * Handles particle movement, fading, and rendering during egg transformation
     */
    private class TransformationParticle {
        double x, y;                // Particle position
        double vx, vy;              // Particle velocity
        int size;                   // Particle size
        float alpha;                // Transparency level
        float alphaDecay = 0.02f;   // Rate of alpha decay
        Color color;                // Particle color

        /**
         * Constructor for creating a transformation particle
         * @param x Initial X position
         * @param y Initial Y position
         * @param vx X velocity
         * @param vy Y velocity
         * @param size Particle size
         * @param color Particle color
         */
        TransformationParticle(double x, double y, double vx, double vy, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = 1.0f;  // Start fully opaque
            this.color = color;
        }

        /**
         * Updates particle position and fade effect
         * Applies gravity and alpha decay
         */
        void update() {
            x += vx;  // Update X position
            y += vy;  // Update Y position
            vy += 0.1;  // Apply gravity
            alpha -= alphaDecay;  // Fade out particle
            if (alpha < 0) alpha = 0;  // Ensure alpha doesn't go negative
        }

        /**
         * Draws the transformation particle with color and transparency
         * @param g Graphics context
         */
        void draw(Graphics g) {
            if (alpha <= 0) return;  // Don't draw if fully transparent
            
            // Apply particle color with current alpha
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            fillEllipse(g, (int) x, (int) y, size, size);  // Draw particle
        }

        /**
         * Checks if the transformation particle should be removed
         * @return true if particle is dead, false otherwise
         */
        boolean isDead() {
            return alpha <= 0;  // Dead if fully transparent
        }
    }

    /**
     * Main method - entry point of the application
     * Creates and displays the animation window
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Create the main window
        JFrame frame = new JFrame("The Great Mosquito Adventure");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        
        // Add the animation panel
        frame.add(new main());
        frame.pack();
        
        // Center the window on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
