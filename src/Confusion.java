import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Confusion {

    private static final int SIZE = 256;
    private long[][][] skinValues, nonSkinValues;
    private boolean [][][][] isColorSkin;
    long totalSkinCount, totalNonSkinCount;
    BigDecimal totalSkinCountBig, totalNonSkinCountBig, thresholdBig;
    ArrayList<String> maskImageNames;
    ArrayList<BigDecimal> skinNonSkinRatio;
    long TN, TP;
    Confusion(){
        skinValues = new long[SIZE][SIZE][SIZE];
        nonSkinValues = new long[SIZE][SIZE][SIZE];
        isColorSkin = new boolean[60][SIZE][SIZE][SIZE];
        skinNonSkinRatio = new ArrayList<>();
        thresholdBig = new BigDecimal("0.4");
        if (maskImageNames == null){
            maskImageNames = new ArrayList<>(List.of(new File("./ibtd/Mask/").list()));
            System.out.println("size " + maskImageNames.size());
        }
        resetValues();

    }

    public void train() throws IOException {
        for (int i=0;i<500;i++){
            String maskName = maskImageNames.get(i);
            String nonMaskName = maskName.replaceAll("bmp", "jpg");

            String maskImagePath = "./ibtd/Mask/".concat(maskName);
            String nonmaskImagePath = "./ibtd/Nonmask/".concat(nonMaskName);

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

                    if (r > 240 && g > 240 && b > 240){

                        int nonmaskPixel = nonmaskImage.getRGB(w,h);

                        Color nonmaskColor = new Color(nonmaskPixel, true);

                        int r_nm = nonmaskColor.getRed();
                        int g_nm = nonmaskColor.getGreen();
                        int b_nm = nonmaskColor.getBlue();

                        nonSkinValues[r_nm][g_nm][b_nm]++;
                        totalNonSkinCount++;
                    } else {
                        skinValues[r][g][b]++;
                        totalSkinCount++;
                    }
                }
            }
        }
        totalSkinCountBig = BigDecimal.valueOf(totalSkinCount);
        totalNonSkinCountBig = BigDecimal.valueOf(totalNonSkinCount);

        for (int r=0;r<SIZE;r++){
            for (int g=0;g<SIZE;g++){
                for (int b=0;b<SIZE;b++){
                    BigDecimal skin, non_skin, skin_p, non_skin_p, probability;
                    if (skinValues[r][g][b] != 0) {
                        skin = BigDecimal.valueOf(skinValues[r][g][b]);
                        skin_p = skin.divide(totalSkinCountBig, 20, RoundingMode.CEILING);
                    } else {
                        if (nonSkinValues[r][g][b] == 0){
                            skinNonSkinRatio.add(BigDecimal.valueOf(0));
                        } else {
                            skinNonSkinRatio.add(BigDecimal.valueOf(1));
                        }
                        continue;
                    }
                    if (nonSkinValues[r][g][b] != 0){
                        non_skin = BigDecimal.valueOf(nonSkinValues[r][g][b]);
                        non_skin_p = non_skin.divide(totalNonSkinCountBig, 20, RoundingMode.CEILING);
                    } else {
                        if (skinValues[r][g][b] == 0){
                            skinNonSkinRatio.add(BigDecimal.valueOf(0));
                        } else {
                            skinNonSkinRatio.add(BigDecimal.valueOf(1));
                        }
                        continue;
                    }
                    probability = skin_p.divide(non_skin_p,20,RoundingMode.CEILING);
                    skinNonSkinRatio.add(probability);
//                    System.out.println(probability.toString());
                }
            }
        }
        for (int i=500;i<maskImageNames.size();i++){
            String nonmaskName = maskImageNames.get(i).replaceAll("bmp","jpg");
            BufferedImage image = ImageIO.read(new File("./ibtd/Nonmask/".concat(nonmaskName)));
            for (int h=0;h<image.getHeight();h++){
                for (int w=0;w<image.getWidth();w++) {

                    int pixel = image.getRGB(w,h);
                    Color color = new Color(pixel, true);

                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();

                    if (r > 240 && g > 240 && b > 240){
                        isColorSkin[i%500][r][g][b] = false;
                    } else {
                        isColorSkin[i%500][r][g][b] = true;
                    }
                }
            }
        }
    }

    private void test(int run) throws IOException {
        int testTotal = 0;
        for (int i=500;i<maskImageNames.size();i++){

            String nonmaskName = maskImageNames.get(i).replaceAll("bmp","jpg");
            BufferedImage image = ImageIO.read(new File("./ibtd/Nonmask/".concat(nonmaskName)));
            for (int h=0;h<image.getHeight();h++){
                for (int w=0;w<image.getWidth();w++) {
                    testTotal++;
                    int pixel = image.getRGB(w,h);
                    Color color = new Color(pixel, true);

                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();

                    int index = SIZE*(SIZE*r + g) + b;

                    if (skinNonSkinRatio.get(index).compareTo(thresholdBig) >= 0) {
                        if (isColorSkin[i%500][r][g][b] == true) {
                            TP++;
                        }
                    } else {
                        if (isColorSkin[i%500][r][g][b] == false){
                            TN++;
                        }
                    }
                }
            }
        }

        BigDecimal totalBig = BigDecimal.valueOf(testTotal);
        BigDecimal totalCorrectBig = BigDecimal.valueOf(TN + TP);
        BigDecimal accuracy = totalCorrectBig.divide(totalBig, 20, RoundingMode.CEILING);

        System.out.println(run + ". Accuracy = " + accuracy);
    }
    private void resetValues(){
        for (int r=0;r<SIZE;r++){
            for (int g=0;g<SIZE;g++){
                for (int b=0;b<SIZE;b++){
                    skinValues[r][g][b] = 0;
                    nonSkinValues[r][g][b] = 0;
                }
            }
        }
//        skinValues = new long[SIZE][SIZE][SIZE];
//        nonSkinValues = new long[SIZE][SIZE][SIZE];
        skinNonSkinRatio.clear();
        totalNonSkinCount = 0;
        totalSkinCount = 0;
        TP = 0;
        TN = 0;
        Collections.shuffle(maskImageNames);
        System.out.println("shuffled");
    }
    public void clear(){
        resetValues();
    }
    public static void main(String[] args) throws IOException {
        Confusion confusion = new Confusion();

        long readingTimeStart = System.currentTimeMillis();
        long readingTimeEnd;

        for (int i=0;i<10;i++){
            confusion.train();
            confusion.test(i+1);
            confusion.clear();
            readingTimeEnd = System.currentTimeMillis();
            System.out.println("Iteration #" + (i+1) + " in "
                            + ((readingTimeEnd - readingTimeStart) / 1000)
                            + " seconds from start"
                            );
        }
    }
}
