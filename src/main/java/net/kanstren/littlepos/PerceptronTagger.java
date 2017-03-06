package net.kanstren.littlepos;

import net.kanstren.littlepos.datamodel.Features;
import net.kanstren.littlepos.datamodel.Statistics;
import net.kanstren.littlepos.datamodel.TagScore;
import net.kanstren.littlepos.datamodel.TagSentence;
import net.kanstren.littlepos.datamodel.WordTag;
import net.kanstren.littlepos.generated.protobuf.Perceptron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part of sentence tagger, based on
 * http://honnibal.wordpress.com/2013/09/11/a-good-part-of-speechpos-tagger-in-about-200-lines-of-python/
 *
 * @author Teemu Kanstren.
 */
public class PerceptronTagger {
  private static final Logger log = LogManager.getLogger();
  /** Synthetic start for sentence to allow algorithm to always look at all features even at start of sentence (previous words). */
  private final String[] START = new String[] {"-START1-", "-START2-"};
  /** Synthetic end for sentence to allow algorithm to always look at all features even at end of sentence (next words). */
  private final String[] END = new String[] {"-END1-", "-END2-"};
  /** Keeping statistics on how often words/tags appear in training set. */
  private final Statistics statistics = new Statistics();
  /** Maps words to tags when a word clearly has a specific tag. */
  private final Map<String, String> singleTags = new HashMap<>();
  /** The other part of this tagger.. ? :) */
  private AveragedPerceptron model;

  /**
   * Train the tagger based on the given sentences.
   * This requires having loaded all the sentences beforehand.
   * For larger datasets this would need to be modified to load sentences from an external DB or something similar.
   * To avoid running out of memory..
   *
   * @param sentences to train on.
   */
  public void train(List<TagSentence> sentences) {
    log.info("Updating stats for word frequencies.");
    statistics.updateWith(sentences);
    updateSingleMap();
    log.info("Finished stats update. Starting to train predictor model.");
    model = new AveragedPerceptron();
    //TODO: try with 100
    int ITERATIONS = 10;
    String prev1 = START[0];
    String prev2 = START[1];
    for (int iteration = 0 ; iteration < ITERATIONS ; iteration++) {
      log.info("training iteration "+iteration+"/"+ITERATIONS);
      int correct = 0;
      int guesses = 0; //or predictions..
      for (TagSentence sentence : sentences) {
        //this is the sentence we are training on but with the words only (and start+end adds)
        String[] context = createContext(sentence.wordArray());
        //start guessing at index 2 to skip the START START synthetic prefix
        int guessIndex = 2;
        for (WordTag wordTag : sentence.getWordTags()) {
          Features features = new Features(guessIndex, wordTag.word, context, prev1, prev2);
          TagScore guess = model.predict(features);
          guesses++;
          guessIndex++;
          if (guess.tag.equals(wordTag.tag)) {
            correct++;
            //we could also insert "continue" here as correct guesses do not update feature weights
          }
          model.update(wordTag.tag, guess.tag, features);
        }
        //shuffle the order to get another iteration..
      }
      //this is the part where the sentences are shuffled to avoid the ordering favouring specific faetures too much
      //->because the averaging counts total for the weights over time so early vs late appearing features get different weight
      Collections.shuffle(sentences);
      log.info("Iteration "+iteration+". results: correct="+correct+" guesses="+guesses);
    }
    log.info("Finished all iterations. Averaging models.");
    model.averageWeights();
  }

  /**
   * Update the list of unambiguous words, meaning words that are considered to only have a single tag associated with them.
   */
  private void updateSingleMap() {
    singleTags.clear();
    //minimum of 20 instances of word should be seen before classifying as ambiguous or not
    int freqThreshold = 20;
    //97% of seen instances should have the same tag to be always given that prediction
    float ambiquityThreshold = 0.97f;
    for (String word : statistics.words()) {
      Statistics.TagCounter tagCounter = statistics.tagCountsFor(word);
      Statistics.TagCount max = tagCounter.max();
      int sum = tagCounter.totalTagCount;
      float fmax = max.count;
      float maxPrct = fmax / sum;
      if (sum > freqThreshold && maxPrct >= ambiquityThreshold) {
        singleTags.put(word, max.tag);
      }
    }
  }

