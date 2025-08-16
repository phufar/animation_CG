import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class main extends JPanel implements ActionListener {

    private Timer timer;
    private final int WIDTH = 600, HEIGHT = 600;
    private ArrayList<Bubble> bubbles;
    private ArrayList<Point> clouds;
    private Random rand;

    // --- Drawing Algorithms ---
    
    // Bresenham's line algorithm
    private void drawBresenhamLine(Graphics2D g, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        int x = x1, y = y1;
        
        while (true) {
            g.fillRect(x, y, 1, 1);
            
            if (x == x2 && y == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
    
    // Midpoint circle algorithm
    private void drawMidpointCircle(Graphics2D g, int xc, int yc, int r) {
        int x = 0, y = r;
        int d = 1 - r;
        
        plotCirclePoints(g, xc, yc, x, y);
        
        while (x < y) {
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
            plotCirclePoints(g, xc, yc, x, y);
        }
    }
    
    private void plotCirclePoints(Graphics2D g, int xc, int yc, int x, int y) {
        g.fillRect(xc + x, yc + y, 1, 1);
        g.fillRect(xc - x, yc + y, 1, 1);
        g.fillRect(xc + x, yc - y, 1, 1);
        g.fillRect(xc - x, yc - y, 1, 1);
        g.fillRect(xc + y, yc + x, 1, 1);
        g.fillRect(xc - y, yc + x, 1, 1);
        g.fillRect(xc + y, yc - x, 1, 1);
        g.fillRect(xc - y, yc - x, 1, 1);
    }
    
    // Midpoint ellipse algorithm
    private void drawMidpointEllipse(Graphics2D g, int xc, int yc, int rx, int ry) {
        int x = 0, y = ry;
        int rx2 = rx * rx;
        int ry2 = ry * ry;
        int twoRx2 = 2 * rx2;
        int twoRy2 = 2 * ry2;
        int p;
        
        // Region 1
        p = (int) (ry2 - rx2 * ry + 0.25 * rx2);
        int px = 0, py = twoRx2 * y;
        
        while (px < py) {
            plotEllipsePoints(g, xc, yc, x, y);
            x++;
            px += twoRy2;
            if (p < 0) {
                p += ry2 + px;
            } else {
                y--;
                py -= twoRx2;
                p += ry2 + px - py;
            }
        }
        
        // Region 2
        p = (int) (ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        px = 0;
        py = twoRx2 * y;
        
        while (y > 0) {
            plotEllipsePoints(g, xc, yc, x, y);
            y--;
            py -= twoRx2;
            if (p > 0) {
                p += rx2 - py;
            } else {
                x++;
                px += twoRy2;
                p += rx2 - py + px;
            }
        }
    }
    
    private void plotEllipsePoints(Graphics2D g, int xc, int yc, int x, int y) {
        g.fillRect(xc + x, yc + y, 1, 1);
        g.fillRect(xc - x, yc + y, 1, 1);
        g.fillRect(xc + x, yc - y, 1, 1);
        g.fillRect(xc - x, yc - y, 1, 1);
    }
    
    // Bezier curve algorithm
    private void drawBezierCurve(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        int steps = 50;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;
            
            int x = (int) (u * u * u * x1 + 3 * u * u * t * x2 + 3 * u * t * t * x3 + t * t * t * x4);
            int y = (int) (u * u * u * y1 + 3 * u * u * t * y2 + 3 * u * t * t * y3 + t * t * t * y4);
            
            g.fillRect(x, y, 1, 1);
        }
    }
    
    // Fill circle using midpoint algorithm
    private void fillMidpointCircle(Graphics2D g, int xc, int yc, int r) {
        int x = 0, y = r;
        int d = 1 - r;
        
        while (x <= y) {
            // Draw horizontal lines to fill the circle
            drawBresenhamLine(g, xc - x, yc + y, xc + x, yc + y);
            drawBresenhamLine(g, xc - x, yc - y, xc + x, yc - y);
            drawBresenhamLine(g, xc - y, yc + x, xc + y, yc + x);
            drawBresenhamLine(g, xc - y, yc - x, xc + y, yc - x);
            
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
    }
    
    // Fill ellipse using midpoint algorithm
    private void fillMidpointEllipse(Graphics2D g, int xc, int yc, int rx, int ry) {
        int x = 0, y = ry;
        int rx2 = rx * rx;
        int ry2 = ry * ry;
        int twoRx2 = 2 * rx2;
        int twoRy2 = 2 * ry2;
        int p;
        
        // Region 1
        p = (int) (ry2 - rx2 * ry + 0.25 * rx2);
        int px = 0, py = twoRx2 * y;
        
        while (px < py) {
            // Fill horizontal lines
            drawBresenhamLine(g, xc - x, yc + y, xc + x, yc + y);
            drawBresenhamLine(g, xc - x, yc - y, xc + x, yc - y);
            
            x++;
            px += twoRy2;
            if (p < 0) {
                p += ry2 + px;
            } else {
                y--;
                py -= twoRx2;
                p += ry2 + px - py;
            }
        }
        
        // Region 2
        p = (int) (ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        px = 0;
        py = twoRx2 * y;
        
        while (y > 0) {
            // Fill horizontal lines
            drawBresenhamLine(g, xc - x, yc + y, xc + x, yc + y);
            drawBresenhamLine(g, xc - x, yc - y, xc + x, yc - y);
            
            y--;
            py -= twoRx2;
            if (p > 0) {
                p += rx2 - py;
            } else {
                x++;
                px += twoRy2;
                p += rx2 - py + px;
            }
        }
    }

    // --- State Variables ---
    private int frameCount = 0;
    private int sceneState = -1; // -1 : intro,0: Egg, 1: Evolving, 2: Flying, 3: Landed, 4: Swatted, 5: Falling

    // Intro state
    private float introAlpha = 0f;
    private int introStep = 0;
    private long introStartTime = System.currentTimeMillis();
    // private String[] introTexts = {
    //         "I died seeing only the final light\nin my last moments.",
    //         "It's time for me to be reborn.",
    //         "The last things I known was that \na male mosquito lives only seven days...\nso short, isn't it?"
    // };

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
    private int puddleY = 500; // กำหนดพิกัด Y ของบ่อน้ำเอง
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
    private int explosionRadius = 0;
    private boolean explosionStarted = false;
    private int shockwaveRadius = 0;
    private float shockwaveAlpha = 0f;
    private float flashAlpha = 0f;
    private double shakeIntensity = 0.0;
    private int shakeFrames = 0;

    private void startFlash() {
        isFlashing = true;
        flashAlpha = 1f; // เริ่มจากขาวเต็มจอ
    }

    public main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        timer = new Timer(33, this);
        timer.start();
        rand = new Random();

        // Initialize explosion particles
        explosionParticles = new ArrayList<>();
        smokeParticles = new ArrayList<>();

        // Initialize objects
        bubbles = new ArrayList<>();
        clouds = new ArrayList<>();
        seaweeds = new ArrayList<>();
        seaGrasses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clouds.add(new Point(rand.nextInt(WIDTH), rand.nextInt(200) + 50));
            cloudSpeeds.add(1 + rand.nextInt(3)); // ความเร็ว 1-3 pixel/frame
        }
        // Seaweed stalks along bottom - ลดคุณภาพ
        int seaweedCount = 8; // ลดจาก 15 เป็น 8
        for (int i = 0; i < seaweedCount; i++) {
            int baseX = 30 + i * (WIDTH - 60) / seaweedCount + rand.nextInt(20) - 10;
            int height = 60 + rand.nextInt(60);
            double amp = 6 + rand.nextDouble() * 8;
            double speed = 0.02 + rand.nextDouble() * 0.03;
            int segments = 8 + rand.nextInt(4); // ลดจาก 12+6 เป็น 8+4
            seaweeds.add(new Seaweed(baseX, height, amp, speed, segments));
        }

        // Small sea grass clusters along bottom - ลดคุณภาพ
        int clusterCount = 15; // ลดจาก 30 เป็น 15
        for (int i = 0; i < clusterCount; i++) {
            int baseX = 15 + i * (WIDTH - 30) / clusterCount + rand.nextInt(10) - 5;
            int blades = 4 + rand.nextInt(4); // ลดจาก 6+6 เป็น 4+4
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
        
        // Create trees across the bottom - ลดคุณภาพ
        int treeCount = 5; // ลดจาก 8 เป็น 5
        for (int i = 0; i < treeCount; i++) {
            int baseX = 50 + i * (WIDTH - 100) / treeCount + rand.nextInt(40) - 20;
            int height = 80 + rand.nextInt(60); // Shorter trees so mosquito flies above
            int trunkWidth = 8 + rand.nextInt(6);
            trees.add(new Tree(baseX, height, trunkWidth));
        }
        
        // Create bushes between trees - ลดคุณภาพ
        int bushCount = 8; // ลดจาก 12 เป็น 8
        for (int i = 0; i < bushCount; i++) {
            int baseX = 30 + i * (WIDTH - 60) / bushCount + rand.nextInt(30) - 15;
            int size = 20 + rand.nextInt(25);
            bushes.add(new Bush(baseX, size));
        }
        
        // Create flying birds - ลบออก
        // for (int i = 0; i < 5; i++) {
        //     int startX = rand.nextInt(WIDTH);
        //     int startY = 80 + rand.nextInt(100); // Lower height to fly above forest
        //     birds.add(new Bird(startX, startY));
        // }
        
        // Create floating leaves - ลบออก
        // for (int i = 0; i < 15; i++) {
        //     int startX = rand.nextInt(WIDTH);
        //     int startY = 150 + rand.nextInt(100);
        //     floatingLeaves.add(new FloatingLeaf(startX, startY));
        // }
        // กำหนดตำแหน่งกลางจอ (ตรงกลางและไม่ซ้ำกับตำแหน่งยุงตัวเมียเก่า)
        meetingX = WIDTH / 2.0;
        meetingY = 120; // Lower height to fly above forest
        // กำหนดตำแหน่งยุงตัวเมีย (ประมาณกลางจอด้านขวา)
        femaleMosquitoX = (meetingX + 60);
        femaleMosquitoY = meetingY;

        // เริ่มบินจากซ้ายออกนอกจอ
        // ให้ยุงตัวผู้ซ้ายกว่ากึ่งกลางประมาณ 30 pixel
        mosquitoX_air = meetingX - 60;
        mosquitoY_air = meetingY;

        // หัวใจอยู่ตรงกลางระหว่างสองยุง (meetingX, meetingY)
        heart = new Heart((femaleMosquitoX + mosquitoX_air) / 2, meetingY, 30, 0);

        // เริ่มตำแหน่งอุกาบาตนอกจอ
        meteorX = mosquitoX_air; // เริ่มที่ตัวผู้
        meteorY = mosquitoY_air - 100; // อยู่สูงกว่ายุงตัวผู้เล็กน้อย (เริ่มนอกจอ)

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene((Graphics2D) g);
    }

    private void drawScene(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform sceneOld = g.getTransform();
        if (sceneState == 6 && shakeIntensity > 0) {
            double sx = (rand.nextDouble() * 2 - 1) * shakeIntensity;
            double sy = (rand.nextDouble() * 2 - 1) * shakeIntensity;
            g.translate(sx, sy);
        }

        // พื้นหลัง
        if (sceneState <= 1 || sceneState == 5) { // ใต้น้ำ หรือ กำลังตกน้ำ
            g.setColor(new Color(70, 130, 150));
            // Fill background using horizontal lines (Bresenham)
            for (int y = 0; y < HEIGHT; y++) {
                drawBresenhamLine(g, 0, y, WIDTH, y);
            }
            drawBottomGround(g);
            drawSeaGrass(g);
            drawSeaweed(g);
            manageBubbles(g);
            if (sceneState == 5) {
                drawSkyBackground(g, (int) mosquitoY_air);
            }
        } else if (sceneState >= 2 && sceneState <= 4) { // บินบนฟ้า (รวม scene 4 ที่อุกาบาตตกลงมา)
            drawSkyBackground(g, 0);
        }
        if (sceneState == -1) {
            g.setColor(Color.BLACK);
            // Fill background using horizontal lines (Bresenham)
            for (int y = 0; y < HEIGHT; y++) {
                drawBresenhamLine(g, 0, y, WIDTH, y);
            }

            g.setColor(new Color(1f, 1f, 1f, introAlpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            String text = introTexts[introStep];
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                int textWidth = g.getFontMetrics().stringWidth(lines[i]);
                g.drawString(lines[i], (WIDTH - textWidth) / 2, HEIGHT / 2 + i * 30);
            }

            return; // ไม่วาดอย่างอื่น
        }


        //flash check
        if (isFlashing) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(new Color(1f, 1f, 1f, flashAlpha)); // สีขาวโปร่งใส
            // Fill flash using horizontal lines (Bresenham)
            for (int y = 0; y < getHeight(); y++) {
                drawBresenhamLine(g2d, 0, y, getWidth(), y);
            }
            g2d.dispose();
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
                // วาดเหมือน scene 3 แต่เพิ่มอุกาบาต
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
                // วาดร่างยุงทั้งสองที่ตายแล้วกำลังตก
                drawDeadMosquito(g, mosquitoX_air, mosquitoY_air, corpseRotationDeg);
                drawDeadMosquito(g, femaleMosquitoX + 60, femaleMosquitoY, femaleCorpseRotationDeg);
                // Meteor is hidden after explosion, so don't draw it
            }
            case 6 -> {
                drawExplosion(g, (int) mosquitoX_air, (int) mosquitoY_air);
                drawExplosionParticles(g);
                drawSmokeParticles(g);
            }
        }
        g.setTransform(sceneOld);
        // Screen flash overlay on top of everything (not affected by shake)
        if (flashAlpha > 0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            Composite original = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, flashAlpha)));
            g2.setColor(Color.WHITE);
            // Fill flash using horizontal lines (Bresenham)
            for (int y = 0; y < HEIGHT; y++) {
                drawBresenhamLine(g2, 0, y, WIDTH, y);
            }
            g2.setComposite(original);
            g2.dispose();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isFlashing) {
            flashAlpha -= 0.02f; // ลดความขาวลงทีละนิด
            if (flashAlpha <= 0f) {
                flashAlpha = 0f;
                isFlashing = false;
                sceneState = 0; // เปลี่ยนฉากจริงๆ ที่นี่
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
                // Falling corpse physics with rotation and water impact (both mosquitoes)
                // Male
                if (!corpseOnWater) {
                    corpseVy += 0.5; // gravity
                    mosquitoY_air += corpseVy;
                    corpseRotationDeg += corpseRotSpeedDeg;
                    if (mosquitoY_air >= eggBaseY) {
                        mosquitoY_air = eggBaseY;
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
                }

                // Female
                if (!femaleCorpseOnWater) {
                    femaleCorpseVy += 0.5;
                    femaleMosquitoY += femaleCorpseVy;
                    femaleCorpseRotationDeg += femaleCorpseRotSpeedDeg;
                    if (femaleMosquitoY >= eggBaseY) {
                        femaleMosquitoY = eggBaseY;
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
                }

                // After both are on water and a short linger, reset scene
                if (corpseOnWater && femaleCorpseOnWater && frameCount > 60) {
                    frameCount = 0;
                    sceneState = 0;
                    eggSwingAngle = 0;
                    bubbles.clear();
                    heart.visible = false;
                    meteorX = -100;
                    meteorY = -100;
                    explosionParticles.clear();
                    smokeParticles.clear();
                }

                meteorY += 8;
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
    private void drawFemaleMosquito(Graphics2D g, double x, double y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);

        g.setColor(new Color(100, 20, 20)); // สีเข้มกว่ายุงตัวผู้
        // Draw body using ellipse (Midpoint algorithm)
        fillMidpointEllipse(g, 0, -10, 5, 10); // ลำตัว
        fillMidpointEllipse(g, 0, -20, 4, 4); // หัว

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        // Draw legs using Bresenham lines
        drawBresenhamLine(g, 0, -15, -15, -10);
        drawBresenhamLine(g, 0, -15, 15, -10);
        drawBresenhamLine(g, 0, -10, -15, -5);
        drawBresenhamLine(g, 0, -10, 15, -5);

        g.setColor(new Color(200, 200, 200));
        int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
        // Draw wings using Bezier curves
        drawBezierCurve(g, -25, -15 + wingYOffset, -12, -20 + wingYOffset, -12, -10 + wingYOffset, 0, -15 + wingYOffset);
        drawBezierCurve(g, 0, -15 + wingYOffset, 12, -20 + wingYOffset, 12, -10 + wingYOffset, 25, -15 + wingYOffset);

        g.setTransform(old);
    }

    private void drawMeteor(Graphics2D g, double x, double y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        int size = 300; // Meteor diameter

        // Tail effect above meteor (orange to transparent)
        GradientPaint tail = new GradientPaint(
                size / 2, -size * 2, new Color(255, 140, 0, 180), // Start (bright)
                size / 2, 0, new Color(255, 140, 0, 0) // End (transparent at meteor)
        );
        g.setPaint(tail);
        fillMidpointEllipse(g, size / 2, -size, size / 6, size); // Vertical oval tail above meteor

        // Meteor body gradient
        GradientPaint body = new GradientPaint(
                0, 0, new Color(100, 50, 0),
                size, size, new Color(180, 90, 40));
        g.setPaint(body);
        fillMidpointCircle(g, size / 2, size / 2, size / 2);

        // Inner heated core
        g.setColor(new Color(255, 100, 0, 200));
        fillMidpointCircle(g, size / 2, size / 2, size * 3 / 8);

        // Big craters
        g.setColor(new Color(50, 25, 0));
        fillMidpointCircle(g, size / 2, size / 3, size / 20);
        fillMidpointCircle(g, size / 2, size / 4, size / 16);
        fillMidpointCircle(g, size / 4, size / 2, size / 24);

        g.setTransform(old);
    }

    // --- เมธอดวาดฉากใหม่ ---
    private void drawSkyBackground(Graphics2D g, int waterLevel) {
        // ท้องฟ้าแบบไล่สี
        GradientPaint skyPaint = new GradientPaint(0, 0, new Color(135, 206, 250), 0, HEIGHT, new Color(240, 248, 255));
        g.setPaint(skyPaint);
        // Fill sky using horizontal lines (Bresenham)
        for (int y = 0; y < HEIGHT - waterLevel; y++) {
            drawBresenhamLine(g, 0, y, WIDTH, y);
        }

        // ก้อนเมฆ - only draw if not underwater (scene 5)
        if (sceneState != 5) {
            g.setColor(Color.WHITE);
            for (Point cloud : clouds) {
                fillMidpointEllipse(g, cloud.x + 50, cloud.y + 20, 50, 20);
                fillMidpointEllipse(g, cloud.x + 70, cloud.y + 5, 40, 25);
            }
        }
        
        // Draw forest when mosquito is flying (scenes 2, 3, 4)
        if (sceneState >= 2 && sceneState <= 4) {
            drawForest(g);
            // drawBirds(g); // ลบการวาดนกออก
            // Draw puddle on the ground when flying
            drawPuddle(g);
        }
    }

    private void drawFlyingMosquito(Graphics2D g, double x, double y, boolean flapping, double angle) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(angle));

        // ลำตัว (สีเดียวกับตอน Evolve)
        g.setColor(new Color(40, 40, 40));
        fillMidpointEllipse(g, 0, -10, 5, 10); // Thorax & Abdomen
        fillMidpointEllipse(g, 0, -20, 4, 4); // Head

        // ขา
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        // Draw legs using Bresenham lines
        drawBresenhamLine(g, 0, -15, -15, -10);
        drawBresenhamLine(g, 0, -15, 15, -10);
        drawBresenhamLine(g, 0, -10, -15, -5);
        drawBresenhamLine(g, 0, -10, 15, -5);

        // ปีก
        if (flapping) {
            int wingAlpha = 200;
            g.setColor(new Color(200, 200, 200, wingAlpha));
            int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
            // Draw wings using Bezier curves
            drawBezierCurve(g, -25, -15 + wingYOffset, -12, -20 + wingYOffset, -12, -10 + wingYOffset, 0, -15 + wingYOffset);
            drawBezierCurve(g, 0, -15 + wingYOffset, 12, -20 + wingYOffset, 12, -10 + wingYOffset, 25, -15 + wingYOffset);
        }

        g.setTransform(old);
    }

    private void drawDeadMosquito(Graphics2D g, double x, double y, double rotationDeg) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(rotationDeg));

        // Body
        g.setColor(new Color(50, 50, 50));
        fillMidpointEllipse(g, 0, -10, 5, 10); // Thorax & Abdomen

        // Head
        g.setColor(new Color(60, 60, 60));
        fillMidpointEllipse(g, 0, -20, 4, 4);

        // X-eyes
        g.setColor(Color.RED.darker());
        g.setStroke(new BasicStroke(1.5f));
        // Draw X-eyes using Bresenham lines
        drawBresenhamLine(g, -6, -26, -2, -22);
        drawBresenhamLine(g, -6, -22, -2, -26);
        drawBresenhamLine(g, 2, -26, 6, -22);
        drawBresenhamLine(g, 2, -22, 6, -26);

        // Limp legs
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        // Draw legs using Bresenham lines
        drawBresenhamLine(g, 0, -10, -12, -2);
        drawBresenhamLine(g, 0, -10, 12, -2);
        drawBresenhamLine(g, 0, -15, -10, -6);
        drawBresenhamLine(g, 0, -15, 10, -6);

        // Drooped wings (faint) using Bezier curves
        g.setColor(new Color(180, 180, 180, 120));
        drawBezierCurve(g, -22, -10, -15, -15, -15, -5, 0, -10);
        drawBezierCurve(g, 0, -10, 15, -15, 15, -5, 22, -10);

        g.setTransform(old);
    }

    // --- โค้ดของฉากใต้น้ำ (แก้ไขสี) ---
    private void drawEvolvingMosquito(Graphics2D g, int x, int y, double progress) {
        x = x - 25;
        AffineTransform old = g.getTransform();
        g.rotate(Math.sin(y * 0.1) * 0.1, x, y);

        // --- START: MODIFIED COLOR ---
        // ใช้สีเทาเข้มเหมือนยุงตัวเต็มวัย
        g.setColor(new Color(40, 40, 40));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // --- END: MODIFIED COLOR ---

        if (progress < 0.2) {
            // Draw polyline using Bresenham lines
            drawBresenhamLine(g, x, y, x + 2, y - 10);
            drawBresenhamLine(g, x + 2, y - 10, x - 2, y - 20);
            drawBresenhamLine(g, x - 2, y - 20, x, y - 30);
        } else {
            int headSize = (int) (8 * ((progress - 0.2) / 0.8));
            fillMidpointEllipse(g, x, y - 10, 5, 10);
            fillMidpointEllipse(g, x, y - 20, headSize / 2, headSize / 2);
        }
        if (progress > 0.4) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            int legLength = (int) (15 * ((progress - 0.4) / 0.6));
            // Draw legs using Bresenham lines
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
            // Draw wings using Bezier curves
            drawBezierCurve(g, x - wingSize, y - 15 + wingYOffset, x - wingSize/2, y - 20 + wingYOffset, x - wingSize/2, y - 10 + wingYOffset, x, y - 15 + wingYOffset);
            drawBezierCurve(g, x, y - 15 + wingYOffset, x + wingSize/2, y - 20 + wingYOffset, x + wingSize/2, y - 10 + wingYOffset, x + wingSize, y - 15 + wingYOffset);
        }
        g.setTransform(old);
    }

    private void drawCrackedEgg(Graphics2D g, int offset) {
        Color eggColor = new Color(139, 69, 19, 200);
        g.setColor(eggColor);
        int x = eggBaseX - 50;
        int y = eggBaseY;
        int width = 50;
        int height = 25;
        // Draw egg halves using filled ellipses (Midpoint algorithm)
        fillMidpointEllipse(g, x - offset + width/4, y + height/2, width/4, height/2);
        fillMidpointEllipse(g, x + offset + width/4, y + height/2, width/4, height/2);
    }

    private void drawMosquitoEgg(Graphics2D g) {
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
        fillMidpointEllipse(g, x + width/2, y + height/2, width/2, height/2);
    }

    private void drawBottomGround(Graphics2D g) { /* ...โค้ดเดิม... */
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight;
        g.setColor(new Color(194, 178, 128));
        // Fill ground using horizontal lines (Bresenham)
        for (int y = yStart; y < HEIGHT; y++) {
            drawBresenhamLine(g, 0, y, WIDTH, y);
        }
        g.setColor(new Color(120, 120, 120));
        for (int i = 0; i < 15; i++) {
            int x = 20 + i * 40;
            int size = 10 + (i % 3) * 5;
            fillMidpointCircle(g, x + size/2, yStart + 20 + (i % 2) * 10 + size/2, size/2);
        }
    }

    private void drawSeaweed(Graphics2D g) {
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight; // top of sand
        for (Seaweed s : seaweeds) {
            s.draw(g, yStart, frameCount);
        }
    }

    private void drawSeaGrass(Graphics2D g) {
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight; // top of sand
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

        void draw(Graphics2D g, int baseY, int frame) {
            double t = frame * swaySpeed;
            double segLen = (double) height / segments;
            Path2D.Double path = new Path2D.Double();
            double x = baseX;
            double y = baseY;
            path.moveTo(x, y);
            for (int i = 1; i <= segments; i++) {
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress; // slimmer sway at tip
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff;
                double nx = baseX + dx;
                double ny = baseY - segLen * i;
                path.lineTo(nx, ny);
            }

            g.setColor(new Color(20, 100, 60));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);

            // small leaves along the stalk
            g.setColor(new Color(30, 140, 80));
            for (int i = 3; i < segments; i += 3) {
                double progress = (double) i / segments;
                double ampFalloff = 1.0 - 0.4 * progress;
                double dx = Math.sin(t + i * 0.6) * swayAmplitude * ampFalloff;
                double nx = baseX + dx;
                double ny = baseY - segLen * i;
                int leafW = 8;
                int leafH = 14;
                fillMidpointEllipse(g, (int)(nx - leafW - 3 + leafW/2), (int)(ny - leafH/2 + leafH/2), leafW/2, leafH/2);
                fillMidpointEllipse(g, (int)(nx + 3 + leafW/2), (int)(ny - leafH/2 + leafH/2), leafW/2, leafH/2);
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

        void draw(Graphics2D g, int baseY, int frame) {
            double t = frame * swaySpeed;
            // draw many thin blades varying in height and phase
            for (int i = 0; i < bladeCount; i++) {
                int x = baseX + (int) ((i - bladeCount / 2.0) * (spread / (double) bladeCount));
                int h = minHeight + (i * 37 % (maxHeight - minHeight + 1));
                double localPhase = i * 0.45;
                // blade path as a short curved polyline
                Path2D.Double blade = new Path2D.Double();
                blade.moveTo(x, baseY);
                int segments = 5;
                for (int s = 1; s <= segments; s++) {
                    double p = s / (double) segments;
                    double ampFalloff = 1.0 - 0.5 * p;
                    double dx = Math.sin(t + localPhase + p * 1.2) * swayAmplitude * ampFalloff;
                    double nx = x + dx;
                    double ny = baseY - h * p;
                    blade.lineTo(nx, ny);
                }
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(25, 120, 70));
                g.draw(blade);
            }
        }
    }

    private void manageBubbles(Graphics2D g) { /* ...โค้ดเดิม... */
        if (rand.nextInt(8) == 0 && bubbles.size() < 25) { // ลดความถี่การสร้างฟองและจำนวนสูงสุด
            int x = rand.nextInt(WIDTH - 40) + 20;
            int size = rand.nextInt(15) + 10;
            double speed = 1 + rand.nextDouble() * 2;
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

    private class Bubble { /* ...โค้ดเดิม... */
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
                alpha -= 0.02f;
                if (alpha < 0)
                    alpha = 0;
            }
        }

        void draw(Graphics2D g) {
            Composite original = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(Color.WHITE);
            drawMidpointCircle(g, x, y, size / 2);
            g.setColor(new Color(255, 255, 255, (int) (alpha * 150)));
            fillMidpointCircle(g, x - size / 8, y - size / 6, size / 8);
            g.setComposite(original);
        }

        void drawMidpointCircle(Graphics g, int xc, int yc, int r) {
            int x = 0, y = r, d = 1 - r;
            plotCirclePoints(g, xc, yc, x, y);
            while (x < y) {
                if (d < 0)
                    d += 2 * x + 3;
                else {
                    d += 2 * (x - y) + 5;
                    y--;
                }
                x++;
                plotCirclePoints(g, xc, yc, x, y);
            }
        }

        void plotCirclePoints(Graphics g, int xc, int yc, int x, int y) {
            g.fillRect(xc + x, yc + y, 1, 1);
            g.fillRect(xc - x, yc + y, 1, 1);
            g.fillRect(xc + x, yc - y, 1, 1);
            g.fillRect(xc - x, yc - y, 1, 1);
            g.fillRect(xc + y, yc + x, 1, 1);
            g.fillRect(xc - y, yc + x, 1, 1);
            g.fillRect(xc + y, yc - x, 1, 1);
            g.fillRect(xc - y, yc - x, 1, 1);
        }
    }
    

    private class Heart {
        int x, y, size;
        boolean visible = true;
        double speed;
        int blinkCounter = 0;
        int blinkRate = 15; // lower = faster

        Heart(double x, double y, int size, double speed) {
            this.x = (int) x;
            this.y = (int) y;
            this.size = size;
            this.speed = speed;
        }

        void update() {
            // y -= speed;

            blinkCounter++;
            if (blinkCounter >= blinkRate) {
                visible = !visible;
                blinkCounter = 0;
            }
        }

        void draw(Graphics2D g) {
            if (!visible)
                return;

            g.setColor(Color.RED);
            drawHeartShape(g, x, y, size);
        }

        void drawHeartShape(Graphics2D g, int cx, int cy, int size) {
            double scale = size / 100.0;
            Path2D.Double heart = new Path2D.Double();

            // จุดเริ่มต้น
            heart.moveTo(50, 30);

            // ซีกขวาของหัวใจ
            heart.curveTo(50, 0, 90, 0, 90, 30);
            heart.curveTo(90, 60, 50, 80, 50, 100);

            // ซีกซ้ายของหัวใจ
            heart.curveTo(50, 80, 10, 60, 10, 30);
            heart.curveTo(10, 0, 50, 0, 50, 30);

            heart.closePath();

            AffineTransform transform = new AffineTransform();
            transform.translate(cx, cy);
            transform.scale(scale, scale);
            Shape transformedHeart = transform.createTransformedShape(heart);

            g.fill(transformedHeart);
            g.setColor(Color.RED.darker());
            g.setStroke(new BasicStroke(2f));
            g.draw(transformedHeart);
        }
    }

    private void createExplosionParticles() {
        explosionParticles.clear();
        smokeParticles.clear();
        Random rand = new Random();

        // Fiery sparks
        for (int i = 0; i < 140; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 3 + rand.nextDouble() * 6; // faster
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            explosionParticles.add(new ExplosionParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,
                    rand.nextInt(12) + 6, // size smaller sparks
                    0.6f + rand.nextFloat() * 0.4f // alpha
            ));
        }

        // Smoke puffs
        for (int i = 0; i < 80; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 0.5 + rand.nextDouble() * 1.5;
            double vx = Math.cos(angle) * speed * 0.6;
            double vy = Math.sin(angle) * speed * 0.6 - (0.5 + rand.nextDouble() * 0.5); // rise
            smokeParticles.add(new SmokeParticle(
                    mosquitoX_air, mosquitoY_air, vx, vy,
                    18 + rand.nextInt(22), // initial size
                    40 + rand.nextInt(50) // life
            ));
        }
    }

    private void drawExplosion(Graphics2D g, int x, int y) {
        // Main explosion circle
        if (explosionRadius > 0) {
            // Outer glow
            RadialGradientPaint glowPaint = new RadialGradientPaint(
                    x, y, explosionRadius,
                    new float[] { 0.0f, 0.7f, 1.0f },
                    new Color[] {
                            new Color(255, 255, 0, 200), // Bright yellow center
                            new Color(255, 165, 0, 150), // Orange middle
                            new Color(255, 0, 0, 0) // Transparent red edge
                    });
            g.setPaint(glowPaint);
            fillMidpointCircle(g, x, y, explosionRadius);

            // Inner bright core
            g.setColor(new Color(255, 255, 255, 180));
            fillMidpointCircle(g, x, y, explosionRadius / 3);

            // Shockwave ring
            if (shockwaveAlpha > 0f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shockwaveAlpha));
                g2.setColor(new Color(255, 255, 255));
                g2.setStroke(new BasicStroke(8f));
                drawMidpointCircle(g2, x, y, shockwaveRadius);
                g2.dispose();
            }
        }
    }

    private void drawExplosionParticles(Graphics2D g) {
        for (ExplosionParticle particle : explosionParticles) {
            particle.draw(g);
        }
    }

    private void drawSmokeParticles(Graphics2D g) {
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
            vy += 0.1; // Gravity effect
            alpha -= alphaDecay;
            if (alpha < 0)
                alpha = 0;
        }

        void draw(Graphics2D g) {
            if (alpha <= 0)
                return;

            Composite original = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Random colors for particles
            Color[] colors = {
                    new Color(255, 255, 0), // Yellow
                    new Color(255, 165, 0), // Orange
                    new Color(255, 69, 0), // Red-orange
                    new Color(255, 0, 0) // Red
            };
            g.setColor(colors[(int) (Math.random() * colors.length)]);

            fillMidpointCircle(g, (int) x, (int) y, size / 2);

            g.setComposite(original);
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
            vy *= 0.98; // slow rise
            size += growth;
            age++;
            // fade in then out
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

        void draw(Graphics2D g) {
            if (alpha <= 0)
                return;
            Composite original = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(50, 50, 50));
            fillMidpointCircle(g, (int) x, (int) y, (int) size / 2);
            g.setComposite(original);
        }
    }

    private void drawForest(Graphics2D g) {
        // Draw ground/grass line
        int groundY = HEIGHT - 180; // Higher ground so mosquito flies above trees
        g.setColor(new Color(34, 139, 34)); // Forest green
        // Fill ground using horizontal lines (Bresenham)
        for (int y = groundY; y < HEIGHT; y++) {
            drawBresenhamLine(g, 0, y, WIDTH, y);
        }
        
        // Add grass texture - ลดคุณภาพ
        g.setColor(new Color(0, 100, 0)); // Darker green for grass
        for (int i = 0; i < WIDTH; i += 16) { // เพิ่มระยะห่างจาก 8 เป็น 16
            int grassHeight = 3 + rand.nextInt(4);
            drawBresenhamLine(g, i, groundY, i, groundY + grassHeight);
        }
        
        // Draw puddle on the ground
        drawPuddle(g);
        
        // Draw trees
        for (Tree tree : trees) {
            tree.draw(g, groundY);
        }
        
        // Draw bushes
        for (Bush bush : bushes) {
            bush.draw(g, groundY);
        }
        
        // Draw floating leaves - ลบออก
        // for (FloatingLeaf leaf : floatingLeaves) {
        //     leaf.update();
        //     leaf.draw(g);
        // }
    }
    
    private void drawPuddle(Graphics2D g) {
        // Draw puddle shadow first
        g.setColor(new Color(0, 0, 0, 40));
        fillMidpointEllipse(g, puddleX + puddleWidth/2, puddleY + puddleHeight/2, puddleWidth/2 + 8, puddleHeight/2 + 8);
        
        // Draw main puddle with gradient
        GradientPaint puddlePaint = new GradientPaint(
            puddleX, puddleY, new Color(100, 150, 255, 180), // Blue with transparency
            puddleX, puddleY + puddleHeight, new Color(70, 130, 200, 220) // Darker blue at bottom
        );
        g.setPaint(puddlePaint);
        fillMidpointEllipse(g, puddleX + puddleWidth/2, puddleY + puddleHeight/2, puddleWidth/2, puddleHeight/2);
        
        // Add water reflection highlights (more highlights for bigger puddle) - ลดคุณภาพ
        g.setColor(new Color(255, 255, 255, 120));
        fillMidpointEllipse(g, puddleX + 25, puddleY + 12, puddleWidth/4, puddleHeight/3);
        fillMidpointEllipse(g, puddleX + puddleWidth/2, puddleY + 15, puddleWidth/5, puddleHeight/4);
        // ลด highlights จาก 3 เป็น 2
        
        // Add small ripples (more ripples for bigger puddle) - ลดคุณภาพ
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(2.5f));
        // Draw ripples as simple arcs using multiple line segments - ลดคุณภาพ
        for (int i = 0; i < 180; i += 20) { // เพิ่มระยะห่างจาก 10 เป็น 20
            double angle1 = Math.toRadians(i);
            double angle2 = Math.toRadians(i + 20);
            int x1 = (int)(puddleX + puddleWidth/2 + (puddleWidth/2 - 30) * Math.cos(angle1));
            int y1 = (int)(puddleY + puddleHeight/2 + (puddleHeight/2 - 15) * Math.sin(angle1));
            int x2 = (int)(puddleX + puddleWidth/2 + (puddleWidth/2 - 30) * Math.cos(angle2));
            int y2 = (int)(puddleY + puddleHeight/2 + (puddleHeight/2 - 15) * Math.sin(angle2));
            drawBresenhamLine(g, x1, y1, x2, y2);
        }
        
        // Add some grass around the puddle (more grass for bigger puddle) - ลดคุณภาพ
        g.setColor(new Color(0, 100, 0));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 6; i++) { // ลดจาก 12 เป็น 6
            int grassX = puddleX + (i * 36) + 15; // เพิ่มระยะห่างจาก 18 เป็น 36
            int grassHeight = 4 + rand.nextInt(4);
            drawBresenhamLine(g, grassX, puddleY, grassX, puddleY - grassHeight);
        }
    }
    
    private void drawBirds(Graphics2D g) {
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
        
        void draw(Graphics2D g, int groundY) {
            // Draw tree shadow
            g.setColor(new Color(0, 0, 0, 30));
            fillMidpointEllipse(g, baseX, groundY, 25, 10);
            
            // Draw trunk
            g.setColor(new Color(101, 67, 33)); // Brown trunk
                    // Fill trunk using horizontal lines (Bresenham) - ลดคุณภาพ
        for (int y = groundY - height + 40; y < groundY; y += 2) { // เพิ่มระยะห่างจาก 1 เป็น 2
            drawBresenhamLine(g, baseX - trunkWidth/2, y, baseX + trunkWidth/2, y);
        }
        
        // Draw trunk shadow - ลดคุณภาพ
        g.setColor(new Color(0, 0, 0, 40));
        for (int y = groundY - height + 40; y < groundY; y += 2) { // เพิ่มระยะห่างจาก 1 เป็น 2
            drawBresenhamLine(g, baseX - trunkWidth/2 + 2, y, baseX + trunkWidth/2, y);
        }
            
            // Draw foliage (multiple circles for tree crown) - ลดคุณภาพ
            g.setColor(new Color(0, 100, 0)); // Dark green
            fillMidpointEllipse(g, baseX, groundY - height + 30, 35, 30);
            fillMidpointEllipse(g, baseX, groundY - height + 45, 25, 25);
            // ลด foliage circles จาก 3 เป็น 2
            
            // Add lighter green highlights - ลดคุณภาพ
            g.setColor(new Color(0, 128, 0));
            fillMidpointEllipse(g, baseX, groundY - height + 30, 30, 25);
            // ลด highlights จาก 2 เป็น 1
        }
    }
    
    private class Bush {
        int baseX, size;
        
        Bush(int baseX, int size) {
            this.baseX = baseX;
            this.size = size;
        }
        
        void draw(Graphics2D g, int groundY) {
            // Draw bush shadow
            g.setColor(new Color(0, 0, 0, 25));
            fillMidpointEllipse(g, baseX + 2, groundY, size/2, size/2);
            
            g.setColor(new Color(0, 128, 0)); // Medium green
            fillMidpointEllipse(g, baseX, groundY - size/2, size/2, size/2);
            
            // Add darker green detail
            g.setColor(new Color(0, 100, 0));
            fillMidpointEllipse(g, baseX, groundY - size + size/4, size/4, size/4);
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
            
            // Wrap around screen
            if (x > WIDTH + 50) {
                x = -50;
                y = 80 + Math.random() * 100; // Maintain height above forest
            }
            
            // Flap wings
            wingState = (wingState + 1) % 6;
        }
        
        void draw(Graphics2D g) {
            AffineTransform old = g.getTransform();
            g.translate(x, y);
            
            // Bird body
            g.setColor(new Color(139, 69, 19)); // Brown
            fillMidpointEllipse(g, 0, 0, 8, 4);
            
            // Bird head
            fillMidpointEllipse(g, -8, -2, 4, 4);
            
            // Beak - using Polygon (allowed)
            g.setColor(new Color(255, 165, 0)); // Orange
            int[] xPoints = {-12, -16, -12};
            int[] yPoints = {-2, -2, 2};
            g.fillPolygon(xPoints, yPoints, 3);
            
            // Wings (flapping animation)
            g.setColor(new Color(160, 82, 45)); // Saddle brown
            int wingOffset = wingState < 3 ? -2 : 2;
            fillMidpointEllipse(g, -9, wingOffset, 6, 3);
            fillMidpointEllipse(g, 9, wingOffset, 6, 3);
            
            g.setTransform(old);
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
            this.size = 8 + (int)(Math.random() * 6);
            this.alpha = 0.6f + (float)(Math.random() * 0.4f);
        }
        
        void update() {
            x += vx;
            y += vy;
            rotation += rotationSpeed;
            
            // Wrap around screen
            if (x < -20) x = WIDTH + 20;
            if (x > WIDTH + 20) x = -20;
            if (y > HEIGHT + 20) {
                y = -20;
                x = Math.random() * WIDTH;
            }
        }
        
        void draw(Graphics2D g) {
            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(Math.toRadians(rotation));
            
            Composite original = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // Draw leaf shape
            g.setColor(new Color(0, 100, 0));
            fillMidpointEllipse(g, 0, 0, size/2, size/4);
            
            // Add leaf detail
            g.setColor(new Color(0, 80, 0));
            drawBresenhamLine(g, -size/3, 0, size/3, 0);
            
            g.setComposite(original);
            g.setTransform(old);
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
