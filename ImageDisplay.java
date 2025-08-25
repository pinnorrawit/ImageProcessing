import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageDisplay {
    private static final int SCREEN_WIDTH = 2880;
    private static final int SCREEN_HEIGHT = 1800;
    private static final int MAX_ROWS = 3;
    private static final int MAX_COLS = 4;
    private static final int GAP = 0; // Padding between images

    /**
     * Displays multiple images in a grid layout (2 rows × 3 columns max)
     * @param images List of images to display
     * @param title Window title
     */
    public static void showImagesGrid(List<BufferedImage> images, String title) {
        if (images == null || images.isEmpty()) return;

        int numImages = Math.min(images.size(), MAX_ROWS * MAX_COLS);
        int rows = Math.min(MAX_ROWS, (int) Math.ceil((double) numImages / MAX_COLS));
        int cols = Math.min(MAX_COLS, numImages);

        // Calculate maximum dimensions per image
        int imgWidth = (SCREEN_WIDTH - (cols + 1) * GAP) / cols;
        int imgHeight = (SCREEN_HEIGHT - (rows + 1) * GAP) / rows;

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridLayout(rows, cols, GAP, GAP));

        for (int i = 0; i < numImages; i++) {
            BufferedImage img = images.get(i);
            BufferedImage scaledImg = scaleImageToFit(img, imgWidth, imgHeight);
            JLabel label = new JLabel(new ImageIcon(scaledImg));
            frame.add(label);
        }

        frame.pack();
        frame.setLocationRelativeTo(null); // Center window
        frame.setVisible(true);
    }

    /**
     * Scales an image to fit within specified dimensions while maintaining aspect ratio
     */
    private static BufferedImage scaleImageToFit(BufferedImage original, int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / original.getWidth();
        double heightRatio = (double) maxHeight / original.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (original.getWidth() * ratio);
        int newHeight = (int) (original.getHeight() * ratio);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, original.getType());
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaled;
    }

    /**
     * Displays a single image
     */
    public static void showImage(BufferedImage image, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Scale large images to fit screen
        int maxWidth = SCREEN_WIDTH * 3/4;
        int maxHeight = SCREEN_HEIGHT * 3/4;
        BufferedImage scaledImg = scaleImageToFit(image, maxWidth, maxHeight);
        
        frame.getContentPane().add(new JLabel(new ImageIcon(scaledImg)));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    public static void showImageTrueSize(BufferedImage image, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JLabel label = new JLabel(new ImageIcon(image));
        
        // Create a scroll pane to handle large images
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Set the preferred size to the image dimensions (but limit to screen size if needed)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.9);
        int maxHeight = (int) (screenSize.height * 0.9);
        
        int prefWidth = Math.min(image.getWidth(), maxWidth);
        int prefHeight = Math.min(image.getHeight(), maxHeight);
        
        scrollPane.setPreferredSize(new Dimension(prefWidth, prefHeight));
        
        frame.add(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        System.out.println("Image displayed at dimensions: " + image.getWidth() + "x" + image.getHeight());
    }
}