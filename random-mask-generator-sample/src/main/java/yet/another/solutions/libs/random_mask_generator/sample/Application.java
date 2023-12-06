package yet.another.solutions.libs.random_mask_generator.sample;

import yet.another.solutions.libs.random_mask_generator.Generator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Application {

    public static void main(String[] args) throws IOException {
        Random random = new Random();
        int size = 1000;
        int space = Double.valueOf(random.nextDouble(0.05,0.2)*size).intValue();
        int edges = random.nextInt(5,11);
        BufferedImage image = Generator.createRandom(size, size, space,edges);
        ImageIO.write(image, "png", new File("./image.png"));
    }

}
