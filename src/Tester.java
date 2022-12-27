import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;

public class Tester {
    private final int SIZE = 256;
    BigDecimal[] RGB_prob;
    public void run() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("rgb_prob.txt"))) {
            RGB_prob = new BigDecimal[SIZE*SIZE*SIZE];
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null){
                RGB_prob[i] = new BigDecimal(line);
                i++;
            }
            System.out.println("Values read " + i);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File imageFile = new File("image2.jpg");
        BufferedImage image = ImageIO.read(imageFile);
        BufferedImage new_image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Color whiteColor = new Color(255,255,255,255);
        BigDecimal threshold = new BigDecimal("0.4");
        int alpha = 256;
        for (int h=0;h<image.getHeight();h++){
            for (int w=0;w<image.getWidth();w++){
                int pixel = image.getRGB(w,h);
                Color color = new Color(pixel, true);
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                int index = SIZE*(SIZE*r + g) + b;
                if (RGB_prob[index].compareTo(threshold) >= 0){
                    new_image.setRGB(w,h,color.getRGB());
                } else {
                    new_image.setRGB(w,h,whiteColor.getRGB());
                }
            }
        }
        File final_image = new File("final");
        try {
            ImageIO.write(new_image, "png", final_image);
            System.out.println("image created");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Tester tester = new Tester();
        tester.run();
    }
}
