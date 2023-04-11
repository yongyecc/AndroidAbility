package cn.yongye.androidability.common;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

public class SimilarPicture {


    /**
     * 将彩色图转换为灰度图
     *
     * @param img 位图
     * @return 返回转换好的位图
     */
    public static Bitmap convertGreyImg(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int original = pixels[width * i + j];
                int red = ((original & 0x00FF0000) >> 16);
                int green = ((original & 0x0000FF00) >> 8);
                int blue = (original & 0x000000FF);

                int grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;

            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * 计算所有64个像素的灰度平均值。
     * @param img
     * @return
     */
    public static int getAvg(Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];
        img.getPixels(pixels, 0, width, 0, 0, width, height);

        int avgPixel = 0;
        for (int pixel : pixels) {
            avgPixel += pixel;
        }
        return avgPixel / pixels.length;
    }

    /**
     * 将每个像素的灰度，与平均值进行比较。大于或等于平均值，记为1；小于平均值，记为0。
     * @param img
     * @param average
     * @return
     */
    public static String getBinary(Bitmap img, int average) {
        StringBuilder sb = new StringBuilder();

        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int original = pixels[width * i + j];
                if (original >= average) {
                    pixels[width * i + j] = 1;
                } else {
                    pixels[width * i + j] = 0;
                }
                sb.append(pixels[width * i + j]);
            }
        }
        return sb.toString();
    }

    /**
     * 将上一步的比较结果，组合在一起，就构成了一个64位的整数，这就是这张图片的指纹。
     * @param bString
     * @return
     */
    public static String binaryString2hexString(String bString) {
        if (bString == null || bString.equals("") || bString.length() % 8 != 0)
            return null;
        StringBuilder sb = new StringBuilder();
        int iTmp;
        for (int i = 0; i < bString.length(); i += 4) {
            iTmp = 0;
            for (int j = 0; j < 4; j++) {
                iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) << (4 - j - 1);
            }
            sb.append(Integer.toHexString(iTmp));
        }
        return sb.toString();
    }

    /**
     * 两张图片的消息指纹比较，0-5极其相似，6-10较为相似，10以上不相似.
     * @param s1
     * @param s2
     * @return
     */
    public static int diff(String s1, String s2) {
        char[] s1s = s1.toCharArray();
        char[] s2s = s2.toCharArray();
        int diffNum = 0;
        for (int i = 0; i<s1s.length; i++) {
            if (s1s[i] != s2s[i]) {
                diffNum++;
            }
        }
        return diffNum;
    }

    /**
     * 两张图片的消息指纹比较，0-5极其相似，6-10较为相似，10以上不相似.
     * @param srcBitmap
     * @param dstBitmap
     * @return
     */
    public static int diff(Bitmap srcBitmap, Bitmap dstBitmap) {
        Bitmap srcThumb = ThumbnailUtils.extractThumbnail(srcBitmap, 8, 8);
        Bitmap dstThumb = ThumbnailUtils.extractThumbnail(dstBitmap, 8, 8);
        int srcConver = SimilarPicture.getAvg(srcThumb);
        int dstConver = SimilarPicture.getAvg(dstThumb);
        String srcBin = SimilarPicture.getBinary(srcThumb, srcConver);
        String dstBin = SimilarPicture.getBinary(dstThumb, dstConver);
        String srcHash = SimilarPicture.binaryString2hexString(srcBin);
        String dstHash = SimilarPicture.binaryString2hexString(dstBin);
        char[] s1s = srcHash.toCharArray();
        char[] s2s = dstHash.toCharArray();
        int diffNum = 0;
        for (int i = 0; i<s1s.length; i++) {
            if (s1s[i] != s2s[i]) {
                diffNum++;
            }
        }
        return diffNum;
    }
}