  /**
   * Build the POS tagging context as prefix+sentence words+suffix.
   * Prefix and suffix are simply the START+START and END+END words added to make algorithm handle start and end of sentence better.
   *
   * @param words Actual sentence words.
   * @return Given words with prefix and suffix added.
   */
  private String[] createContext(String[] words) {
    String[] context = new String[2 + words.length + 2];
    System.arraycopy(START, 0, context, 0, 2);
    System.arraycopy(words, 0, context, 2, words.length);
    System.arraycopy(END, 0, context, words.length + 2, 2);
    return context;
  }

  /**
   * Make a prediction for each word in the sentence and given them POS tags/labels.
   *
   * @param sentence To tag.
   * @return Tagged sentence.
   */
  public TagSentence tag(String sentence) {
    String[] words = sentence.split(" ");
    words = normalize(words);
    String[] context = createContext(words);
    String prev1 = START[0];
    String prev2 = START[1];
    TagSentence tokens = new TagSentence();
    int i = 2;
    for (String word : words) {
      String tag = singleTags.get(word);
      if (tag == null) {
        Features features = new Features(i, word, context, prev1, prev2);
        tag = model.predict(features).tag;
      }
      tokens.add(new WordTag(word, tag));
      prev2 = prev1;
      prev1 = tag;
      i++;
    }
    return tokens;
  }

  /**
   * Turn all words to lowercase, try to find years and numerical values and change them to !YEAR and !DIGIT.
   *
   * @param rawWords to normalize.
   * @return Normalized version of the words.
   */
  public String[] normalize(String[] rawWords) {
    String[] normalized = new String[rawWords.length];
    int i = 0;
    for (String rawWord : rawWords) {
      Integer digits = parseDigits(rawWord);
      if (digits != null) {
        if (digits >= 1800 && digits <= 2100) {
          normalized[i++] = "!YEAR";
        } else {
          normalized[i++] = "!DIGIT";
        }
        continue;
      }
      normalized[i++] = rawWord.toLowerCase();
    }
    return normalized;
  }

  /**
   * Parse given word to see if it is a 4-digit string between 1800-2100. If so, mark it as a year..
   *
   * @param word To parse.
   * @return Null if not an integer in scale 1800-2100, otherwise parse integer value.
   */
  public Integer parseDigits(String word) {
    if (word.length() != 4) {
      return null;
    }
    char[] chars = word.toCharArray();
    for (char c : chars) {
      if (c < '0' || c > '9') {
        return null;
      }
    }
    return Integer.parseInt(word);
  }

  /**
   * Build a protobuf binary to store the model.
   *
   * @param pb for adding the model data to.
   */
  public void buildPB(Perceptron.PerceptronModel.Builder pb) {
    for (String word : singleTags.keySet()) {
      Perceptron.WordTag value = Perceptron.WordTag
          .newBuilder()
          .setWord(word)
          .setTag(singleTags.get(word))
          .build();
      pb.addSingleTag(value);
    }
    statistics.buildPB(pb);
    model.buildPB(pb);
  }

  /**
   * Build the prediction model based on stored protobuf.
   *
   * @param model To load the model data from.
   * @return The model based on the loaded data.
   */
  public static PerceptronTagger buildFromPB(Perceptron.PerceptronModel model) {
    PerceptronTagger tagger = new PerceptronTagger();
    List<Perceptron.WordTag> modelSingleTagList = model.getSingleTagList();
    for (Perceptron.WordTag singleTag : modelSingleTagList) {
      tagger.singleTags.put(singleTag.getWord(), singleTag.getTag());
    }
    tagger.statistics.initFromPB(model);
    tagger.model = new AveragedPerceptron();
    tagger.model.initFromPB(model);
    return tagger;
  }
}
