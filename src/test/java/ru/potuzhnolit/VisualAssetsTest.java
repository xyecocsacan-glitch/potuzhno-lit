package ru.potuzhnolit;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualAssetsTest {
    private BufferedImage image(String path) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/potuzhnolit/textures/" + path + ".png")) {
            assertNotNull("Missing texture " + path, stream);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull("Invalid PNG " + path, image);
            return image;
        }
    }
    private void size(BufferedImage image, int size) {
        assertEquals(size, image.getWidth()); assertEquals(size, image.getHeight());
    }
    @Test public void entityTexturesAreValidAtlases() throws Exception {
        size(image("entity/drone"), 128); size(image("entity/potuzhno_lit"), 128);
    }
    @Test public void itemTexturesAre64PixelsWithTransparentCorners() throws Exception {
        for (String name : new String[]{"drone", "potuzhno_lit"}) {
            BufferedImage icon = image("item/" + name); size(icon, 64);
            assertTrue(icon.getColorModel().hasAlpha());
            assertEquals(0, icon.getRGB(0, 0) >>> 24);
            assertEquals(0, icon.getRGB(63, 63) >>> 24);
        }
    }
    @Test public void iconsAreDifferentAndNotEmpty() throws Exception {
        BufferedImage a = image("item/drone"), b = image("item/potuzhno_lit");
        int[] left = a.getRGB(0, 0, 64, 64, null, 0, 64), right = b.getRGB(0, 0, 64, 64, null, 0, 64);
        assertFalse(Arrays.equals(left, right));
        assertTrue(Arrays.stream(left).filter(pixel -> (pixel >>> 24) > 0).count() > 300);
        assertTrue(Arrays.stream(right).filter(pixel -> (pixel >>> 24) > 0).count() > 300);
    }
    @Test public void atlasesHaveBlueAndYellowMaterials() throws Exception {
        for (String name : new String[]{"drone", "potuzhno_lit"}) {
            BufferedImage a = image("entity/" + name);
            int blue = a.getRGB(70, 10), yellow = a.getRGB(10, 100);
            assertTrue((blue & 255) > ((blue >>> 16) & 255));
            assertTrue(((yellow >>> 16) & 255) > (yellow & 255));
        }
    }
}
