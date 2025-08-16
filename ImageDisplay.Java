import javax.swing.*;
import java.awt.image.BufferedImage;

public class ImageDisplay {
    public static void showImage(BufferedImage image, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setLocationRelativeTo(null); // Center window
        frame.setVisible(true);
    }
    
    public static void closeAll() {
        // No longer needed with EXIT_ON_CLOSE
    }
}