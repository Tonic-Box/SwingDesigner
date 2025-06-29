package designer.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ResourceUtil
{
    public static BufferedImage loadImageResource(final Class<?> c, final String path)
    {
        try (InputStream in = c.getResourceAsStream(path))
        {
            assert in != null;
            synchronized (ImageIO.class)
            {
                return ImageIO.read(in);
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
