package whiteboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class App extends JFrame {

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

    public App() {
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
        String[] colorNames = {"Preto", "Vermelho", "Azul", "Verde", "Amarelo"};
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
            Point2D lastPoint = currentLine.get(currentLine.size() - 1);
            canvas.drawLine(lastPoint.getX(), lastPoint.getY(), p.x, p.y, currentColor, penThickness);
            currentLine.add(new Point2D.Double(p.x, p.y));
        } else if (erasing) {
            canvas.erase(p.x, p.y);
        }
    }

    private void stopDrawing(Point p) {
        if (drawingLine && !erasing) {
            currentLine.add(new Point2D.Double(p.x, p.y));
            allLines.add(new ColoredLine(new ArrayList<>(currentLine), currentColor, penThickness));
            drawingLine = false;
        }
        erasing = false;
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

    private void alignLine() {
        if (currentLine.size() < 2) {
            JOptionPane.showMessageDialog(this, "Desenhe uma linha primeiro antes de alinhar.");
            return;
        }

        Point2D start = currentLine.get(0);
        Point2D end = currentLine.get(currentLine.size() - 1);

        if (!allLines.isEmpty()) {
            allLines.remove(allLines.size() - 1);
        }
        List<Point2D> alignedLine = new ArrayList<>();
        alignedLine.add(start);
        alignedLine.add(end);
        allLines.add(new ColoredLine(alignedLine, currentColor, penThickness));

        redrawCanvas();
        currentLine.clear();
    }

    private void redrawCanvas() {
        canvas.clear();
        for (ColoredLine line : allLines) {
            canvas.drawSmoothLine(line.points, line.color, line.thickness);
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
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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

        public void drawLine(double x1, double y1, double x2, double y2, Color color, int thickness) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
            repaint();
        }

        public void drawSmoothLine(List<Point2D> points, Color color, int thickness) {
            if (points.size() < 2) return;

            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            GeneralPath path = new GeneralPath();
            path.moveTo(points.get(0).getX(), points.get(0).getY());

            for (int i = 1; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i - 1);
                Point2D p2 = points.get(i);
                Point2D p3 = points.get(i + 1);

                double cx1 = (p1.getX() + p2.getX()) / 2;
                double cy1 = (p1.getY() + p2.getY()) / 2;
                double cx2 = (p2.getX() + p3.getX()) / 2;
                double cy2 = (p2.getY() + p3.getY()) / 2;

                path.quadTo(p2.getX(), p2.getY(), cx2, cy2);
            }

            path.lineTo(points.get(points.size() - 1).getX(), points.get(points.size() - 1).getY());
            g2.draw(path);
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