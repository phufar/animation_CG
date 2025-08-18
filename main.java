import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class main extends JPanel implements ActionListener {

    private Timer timer;
    private final int WIDTH = 600, HEIGHT = 600;
    private ArrayList<Bubble> bubbles;
    private ArrayList<Point> clouds;
    private Random rand;

    // --- State Variables ---
    private int frameCount = 0;
    private int sceneState = -1; // -1 : intro,0: Egg, 1: Evolving, 2: Flying, 3: Landed, 4: Swatted, 5: Falling

    // Intro state
    private float introAlpha = 0f;
    private int introStep = 0;
    private long introStartTime = System.currentTimeMillis();
    private String[] introTexts = {
            "In my final breath,\nall I saw was the blinding glow\nthe last light before the dark claimed me",
            "Now... the wheel turns\nIt's time for me to be reborn",
            "The last memory that clung to me\n was the simple truth that\na male mosquito lives only seven days",
            "Such a brief flicker of existence, isn't it?"
    };
    private boolean isFlashing = false;

    // Underwater state
    private int eggBaseX = 300, eggBaseY = 530;
    private double eggSwingAngle = 0;
    private double mosquitoY;
    private int shellOffset;

    // Air scene state
    private double mosquitoX_air, mosquitoY_air;
    private double femaleMosquitoX, femaleMosquitoY;
    private double meetingX, meetingY;
    private Heart heart;
    double meteorX, meteorY;
    private double meteorSpeedY = 10;
    private boolean meteorHit = false;
    ArrayList<Integer> cloudSpeeds = new ArrayList<>();
    private ArrayList<Seaweed> seaweeds;
    private ArrayList<SeaGrass> seaGrasses;

    // Puddle on the ground
    private int puddleX = 30;
    private int puddleY = 500;
    private int puddleWidth = 500;
    private int puddleHeight = 60;

    // Forest scene variables
    private ArrayList<Tree> trees;
    private ArrayList<Bush> bushes;
    private ArrayList<Bird> birds;
    private ArrayList<FloatingLeaf> floatingLeaves;

    // Falling corpse state
    private double corpseVy = 0;
    private double corpseRotationDeg = 0;
    private double corpseRotSpeedDeg = 0;
    private boolean corpseOnWater = false;
    private double femaleCorpseVy = 0;
    private double femaleCorpseRotationDeg = 0;
    private double femaleCorpseRotSpeedDeg = 0;
    private boolean femaleCorpseOnWater = false;
    private boolean corpseSettleStarted = false;

    // Explosion state
    private ArrayList<ExplosionParticle> explosionParticles;
    private ArrayList<SmokeParticle> smokeParticles;
    private ArrayList<TransformationParticle> transformationParticles;
    private int explosionRadius = 0;
    private boolean explosionStarted = false;
    private boolean underwaterTransition = false;
    private double underwaterMosquitoY = 100;
    private double eggTransformationProgress = 0.0;
    private int shockwaveRadius = 0;
    private float shockwaveAlpha = 0f;
    private float flashAlpha = 0f;
    private double shakeIntensity = 0.0;
    private int shakeFrames = 0;

    // BufferedImage for drawing shapes
    private BufferedImage circleImage;
    private BufferedImage ellipseImage;
    private BufferedImage rectangleImage;

    // Custom drawing algorithms using Polygon and BufferedImage
    private void drawBresenhamLine(Graphics g, int x1, int y1, int x2, int y2) {
        // ใช้ Polygon แทน drawLine เพื่อประสิทธิภาพที่ดีกว่า
        Polygon line = new Polygon();
        line.addPoint(x1, y1);
        line.addPoint(x2, y2);
        g.drawPolygon(line);
    }

    // Helper method to create circle polygon
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

    // Helper method to create ellipse polygon
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

    // Helper method to create rectangle polygon
    private Polygon createRectanglePolygon(int x, int y, int width, int height) {
        Polygon rect = new Polygon();
        rect.addPoint(x, y);
        rect.addPoint(x + width, y);
        rect.addPoint(x + width, y + height);
        rect.addPoint(x, y + height);
        return rect;
    }

    // Fill circle using Polygon
    private void fillCircle(Graphics g, int xc, int yc, int r) {
        Polygon circle = createCirclePolygon(xc, yc, r);
        g.fillPolygon(circle);
    }

    // Draw circle outline using Polygon
    private void drawCircle(Graphics g, int xc, int yc, int r) {
        Polygon circle = createCirclePolygon(xc, yc, r);
        g.drawPolygon(circle);
    }

    // Fill ellipse using Polygon
    private void fillEllipse(Graphics g, int xc, int yc, int rx, int ry) {
        Polygon ellipse = createEllipsePolygon(xc, yc, rx, ry);
        g.fillPolygon(ellipse);
    }

    // Draw ellipse outline using Polygon
    private void drawEllipse(Graphics g, int xc, int yc, int rx, int ry) {
        Polygon ellipse = createEllipsePolygon(xc, yc, rx, ry);
        g.drawPolygon(ellipse);
    }

    // Fill rectangle using Polygon
    private void fillRectangle(Graphics g, int x, int y, int width, int height) {
        Polygon rect = createRectanglePolygon(x, y, width, height);
        g.fillPolygon(rect);
    }

    // Draw rectangle outline using Polygon
    private void drawRectangle(Graphics g, int x, int y, int width, int height) {
        Polygon rect = createRectanglePolygon(x, y, width, height);
        g.drawPolygon(rect);
    }

    // Create BufferedImage for circle
    private BufferedImage createCircleImage(int radius, Color color) {
        BufferedImage img = new BufferedImage(radius * 2 + 2, radius * 2 + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillOval(1, 1, radius * 2, radius * 2);
        g2d.dispose();
        return img;
    }

    // Create BufferedImage for ellipse
    private BufferedImage createEllipseImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillOval(1, 1, width, height);
        g2d.dispose();
        return img;
    }

    // Create BufferedImage for rectangle
    private BufferedImage createRectangleImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(1, 1, width, height);
        g2d.dispose();
        return img;
    }

    // Draw circle using BufferedImage
    private void drawCircleImage(Graphics g, int x, int y, int radius, Color color) {
        BufferedImage img = createCircleImage(radius, color);
        g.drawImage(img, x - radius - 1, y - radius - 1, null);
    }

    // Draw ellipse using BufferedImage
    private void drawEllipseImage(Graphics g, int x, int y, int width, int height, Color color) {
        BufferedImage img = createEllipseImage(width, height, color);
        g.drawImage(img, x - width/2 - 1, y - height/2 - 1, null);
    }

    // Draw rectangle using BufferedImage
    private void drawRectangleImage(Graphics g, int x, int y, int width, int height, Color color) {
        BufferedImage img = createRectangleImage(width, height, color);
        g.drawImage(img, x - 1, y - 1, null);
    }

    private void startFlash() {
        isFlashing = true;
        flashAlpha = 1f;
    }

    public main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        timer = new Timer(33, this);
        timer.start();
        rand = new Random();

        // Initialize explosion particles
        explosionParticles = new ArrayList<>();
        smokeParticles = new ArrayList<>();
        transformationParticles = new ArrayList<>();

        // Initialize objects
        bubbles = new ArrayList<>();
        clouds = new ArrayList<>();
        seaweeds = new ArrayList<>();
        seaGrasses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clouds.add(new Point(rand.nextInt(WIDTH), rand.nextInt(200) + 50));
            cloudSpeeds.add(1 + rand.nextInt(3));
        }

        // Seaweed stalks along bottom
        int seaweedCount = 15;
        for (int i = 0; i < seaweedCount; i++) {
            int baseX = 30 + i * (WIDTH - 60) / seaweedCount + rand.nextInt(20) - 10;
            int height = 60 + rand.nextInt(60);
            double amp = 6 + rand.nextDouble() * 8;
            double speed = 0.02 + rand.nextDouble() * 0.03;
            int segments = 12 + rand.nextInt(6);
            seaweeds.add(new Seaweed(baseX, height, amp, speed, segments));
        }

        // Small sea grass clusters along bottom
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

        // Initialize forest elements
        trees = new ArrayList<>();
        bushes = new ArrayList<>();
        birds = new ArrayList<>();
        floatingLeaves = new ArrayList<>();

        // Create trees across the bottom
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

        // Create flying birds
        for (int i = 0; i < 5; i++) {
            int startX = rand.nextInt(WIDTH);
            int startY = 80 + rand.nextInt(100);
            birds.add(new Bird(startX, startY));
        }

        // Create floating leaves
        for (int i = 0; i < 15; i++) {
            int startX = rand.nextInt(WIDTH);
            int startY = 150 + rand.nextInt(100);
            floatingLeaves.add(new FloatingLeaf(startX, startY));
        }

        meetingX = WIDTH / 2.0;
        meetingY = 120;
        femaleMosquitoX = (meetingX + 60);
        femaleMosquitoY = meetingY;
        mosquitoX_air = meetingX - 60;
        mosquitoY_air = meetingY;
        heart = new Heart((femaleMosquitoX + mosquitoX_air) / 2, meetingY, 30, 0);
        meteorX = mosquitoX_air;
        meteorY = mosquitoY_air - 100;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene(g);
    }

    private void drawScene(Graphics g) {
        if (sceneState == 6 && shakeIntensity > 0) {
            int sx = (int) ((rand.nextDouble() * 2 - 1) * shakeIntensity);
            int sy = (int) ((rand.nextDouble() * 2 - 1) * shakeIntensity);
            g.translate(sx, sy);
        }

        // พื้นหลัง
        if (sceneState <= 1 || sceneState == 5) {
            g.setColor(new Color(70, 130, 150));
            fillRectangle(g, 0, 0, WIDTH, HEIGHT);
            drawBottomGround(g);
            drawSeaGrass(g);
            drawSeaweed(g);
            manageBubbles(g);
            if (sceneState == 5) {
                drawSkyBackground(g, (int) mosquitoY_air);
            }
        } else if (sceneState >= 2 && sceneState <= 4) {
            drawSkyBackground(g, 0);
        } else if (sceneState == 5) {
            // Sky background will be drawn in case 5
        }

        if (sceneState == -1) {
            g.setColor(Color.BLACK);
            fillRectangle(g, 0, 0, WIDTH, HEIGHT);

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

        // flash check
        if (isFlashing) {
            g.setColor(new Color(255, 255, 255, (int) (flashAlpha * 255)));
            fillRectangle(g, 0, 0, getWidth(), getHeight());
        }

        // --- วาดองค์ประกอบหลักตามฉาก ---
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
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 10);
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 25;
                heart.x = heartX;
                heart.y = heartY;
                heart.draw(g);
            }
            case 4 -> {
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX + 60, (int) femaleMosquitoY);
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 10);
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 25;
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isFlashing) {
            flashAlpha -= 0.02f; // ลดความขาวลงทีละนิด
            if (flashAlpha <= 0f) {
                flashAlpha = 0f;
                isFlashing = false;
                // Don't change sceneState here anymore, let the transformation logic handle it
            }
            repaint();
            return;
        }
        if (sceneState == -1) {
            long elapsed = System.currentTimeMillis() - introStartTime;

            if (introAlpha < 1f) {
                introAlpha += 0.015f; // fade in
            } else if (elapsed > 3500) {
                introAlpha = 0f;
                introStep++;
                introStartTime = System.currentTimeMillis();

                if (introStep >= introTexts.length) {
                    isFlashing = true;
                    flashAlpha = 1.0f;
                    introStep++;
                    sceneState = 0; // ไปฉากแรก
                }
            }

            repaint();
            return; // หยุดไม่ให้ไปทำ logic เดิม
        }
        frameCount++;
        switch (sceneState) {
            case 0 -> { // ไข่สั่น
                if (frameCount > 60)
                    eggSwingAngle += 0.1;
                if (frameCount > 150) {
                    sceneState = 1;
                    mosquitoY = eggBaseY;
                    shellOffset = 0;
                }
            }
            case 1 -> { // ลอกคราบ
                mosquitoY -= 1.5;
                if (shellOffset < 25)
                    shellOffset++;
                if (mosquitoY < 80) {
                    sceneState = 2; // บินขึ้นฟ้า
                    mosquitoX_air = -40;
                    mosquitoY_air = meetingY;
                }
            }
            case 2 -> { // บินหาเป้าหมาย (บินไปตำแหน่ง mosquitoX_target, meetingY)
                double targetX = meetingX - 30;
                if (mosquitoX_air < targetX) {
                    mosquitoX_air += 3.0;

                    // ให้ทั้งตัวผู้และตัวเมียแกว่งพร้อมกัน
                    double deltaY = Math.sin(frameCount * 0.1) * 2;
                    mosquitoY_air = meetingY + deltaY;
                    femaleMosquitoY = meetingY + deltaY;

                } else {
                    mosquitoX_air = targetX;
                    mosquitoY_air = meetingY;
                    femaleMosquitoY = meetingY;

                    sceneState = 3;
                    frameCount = 0;
                    heart.visible = true;
                    heart.blinkCounter = 0;
                }
            }

            case 3 -> {
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2) - 15;
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 25;
                heart.x = heartX;
                heart.y = heartY;
                heart.update();

                if (frameCount > 120) {
                    sceneState = 4;
                    meteorX = mosquitoX_air;
                    meteorY = -100;
                    meteorHit = false;
                    frameCount = 0;
                }

            }

            case 4 -> {
                meteorY += meteorSpeedY;
                if (meteorY >= mosquitoY_air) {
                    meteorHit = true;
                    frameCount++;
                    if (frameCount > 15) {
                        sceneState = 6;
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
            case 5 -> {
                // Falling corpse physics with rotation and puddle impact (both mosquitoes)
                int centerPuddleX = puddleX + puddleWidth / 2;

                // Male - fall to center of puddle
                if (!corpseOnWater) {
                    corpseVy += 1.0; // Faster gravity
                    mosquitoY_air += corpseVy;
                    // Move X position towards center of puddle
                    if (mosquitoX_air < centerPuddleX - 15) {
                        mosquitoX_air += 4.0; // Faster movement
                    } else if (mosquitoX_air > centerPuddleX - 15) {
                        mosquitoX_air -= 4.0; // Faster movement
                    }
                    corpseRotationDeg += corpseRotSpeedDeg;
                    if (mosquitoY_air >= puddleY + puddleHeight) {
                        mosquitoY_air = puddleY + puddleHeight;
                        mosquitoX_air = centerPuddleX - 15; // Snap to center
                        corpseOnWater = true;
                        corpseVy *= -0.25; // small bounce
                        corpseRotSpeedDeg *= 0.5; // damp rotation
                        if (!corpseSettleStarted) {
                            frameCount = 0;
                            corpseSettleStarted = true;
                        }
                    }
                } else {
                    corpseVy *= 0.85;
                    corpseRotSpeedDeg *= 0.85;
                    mosquitoY_air += corpseVy;
                    corpseRotationDeg += corpseRotSpeedDeg;

                    // Faster sinking into puddle
                    if (mosquitoY_air < puddleY + puddleHeight + 20) {
                        mosquitoY_air += 1.0; // Faster sinking
                    }
                }

                // Female - fall to center of puddle
                if (!femaleCorpseOnWater) {
                    femaleCorpseVy += 1.0; // Faster gravity
                    femaleMosquitoY += femaleCorpseVy;
                    // Move X position towards center of puddle
                    if (femaleMosquitoX < centerPuddleX + 15) {
                        femaleMosquitoX += 4.0; // Faster movement
                    } else if (femaleMosquitoX > centerPuddleX + 15) {
                        femaleMosquitoX -= 4.0; // Faster movement
                    }
                    femaleCorpseRotationDeg += femaleCorpseRotSpeedDeg;
                    if (femaleMosquitoY >= puddleY + puddleHeight) {
                        femaleMosquitoY = puddleY + puddleHeight;
                        femaleMosquitoX = centerPuddleX + 15; // Snap to center
                        femaleCorpseOnWater = true;
                        femaleCorpseVy *= -0.25;
                        femaleCorpseRotSpeedDeg *= 0.5;
                        if (!corpseSettleStarted) {
                            frameCount = 0;
                            corpseSettleStarted = true;
                        }
                    }
                } else {
                    femaleCorpseVy *= 0.85;
                    femaleCorpseRotSpeedDeg *= 0.85;
                    femaleMosquitoY += femaleCorpseVy;
                    femaleCorpseRotationDeg += femaleCorpseRotSpeedDeg;

                    // Faster sinking into puddle
                    if (femaleMosquitoY < puddleY + puddleHeight + 20) {
                        femaleMosquitoY += 1.0; // Faster sinking
                    }
                }

                // After both are sunk in puddle and a short linger, transition to underwater
                if (corpseOnWater && femaleCorpseOnWater &&
                        mosquitoY_air >= puddleY + puddleHeight + 20 &&
                        femaleMosquitoY >= puddleY + puddleHeight + 20 &&
                        frameCount > 30 && !underwaterTransition) { // Add check to prevent multiple transitions
                    underwaterTransition = true;
                    underwaterMosquitoY = 0; // Start from top of screen
                    eggTransformationProgress = 0.0;
                    frameCount = 0;
                    // Reset corpse positions to prevent further sinking
                    mosquitoY_air = puddleY + puddleHeight + 20;
                    femaleMosquitoY = puddleY + puddleHeight + 20;
                    System.out.println("Transitioning to underwater scene!"); // Debug
                }

                // Underwater scene logic
                if (underwaterTransition) {
                    // Simple, smooth sinking to egg position - no bouncing
                    if (underwaterMosquitoY < eggBaseY) {
                        underwaterMosquitoY += 15.0; // Very fast, direct sinking

                        // Prevent overshooting and lock position
                        if (underwaterMosquitoY >= eggBaseY) {
                            underwaterMosquitoY = eggBaseY;
                        }
                    }

                    // Once at egg position, start transformation
                    if (underwaterMosquitoY >= eggBaseY) {
                        eggTransformationProgress += 0.08; // Slower transformation for better visual effect

                        // Create transformation particles
                        if (eggTransformationProgress > 0.2 && eggTransformationProgress < 0.8 && transformationParticles.size() < 50) {
                            if (rand.nextInt(3) == 0) { // Create particles occasionally
                                double angle = rand.nextDouble() * 2 * Math.PI;
                                double speed = 1 + rand.nextDouble() * 2;
                                double vx = Math.cos(angle) * speed;
                                double vy = Math.sin(angle) * speed - 1;
                                
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

                        // When transformation complete, reset to scene 0
                        if (eggTransformationProgress >= 1.0) {
                            // Add final flash effect
                            flashAlpha = 0.8f;
                            isFlashing = true;
                            frameCount = 0;
                            sceneState = 0;
                            underwaterTransition = false;
                            eggSwingAngle = 0;
                            bubbles.clear();
                            heart.visible = false;
                            meteorX = -100;
                            meteorY = -100;
                            explosionParticles.clear();
                            smokeParticles.clear();
                            transformationParticles.clear();
                            // Reset all corpse variables
                            corpseOnWater = false;
                            femaleCorpseOnWater = false;
                            corpseSettleStarted = false;
                            // Reset underwater variables
                            underwaterMosquitoY = 0;
                            eggTransformationProgress = 0.0;
                            // Reset flash variables
                            flashAlpha = 0f;
                            isFlashing = false;
                            // Reset all mosquito positions for new cycle
                            mosquitoX_air = -40;
                            mosquitoY_air = meetingY;
                            femaleMosquitoX = meetingX + 60;
                            femaleMosquitoY = meetingY;
                            // Reset heart
                            heart.visible = false;
                            heart.blinkCounter = 0;
                            // Reset corpse velocities and rotations
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
            case 6 -> {
                if (!explosionStarted) {
                    explosionStarted = true;
                    explosionRadius = 0;
                }

                // Big fireball expands quickly, then starts fading
                if (explosionRadius < 180) {
                    explosionRadius += 10;
                } else if (explosionRadius < 220) {
                    explosionRadius += 4;
                } else {
                    // Start shrinking the explosion after reaching max size
                    explosionRadius = Math.max(0, explosionRadius - 8);
                }

                // Shockwave ring
                shockwaveRadius += 15;
                shockwaveAlpha = Math.max(0f, shockwaveAlpha - 0.08f);

                // Screen flash fades quickly
                if (flashAlpha > 0f) {
                    flashAlpha *= 0.92f;
                    if (flashAlpha < 0.02f)
                        flashAlpha = 0f;
                }

                // Camera shake decay
                if (shakeFrames > 0) {
                    shakeIntensity *= 0.85;
                    shakeFrames--;
                } else {
                    shakeIntensity = 0;
                }

                // Update particles
                for (ExplosionParticle particle : explosionParticles) {
                    particle.update();
                }
                for (SmokeParticle particle : smokeParticles) {
                    particle.update();
                }

                // Remove dead particles
                explosionParticles.removeIf(particle -> particle.alpha <= 0);
                smokeParticles.removeIf(p -> p.isDead());

                // Shorter explosion scene - transition when explosion fades out
                if (frameCount > 45 || explosionRadius <= 0) {
                    sceneState = 5;
                    frameCount = 0;
                    explosionParticles.clear();
                    smokeParticles.clear();
                    flashAlpha = 0f;
                    shockwaveAlpha = 0f;
                    shakeIntensity = 0;
                    // Initialize corpse fall physics
                    corpseVy = 0.0;
                    corpseRotationDeg = rand.nextBoolean() ? -20 : 20;
                    corpseRotSpeedDeg = (rand.nextDouble() * 6 + 2) * (rand.nextBoolean() ? 1 : -1);
                    corpseOnWater = false;
                    femaleCorpseVy = 0.0;
                    femaleCorpseRotationDeg = rand.nextBoolean() ? -15 : 15;
                    femaleCorpseRotSpeedDeg = (rand.nextDouble() * 5 + 2) * (rand.nextBoolean() ? 1 : -1);
                    femaleCorpseOnWater = false;
                    corpseSettleStarted = false;
                }
            }
        }

        // อัปเดตก้อนเมฆ เฉพาะสถานะที่บิน (2,3,4)
        if (sceneState == 2 || sceneState == 3 || sceneState == 4) {
            for (int i = 0; i < clouds.size(); i++) {
                Point cloud = clouds.get(i);
                int speed = cloudSpeeds.get(i);
                cloud.x -= speed;
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

    // เพิ่ม method วาดยุงตัวเมียและหัวใจ
    private void drawFemaleMosquito(Graphics g, double x, double y) {
        // Use custom algorithms instead of Graphics 2D
        int centerX = (int) x;
        int centerY = (int) y;

        // Body using fillEllipse
        g.setColor(new Color(100, 20, 20));
        fillEllipse(g, centerX, centerY - 10, 5, 10); // Body
        fillEllipse(g, centerX, centerY - 20, 4, 4); // Head

        // Legs using Bresenham line algorithm
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 15, centerY - 10);
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 15, centerY - 10);
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 15, centerY - 5);
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 15, centerY - 5);

        // Wings using fillEllipse
        g.setColor(new Color(200, 200, 200));
        int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
        fillEllipse(g, centerX - 12, centerY - 15 + wingYOffset, 12, 7);
        fillEllipse(g, centerX + 12, centerY - 15 + wingYOffset, 12, 7);
    }

    private void drawMeteor(Graphics g, double x, double y) {
        int centerX = (int) x;
        int centerY = (int) y;
        int size = 300;

        // Tail effect using fillEllipse
        g.setColor(new Color(255, 140, 0, 180));
        fillEllipse(g, centerX + size / 6, centerY - size, size / 6, size);

        // Meteor body using fillEllipse
        g.setColor(new Color(180, 90, 40));
        fillEllipse(g, centerX, centerY, size / 2, size / 2);

        // Inner heated core using fillEllipse
        g.setColor(new Color(255, 100, 0, 200));
        fillEllipse(g, centerX + size / 16, centerY + size / 16, size * 3 / 8, size * 3 / 8);

        // Craters using fillEllipse
        g.setColor(new Color(50, 25, 0));
        fillEllipse(g, centerX + size / 6, centerY + size / 6, size / 20, size / 20);
        fillEllipse(g, centerX + size / 4, centerY + size / 8, size / 16, size / 16);
        fillEllipse(g, centerX + size / 8, centerY + size / 4, size / 24, size / 24);
    }

    // --- เมธอดวาดฉากใหม่ ---
    private void drawSkyBackground(Graphics g, int waterLevel) {
        // Sky gradient using fillRectangle (custom method) - fill entire screen
        g.setColor(new Color(135, 206, 250));
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // Clouds using fillEllipse
        if (sceneState != 5) {
            g.setColor(Color.WHITE);
            for (Point cloud : clouds) {
                fillEllipse(g, cloud.x + 50, cloud.y + 20, 50, 20);
                fillEllipse(g, cloud.x + 70, cloud.y, 40, 25);
            }
        }

        if (sceneState >= 2 && sceneState <= 4) {
            drawForest(g);
            drawBirds(g);
            drawPuddle(g);
        }
    }

    // Destroyed sky background - original scene but with destroyed elements
    private void drawDestroyedSkyBackground(Graphics g) {
        // Sky background (same as original)
        g.setColor(new Color(135, 206, 250));
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // Clouds (keep them)
        g.setColor(Color.WHITE);
        for (Point cloud : clouds) {
            fillEllipse(g, cloud.x + 50, cloud.y + 20, 50, 20);
            fillEllipse(g, cloud.x + 70, cloud.y, 40, 25);
        }

        // Ground with destroyed forest and changed water color
        drawDestroyedGround(g);
    }

    // Destroyed ground - trees and bushes destroyed, water color changed
    private void drawDestroyedGround(Graphics g) {
        int groundY = HEIGHT - 180;

        // Ground (keep it)
        g.setColor(new Color(34, 139, 34));
        fillRectangle(g, 0, groundY, WIDTH, HEIGHT - groundY);

        // Grass texture (keep it)
        g.setColor(new Color(0, 100, 0));
        for (int i = 0; i < WIDTH; i += 8) {
            int grassHeight = 3 + rand.nextInt(4);
            drawBresenhamLine(g, i, groundY, i, groundY + grassHeight);
        }

        // Puddle with changed color (darker, more ominous)
        drawDestroyedPuddle(g);

        // No trees, no bushes, no birds, no floating leaves (destroyed by meteor)
    }

    // Destroyed puddle with changed color
    private void drawDestroyedPuddle(Graphics g) {
        // Darker water color to show destruction
        g.setColor(new Color(0, 0, 0, 60));
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 + 8,
                puddleHeight / 2 + 8);

        g.setColor(new Color(50, 75, 128, 200)); // Darker blue
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2, puddleHeight / 2);

        // Darker highlights
        g.setColor(new Color(200, 200, 200, 80));
        fillEllipse(g, puddleX + 25 + puddleWidth / 8, puddleY + 12 + puddleHeight / 6, puddleWidth / 8,
                puddleHeight / 6);
        fillEllipse(g, puddleX + puddleWidth / 2 + puddleWidth / 10, puddleY + 15 + puddleHeight / 8, puddleWidth / 10,
                puddleHeight / 8);
        fillEllipse(g, puddleX + puddleWidth - 40 + puddleWidth / 12, puddleY + 10 + puddleHeight / 6, puddleWidth / 12,
                puddleHeight / 6);

        // Darker ripples
        g.setColor(new Color(200, 200, 200, 60));
        drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 30);
        drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 50);
        drawCircle(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 - 70);

        // Grass around puddle (keep it)
        g.setColor(new Color(0, 100, 0));
        for (int i = 0; i < 12; i++) {
            int grassX = puddleX + (i * 18) + 15;
            int grassHeight = 4 + rand.nextInt(4);
            drawBresenhamLine(g, grassX, puddleY, grassX, puddleY - grassHeight);
        }
    }

    // Underwater scene with floating mosquitoes that transform into egg
    private void drawUnderwaterScene(Graphics g) {
        // Underwater background
        g.setColor(new Color(70, 130, 150));
        fillRectangle(g, 0, 0, WIDTH, HEIGHT);

        // Draw underwater elements
        drawBottomGround(g);
        drawSeaGrass(g);
        drawSeaweed(g);
        manageBubbles(g);
        
        // Draw transformation particles
        for (TransformationParticle particle : transformationParticles) {
            particle.update();
            particle.draw(g);
        }
        // Remove dead particles
        transformationParticles.removeIf(particle -> particle.isDead());
        
        // Safety check - if too many particles, clear some
        if (transformationParticles.size() > 100) {
            transformationParticles.clear();
        }

        // Draw floating mosquitoes that slowly transform into egg
        if (eggTransformationProgress < 1.0) {
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

            // Draw egg forming with smoother alpha transition and glow effect
            if (eggTransformationProgress > 0.1) { // Start egg formation slightly later
                float eggAlpha = (float) ((eggTransformationProgress - 0.1) / 0.9);
                int finalEggAlpha = Math.min(255, (int) (eggAlpha * 255));
                
                // Add glow effect around the egg
                if (eggAlpha > 0.3) {
                    int glowSize = (int) (eggAlpha * 30);
                    g.setColor(new Color(255, 255, 200, (int) (eggAlpha * 100)));
                    fillEllipse(g, eggBaseX, eggBaseY, 50 + glowSize, 25 + glowSize);
                }
                
                g.setColor(new Color(139, 69, 19, finalEggAlpha));
                drawMosquitoEgg(g);
            }
        } else {
            // Fully transformed egg with glow
            g.setColor(new Color(255, 255, 200, 100));
            fillEllipse(g, eggBaseX, eggBaseY, 80, 55);
            g.setColor(new Color(139, 69, 19, 255));
            drawMosquitoEgg(g);
        }
    }

    private void drawFlyingMosquito(Graphics g, double x, double y, boolean flapping, double angle) {
        int centerX = (int) x;
        int centerY = (int) y;

        // Body using fillEllipse
        g.setColor(new Color(40, 40, 40));
        fillEllipse(g, centerX, centerY - 10, 5, 10);
        fillEllipse(g, centerX, centerY - 20, 4, 4);

        // Legs using Bresenham line algorithm
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 15, centerY - 10);
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 15, centerY - 10);
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 15, centerY - 5);
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 15, centerY - 5);

        // Wings using fillEllipse
        if (flapping) {
            g.setColor(new Color(200, 200, 200, 200));
            int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
            fillEllipse(g, centerX - 12, centerY - 15 + wingYOffset, 12, 7);
            fillEllipse(g, centerX + 12, centerY - 15 + wingYOffset, 12, 7);
        }
    }

    private void drawDeadMosquito(Graphics g, double x, double y, double rotationDeg) {
        int centerX = (int) x;
        int centerY = (int) y;

        // Body using fillEllipse
        g.setColor(new Color(50, 50, 50));
        fillEllipse(g, centerX, centerY - 10, 5, 10);

        // Head using fillEllipse
        g.setColor(new Color(60, 60, 60));
        fillEllipse(g, centerX, centerY - 20, 4, 4);

        // X-eyes using Bresenham line algorithm
        g.setColor(Color.RED.darker());
        drawBresenhamLine(g, centerX - 6, centerY - 26, centerX - 2, centerY - 22);
        drawBresenhamLine(g, centerX - 6, centerY - 22, centerX - 2, centerY - 26);
        drawBresenhamLine(g, centerX + 2, centerY - 26, centerX + 6, centerY - 22);
        drawBresenhamLine(g, centerX + 2, centerY - 22, centerX + 6, centerY - 26);

        // Limp legs using Bresenham line algorithm
        g.setColor(Color.BLACK);
        drawBresenhamLine(g, centerX, centerY - 10, centerX - 12, centerY - 2);
        drawBresenhamLine(g, centerX, centerY - 10, centerX + 12, centerY - 2);
        drawBresenhamLine(g, centerX, centerY - 15, centerX - 10, centerY - 6);
        drawBresenhamLine(g, centerX, centerY - 15, centerX + 10, centerY - 6);

        // Drooped wings using fillEllipse
        g.setColor(new Color(180, 180, 180, 120));
        fillEllipse(g, centerX - 11, centerY - 10, 11, 6);
        fillEllipse(g, centerX + 11, centerY - 10, 11, 6);
    }

    // --- โค้ดของฉากใต้น้ำ (แก้ไขสี) ---
    private void drawEvolvingMosquito(Graphics g, int x, int y, double progress) {
        x = x - 25;

        g.setColor(new Color(40, 40, 40));

        if (progress < 0.2) {
            // Draw evolving shape using Bresenham lines
            int[] xPoints = { x, x + 2, x - 2, x };
            int[] yPoints = { y, y - 10, y - 20, y - 30 };
            for (int i = 0; i < xPoints.length - 1; i++) {
                drawBresenhamLine(g, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }
        } else {
            int headSize = (int) (8 * ((progress - 0.2) / 0.8));
            fillEllipse(g, x, y - 10, 5, 10);
            fillEllipse(g, x, y - 20, headSize / 2, headSize / 2);
        }

        if (progress > 0.4) {
            g.setColor(Color.BLACK);
            int legLength = (int) (15 * ((progress - 0.4) / 0.6));
            drawBresenhamLine(g, x, y - 15, x - legLength, y - 10);
            drawBresenhamLine(g, x, y - 15, x + legLength, y - 10);
            drawBresenhamLine(g, x, y - 10, x - legLength, y - 5);
            drawBresenhamLine(g, x, y - 10, x + legLength, y - 5);
        }

        if (progress > 0.6) {
            int wingSize = (int) (25 * ((progress - 0.6) / 0.4));
            int wingAlpha = (int) (150 * ((progress - 0.6) / 0.4));
            g.setColor(new Color(200, 200, 200, wingAlpha));
            boolean flap = (y % 10 < 5);
            int wingYOffset = flap ? -5 : 0;
            fillEllipse(g, x - wingSize / 2, y - 15 + wingYOffset, wingSize / 2, 7);
            fillEllipse(g, x + wingSize / 2, y - 15 + wingYOffset, wingSize / 2, 7);
        }
    }

    private void drawCrackedEgg(Graphics g, int offset) {
        Color eggColor = new Color(139, 69, 19, 200);
        g.setColor(eggColor);
        int x = eggBaseX - 50;
        int y = eggBaseY;
        int width = 50;
        int height = 25;

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

    private void drawMosquitoEgg(Graphics g) {
        Color eggColor = new Color(139, 69, 19, 200);
        g.setColor(eggColor);
        int swingX = 0;
        if (frameCount > 60) {
            double sineValue = Math.sin(eggSwingAngle * 20);
            swingX = (int) (10 * Math.signum(sineValue));
        }
        int x = eggBaseX - 50 + swingX;
        int y = eggBaseY;
        int width = 50;
        int height = 25;

        // Use fillEllipse for egg
        fillEllipse(g, x + width / 2, y + height / 2, width / 2, height / 2);
    }

    private void drawBottomGround(Graphics g) {
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight;
        g.setColor(new Color(194, 178, 128));
        fillRectangle(g, 0, yStart, WIDTH, groundHeight);
        g.setColor(new Color(120, 120, 120));
        for (int i = 0; i < 15; i++) {
            int x = 20 + i * 40;
            int size = 10 + (i % 3) * 5;
            fillEllipse(g, x + size / 2, yStart + 20 + (i % 2) * 10 + size / 2, size / 2, size / 2);
        }
    }

    private void drawSeaweed(Graphics g) {
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight;
        for (Seaweed s : seaweeds) {
            s.draw(g, yStart, frameCount);
        }
    }

    private void drawSeaGrass(Graphics g) {
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight;
        for (SeaGrass c : seaGrasses) {
            c.draw(g, yStart, frameCount);
        }
    }

    private class Seaweed {
        int baseX;
        int height;
        double swayAmplitude;
        double swaySpeed;
        int segments;

        Seaweed(int baseX, int height, double swayAmplitude, double swaySpeed, int segments) {
            this.baseX = baseX;
            this.height = height;
            this.swayAmplitude = swayAmplitude;
            this.swaySpeed = swaySpeed;
            this.segments = segments;
        }

        void draw(Graphics g, int baseY, int frame) {
            // Reduce movement frequency for smoother animation
            double t = frame * swaySpeed * 0.5;
            double segLen = (double) height / segments;

            // Draw seaweed stalk using Bresenham lines
            g.setColor(new Color(20, 100, 60));
            int prevX = baseX;
            int prevY = baseY;

            for (int i = 1; i <= segments; i++) {
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress;
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff * 0.7; // Reduced amplitude
                int nx = baseX + (int) dx;
                int ny = baseY - (int) (segLen * i);

                // Draw line segment using Bresenham
                drawBresenhamLine(g, prevX, prevY, nx, ny);
                prevX = nx;
                prevY = ny;
            }

            // Small leaves using fillEllipse - reduced frequency
            g.setColor(new Color(30, 140, 80));
            for (int i = 3; i < segments; i += 4) { // Increased spacing
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress;
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff * 0.7;
                int nx = baseX + (int) dx;
                int ny = baseY - (int) (segLen * i);
                int leafW = 8;
                int leafH = 14;
                fillEllipse(g, nx - leafW - 3 + leafW / 2, ny - leafH / 2 + leafH / 2, leafW / 2, leafH / 2);
                fillEllipse(g, nx + 3 + leafW / 2, ny - leafH / 2 + leafH / 2, leafW / 2, leafH / 2);
            }
        }
    }

    private class SeaGrass {
        int baseX;
        int bladeCount;
        int spread;
        int minHeight;
        int maxHeight;
        double swayAmplitude;
        double swaySpeed;

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

        void draw(Graphics g, int baseY, int frame) {
            // Reduce movement frequency for smoother animation
            double t = frame * swaySpeed * 0.6;
            for (int i = 0; i < bladeCount; i++) {
                int x = baseX + (int) ((i - bladeCount / 2.0) * (spread / (double) bladeCount));
                int h = minHeight + (i * 37 % (maxHeight - minHeight + 1));
                double localPhase = i * 0.45;

                // Draw blade using Bresenham lines
                int prevX = x;
                int prevY = baseY;
                int segments = 5;

                for (int s = 1; s <= segments; s++) {
                    double p = s / (double) segments;
                    double ampFalloff = 1.0 - 0.5 * p;
                    double dx = Math.sin(t + localPhase + p * 1.2) * swayAmplitude * ampFalloff * 0.6; // Reduced
                                                                                                       // amplitude
                    int nx = x + (int) dx;
                    int ny = baseY - (int) (h * p);

                    drawBresenhamLine(g, prevX, prevY, nx, ny);
                    prevX = nx;
                    prevY = ny;
                }
            }
        }
    }

    private void manageBubbles(Graphics g) {
        // Limit bubble creation rate to reduce jitter
        if (rand.nextInt(8) == 0 && bubbles.size() < 30) {
            int x = rand.nextInt(WIDTH - 40) + 20;
            int size = rand.nextInt(15) + 10;
            double speed = 0.8 + rand.nextDouble() * 1.5; // Slower, more stable
            bubbles.add(new Bubble(x, HEIGHT - size - 10, size, speed));
        }

        Iterator<Bubble> iter = bubbles.iterator();
        while (iter.hasNext()) {
            Bubble b = iter.next();
            b.update();
            if (b.alpha <= 0)
                iter.remove();
            else
                b.draw(g);
        }
    }

    private class Bubble {
        int x, y, size;
        double speed;
        float alpha;

        Bubble(int x, int y, int size, double speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.alpha = 1.0f;
        }

        void update() {
            y -= speed;
            if (y <= 100) {
                alpha -= 0.015f; // Slower fade for smoother effect
                if (alpha < 0)
                    alpha = 0;
            }
        }

        void draw(Graphics g) {
            // Only draw if alpha is significant to reduce flickering
            if (alpha > 0.1f) {
                g.setColor(Color.WHITE);
                drawCircle(g, x, y, size / 2);
                g.setColor(new Color(255, 255, 255, (int) (alpha * 150)));
                fillEllipse(g, x - size / 4 + size / 8, y - size / 3 + size / 8, size / 8, size / 8);
            }
        }
    }

    private class Heart {
        int x, y, size;
        boolean visible = true;
        double speed;
        int blinkCounter = 0;
        int blinkRate = 15;

        Heart(double x, double y, int size, double speed) {
            this.x = (int) x;
            this.y = (int) y;
            this.size = size;
            this.speed = speed;
        }

        void update() {
            blinkCounter++;
            if (blinkCounter >= blinkRate) {
                visible = !visible;
                blinkCounter = 0;
            }
        }

        void draw(Graphics g) {
            if (!visible)
                return;

            g.setColor(Color.RED);
            drawHeartShape(g, x, y, size);
        }

        void drawHeartShape(Graphics g, int cx, int cy, int size) {
            double scale = size / 100.0;
            int heartWidth = (int) (60 * scale);
            int heartHeight = (int) (70 * scale);

            // Fill heart shape using proper heart equation
            for (int y = -heartHeight / 2; y <= heartHeight / 2; y++) {
                for (int x = -heartWidth / 2; x <= heartWidth / 2; x++) {
                    // Proper heart equation: (x² + y² - 1)³ - x²y³ ≤ 0
                    // But we need to scale and position it correctly
                    double nx = (double) x / (heartWidth / 2);
                    double ny = (double) y / (heartHeight / 2);

                    // Check if point is inside heart shape using better heart equation
                    if (isInsideHeart(nx, ny)) {
                        // ใช้ Polygon สำหรับจุดเดียว
                        Polygon point = new Polygon();
                        point.addPoint(cx + x, cy + y);
                        point.addPoint(cx + x, cy + y);
                        g.drawPolygon(point);
                    }
                }
            }
        }

        private boolean isInsideHeart(double x, double y) {
            // Better heart equation that actually looks like a heart
            // Using polar coordinates approach
            double r = Math.sqrt(x * x + y * y);
            double theta = Math.atan2(y, x);

            // Heart shape in polar coordinates
            double heartR = 0.5 * (1 + Math.sin(theta)) * (1 + 0.3 * Math.cos(theta) - 0.1 * Math.cos(2 * theta));

            return r <= heartR;
        }
    }

    private void createExplosionParticles() {
        explosionParticles.clear();
        smokeParticles.clear();
        Random rand = new Random();

        for (int i = 0; i < 140; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 3 + rand.nextDouble() * 6;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            explosionParticles.add(new ExplosionParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,
                    rand.nextInt(12) + 6,
                    0.6f + rand.nextFloat() * 0.4f));
        }

        for (int i = 0; i < 80; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 0.5 + rand.nextDouble() * 1.5;
            double vx = Math.cos(angle) * speed * 0.6;
            double vy = Math.sin(angle) * speed * 0.6 - (0.5 + rand.nextDouble() * 0.5);
            smokeParticles.add(new SmokeParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,
                    18 + rand.nextInt(22),
                    40 + rand.nextInt(50)));
        }
    }

    private void drawExplosion(Graphics g, int x, int y) {
        if (explosionRadius > 0) {
            // Outer glow using fillEllipse
            g.setColor(new Color(255, 255, 0, 200));
            fillEllipse(g, x, y, explosionRadius, explosionRadius);

            // Inner bright core using fillEllipse
            g.setColor(new Color(255, 255, 255, 180));
            fillEllipse(g, x, y, explosionRadius * 2 / 3, explosionRadius * 2 / 3);

            // Shockwave ring using drawMidpointCircle
            if (shockwaveAlpha > 0f) {
                g.setColor(new Color(255, 255, 255, (int) (shockwaveAlpha * 255)));
                drawCircle(g, x, y, shockwaveRadius);
            }
        }
    }

    private void drawExplosionParticles(Graphics g) {
        for (ExplosionParticle particle : explosionParticles) {
            particle.draw(g);
        }
    }

    private void drawSmokeParticles(Graphics g) {
        for (SmokeParticle particle : smokeParticles) {
            particle.draw(g);
        }
    }

    private class ExplosionParticle {
        double x, y, vx, vy;
        int size;
        float alpha;
        float alphaDecay = 0.02f;

        ExplosionParticle(double x, double y, double vx, double vy, int size, float alpha) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = alpha;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.1;
            alpha -= alphaDecay;
            if (alpha < 0)
                alpha = 0;
        }

        void draw(Graphics g) {
            if (alpha <= 0)
                return;

            Color[] colors = {
                    new Color(255, 255, 0),
                    new Color(255, 165, 0),
                    new Color(255, 69, 0),
                    new Color(255, 0, 0)
            };
            g.setColor(colors[(int) (Math.random() * colors.length)]);

            fillEllipse(g, (int) x, (int) y, size, size);
        }
    }

    private class SmokeParticle {
        double x, y, vx, vy;
        double size;
        double growth = 0.6;
        float alpha = 0.0f;
        int life;
        int age = 0;

        SmokeParticle(double x, double y, double vx, double vy, int startSize, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = startSize;
            this.life = life;
        }

        void update() {
            x += vx;
            y += vy;
            vy *= 0.98;
            size += growth;
            age++;
            float half = life / 2f;
            if (age < half) {
                alpha = Math.min(0.6f, alpha + 0.03f);
            } else {
                alpha = Math.max(0f, alpha - 0.02f);
            }
        }

        boolean isDead() {
            return age >= life || alpha <= 0f;
        }

        void draw(Graphics g) {
            if (alpha <= 0)
                return;
            g.setColor(new Color(50, 50, 50, (int) (alpha * 255)));
            fillEllipse(g, (int) (x), (int) (y), (int) size, (int) size);
        }
    }

    private void drawForest(Graphics g) {
        int groundY = HEIGHT - 180;
        g.setColor(new Color(34, 139, 34));
        fillRectangle(g, 0, groundY, WIDTH, HEIGHT - groundY);

        g.setColor(new Color(0, 100, 0));
        for (int i = 0; i < WIDTH; i += 8) {
            int grassHeight = 3 + rand.nextInt(4);
            drawBresenhamLine(g, i, groundY, i, groundY + grassHeight);
        }

        drawPuddle(g);

        for (Tree tree : trees) {
            tree.draw(g, groundY);
        }

        for (Bush bush : bushes) {
            bush.draw(g, groundY);
        }

        for (FloatingLeaf leaf : floatingLeaves) {
            leaf.update();
            leaf.draw(g);
        }
    }

    private void drawPuddle(Graphics g) {
        g.setColor(new Color(0, 0, 0, 40));
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2 + 8,
                puddleHeight / 2 + 8);

        g.setColor(new Color(100, 150, 255, 180));
        fillEllipse(g, puddleX + puddleWidth / 2, puddleY + puddleHeight / 2, puddleWidth / 2, puddleHeight / 2);

        g.setColor(new Color(255, 255, 255, 120));
        fillEllipse(g, puddleX + 25 + puddleWidth / 8, puddleY + 12 + puddleHeight / 6, puddleWidth / 8,
                puddleHeight / 6);
        fillEllipse(g, puddleX + puddleWidth / 2 + puddleWidth / 10, puddleY + 15 + puddleHeight / 8, puddleWidth / 10,
                puddleHeight / 8);
        fillEllipse(g, puddleX + puddleWidth - 40 + puddleWidth / 12, puddleY + 10 + puddleHeight / 6, puddleWidth / 12,
                puddleHeight / 6);

        g.setColor(new Color(255, 255, 255, 100));

        g.setColor(new Color(0, 100, 0));
        for (int i = 0; i < 12; i++) {
            int grassX = puddleX + (i * 18) + 15;
            int grassHeight = 4 + rand.nextInt(4);
            drawBresenhamLine(g, grassX, puddleY, grassX, puddleY - grassHeight);
        }
    }

    private void drawBirds(Graphics g) {
        for (Bird bird : birds) {
            bird.update();
            bird.draw(g);
        }
    }

    private class Tree {
        int baseX, height, trunkWidth;

        Tree(int baseX, int height, int trunkWidth) {
            this.baseX = baseX;
            this.height = height;
            this.trunkWidth = trunkWidth;
        }

        void draw(Graphics g, int groundY) {
            g.setColor(new Color(0, 0, 0, 30));
            fillEllipse(g, baseX, groundY - 10, 25, 10);

            g.setColor(new Color(101, 67, 33));
            fillRectangle(g, baseX - trunkWidth / 2, groundY - height + 40, trunkWidth, height - 40);

            g.setColor(new Color(0, 0, 0, 40));
            fillRectangle(g, baseX - trunkWidth / 2 + 2, groundY - height + 40, trunkWidth, height - 40);

            g.setColor(new Color(0, 100, 0));
            fillEllipse(g, baseX, groundY - height + 30, 35, 30);
            fillEllipse(g, baseX, groundY - height + 45, 25, 25);
            fillEllipse(g, baseX, groundY - height + 60, 30, 20);

            g.setColor(new Color(0, 128, 0));
            fillEllipse(g, baseX, groundY - height + 30, 30, 25);
            fillEllipse(g, baseX, groundY - height + 52, 20, 17);
        }
    }

    private class Bush {
        int baseX, size;

        Bush(int baseX, int size) {
            this.baseX = baseX;
            this.size = size;
        }

        void draw(Graphics g, int groundY) {
            g.setColor(new Color(0, 0, 0, 25));
            fillEllipse(g, baseX, groundY - size / 2, size / 2, size / 2);

            g.setColor(new Color(0, 128, 0));
            fillEllipse(g, baseX, groundY - size, size / 2, size / 2);

            g.setColor(new Color(0, 100, 0));
            fillEllipse(g, baseX, groundY - size + size / 4, size / 4, size / 4);
        }
    }

    private class Bird {
        double x, y;
        double vx, vy;
        int wingState = 0;

        Bird(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.vx = 1 + Math.random() * 2;
            this.vy = Math.sin(System.currentTimeMillis() * 0.001) * 0.5;
        }

        void update() {
            x -= vx;
            y += vy;

            if (x > WIDTH + 50) {
                x = -50;
                y = 80 + Math.random() * 100;
            }

            wingState = (wingState + 1) % 6;
        }

        void draw(Graphics g) {
            int centerX = (int) x;
            int centerY = (int) y;

            g.setColor(new Color(139, 69, 19));
            fillEllipse(g, centerX, centerY, 8, 4);
            fillEllipse(g, centerX - 6, centerY, 4, 4);

            g.setColor(new Color(255, 165, 0));
            int[] xPoints = { centerX - 12, centerX - 16, centerX - 12 };
            int[] yPoints = { centerY - 2, centerY - 2, centerY + 2 };
            for (int i = 0; i < xPoints.length - 1; i++) {
                drawBresenhamLine(g, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            g.setColor(new Color(160, 82, 45));
            int wingOffset = wingState < 3 ? -2 : 2;
            fillEllipse(g, centerX - 15 + 6, centerY - 2 + wingOffset, 6, 3);
            fillEllipse(g, centerX + 3 + 6, centerY - 2 + wingOffset, 6, 3);
        }
    }

    private class FloatingLeaf {
        double x, y;
        double vx, vy;
        double rotation;
        double rotationSpeed;
        int size;
        float alpha;

        FloatingLeaf(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.vx = (Math.random() - 0.5) * 0.5;
            this.vy = Math.random() * 0.3 + 0.2;
            this.rotation = Math.random() * 360;
            this.rotationSpeed = (Math.random() - 0.5) * 2;
            this.size = 8 + (int) (Math.random() * 6);
            this.alpha = 0.6f + (float) (Math.random() * 0.4f);
        }

        void update() {
            x += vx;
            y += vy;
            rotation += rotationSpeed;

            if (x < -20)
                x = WIDTH + 20;
            if (x > WIDTH + 20)
                x = -20;
            if (y > HEIGHT + 20) {
                y = -20;
                x = Math.random() * WIDTH;
            }
        }

        void draw(Graphics g) {
            g.setColor(new Color(0, 100, 0, (int) (alpha * 255)));
            fillEllipse(g, (int) x, (int) y, size / 2, size / 4);

            g.setColor(new Color(0, 80, 0, (int) (alpha * 255)));
            drawBresenhamLine(g, (int) (x - size / 3), (int) y, (int) (x + size / 3), (int) y);
        }
    }

    private class TransformationParticle {
        double x, y;
        double vx, vy;
        int size;
        float alpha;
        float alphaDecay = 0.02f;
        Color color;

        TransformationParticle(double x, double y, double vx, double vy, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = 1.0f;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.1; // Gravity
            alpha -= alphaDecay;
            if (alpha < 0) alpha = 0;
        }

        void draw(Graphics g) {
            if (alpha <= 0) return;
            
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            fillEllipse(g, (int) x, (int) y, size, size);
        }

        boolean isDead() {
            return alpha <= 0;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("The Great Mosquito Adventure");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new main());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
