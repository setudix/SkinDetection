import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class Trainer {
    private static final int SIZE = 256;
    private long[][][] skinValues, nonSkinValues;
    public void run() throws IOException {
        skinValues = new long[SIZE][SIZE][SIZE];
        nonSkinValues = new long[SIZE][SIZE][SIZE];
        File maskFolder = new File("./ibtd/Mask/");
        File nonmaskFolder = new File("./ibtd/Nonmask/");
        long readingTimeStart = System.currentTimeMillis();
        for (String maskName: maskFolder.list()){
            String nonmaskName = maskName.replaceAll("bmp", "jpg");
            String maskImagePath = maskFolder.getPath().concat("/").concat(maskName);
            String nonmaskImagePath = nonmaskFolder.getPath().concat("/").concat(nonmaskName);
            File maskImageFile = new File(maskImagePath);
            File nonmaskImageFile = new File(nonmaskImagePath);
            BufferedImage maskImage = ImageIO.read(maskImageFile);
            BufferedImage nonmaskImage = ImageIO.read(nonmaskImageFile);
            for (int h=0;h<maskImage.getHeight();h++){
                for (int w=0;w<maskImage.getWidth();w++) {
                    int maskPixel = maskImage.getRGB(w,h);
                    Color maskColor = new Color(maskPixel, true);
                    int r = maskColor.getRed();
                    int g = maskColor.getGreen();
                    int b = maskColor.getBlue();
                    if (r > 220 && g > 220 && b > 220){
                        int nonmaskPixel = nonmaskImage.getRGB(w,h);
                        Color nonmaskColor = new Color(nonmaskPixel, true);
                        int r_nm = nonmaskColor.getRed();
                        int g_nm = nonmaskColor.getGreen();
                        int b_nm = nonmaskColor.getBlue();
                        nonSkinValues[r_nm][g_nm][b_nm]++;
                    } else {
                        skinValues[r][g][b]++;
                    }
                }
            }
        }
        long readingTimeEnd = System.currentTimeMillis();
        int total_skin = 0, total_non_skin = 0;
        int c = 0;
        for (int r=0;r<SIZE;r++){
            for (int g=0;g<SIZE;g++){
                for (int b=0;b<SIZE;b++){
                    total_skin += skinValues[r][g][b];
                    total_non_skin += nonSkinValues[r][g][b];
                    if (skinValues[r][g][b] == 0 || nonSkinValues[r][g][b] == 0) c++;
                }
            }
        }
        System.out.println("unique colors: " + (256*256*256 - c));
        BigDecimal total_skin_big = BigDecimal.valueOf(total_skin);
        BigDecimal total_non_skin_big = BigDecimal.valueOf(total_non_skin);
        ArrayList<String> prob = new ArrayList<>();
        BufferedWriter writer = new BufferedWriter(new FileWriter("rgb_prob.txt"));
        int __zero = 0;
        for (int r=0;r<SIZE;r++){
            for (int g=0;g<SIZE;g++){
                for (int b=0;b<SIZE;b++){
                    BigDecimal skin, non_skin, skin_p, non_skin_p, probability;
                    if (skinValues[r][g][b] != 0) {
                        skin = BigDecimal.valueOf(skinValues[r][g][b]);
                        skin_p = skin.divide(total_skin_big, 20, RoundingMode.CEILING);
                    } else {
                        prob.add("0");
                        __zero++;
                        continue;
                    }
                    if (nonSkinValues[r][g][b] != 0){
                        non_skin = BigDecimal.valueOf(nonSkinValues[r][g][b]);
                        non_skin_p = non_skin.divide(total_non_skin_big, 20, RoundingMode.CEILING);
                    } else {
                        if (skinValues[r][g][b] == 0){
                            prob.add("0");
                        } else{
                           prob.add("1");
                        }
//                        __zero++;
                        continue;
                    }
                    probability = skin_p.divide(non_skin_p,20,RoundingMode.CEILING);
                    prob.add(probability.toString());
                    System.out.println(probability.toString());
                }
            }
        }
        int __zero_s = 0;
        for (String p : prob){
            writer.write(p + "\n");
            if (p.equals("0"))
                __zero_s++;
        }
        writer.close();
        System.out.println("Read all images in " + (double)(readingTimeEnd - readingTimeStart)/1000 + "s");
        System.out.println("Output saved in file");
//        System.out.println("zeros: " + __zero + "\nzeroes in string: " + __zero_s);
//        System.out.println("size: " + prob.size());
    }
}
