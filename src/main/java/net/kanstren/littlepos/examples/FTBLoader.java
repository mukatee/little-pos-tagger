package net.kanstren.littlepos.examples;

import net.kanstren.littlepos.PerceptronTagger;
import net.kanstren.littlepos.datamodel.TagSentence;
import net.kanstren.littlepos.datamodel.WordTag;
import net.kanstren.littlepos.persist.PBWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader for transformed FinnTreeBank data.
 * Loads the data from a file and trains the tagger with it.
 *
 * @author Teemu Kanstren.
 */
public class FTBLoader {
  private static final Logger log = LogManager.getLogger();
  private static List<TagSentence> sentences = new ArrayList<>();
  private static TagSentence sentence = null;
  private static int loadedWords = 0;
  private static int loadedSentences = 0;

  public static void main(String[] args) throws Exception {
    PerceptronTagger tagger = new PerceptronTagger();
    String size = args[0];
    log.info("Starting Finnish Treebank Loader with "+size+" size.");
    try (BufferedReader br = new BufferedReader(new FileReader("transformed_train_"+size+".conllx"))) {
      String line;
      while ((line = br.readLine()) != null) {
        process(line);
      }
    }
//    try (Stream<String> stream = Files.lines(Paths.get("transformed_1M.conllx"))) {
//      stream.forEach(FTBLoader::process);
//    }
    log.info("Finished loading. Starting to train.");
    tagger.train(sentences);
    String filename = "tagger_model_"+size+".pb";
    log.info("Finished training. Saving model to file "+filename+".");
    PBWriter writer = new PBWriter(tagger);
    writer.writeToFile(filename);
    log.info("Finished writing model to file. Trying test sentence.");
    TagSentence tagSentence = tagger.tag("Heimo on heimonsa päällikkö.");
    log.info("test sentence result:"+tagSentence);
  }

  private static void process(String line) {
    String[] split = line.split(" ");
    if (sentence == null || split.length < 2) {
      if (sentence != null) {
        sentences.add(sentence);
      }
      sentence = new TagSentence();
      loadedSentences++;
      if (loadedSentences % 50000 == 0) {
        log.debug("processed sentences:"+ loadedSentences);
      }
      return;
    }
    sentence.add(new WordTag(split[0], split[1]));
    loadedWords++;
    if (loadedWords % 100000 == 0) {
//      log.debug("processed words:"+loadedWords);
    }
  }
}
