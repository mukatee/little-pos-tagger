package net.kanstren.littlepos;


import net.kanstren.littlepos.datamodel.Features;
import net.kanstren.littlepos.datamodel.TagScore;
import net.kanstren.littlepos.generated.protobuf.Perceptron;

import java.util.ArrayList;
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
public class AveragedPerceptron {
  /** how many times we have processed a tag/feature pair, or how many instances have been processed */
  private int updateCount = 0;
  /** the latest iteration where a tag+feature pair weight was updated. key = pair, value = iteration number. */
  private Map<String, Integer> timeStamps = new HashMap<>();
  /** key=feature id, value={key=tag, value=weight} */
  private Map<String, Map<String, Double>> weights = new HashMap<>();
  /** total weights for a tag for a given feature that it has had over time. used to calculate average weight at the end.
  key = pair of tag+feature, value = total weight over iterations for this feature´+tag pair */
  private Map<String, Double> totals = new HashMap<>();

  public AveragedPerceptron() {
  }

  /**
   * Predict a tag from the given set of features.
   *
   * @param features To predict from.
   * @return Highest scoring tag for the features. If there is a tie, its an alphabetic sort by tag name.
   */
  public TagScore predict(Features features) {
    Map<String, Double> scores = new HashMap<>();
    for (String fname : features.keys()) {
      Map<String, Double> featureWeights = weights.get(fname);
      if (featureWeights == null) {
        continue; //never before seen features (not in training set) might be here?
      }
      //sum up all weights for all features for a tag. use those sums to pick highest scoring tag
      for (String tag : featureWeights.keySet()) {
        double score = scores.getOrDefault(tag, 0.0);
        score += features.valueFor(fname) * featureWeights.get(tag);
        scores.put(tag, score);
      }
    }
    return getFirstMaxScore(scores);
  }

  /**
   * Find the largest score in the given set (map) of tag scores, and return alphabetically (by tag name) first one of those.
   *
   * @param scores Key = tag name, value = tag score
   * @return tag with highest score, if several have same then alphabetically sorted by tag name the first of those.
   */
  public TagScore getFirstMaxScore(Map<String, Double> scores) {
    //e.g., a set of features never before seen? umm.. happened when the protobuf code was bugged and scores did not get saved. anyway left it here just in case
    if (scores.size() == 0) {
      return new TagScore("UNKNOWN", 0);
    }

    //this check seems unnecessary as updatecount can only be zero if nothing was trained and prediction is called. which makes no sense. any, lets keep it?? :)
    if (updateCount == 0) return new TagScore("", 0);

    Map.Entry<String, Double> max = Collections.max(scores.entrySet(), Map.Entry.comparingByValue());

    List<String> maxTags = new ArrayList<>();
    for (Map.Entry<String, Double> entry : scores.entrySet()) {
      if (entry.getValue().doubleValue() == max.getValue().doubleValue()) {
        maxTags.add(entry.getKey());
      }
    }
    //finally sort all results and if there were several tags with even score, pick aplhabetically sorted first one for deterministic result
    Collections.sort(maxTags);
    String tag = maxTags.get(0);
    //since all tags had same score, we can just pick any and use their score
    return new TagScore(tag, max.getValue());
  }

  /**
   * Update feature weights for given tags based on how the guessing/prediction of a tag for a word went.
   *
   * @param trueTag The tag that would have been correct to predict.
   * @param guessTag The tag that was predicted.
   * @param features Features used in prediction.
   */
  public void update(String trueTag, String guessTag, Features features) {
    updateCount++;
    if (trueTag.equals(guessTag)) return;
    for (String fname : features.keys()) {
      Map<String, Double> featureWeights = weights.computeIfAbsent(fname, fn -> new HashMap<>());
      //increase score for true tag for the features
      updateFeatureWeight(featureWeights, trueTag, fname, 1.0);
      //decrease score for guessed tag. note if guess is same as true, the weights stay the same. if wrong, this decreases and true increases.
      updateFeatureWeight(featureWeights, guessTag, fname, -1.0);
    }
  }

