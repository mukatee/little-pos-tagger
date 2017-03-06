package net.kanstren.littlepos.persist;

import net.kanstren.littlepos.PerceptronTagger;
import net.kanstren.littlepos.generated.protobuf.Perceptron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;

/**
 * For writing the data model to protobuf format in file.
 *
 * @author Teemu Kanstren.
 */
public class PBWriter {
  private static final Logger log = LogManager.getLogger();
  private final PerceptronTagger tagger;

  public PBWriter(PerceptronTagger tagger) {
    this.tagger = tagger;
  }

  public byte[] toBytes() {
    Perceptron.PerceptronModel perceptronModel = buildPB();
    return perceptronModel.toByteArray();
  }

  public void writeToFile(String filename) {
    Perceptron.PerceptronModel perceptronModel = buildPB();
    try (FileOutputStream output = new FileOutputStream(filename)) {
      perceptronModel.writeTo(output);
    } catch (Exception e) {
      log.error(e);
    }
  }

  private Perceptron.PerceptronModel buildPB() {
    Perceptron.PerceptronModel.Builder builder = Perceptron.PerceptronModel.newBuilder();
    tagger.buildPB(builder);
    return builder.build();
  }
}
