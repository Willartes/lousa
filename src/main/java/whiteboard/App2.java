package whiteboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class App2 extends JFrame {

    private CanvasPanel canvas;
    private boolean erasing = false;
    private boolean drawingLine = false;
    private boolean highlighting = false;
    private Color currentColor = Color.BLACK;
    private int penThickness = 2;
    private int defaultPenThickness = 2;
    private int thickerPenThickness = 5;
    private boolean isThicker = false;
    private boolean gridVisible = false;
    private JButton thicknessButton;
    private JButton gridButton;
    private List<Point> currentLine = new ArrayList<>();
    private List<ColoredLine> allLines = new ArrayList<>();
    private Point highlightStart;
    private Point highlightEnd;

    private static class ColoredLine {
        List<Point> points;
        Color color;
        int thickness;

        ColoredLine(List<Point> points, Color color, int thickness) {
            this.points = points;
            this.color = color;
            this.thickness = thickness;
        }
    }

    public App2() {
        initializeUI();
        setupMouseListeners();
    }

    private void initializeUI() {
        setTitle("Whiteboard Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        canvas = new CanvasPanel();
        canvas.setBackground(Color.WHITE);

        JPanel buttonPanel = createButtonPanel();

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(createColorButton("Cor Preta", Color.BLACK));
        buttonPanel.add(createColorButton("Cor Vermelha", Color.RED));
        buttonPanel.add(createColorButton("Cor Azul", Color.BLUE));

        JButton highlightButton = new JButton("Marca Texto");
        highlightButton.addActionListener(e -> toggleHighlight());
        buttonPanel.add(highlightButton);

        thicknessButton = new JButton("Espessura Maior");
        thicknessButton.addActionListener(e -> togglePenThickness());
        buttonPanel.add(thicknessButton);

        JButton alignLineButton = new JButton("Alinhar Reta");
        alignLineButton.addActionListener(e -> alignLine());
        buttonPanel.add(alignLineButton);

        JButton eraseButton = new JButton("Apagar");
        eraseButton.addActionListener(e -> startErase());
        buttonPanel.add(eraseButton);

        JButton clearButton = new JButton("Limpar");
        clearButton.addActionListener(e -> clearCanvas());
        buttonPanel.add(clearButton);

        gridButton = new JButton("Fundo Quadriculado");
        gridButton.addActionListener(e -> toggleGrid());
        buttonPanel.add(gridButton);

        JButton eraseLastLineButton = new JButton("Apagar Última Linha");
        eraseLastLineButton.addActionListener(e -> eraseLastLine());
        buttonPanel.add(eraseLastLineButton);

        return buttonPanel;
    }

    private JButton createColorButton(String label, Color color) {
        JButton button = new JButton(label);
        button.addActionListener(e -> setDrawColor(color));
        return button;
    }

    private void setupMouseListeners() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (highlighting) {
                    highlightStart = e.getPoint();
                } else {
                    startDrawing(e.getPoint());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (highlighting) {
                    highlightEnd = e.getPoint();
                    highlightText();
                } else {
                    stopDrawing(e.getPoint());
                }
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (highlighting) {
                    highlightEnd = e.getPoint();
                    canvas.repaint();
                } else {
                    draw(e.getPoint());
                }
            }
        });
    }

    private void startDrawing(Point p) {
        if (!erasing) {
            drawingLine = true;
            currentLine.clear();
            currentLine.add(p);
        }
    }

    private void draw(Point p) {
        if (drawingLine && !erasing) {
            Point lastPoint = currentLine.get(currentLine.size() - 1);
            canvas.drawLine(lastPoint.x, lastPoint.y, p.x, p.y, currentColor, penThickness);
            currentLine.add(p);
        } else if (erasing) {
            canvas.erase(p.x, p.y);
        }
    }

    private void stopDrawing(Point p) {
        if (drawingLine && !erasing) {
            currentLine.add(p);
            allLines.add(new ColoredLine(new ArrayList<>(currentLine), currentColor, penThickness));
            drawingLine = false;
        }
        erasing = false;
    }

    private void setDrawColor(Color color) {
        erasing = false;
        currentColor = color;
    }

    private void togglePenThickness() {
        isThicker = !isThicker;
        penThickness = isThicker ? thickerPenThickness : defaultPenThickness;
        thicknessButton.setText(isThicker ? "Espessura Padrão" : "Espessura Maior");
    }

    private void toggleGrid() {
        gridVisible = !gridVisible;
        gridButton.setText(gridVisible ? "Fundo Padrão" : "Fundo Quadriculado");
        canvas.repaint();
    }

    private void startErase() {
        erasing = true;
        drawingLine = false;
    }

    private void alignLine() {
        if (currentLine.size() < 2) {
            JOptionPane.showMessageDialog(this, "Desenhe uma linha primeiro antes de alinhar.");
            return;
        }

        Point start = currentLine.get(0);
        Point end = currentLine.get(currentLine.size() - 1);

        if (!allLines.isEmpty()) {
            allLines.remove(allLines.size() - 1);
        }
        List<Point> alignedLine = new ArrayList<>();
        alignedLine.add(start);
        alignedLine.add(end);
        allLines.add(new ColoredLine(alignedLine, currentColor, penThickness));

        redrawCanvas();
        currentLine.clear();
    }

    private void clearCanvas() {
        canvas.clear();
        currentLine.clear();
        allLines.clear();
    }

    private void toggleHighlight() {
        highlighting = !highlighting;
        if (highlighting) {
            drawingLine = false;
            erasing = false;
        }
    }

    private void highlightText() {
        if (highlightStart != null && highlightEnd != null) {
            int x = Math.min(highlightStart.x, highlightEnd.x);
            int y = Math.min(highlightStart.y, highlightEnd.y);
            int width = Math.abs(highlightEnd.x - highlightStart.x);
            int height = Math.abs(highlightEnd.y - highlightStart.y);
            canvas.highlight(x, y, width, height);
        }
        highlighting = false;
        highlightStart = null;
        highlightEnd = null;
    }

    private void eraseLastLine() {
        if (!allLines.isEmpty()) {
            allLines.remove(allLines.size() - 1);
            redrawCanvas();
        }
    }

    private void redrawCanvas() {
        canvas.clear();
        for (ColoredLine line : allLines) {
            for (int i = 1; i < line.points.size(); i++) {
                Point start = line.points.get(i - 1);
                Point end = line.points.get(i);
                canvas.drawLine(start.x, start.y, end.x, end.y, line.color, line.thickness);
            }
        }
    }

    class CanvasPanel extends JPanel {
        private BufferedImage image;
        private Graphics2D g2;

        public CanvasPanel() {
            setPreferredSize(new Dimension(800, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2 = (Graphics2D) image.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                clear();
            }
            g.drawImage(image, 0, 0, this);
            if (gridVisible) {
                drawGrid(g);
            }
            if (highlighting && highlightStart != null && highlightEnd != null) {
                drawHighlight(g);
            }
        }

        private void drawGrid(Graphics g) {
            g.setColor(new Color(200, 200, 200, 100));
            int gridSize = 40;
            for (int i = 0; i < getWidth(); i += gridSize) {
                g.drawLine(i, 0, i, getHeight());
            }
            for (int i = 0; i < getHeight(); i += gridSize) {
                g.drawLine(0, i, getWidth(), i);
            }
        }

        private void drawHighlight(Graphics g) {
            g.setColor(new Color(255, 255, 0, 100));
            int x = Math.min(highlightStart.x, highlightEnd.x);
            int y = Math.min(highlightStart.y, highlightEnd.y);
            int width = Math.abs(highlightEnd.x - highlightStart.x);
            int height = Math.abs(highlightEnd.y - highlightStart.y);
            g.fillRect(x, y, width, height);
        }

        public void clear() {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            repaint();
        }

        public void drawLine(int x1, int y1, int x2, int y2, Color color, int thickness) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            repaint();
        }

        public void erase(int x, int y) {
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 10, y - 10, 20, 20);
            repaint();
        }

        public void highlight(int x, int y, int width, int height) {
            g2.setColor(new Color(255, 255, 0, 100));
            g2.fillRect(x, y, width, height);
            repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }
}