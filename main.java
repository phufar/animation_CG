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

    // --- State Variables ---
    private int frameCount = 0;
    private int sceneState = 0; // 0: Egg, 1: Evolving, 2: Flying, 3: Landed, 4: Swatted, 5: Falling

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

    //Explosion state
    private ArrayList<ExplosionParticle> explosionParticles;
    private int explosionRadius = 0;
    private boolean explosionStarted = false;

    public main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        timer = new Timer(33, this);
        timer.start();
        rand = new Random();

        // Initialize explosion particles
        explosionParticles = new ArrayList<>();

        // Initialize objects
        bubbles = new ArrayList<>();
        clouds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clouds.add(new Point(rand.nextInt(WIDTH), rand.nextInt(200) + 50));
            cloudSpeeds.add(1 + rand.nextInt(3)); // ความเร็ว 1-3 pixel/frame
        }
        // กำหนดตำแหน่งกลางจอ (ตรงกลางและไม่ซ้ำกับตำแหน่งยุงตัวเมียเก่า)
        meetingX = WIDTH / 2.0;
        meetingY = 150;
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

        // พื้นหลัง
        if (sceneState <= 1 || sceneState == 5) { // ใต้น้ำ หรือ กำลังตกน้ำ
            g.setColor(new Color(70, 130, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawBottomGround(g);
            manageBubbles(g);
            if (sceneState == 5) {
                drawSkyBackground(g, (int) mosquitoY_air);
            }
        } else {
            drawSkyBackground(g, 0);
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
                drawFemaleMosquito(g, (int) femaleMosquitoX+60, (int) femaleMosquitoY);
            }
            case 3 -> {
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX+60, (int) femaleMosquitoY);
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 10) ;
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 50;
                heart.x = heartX;
                heart.y = heartY;
                heart.draw(g);
            }
            case 4 -> {
                // วาดเหมือน scene 3 แต่เพิ่มอุกาบาต
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX+60, (int) femaleMosquitoY);
                int heartX = (int) ((mosquitoX_air + femaleMosquitoX) / 2 + 10) ;
                int heartY = (int) ((mosquitoY_air + femaleMosquitoY) / 2) - 50;
                heart.x = heartX;
                heart.y = heartY;
                heart.draw(g);
                drawMeteor(g, meteorX-150, meteorY);

            }
            case 5 -> {
                // วาดผลหลังชน เช่น ตัวผู้โดนกระแทก
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, false, 0);
                drawFemaleMosquito(g, (int) femaleMosquitoX+60, (int) femaleMosquitoY);
                drawMeteor(g, meteorX-150, meteorY);
            }
            case 6 -> {
                drawExplosion(g, (int) mosquitoX_air, (int) mosquitoY_air);
                drawExplosionParticles(g);
            }
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
                        createExplosionParticles();
                    }
                }
            }
            case 5 -> {
                mosquitoY_air += 5;
                meteorY += 5;
                if (mosquitoY_air == eggBaseY) {
                    frameCount = 0;
                    sceneState = 0;
                    eggSwingAngle = 0;
                    bubbles.clear();
                    heart.visible = false;
                    meteorX = -100;
                    meteorY = -100;
                    explosionParticles.clear();
                }
            }
            case 6 -> {
                if(!explosionStarted){
                    explosionStarted = true;
                    explosionRadius = 0;
                }
                if(explosionRadius < 100){
                    explosionRadius += 2;
                }

                for(ExplosionParticle particle : explosionParticles){
                    particle.update();
                }

                explosionParticles.removeIf(particle -> particle.alpha <= 0);

                if(frameCount > 60){
                    sceneState = 5;
                    frameCount = 0;
                    explosionParticles.clear();
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
        repaint();
    }

    // เพิ่ม method วาดยุงตัวเมียและหัวใจ
    private void drawFemaleMosquito(Graphics2D g, double x, double y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);

        g.setColor(new Color(100, 20, 20)); // สีเข้มกว่ายุงตัวผู้
        g.fillOval(-5, -20, 10, 20); // ลำตัว
        g.fillOval(-4, -24, 8, 8); // หัว

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(0, -15, -15, -10);
        g.drawLine(0, -15, 15, -10);
        g.drawLine(0, -10, -15, -5);
        g.drawLine(0, -10, 15, -5);

        g.setColor(new Color(200, 200, 200));
        int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
        g.fillArc(-25, -15 + wingYOffset, 25, 15, 0, 180);
        g.fillArc(0, -15 + wingYOffset, 25, 15, 0, 180);

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
        g.fillOval(size / 3, -size * 2, size / 3, size * 2); // Vertical oval tail above meteor

        // Meteor body gradient
        GradientPaint body = new GradientPaint(
                0, 0, new Color(100, 50, 0),
                size, size, new Color(180, 90, 40));
        g.setPaint(body);
        g.fillOval(0, 0, size, size);

        // Inner heated core
        g.setColor(new Color(255, 100, 0, 200));
        g.fillOval(size / 8, size / 8, size * 3 / 4, size * 3 / 4);

        // Big craters
        g.setColor(new Color(50, 25, 0));
        g.fillOval(size / 3, size / 3, size / 10, size / 10);
        g.fillOval(size / 2, size / 4, size / 8, size / 8);
        g.fillOval(size / 4, size / 2, size / 12, size / 12);

        g.setTransform(old);
    }

    // --- เมธอดวาดฉากใหม่ ---
    private void drawSkyBackground(Graphics2D g, int waterLevel) {
        // ท้องฟ้าแบบไล่สี
        GradientPaint skyPaint = new GradientPaint(0, 0, new Color(135, 206, 250), 0, HEIGHT, new Color(240, 248, 255));
        g.setPaint(skyPaint);
        g.fillRect(0, 0, WIDTH, HEIGHT - waterLevel);

        // ก้อนเมฆ
        g.setColor(Color.WHITE);
        for (Point cloud : clouds) {
            g.fillOval(cloud.x, cloud.y, 100, 40);
            g.fillOval(cloud.x + 30, cloud.y - 20, 80, 50);
        }
    }

    private void drawFlyingMosquito(Graphics2D g, double x, double y, boolean flapping, double angle) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(angle));

        // ลำตัว (สีเดียวกับตอน Evolve)
        g.setColor(new Color(40, 40, 40));
        g.fillOval(-5, -20, 10, 20); // Thorax & Abdomen
        g.fillOval(-4, -24, 8, 8); // Head

        // ขา
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(0, -15, -15, -10);
        g.drawLine(0, -15, 15, -10);
        g.drawLine(0, -10, -15, -5);
        g.drawLine(0, -10, 15, -5);

        // ปีก
        if (flapping) {
            int wingAlpha = 200;
            g.setColor(new Color(200, 200, 200, wingAlpha));
            int wingYOffset = (frameCount % 6 < 3) ? -5 : 0;
            g.fillArc(-25, -15 + wingYOffset, 25, 15, 0, 180);
            g.fillArc(0, -15 + wingYOffset, 25, 15, 0, 180);
        }

        g.setTransform(old);
    }

    // --- โค้ดของฉากใต้น้ำ (แก้ไขสี) ---
    private void drawEvolvingMosquito(Graphics2D g, int x, int y, double progress) {
        x = x-25;
        AffineTransform old = g.getTransform();
        g.rotate(Math.sin(y * 0.1) * 0.1, x, y);

        // --- START: MODIFIED COLOR ---
        // ใช้สีเทาเข้มเหมือนยุงตัวเต็มวัย
        g.setColor(new Color(40, 40, 40));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // --- END: MODIFIED COLOR ---
        
        if (progress < 0.2) {
            g.drawPolyline(new int[] { x, x + 2, x - 2, x }, new int[] { y, y - 10, y - 20, y - 30 }, 4);
        } else {
            int headSize = (int) (8 * ((progress - 0.2) / 0.8));
            g.fillOval(x - 5, y - 20, 10, 20);
            g.fillOval(x - headSize / 2, y - 20 - headSize / 2, headSize, headSize);
        }
        if (progress > 0.4) {
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            int legLength = (int) (15 * ((progress - 0.4) / 0.6));
            g.drawLine(x, y - 15, x - legLength, y - 10);
            g.drawLine(x, y - 15, x + legLength, y - 10);
            g.drawLine(x, y - 10, x - legLength, y - 5);
            g.drawLine(x, y - 10, x + legLength, y - 5);
        }
        if (progress > 0.6) {
            int wingSize = (int) (25 * ((progress - 0.6) / 0.4));
            int wingAlpha = (int) (150 * ((progress - 0.6) / 0.4));
            g.setColor(new Color(200, 200, 200, wingAlpha));
            boolean flap = (y % 10 < 5);
            int wingYOffset = flap ? -5 : 0;
            g.fillArc(x - wingSize, y - 15 + wingYOffset, wingSize, 15, 0, 180);
            g.fillArc(x, y - 15 + wingYOffset, wingSize, 15, 0, 180);
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
        g.fillArc(x - offset, y, width, height, 90, 180);
        g.fillArc(x + offset, y, width, height, 270, 180);
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
        g.fillOval(x, y, width, height);
    }

    private void drawBottomGround(Graphics2D g) { /* ...โค้ดเดิม... */
        int groundHeight = 80;
        int yStart = HEIGHT - groundHeight;
        g.setColor(new Color(194, 178, 128));
        g.fillRect(0, yStart, WIDTH, groundHeight);
        g.setColor(new Color(120, 120, 120));
        for (int i = 0; i < 15; i++) {
            int x = 20 + i * 40;
            int size = 10 + (i % 3) * 5;
            g.fillOval(x, yStart + 20 + (i % 2) * 10, size, size);
        }
    }

    private void manageBubbles(Graphics2D g) { /* ...โค้ดเดิม... */
        if (rand.nextInt(5) == 0 && bubbles.size() < 50) {
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
            g.fillOval(x - size / 4, y - size / 3, size / 4, size / 4);
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
        Random rand = new Random();
        
        for (int i = 0; i < 50; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 2 + rand.nextDouble() * 4;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            explosionParticles.add(new ExplosionParticle(
                mosquitoX_air, mosquitoY_air, vx, vy, 
                rand.nextInt(20) + 10, // size
                rand.nextFloat() * 0.5f + 0.5f // alpha
            ));
        }
    }

    private void drawExplosion(Graphics2D g, int x, int y) {
        // Main explosion circle
        if (explosionRadius > 0) {
            // Outer glow
            RadialGradientPaint glowPaint = new RadialGradientPaint(
                x, y, explosionRadius,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(255, 255, 0, 200), // Bright yellow center
                    new Color(255, 165, 0, 150), // Orange middle
                    new Color(255, 0, 0, 0)      // Transparent red edge
                }
            );
            g.setPaint(glowPaint);
            g.fillOval(x - explosionRadius, y - explosionRadius, 
                      explosionRadius * 2, explosionRadius * 2);
            
            // Inner bright core
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval(x - explosionRadius/3, y - explosionRadius/3, 
                      explosionRadius * 2/3, explosionRadius * 2/3);
        }
    }

    private void drawExplosionParticles(Graphics2D g) {
        for (ExplosionParticle particle : explosionParticles) {
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
            if (alpha < 0) alpha = 0;
        }
        
        void draw(Graphics2D g) {
            if (alpha <= 0) return;
            
            Composite original = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // Random colors for particles
            Color[] colors = {
                new Color(255, 255, 0),   // Yellow
                new Color(255, 165, 0),   // Orange
                new Color(255, 69, 0),    // Red-orange
                new Color(255, 0, 0)      // Red
            };
            g.setColor(colors[(int)(Math.random() * colors.length)]);
            
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
            
            g.setComposite(original);
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
