import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Random;

public class FishingGame extends JFrame {
    private int money = 10000;
    private int upgradeLevel = 0;
    private final int MAX_DURABILITY = 20;
    private int currentDurability = MAX_DURABILITY;
    private int shipLevel = 1;
    private int nextShipCost = 500000; 

    private enum GameState { IDLE, WAITING_FOR_BITE, BITING, SHARK_FIGHT }
    private GameState currentState = GameState.IDLE;

    private Timer waitTimer, biteTimer, sharkTimer; 
    private String targetFish;
    private int targetPrice;
    private int sharkClickCount = 0;
    private int sharkTimeLeft = 5000; 
    private boolean isFlashing = false;

    private JLabel moneyLabel, rodLabel, durabilityLabel, shipLabel;
    private JTextArea logArea;
    private GameScreen gameScreen; 
    private JButton actionBtn, repairBtn, upgradeRodBtn, upgradeShipBtn;

    private Random random = new Random();

    private String fmt(int number) {
        return NumberFormat.getInstance().format(number);
    }

    public FishingGame() {
        setTitle("1인칭 선상 낚시 게임 - Classic Edition");
        setSize(800, 750); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(18, 18, 18)); 

        // 상단 패널
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBackground(new Color(24, 24, 24));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(218, 165, 32)), 
                new EmptyBorder(15, 20, 15, 20)
        ));
        
        Font statFont = new Font("맑은 고딕", Font.BOLD, 17);
        // 이모티콘 대신 깔끔한 대괄호와 텍스트 사용
        moneyLabel = new JLabel("[자산] " + fmt(money) + " G", SwingConstants.LEFT);
        shipLabel = new JLabel("[선박 Lv] " + shipLevel, SwingConstants.RIGHT);
        rodLabel = new JLabel("[장비 Lv] " + upgradeLevel, SwingConstants.LEFT);
        durabilityLabel = new JLabel("[내구도] " + currentDurability + " / " + MAX_DURABILITY, SwingConstants.RIGHT);
        
        moneyLabel.setFont(statFont); rodLabel.setFont(statFont); 
        durabilityLabel.setFont(statFont); shipLabel.setFont(statFont);
        
        moneyLabel.setForeground(new Color(255, 223, 0)); 
        shipLabel.setForeground(new Color(240, 240, 240)); 
        rodLabel.setForeground(new Color(240, 240, 240)); 
        durabilityLabel.setForeground(new Color(144, 238, 144)); 
        
        statsPanel.add(moneyLabel); statsPanel.add(shipLabel); 
        statsPanel.add(rodLabel); statsPanel.add(durabilityLabel);
        add(statsPanel, BorderLayout.NORTH);

        // 중앙 화면
        gameScreen = new GameScreen();
        add(gameScreen, BorderLayout.CENTER);

        // 하단 로그창
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(18, 18, 18));
        
        logArea = new JTextArea(5, 20);
        logArea.setEditable(false);
        logArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        // 하단 버튼
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBackground(new Color(18, 18, 18));
        buttonPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 이모티콘 제거 후 기호 사용
        actionBtn = createStyledButton("▶ 낚싯대 던지기", new Color(0, 90, 158), Color.WHITE);
        repairBtn = createStyledButton("◆ 장비 수리 (1,000 G)", new Color(60, 60, 60), Color.WHITE);
        upgradeRodBtn = createStyledButton("▲ 낚싯대 강화 (5,000 G)", new Color(75, 0, 130), Color.WHITE); 
        upgradeShipBtn = createStyledButton("★ 선박 업그레이드 (" + fmt(nextShipCost) + " G)", new Color(184, 134, 11), Color.WHITE); 

        actionBtn.addActionListener(e -> handleFishingAction());
        repairBtn.addActionListener(e -> repairRod());
        upgradeRodBtn.addActionListener(e -> upgradeRod());
        upgradeShipBtn.addActionListener(e -> upgradeShip());

        buttonPanel.add(actionBtn); buttonPanel.add(repairBtn); 
        buttonPanel.add(upgradeRodBtn); buttonPanel.add(upgradeShipBtn);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(bgColor.darker(), 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void showTutorial() {
        JDialog tutorialDialog = new JDialog(this, "게임 가이드", true);
        tutorialDialog.setSize(520, 360);
        tutorialDialog.setLocationRelativeTo(this);
        tutorialDialog.setLayout(new BorderLayout());
        tutorialDialog.getContentPane().setBackground(new Color(30, 30, 30));

        String page1 = "<html><div style='padding:15px; font-family:\"맑은 고딕\"; color:white;'>"
                + "<h2 style='color:#66B2FF;'>[ 게임 가이드 1/2 ]</h2>"
                + "<hr style='border: 1px solid #555;'>"
                + "<ul><li style='margin-bottom:8px;'><b>낚시 시작:</b> [낚싯대 던지기]를 누르고 입질을 기다리세요.</li>"
                + "<li style='margin-bottom:8px;'><b>타이밍 액션:</b> 입질이 오면 빠르게 [챔질하기]를 눌러야 합니다.</li>"
                + "<li style='margin-bottom:8px;'><b>등급 시스템:</b> 총 6가지 등급이 존재합니다.<br>"
                + "<span style='font-size:11px; color:#AAA;'>(일반: 3초 / 레어: 2.5초 / 희귀: 2초 / 전설,초월: 1초 이내 클릭)</span></li>"
                + "<li><b>내구도:</b> 낚시를 할 때마다 장비 내구도가 감소하니 상점에서 수리하세요.</li></ul>"
                + "</div></html>";

        String page2 = "<html><div style='padding:15px; font-family:\"맑은 고딕\"; color:white;'>"
                + "<h2 style='color:#FFD700;'>[ 성장 및 보스 2/2 ]</h2>"
                + "<hr style='border: 1px solid #555;'>"
                + "<ul><li style='margin-bottom:8px;'><b>장비 강화:</b> 고급 어종 획득 확률이 증가하며, "
                + "특정 레벨마다 낚싯대 외형이 진화합니다.</li>"
                + "<li style='margin-bottom:8px;'><b>선박 업그레이드:</b> 배를 업그레이드 해보세요! "
                + "낡은 뗏목에서 초호화 요트까지 디자인이 변합니다.</li>"
                + "<li><b>[경고] 거대 상어 출현:</b> 매우 낮은 확률로 거대 상어가 나타납니다.<br>"
                + "상어는 5초 안에 버튼을 10번 연타해야만 잡을 수 있습니다!</li></ul>"
                + "</div></html>";

        JLabel contentLabel = new JLabel(page1);
        contentLabel.setVerticalAlignment(SwingConstants.TOP);
        tutorialDialog.add(contentLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(new Color(30, 30, 30));
        
        JButton prevBtn = createStyledButton("◀ 이전", new Color(60, 60, 60), Color.WHITE);
        JButton nextBtn = createStyledButton("다음 ▶", new Color(60, 60, 60), Color.WHITE);
        JButton startBtn = createStyledButton("게임 시작", new Color(34, 139, 34), Color.WHITE); 

        prevBtn.setEnabled(false); startBtn.setVisible(false); 

        nextBtn.addActionListener(e -> {
            contentLabel.setText(page2); prevBtn.setEnabled(true);
            nextBtn.setVisible(false); startBtn.setVisible(true);
        });
        prevBtn.addActionListener(e -> {
            contentLabel.setText(page1); prevBtn.setEnabled(false);
            nextBtn.setVisible(true); startBtn.setVisible(false);
        });
        startBtn.addActionListener(e -> tutorialDialog.dispose());

        btnPanel.add(prevBtn); btnPanel.add(nextBtn); btnPanel.add(startBtn);
        tutorialDialog.add(btnPanel, BorderLayout.SOUTH);
        tutorialDialog.setVisible(true);
    }

    private void handleFishingAction() {
        if (currentState == GameState.IDLE) startFishing();
        else if (currentState == GameState.WAITING_FOR_BITE) failEarlyBite();
        else if (currentState == GameState.BITING) catchFishSuccess();
        else if (currentState == GameState.SHARK_FIGHT) handleSharkFightClick();
    }

    private void startFishing() {
        if (currentDurability <= 0) {
            JOptionPane.showMessageDialog(this, "내구도가 부족합니다. 장비를 수리해주세요.", "시스템", JOptionPane.ERROR_MESSAGE); return;
        }
        
        currentDurability--; updateUI();
        currentState = GameState.WAITING_FOR_BITE;
        gameScreen.setCatchText("... 찌를 주시하는 중 ...");
        actionBtn.setText("대기...");
        actionBtn.setBackground(new Color(100, 100, 100)); 
        
        repairBtn.setEnabled(false); upgradeRodBtn.setEnabled(false); upgradeShipBtn.setEnabled(false);

        determineFish();
        int randomWaitTime = random.nextInt(3000) + 2000;
        waitTimer = new Timer(randomWaitTime, e -> onBite());
        waitTimer.setRepeats(false); waitTimer.start();
    }

    private void determineFish() {
        int roll = random.nextInt(100) + 1 + upgradeLevel;

        if (roll <= 10) { targetFish = "[꽝] 낡은 장화"; targetPrice = 0; } 
        else if (roll <= 45) { targetFish = "[일반] 바다 물고기"; targetPrice = 1000; } 
        else if (roll <= 70) { targetFish = "[레어] 붉은 도미"; targetPrice = 2000; } 
        else if (roll <= 90) { targetFish = "[희귀] 황금 잉어"; targetPrice = 3000; } 
        else if (roll <= 99) { targetFish = "[전설] 심해 청새치"; targetPrice = 10000; } 
        else { targetFish = "[초월] 환상의 해룡"; targetPrice = 30000; }
    }

    private void onBite() {
        if (random.nextInt(1000) == 0) { startSharkFight(); return; }
        
        currentState = GameState.BITING;
        int targetTime; 
        
        if (targetFish.contains("장화")) targetTime = 3000;
        else if (targetFish.contains("일반")) targetTime = 3000;
        else if (targetFish.contains("레어")) targetTime = 2500;
        else if (targetFish.contains("희귀")) targetTime = 2000;
        else targetTime = 1000; 

        gameScreen.setCatchText("!!! 입질 발생 !!!");
        actionBtn.setText("⚡ 챔질하기 ⚡");
        actionBtn.setBackground(new Color(220, 20, 60)); 

        biteTimer = new Timer(targetTime, e -> failTimeout());
        biteTimer.setRepeats(false); biteTimer.start();
    }

    private void startSharkFight() {
        currentState = GameState.SHARK_FIGHT;
        sharkClickCount = 0; sharkTimeLeft = 5000; 
        
        gameScreen.setCatchText("!!! 거대 상어 출현 !!! (연타)");
        actionBtn.setBackground(new Color(200, 0, 0));
        
        sharkTimer = new Timer(100, e -> {
            sharkTimeLeft -= 100;
            actionBtn.setText(String.format("🔥 연타! (%d/10) - %.1f초", sharkClickCount, sharkTimeLeft/1000.0));
            if (sharkTimeLeft <= 0) { sharkTimer.stop(); failShark(); }
        });
        sharkTimer.start();
    }

    private void handleSharkFightClick() {
        sharkClickCount++;
        if (sharkClickCount >= 10) { sharkTimer.stop(); catchSharkSuccess(); }
    }

    private void catchFishSuccess() {
        biteTimer.stop(); money += targetPrice;
        gameScreen.setCatchText("포획 성공: " + targetFish);
        if (targetPrice == 0) logArea.append(">> 쓰레기를 건졌습니다. (수익 없음)\n");
        else logArea.append(">> 포획 성공! " + targetFish + " (+ " + fmt(targetPrice) + " G)\n");
        endFishingSequence();
    }

    private void catchSharkSuccess() {
        int sharkPrice = 100000; money += sharkPrice;
        logArea.append(">> ★★★ 전설의 거대 상어 포획 성공!!! (+ " + fmt(sharkPrice) + " G) ★★★\n");
        gameScreen.setCatchText("★ 거대 상어 제압 성공 ★");
        triggerFlashEffect(); 
        endFishingSequence();
    }

    private void failEarlyBite() {
        waitTimer.stop(); 
        gameScreen.setCatchText("실패: 너무 일찍 당겼습니다.");
        logArea.append(">> 성급했습니다. 주변에 있던 " + targetFish + "이(가) 도망갔습니다.\n"); 
        endFishingSequence();
    }

    private void failTimeout() {
        gameScreen.setCatchText("실패: 물고기가 도망갔습니다.");
        logArea.append(">> 반응이 늦었습니다. " + targetFish + "이(가) 미끼를 먹고 도망갔습니다.\n"); 
        endFishingSequence();
    }

    private void failShark() {
        gameScreen.setCatchText("실패: 상어에게 낚싯줄이 끊어졌습니다.");
        logArea.append(">> 힘(연타)이 부족하여 거대 상어를 놓쳤습니다!\n"); endFishingSequence();
    }

    private void endFishingSequence() {
        currentState = GameState.IDLE; updateUI();
        actionBtn.setText("▶ 낚싯대 던지기"); actionBtn.setBackground(new Color(0, 90, 158)); 
        repairBtn.setEnabled(true); upgradeRodBtn.setEnabled(true); upgradeShipBtn.setEnabled(true);
        if (currentDurability == 0) logArea.append("[시스템] 장비 내구도가 소진되었습니다.\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void triggerFlashEffect() {
        isFlashing = true; actionBtn.setEnabled(false); 
        Timer flashTimer = new Timer(100, e -> gameScreen.repaint()); flashTimer.start();
        Timer stopFlash = new Timer(2000, e -> {
            isFlashing = false; flashTimer.stop(); actionBtn.setEnabled(true); gameScreen.repaint();
        });
        stopFlash.setRepeats(false); stopFlash.start();
    }

    private void repairRod() {
        if (currentDurability == MAX_DURABILITY) { JOptionPane.showMessageDialog(this, "내구도가 가득 차 있습니다.", "시스템", JOptionPane.INFORMATION_MESSAGE); return; }
        if (money >= 1000) {
            money -= 1000; currentDurability = MAX_DURABILITY; updateUI();
            gameScreen.setCatchText("장비 수리 완료!"); logArea.append(">> 장비를 수리했습니다.\n");
        } else JOptionPane.showMessageDialog(this, "자산이 부족합니다.", "시스템", JOptionPane.WARNING_MESSAGE);
    }

    private void upgradeRod() {
        int cost = 5000 + (upgradeLevel * 1000);
        if (money >= cost) {
            money -= cost; upgradeLevel++; updateUI();
            upgradeRodBtn.setText("▲ 낚싯대 강화 (" + fmt(5000 + upgradeLevel * 1000) + " G)");
            gameScreen.setCatchText("강화 성공! (Lv." + upgradeLevel + ")"); 
            logArea.append(">> 장비 강화 성공! (Lv." + upgradeLevel + ")\n");
        } else JOptionPane.showMessageDialog(this, "자산이 부족합니다. (" + fmt(cost) + " G 필요)", "시스템", JOptionPane.WARNING_MESSAGE);
    }

    private void upgradeShip() {
        if (money >= nextShipCost) {
            money -= nextShipCost; shipLevel++; nextShipCost *= 2; 
            updateUI();
            upgradeShipBtn.setText("★ 선박 업그레이드 (" + fmt(nextShipCost) + " G)");
            gameScreen.setCatchText("선박 업그레이드 완료! (Lv." + shipLevel + ")");
            logArea.append(">> 선박을 Lv." + shipLevel + "(으)로 업그레이드했습니다!\n");
        } else {
            JOptionPane.showMessageDialog(this, "자산이 부족합니다. (" + fmt(nextShipCost) + " G 필요)", "시스템", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateUI() {
        moneyLabel.setText("[자산] " + fmt(money) + " G"); 
        rodLabel.setText("[장비 Lv] " + upgradeLevel);
        shipLabel.setText("[선박 Lv] " + shipLevel);
        durabilityLabel.setText("[내구도] " + currentDurability + " / " + MAX_DURABILITY);
        if (currentDurability <= 5) durabilityLabel.setForeground(new Color(255, 100, 100));
        else durabilityLabel.setForeground(new Color(144, 238, 144));
        gameScreen.repaint(); 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FishingGame game = new FishingGame();
            game.setVisible(true);
            game.showTutorial(); 
        });
    }

    class GameScreen extends JPanel {
        private String catchText = "대기 중...";
        public void setCatchText(String text) { this.catchText = text; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth(); int height = getHeight();

            if (isFlashing) {
                g2d.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(Color.WHITE); g2d.setFont(new Font("맑은 고딕", Font.BOLD, 45));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString("★ SHARK CAUGHT ★", (width - fm.stringWidth("★ SHARK CAUGHT ★")) / 2, height / 2);
                return; 
            }

            GradientPaint skyGradient = new GradientPaint(0, 0, new Color(20, 20, 60), 0, height / 2, new Color(200, 90, 60));
            g2d.setPaint(skyGradient);
            g2d.fillRect(0, 0, width, height / 2); 
            
            if (currentState == GameState.SHARK_FIGHT) {
                g2d.setColor(new Color(255, 0, 0, 80)); g2d.fillRect(0, 0, width, height / 2); 
            }
            
            g2d.setColor(new Color(255, 200, 100, 200)); 
            g2d.fillOval(width - 160, height / 2 - 80, 120, 120); 
            
            GradientPaint oceanGradient = new GradientPaint(0, height / 2, new Color(10, 40, 80), 0, height, new Color(5, 15, 30));
            g2d.setPaint(oceanGradient);
            g2d.fillRect(0, height / 2, width, height / 2); 

            if (shipLevel == 1) {
                g2d.setColor(new Color(100, 50, 20)); g2d.fillArc(-50, height - 100, width + 100, 200, 0, 180); 
                g2d.setColor(new Color(50, 20, 10)); g2d.setStroke(new BasicStroke(5));
                g2d.drawArc(-50, height - 100, width + 100, 200, 0, 180);
            } else if (shipLevel == 2) {
                g2d.setColor(new Color(150, 90, 40)); g2d.fillArc(-50, height - 120, width + 100, 240, 0, 180); 
                g2d.setColor(new Color(80, 80, 80)); g2d.setStroke(new BasicStroke(5)); 
                g2d.drawArc(-40, height - 130, width + 80, 220, 0, 180);
                g2d.setColor(new Color(90, 40, 20)); g2d.setStroke(new BasicStroke(6));
                g2d.drawArc(-50, height - 120, width + 100, 240, 0, 180);
            } else {
                g2d.setColor(new Color(240, 240, 245)); g2d.fillArc(-50, height - 140, width + 100, 280, 0, 180); 
                g2d.setColor(new Color(0, 255, 255, 120)); g2d.setStroke(new BasicStroke(10)); 
                g2d.drawArc(-40, height - 150, width + 80, 260, 0, 180);
                g2d.setColor(new Color(180, 180, 190)); g2d.setStroke(new BasicStroke(4));
                g2d.drawArc(-50, height - 140, width + 100, 280, 0, 180);
            }

            if (currentDurability <= 0) {
                g2d.setColor(new Color(200, 50, 50)); 
            } else {
                if (upgradeLevel < 5) g2d.setColor(new Color(70, 70, 70));             
                else if (upgradeLevel < 10) g2d.setColor(new Color(211, 211, 211));    
                else if (upgradeLevel < 15) g2d.setColor(new Color(255, 215, 0));      
                else if (upgradeLevel < 20) g2d.setColor(new Color(0, 191, 255));      
                else {
                    g2d.setColor(new Color(148, 0, 211, 120)); 
                    g2d.setStroke(new BasicStroke(20, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(width, height, width / 2 + 50, height / 2 - 20); 
                    g2d.setColor(new Color(255, 20, 147)); 
                }
            }
            g2d.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(width, height, width / 2 + 50, height / 2 - 20); 
            
            if (currentDurability > 0) {
                g2d.setColor(new Color(255, 255, 255, 150)); g2d.setStroke(new BasicStroke(2));
                if (currentState == GameState.BITING || currentState == GameState.SHARK_FIGHT) {
                    g2d.drawLine(width / 2 + 50, height / 2 - 20, width / 2 + 50, height / 2 + 80); 
                    g2d.setColor(new Color(255, 215, 0)); g2d.fillOval(width / 2 + 40, height / 2 + 80, 20, 20); 
                } else if (currentState == GameState.WAITING_FOR_BITE) {
                    g2d.drawLine(width / 2 + 50, height / 2 - 20, width / 2 + 50, height / 2 + 50); 
                    g2d.setColor(new Color(220, 20, 60)); g2d.fillOval(width / 2 + 45, height / 2 + 50, 10, 10);
                }
            }

            int legX = 15; int legY = 15;
            g2d.setColor(new Color(0, 0, 0, 160)); 
            g2d.fillRoundRect(legX, legY, 210, 170, 15, 15);
            g2d.setColor(new Color(218, 165, 32)); 
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(legX, legY, 210, 170, 15, 15);
            
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            g2d.setColor(new Color(255, 223, 0)); g2d.drawString("[ 어종 시세표 ]", legX + 15, legY + 25);
            
            g2d.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            int startY = legY + 45; int gap = 20;
            g2d.setColor(new Color(169, 169, 169)); g2d.drawString(" [꽝] 낡은 장화 : 0 G", legX + 15, startY);
            g2d.setColor(Color.WHITE);              g2d.drawString(" [일반] 바다 물고기 : 1,000 G", legX + 15, startY + gap);
            g2d.setColor(new Color(135, 206, 250)); g2d.drawString(" [레어] 붉은 도미 : 2,000 G", legX + 15, startY + gap*2);
            g2d.setColor(new Color(221, 160, 221)); g2d.drawString(" [희귀] 황금 잉어 : 3,000 G", legX + 15, startY + gap*3);
            g2d.setColor(new Color(255, 140, 0));   g2d.drawString(" [전설] 심해 청새치 : 10,000 G", legX + 15, startY + gap*4);
            g2d.setColor(new Color(255, 105, 180)); g2d.drawString(" [초월] 환상의 해룡 : 30,000 G", legX + 15, startY + gap*5);
            
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(width / 2 - 200, height / 2 - 80, 400, 45, 20, 20);
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (width - fm.stringWidth(catchText)) / 2;
            g2d.drawString(catchText, textX, height / 2 - 50);
        }
    }
}