import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Prints screenshots as small base64 JPEGs between markers, so they can be reviewed from a CI
 * job log when artifacts cannot be downloaded. Needs only the JDK:
 *
 *   java -Djava.awt.headless=true tools/ci/PrintScreenshots.java 300 app/build/outputs/roborazzi/*.png
 */
public class PrintScreenshots {
    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            File file = new File(args[i]);
            BufferedImage source = ImageIO.read(file);
            int height = Math.round(source.getHeight() * (width / (float) source.getWidth()));
            BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            small.createGraphics().drawImage(source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.6f);
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            try (ImageOutputStream out = ImageIO.createImageOutputStream(jpeg)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(small, null, null), param);
            }
            writer.dispose();

            System.out.println("=== SCREENSHOT " + file.getName() + " ===");
            System.out.println(Base64.getEncoder().encodeToString(jpeg.toByteArray()));
            System.out.println("=== END ===");
        }
    }
}