  /**
   * Update weight for a feature for a given tag.
   *
   * @param featureWeights Weights for the feature being updated. Key = tag name, value = weight for this feature for the key tag.
   * @param tag The name of tag to update the feature weight for.
   * @param fname Name of feature to update.
   * @param value To add to the current weights. Typically negative for wrong label, positive for correct label.
   */
  public void updateFeatureWeight(Map<String, Double> featureWeights, String tag, String fname, double value) {
    double w = featureWeights.getOrDefault(tag, 0.0);
    String pair = tag + "::" + fname;
    //update the update time for feature-tag pair to keep track of how long it has been at given value (for average calculation later)
    int previousSeenUpdate = timeStamps.getOrDefault(pair, 0);
    timeStamps.put(pair, updateCount);
    int updatesAtThisValue = updateCount - previousSeenUpdate;
    double total = totals.getOrDefault(pair, 0.0);
    total += updatesAtThisValue * w;
    //totals has the overall number for different tag-feature pairs to use for average calculation later
    totals.put(pair, total);
    featureWeights.put(tag, w + value);
  }

  /**
   * Average the current weights over all the observed values in the runs.
   * The model training is run for several iterations to get this into a better average and lessen the
   * impact of the scores recorded at differen times.
   */
  public void averageWeights() {
    Map<String, Map<String, Double>> newWeights = new HashMap<>();
    for (String fname : weights.keySet()) {
      Map<String, Double> featureWeights = weights.get(fname);
      Map<String, Double> newFeatureWeights = new HashMap<>();
      for (String tag : featureWeights.keySet()) {
        String pair = tag + "::" + fname;
        Double weight = featureWeights.get(tag);
        //multiply current value by the times it has not been updated to get total for averaging
        int previousSeenUpdate = timeStamps.getOrDefault(pair, 0);
        int updatesAtThisValue = updateCount - previousSeenUpdate;
        double total = totals.getOrDefault(pair, 0.0);
        total += updatesAtThisValue * weight;
        totals.put(pair, total);
        double averaged = total / (double) (updateCount);
        newFeatureWeights.put(tag, averaged);
      }
      newWeights.put(fname, newFeatureWeights);
    }
    weights = newWeights;
  }

  /**
   * Build a protobuf binary with the model data.
   *
   * @param pb to add the data to.
   */
  public void buildPB(Perceptron.PerceptronModel.Builder pb) {
    pb.setUpdateCount(updateCount);
    for (String pair : timeStamps.keySet()) {
      Perceptron.TimeStamp stamp = Perceptron.TimeStamp.newBuilder()
          .setPair(pair)
          .setIteration(timeStamps.get(pair))
          .build();
      pb.addTimestamp(stamp);
    }
    for (String featureId : weights.keySet()) {
      Perceptron.FeatureWeights.Builder fb = Perceptron.FeatureWeights.newBuilder();
      fb.setFeatureId(featureId);
      Map<String, Double> tagWeights = weights.get(featureId);
      for (String tag : tagWeights.keySet()) {
        Perceptron.TagWeight tagWeight = Perceptron.TagWeight.newBuilder()
            .setTag(tag)
            .setWeight(tagWeights.get(tag))
            .build();
        fb.addWeight(tagWeight);
      }
      pb.addWeight(fb.build());
    }
    for (String pair : totals.keySet()) {
      Perceptron.PairWeight pairWeight = Perceptron.PairWeight.newBuilder()
          .setPair(pair)
          .setWeight(totals.get(pair))
          .build();
      pb.addTotal(pairWeight);
    }
  }

  /**
   * Reinit the model from previously stored protobuf.
   *
   * @param pb to read data from.
   */
  public void initFromPB(Perceptron.PerceptronModel pb) {
    updateCount = pb.getUpdateCount();
    List<Perceptron.TimeStamp> pbTimeStamps = pb.getTimestampList();
    for (Perceptron.TimeStamp stamp : pbTimeStamps) {
      timeStamps.put(stamp.getPair(), stamp.getIteration());
    }
    List<Perceptron.FeatureWeights> weightList = pb.getWeightList();
    for (Perceptron.FeatureWeights featureWeights : weightList) {
      String featureId = featureWeights.getFeatureId();
      List<Perceptron.TagWeight> tagWeightList = featureWeights.getWeightList();
      for (Perceptron.TagWeight tagWeight : tagWeightList) {
        Map<String, Double> localFeatureWeights = weights.computeIfAbsent(featureId, fn -> new HashMap<>());
        localFeatureWeights.put(tagWeight.getTag(), tagWeight.getWeight());
      }
    }
    List<Perceptron.PairWeight> totalList = pb.getTotalList();
    for (Perceptron.PairWeight pairWeight : totalList) {
      totals.put(pairWeight.getPair(), pairWeight.getWeight());
    }
  }
}