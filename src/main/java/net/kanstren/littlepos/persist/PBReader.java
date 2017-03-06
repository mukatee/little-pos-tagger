package net.kanstren.littlepos.persist;

import net.kanstren.littlepos.PerceptronTagger;
import net.kanstren.littlepos.generated.protobuf.Perceptron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;

/**
 * For reading the model from file.
 *
 * @author Teemu Kanstren.
 */
public class PBReader {
  private static final Logger log = LogManager.getLogger();

  public static PerceptronTagger createFrom(byte[] bytes) throws Exception {
    Perceptron.PerceptronModel model = Perceptron.PerceptronModel.parseFrom(bytes);
    return PerceptronTagger.buildFromPB(model);
  }

  public static PerceptronTagger readFrom(String filename) throws Exception {
    try (FileInputStream input = new FileInputStream(filename)) {
      Perceptron.PerceptronModel model = Perceptron.PerceptronModel.parseFrom(input);
      return PerceptronTagger.buildFromPB(model);
    }
  }
}
