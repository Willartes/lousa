package whiteboard;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class App3 extends JFrame {

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
    private List<ImageItem> imageItems = new ArrayList<>();
    private List<TextItem> textItems = new ArrayList<>();

    private static class ImageItem {
        BufferedImage image;
        int x, y;

        ImageItem(BufferedImage image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }

    private static class TextItem {
        String text;
        int x, y;
        Font font;
        Color color;

        TextItem(String text, int x, int y, Font font, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.font = font;
            this.color = color;
        }
    }

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

    public App3() {
        initializeUI();
        setupMouseListeners();
        createPenCursor(); // Adiciona o cursor de caneta
    }

    private void initializeUI() {
        setTitle("Whiteboard Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        canvas = new CanvasPanel();
        canvas.setBackground(Color.WHITE);

        JPanel buttonPanel = createButtonPanel();

        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setupDragAndDrop();

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

        gridButton = new JButton("Quadriculado");
        gridButton.addActionListener(e -> toggleGrid());
        buttonPanel.add(gridButton);

        JButton eraseLastLineButton = new JButton("Apagar Última Linha");
        eraseLastLineButton.addActionListener(e -> eraseLastLine());
        buttonPanel.add(eraseLastLineButton);

        // Criar o menu suspenso para Inserir
        JComboBox<String> insertComboBox = new JComboBox<>(new String[]{"Inserir", "Imagem", "Texto"});
        insertComboBox.addActionListener(e -> {
            if (insertComboBox.getSelectedIndex() == 1) {
                insertImage();
            } else if (insertComboBox.getSelectedIndex() == 2) {
                insertText();
            }
            insertComboBox.setSelectedIndex(0);
        });
        buttonPanel.add(insertComboBox);

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

    private void setupDragAndDrop() {
        canvas.setTransferHandler(new TransferHandler("image") {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                       support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (canImport(support)) {
                    try {
                        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                            Image image = (Image) support.getTransferable().getTransferData(DataFlavor.imageFlavor);
                            Point point = support.getDropLocation().getDropPoint();
                            imageItems.add(new ImageItem((BufferedImage) image, point.x, point.y));
                            canvas.repaint();
                            return true;
                        } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            String text = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                            Point point = support.getDropLocation().getDropPoint();
                            textItems.add(new TextItem(text, point.x, point.y, new Font("Arial", Font.PLAIN, 12), Color.BLACK));
                            canvas.repaint();
                            return true;
                        }
                    } catch (UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

        canvas.setDropTarget(new DropTarget(canvas, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        Transferable transferable = dtde.getTransferable();
                        Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                        Point point = dtde.getLocation();
                        imageItems.add(new ImageItem((BufferedImage) image, point.x, point.y));
                        canvas.repaint();
                        dtde.dropComplete(true);
                    } else if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        Transferable transferable = dtde.getTransferable();
                        String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        Point point = dtde.getLocation();
                        textItems.add(new TextItem(text, point.x, point.y, new Font("Arial", Font.PLAIN, 12), Color.BLACK));
                        canvas.repaint();
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    private void createPenCursor() {
        try {
            // Carrega a imagem da caneta
            BufferedImage cursorImg = ImageIO.read(new File("pen_cursor.png"));
            if (cursorImg == null) {
                System.err.println("Error: The image 'pen_cursor.png' is null.");
                return;
            }
            // Cria um cursor personalizado com a imagem da caneta
            Cursor penCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, cursorImg.getHeight() - 1), "Pen Cursor");
            // Define o cursor personalizado para o canvas
            canvas.setCursor(penCursor);
        } catch (IOException e) {
            System.err.println("Error loading pen cursor image: " + e.getMessage());
            // Fallback to default cursor if image can't be loaded
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
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
        gridButton.setText(gridVisible ? "Padrão" : "Quadriculado");
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
        imageItems.clear();
        textItems.clear();
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

        allLines.remove(allLines.size() - 1);
        allLines.add(new ColoredLine(alignedLine, lastLine.color, lastLine.thickness));

        canvas.repaint();
    }
    
    private void insertImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage image = ImageIO.read(selectedFile);
                if (image != null) {
                    Point center = new Point(canvas.getWidth() / 2, canvas.getHeight() / 2);
                    imageItems.add(new ImageItem(image, center.x - image.getWidth() / 2, center.y - image.getHeight() / 2));
                    canvas.repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Não foi possível carregar a imagem selecionada.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar a imagem: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

    private void insertText() {
        // Cria um campo de texto multilinha
        JTextArea textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Cria um painel para o diálogo
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Digite o texto a ser inserido:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Exibe o diálogo e obtém o texto inserido
        int result = JOptionPane.showConfirmDialog(this, panel, "Inserir Texto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String text = textArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                Point center = new Point(canvas.getWidth() / 2, canvas.getHeight() / 2);
                Font font = new Font("Arial", Font.PLAIN, 12);

                // Calcula o tamanho da fonte para que o texto caiba no canvas
                FontMetrics fm = canvas.getFontMetrics(font);
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();

                // Ajusta o tamanho da fonte se necessário
                while (textWidth > canvas.getWidth() || textHeight > canvas.getHeight()) {
                    font = font.deriveFont((float) (font.getSize() - 1));
                    fm = canvas.getFontMetrics(font);
                    textWidth = fm.stringWidth(text);
                    textHeight = fm.getHeight();
                }

                textItems.add(new TextItem(text, center.x, center.y, font, currentColor));
                canvas.repaint();
            }
        }
    }

    class CanvasPanel extends JPanel {
        private Point lastErasePoint = null;

        public CanvasPanel() {
            setPreferredSize(new Dimension(800, 6000));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            for (ColoredLine line : allLines) {
                g2.setColor(line.color);
                g2.setStroke(new BasicStroke(line.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawSmoothLine(g2, line.points);
            }

            if (drawingLine && !currentLine.isEmpty()) {
                g2.setColor(currentColor);
                g2.setStroke(new BasicStroke(penThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawSmoothLine(g2, currentLine);
            }

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

            if (erasing && lastErasePoint != null) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(penThickness * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(lastErasePoint.x, lastErasePoint.y, lastErasePoint.x, lastErasePoint.y);
            }

            if (gridVisible) {
                drawGrid(g2);
            }

            for (ImageItem item : imageItems) {
                g2.drawImage(item.image, item.x, item.y, null);
            }

            for (TextItem item : textItems) {
                g2.setColor(item.color);
                g2.setFont(item.font);
                g2.drawString(item.text, item.x, item.y);
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
            int eraseSize = penThickness * 5;
            Rectangle2D eraser = new Rectangle2D.Double(x - eraseSize / 2.0, y - eraseSize / 2.0, eraseSize, eraseSize);

            for (int i = 0; i < allLines.size(); i++) {
                ColoredLine line = allLines.get(i);
                List<Point2D> newLinePoints = new ArrayList<>();

                for (Point2D point : line.points) {
                    if (!eraser.contains(point)) {
                        newLinePoints.add(point);
                    }
                }

                if (!newLinePoints.isEmpty()) {
                    allLines.set(i, new ColoredLine(newLinePoints, line.color, line.thickness));
                } else {
                    allLines.remove(i);
                    i--;
                }
            }

            lastErasePoint = new Point(x, y);
            repaint();
        }

        private void drawGrid(Graphics2D g2) {
            int gridSize = 40;
            g2.setColor(new Color(205, 205, 205, 70));
            float gridLineThickness = 1.0f;
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