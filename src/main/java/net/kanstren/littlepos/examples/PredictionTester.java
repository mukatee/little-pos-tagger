package net.kanstren.littlepos.examples;

import net.kanstren.littlepos.PerceptronTagger;
import net.kanstren.littlepos.datamodel.TagSentence;
import net.kanstren.littlepos.datamodel.WordTag;
import net.kanstren.littlepos.persist.PBReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Tester for the POS tagger.
 * Takes a given input file produced by the Python FinnTreeBank transformer script, tries to tag all sentences in the file and checks how many it got right.
 * ALso takes the tagger model file name as parameter to use for tagging in the tests..
 *
 * @author Teemu Kanstren
 */
public class PredictionTester {
  private static final Logger log = LogManager.getLogger();
  private static int loadedWords = 0;
  private static int loadedSentences = 0;
  private static int correct = 0;
  private static int guesses = 0;
  //some unknown words were seen when the program bugged so just to be sure we track them. unknown = no features match
  private static int unknowns = 0;
  private static Map<String, Integer> unknownTagFreqs = new HashMap<>();
  private static long totalTagTime = 0;
  private static TagSentence reference = null;
  private static final StringBuilder input = new StringBuilder();
  private static PerceptronTagger tagger = null;

  public static void main(String[] args) throws Exception {
    String protoFilename = args[0];
    log.info("Starting prediction tester");
    log.info("Loading protofile '"+protoFilename+"'.");
    tagger = PBReader.readFrom(protoFilename);
    log.info("Model loaded.");
    String sentenceFilename = args[1];
    log.info("Starting to test sentences from "+sentenceFilename+".");

    try (Stream<String> stream = Files.lines(Paths.get(sentenceFilename))) {
      stream.forEach(PredictionTester::process);
    }
    double avgTagTime = (double)totalTagTime / (double)guesses;
    double avgSentenceLength = (double) loadedWords / (double) loadedSentences;
    log.info("Finished testing. Results:");
    log.debug("processed sentences:"+ loadedSentences);
    log.debug("correct tags "+correct+"/"+guesses);
    log.debug("unknowns tags "+unknowns);
    log.debug("unknowns tags freqs:"+unknownTagFreqs);
    log.debug("loaded words "+loadedWords);
    log.debug("avg sentence tag time "+avgTagTime);
    log.debug("avg sentence length "+avgSentenceLength);
  }

  private static void process(String line) {
    String[] split = line.split(" ");
    if (split.length < 2 || reference == null) {
      //skip if many empty lines in row, else we mess up the stats calculation for how many sentences were processed
      if (reference != null && reference.getWordTags().size() == 0) return;
      String str = input.toString().trim();
      if (str.length() > 0) {
        long start = System.currentTimeMillis();
        TagSentence predicted = tagger.tag(str);
        long end = System.currentTimeMillis();
        long diff = end - start;
        totalTagTime += diff;
        int wordCount = reference.getWordTags().size();
        for (int i = 0 ; i < wordCount ; i++) {
          String predictedTag = predicted.getWordTags().get(i).tag;
          String referenceTag = reference.getWordTags().get(i).tag;
          if (predictedTag.equals(referenceTag)) {
            correct++;
          } else {
            if (predictedTag.equals("UNKNOWN")) {
              unknowns++;
              int freq = unknownTagFreqs.getOrDefault(referenceTag, 0);
              unknownTagFreqs.put(referenceTag, freq+1);
            }
          }
          guesses++;
        }
      }
      reference = new TagSentence();
      input.setLength(0);
      loadedSentences++;
      if (loadedSentences % 50000 == 0) {
        log.debug("processed sentences:"+ loadedSentences+" correct tags "+correct+"/"+guesses+", loaded "+loadedWords+", unknowns:"+unknowns);
      }
      return;
    }
    reference.add(new WordTag(split[0], split[1]));
    input.append(" ");
    input.append(split[0]);
    loadedWords++;
    if (loadedWords % 100000 == 0) {
//      log.debug("processed words:"+loadedWords);
    }
  }
}
