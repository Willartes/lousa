package whiteboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class App2 extends JFrame {

    private CanvasPanel canvas;
    private boolean erasing = false;
    private boolean drawingLine = false;
    private boolean highlighting = false;
    private Color currentColor = Color.BLACK;
    private int penThickness = 2;
    private boolean gridVisible = false;
    private JButton gridButton;
    private JSlider thicknessSlider;
    private List<Point2D> currentLine = new ArrayList<>();
    private List<ColoredLine> allLines = new ArrayList<>();
    private List<HighlightArea> highlightAreas = new ArrayList<>();
    private Point highlightStart;
    private Point highlightEnd;

    private static class ColoredLine {
        List<Point2D> points;
        Color color;
        int thickness;

        ColoredLine(List<Point2D> points, Color color, int thickness) {
            this.points = points;
            this.color = color;
            this.thickness = thickness;
        }
    }

    private static class HighlightArea {
        int x, y, width, height;

        HighlightArea(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
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

        JComboBox<String> colorComboBox = createColorComboBox();
        buttonPanel.add(colorComboBox);

        JButton highlightButton = new JButton("Marca Texto");
        highlightButton.addActionListener(e -> toggleHighlight());
        buttonPanel.add(highlightButton);

        thicknessSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 2);
        thicknessSlider.setMajorTickSpacing(1);
        thicknessSlider.setPaintTicks(true);
        thicknessSlider.setPaintLabels(true);
        thicknessSlider.addChangeListener(e -> penThickness = thicknessSlider.getValue());
        buttonPanel.add(new JLabel("Espessura:"));
        buttonPanel.add(thicknessSlider);

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

    private JComboBox<String> createColorComboBox() {
        String[] colorNames = {"Preto", "Vermelho", "Azul", "Verde", "Amarelo", "Roxo", "Laranja", "Cinza"};
        JComboBox<String> colorComboBox = new JComboBox<>(colorNames);
        colorComboBox.addActionListener(e -> {
            String selectedColor = (String) colorComboBox.getSelectedItem();
            switch (selectedColor) {
                case "Preto":
                    setDrawColor(Color.BLACK);
                    break;
                case "Vermelho":
                    setDrawColor(Color.RED);
                    break;
                case "Azul":
                    setDrawColor(Color.BLUE);
                    break;
                case "Verde":
                    setDrawColor(Color.GREEN);
                    break;
                case "Amarelo":
                    setDrawColor(Color.YELLOW);
                    break;
                case "Roxo":
                    setDrawColor(new Color(128, 0, 128));
                    break;
                case "Laranja":
                    setDrawColor(Color.ORANGE);
                    break;
                case "Cinza":
                    setDrawColor(Color.GRAY);
                    break;
            }
        });
        return colorComboBox;
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
            currentLine.add(new Point2D.Double(p.x, p.y));
        }
    }

    private void draw(Point p) {
        if (drawingLine && !erasing) {
            currentLine.add(new Point2D.Double(p.x, p.y));
            canvas.repaint();
        } else if (erasing) {
            canvas.erase(p.x, p.y);
        }
    }

    private void stopDrawing(Point p) {
        if (drawingLine && !erasing) {
            currentLine.add(new Point2D.Double(p.x, p.y));
            allLines.add(new ColoredLine(new ArrayList<>(currentLine), currentColor, penThickness));
            drawingLine = false;
            currentLine.clear();
        }
        erasing = false;
        canvas.repaint();
    }

    private void setDrawColor(Color color) {
        erasing = false;
        currentColor = color;
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

    private void clearCanvas() {
        currentLine.clear();
        allLines.clear();
        highlightAreas.clear();
        canvas.repaint();
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
            highlightAreas.add(new HighlightArea(x, y, width, height));
            canvas.repaint();
        }
        highlighting = false;
        highlightStart = null;
        highlightEnd = null;
    }

    private void eraseLastLine() {
        if (!allLines.isEmpty()) {
            allLines.remove(allLines.size() - 1);
            canvas.repaint();
        }
    }

    private void alignLine() {
        if (allLines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Desenhe uma linha primeiro antes de alinhar.");
            return;
        }

        ColoredLine lastLine = allLines.get(allLines.size() - 1);
        List<Point2D> points = lastLine.points;
        if (points.size() < 2) {
            return;
        }

        Point2D start = points.get(0);
        Point2D end = points.get(points.size() - 1);

        List<Point2D> alignedLine = new ArrayList<>();
        alignedLine.add(start);
        alignedLine.add(end);

        // Remove a última linha não alinhada
        allLines.remove(allLines.size() - 1);

        // Adiciona a nova linha alinhada
        allLines.add(new ColoredLine(alignedLine, lastLine.color, lastLine.thickness));

        canvas.repaint();
    }

    class CanvasPanel extends JPanel {
        public CanvasPanel() {
            setPreferredSize(new Dimension(800, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Draw all saved lines
            for (ColoredLine line : allLines) {
                g2.setColor(line.color);
                g2.setStroke(new BasicStroke(line.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawSmoothLine(g2, line.points);
            }

            // Draw current line
            if (drawingLine && !currentLine.isEmpty()) {
                g2.setColor(currentColor);
                g2.setStroke(new BasicStroke(penThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawSmoothLine(g2, currentLine);
            }

            // Draw highlight areas
            for (HighlightArea area : highlightAreas) {
                g2.setColor(new Color(255, 255, 0, 128));
                g2.fillRect(area.x, area.y, area.width, area.height);
            }

            if (highlighting && highlightStart != null && highlightEnd != null) {
                g2.setColor(new Color(255, 255, 0, 128));
                int x = Math.min(highlightStart.x, highlightEnd.x);
                int y = Math.min(highlightStart.y, highlightEnd.y);
                int width = Math.abs(highlightEnd.x - highlightStart.x);
                int height = Math.abs(highlightEnd.y - highlightStart.y);
                g2.fillRect(x, y, width, height);
            }

            if (gridVisible) {
                drawGrid(g2);
            }

            g2.dispose();
        }

        private void drawSmoothLine(Graphics2D g2, List<Point2D> points) {
            if (points.size() < 2) return;

            GeneralPath path = new GeneralPath();
            path.moveTo(points.get(0).getX(), points.get(0).getY());

            for (int i = 1; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i - 1);
                Point2D p2 = points.get(i);
                Point2D p3 = points.get(i + 1);

                double x1 = p1.getX();
                double y1 = p1.getY();
                double x2 = p2.getX();
                double y2 = p2.getY();
                double x3 = p3.getX();
                double y3 = p3.getY();

                double ctrl1X = (x1 + x2) / 2;
                double ctrl1Y = (y1 + y2) / 2;
                double ctrl2X = (x2 + x3) / 2;
                double ctrl2Y = (y2 + y3) / 2;

                path.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, x3, y3);
            }

            if (points.size() > 1) {
                Point2D lastPoint = points.get(points.size() - 1);
                path.lineTo(lastPoint.getX(), lastPoint.getY());
            }

            g2.draw(path);
        }

        private void erase(int x, int y) {
            int eraseSize = penThickness * 10;
            Rectangle2D eraser = new Rectangle2D.Double(x - eraseSize / 2.0, y - eraseSize / 2.0, eraseSize, eraseSize);
            allLines.removeIf(line -> line.points.stream().anyMatch(p -> eraser.contains(p)));
            repaint();
        }

        private void drawGrid(Graphics2D g2) {
            int gridSize = 40; // Define o tamanho do grid (espaçamento entre as linhas)
            g2.setColor(new Color(205, 205, 205, 70));

            // Define a espessura da linha do grid
            float gridLineThickness = 1.0f; // Espessura da linha do grid
            g2.setStroke(new BasicStroke(gridLineThickness));

            for (int i = 0; i < getWidth(); i += gridSize) {
                g2.drawLine(i, 0, i, getHeight());
            }
            for (int i = 0; i < getHeight(); i += gridSize) {
                g2.drawLine(0, i, getWidth(), i);
            }
        }
    }

}