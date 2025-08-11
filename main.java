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
    private int dogButtX;
    private int landingTimer = 0;
    private Point handPosition;
    private double fallSpeed = 0;

    public main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        timer = new Timer(33, this);
        timer.start();
        rand = new Random();

        // Initialize objects
        bubbles = new ArrayList<>();
        clouds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            clouds.add(new Point(rand.nextInt(WIDTH), rand.nextInt(200) + 50));
        }
        handPosition = new Point(WIDTH, -200);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene((Graphics2D) g);
    }

    private void drawScene(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- วาดพื้นหลังตามฉาก ---
        if (sceneState <= 1 || sceneState == 5) { // ฉากใต้น้ำ หรือ ฉากกำลังตกน้ำ
            g.setColor(new Color(10, 40, 80));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawBottomGround(g);
            manageBubbles(g);
            if (sceneState == 5) { // ถ้ากำลังตก ให้วาดฟ้าครึ่งบน
                drawSkyBackground(g, (int) mosquitoY_air);
            }
        } else { // ฉากบนฟ้า
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
            case 2 -> { // บินบนฟ้า
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, true, 0);
                drawDogButt(g, dogButtX);
            }
            case 3 -> { // ลงจอดแล้ว
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, false, 0); // ไม่กระพือปีก
                drawDogButt(g, dogButtX);
            }
            case 4 -> { // กำลังจะโดนตบ
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, false, 0);
                drawDogButt(g, dogButtX);
                drawHand(g, handPosition.x, handPosition.y);
            }
            case 5 -> { // ตกน้ำ
                drawFlyingMosquito(g, (int) mosquitoX_air, (int) mosquitoY_air, false, 45); // หมุนตอนตก
                drawDogButt(g, dogButtX);
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
                if (mosquitoY < 80) { // เมื่อลอยขึ้นสุด
                    sceneState = 2; // เปลี่ยนเป็นฉากบิน
                    mosquitoX_air = -40; // เริ่มจากนอกจอด้านซ้าย
                    mosquitoY_air = 150;
                    dogButtX = WIDTH; // เริ่มจากนอกจอด้านขวา
                }
            }
            case 2 -> { // บินหาเป้าหมาย
                mosquitoX_air += 3.0;
                mosquitoY_air += Math.sin(frameCount * 0.1) * 2; // บินขึ้นลงเล็กน้อย
                if (dogButtX > WIDTH - 250)
                    dogButtX -= 2; // ก้นหมาโผล่มา

                // ตรวจสอบการลงจอด
                if (mosquitoX_air > dogButtX + 40) {
                    sceneState = 3;
                    // ปรับตำแหน่งยุงให้เกาะพอดี
                    mosquitoX_air = dogButtX + 60;
                    mosquitoY_air = 320;
                    landingTimer = frameCount; // เริ่มจับเวลาตอนลงจอด
                }
            }
            case 3 -> { // ลงจอดแล้ว
                // อยู่เฉยๆ 2 วิ (60 เฟรม) แล้วมือจะมา
                if (frameCount > landingTimer + 60) {
                    sceneState = 4;
                    handPosition.x = (int) mosquitoX_air - 50;
                    handPosition.y = -200;
                }
            }
            case 4 -> { // มือมาตบ
                if (handPosition.y < mosquitoY_air - 150) {
                    handPosition.y += 25; // มือเลื่อนลงมา
                } else {
                    // ตบ!
                    sceneState = 5;
                    fallSpeed = 0;
                }
            }
            case 5 -> { // ยุงตก
                fallSpeed += 0.5; // ความเร่ง
                mosquitoY_air += fallSpeed;
                mosquitoX_air -= 1;

                // เมื่อตกถึงพื้นน้ำ
                if (mosquitoY_air > HEIGHT) {
                    // Reset all
                    frameCount = 0;
                    sceneState = 0;
                    eggSwingAngle = 0;
                    bubbles.clear();
                    handPosition = new Point(WIDTH, -200);
                }
            }
        }

        // อัปเดตก้อนเมฆ
        if (sceneState >= 2 && sceneState != 5) {
            for (Point cloud : clouds) {
                cloud.x -= 1;
                if (cloud.x < -150) {
                    cloud.x = WIDTH + 20;
                    cloud.y = rand.nextInt(200) + 50;
                }
            }
        }
        repaint();
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

    private void drawFlyingMosquito(Graphics2D g, int x, int y, boolean flapping, double angle) {
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

    private void drawDogButt(Graphics2D g, int x) {
        AffineTransform old = g.getTransform();
        g.translate(x, 250);

        // บั้นท้าย
        g.setColor(new Color(210, 180, 140)); // Tan
        g.fillOval(0, 0, 100, 120);
        g.fillOval(80, 0, 100, 120);

        // หาง
        g.setColor(new Color(160, 82, 45)); // Sienna
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(150, 40, 50, 80, 180, 120);

        g.setTransform(old);
    }

    private void drawHand(Graphics2D g, int x, int y) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.setColor(new Color(255, 224, 189)); // Skin color
        // Palm
        g.fillOval(0, 0, 100, 120);
        // Fingers
        g.fillRoundRect(5, -60, 20, 70, 10, 10);
        g.fillRoundRect(30, -70, 20, 80, 10, 10);
        g.fillRoundRect(55, -65, 20, 75, 10, 10);
        g.fillRoundRect(80, -50, 20, 60, 10, 10);
        g.setTransform(old);
    }

    // --- โค้ดของฉากใต้น้ำ (แก้ไขสี) ---
    private void drawEvolvingMosquito(Graphics2D g, int x, int y, double progress) {
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


    private void drawCrackedEgg(Graphics2D g, int offset) { /* ...โค้ดเดิม... */
        Color eggColor = new Color(139, 69, 19, 200);
        g.setColor(eggColor);
        int x = eggBaseX - 25;
        int y = eggBaseY;
        int width = 50;
        int height = 25;
        g.fillArc(x - offset, y, width, height, 90, 180);
        g.fillArc(x + offset, y, width, height, 270, 180);
    }

    private void drawMosquitoEgg(Graphics2D g) { /* ...โค้ดเดิม... */
        Color eggColor = new Color(139, 69, 19, 200);
        g.setColor(eggColor);
        int swingX = 0;
        if (frameCount > 60) {
            double sineValue = Math.sin(eggSwingAngle * 20);
            swingX = (int) (10 * Math.signum(sineValue));
        }
        int x = eggBaseX - 25 + swingX;
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

    private class Heart{
        int x, y, size;
        boolean visible = true;
        double speed;
        int blinkCounter = 0;
        int blinkRate = 15; //lower = faster

        Heart(int x, int y, int size, double speed){
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
        }
        
        void update(){
            // y -= speed;

            blinkCounter ++;
            if(blinkCounter >= blinkRate){
                visible = !visible;
                blinkCounter = 0;
            }
        }

        void draw(Graphics2D g){
            if(!visible) return;

            g.setColor(Color.RED);
            drawHeartShape(g, x, y, size);
        }

        void drawHeartShape(Graphics2D g, int cx, int cy, int size){
            double scale = size / 100.0;
            Path2D.Double heart = new Path2D.Double();
            heart.moveTo(50, -50);
            heart.curveTo(50, -100, 100, 0, 0, 100);
            heart.curveTo(-100, 0, -50, -100, 0, -50);
            heart.closePath();

            AffineTransform transform = new AffineTransform();
            transform.translate(cx, cy);
            transform.scale(scale, scale);
            Shape transformedHeart = transform.createTransformedShape(heart);

            g.fill(transformedHeart);
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke());
            g.draw(transformedHeart);
        }


    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("The Great Mosquito Adventure (Modified)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new main());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
