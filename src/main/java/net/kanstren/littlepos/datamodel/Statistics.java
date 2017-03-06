package net.kanstren.littlepos.datamodel;


import net.kanstren.littlepos.generated.protobuf.Perceptron;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Track statistics for how often a word or a tag has been seen generally, and how often a tag has been seen for a word.
 *
 * @author Teemu Kanstren
 */
public class Statistics {
  /** Set of unique tags. */
  private Collection<String> tags = new HashSet<>();
  /** Set of unique words. */
  private Collection<String> words = new HashSet<>();
  /** Key = word, value = number of times a tag has been seen for the word. */
  private final Map<String, TagCounter> statistics = new HashMap<>();

  public void updateWith(List<TagSentence> sentences) {
    for (TagSentence sentence : sentences) {
      updateWith(sentence);
    }
  }

  public void updateWith(TagSentence sentence) {
    for (WordTag wordTag : sentence.getWordTags()) {
      updateWith(wordTag);
    }
  }

  public Collection<String> tags() {
    return tags;
  }

  public Collection<String> words() {
    return words;
  }

  public TagCounter tagCountsFor(String word) {
    return statistics.get(word);
  }

  public void updateWith(WordTag wordTag) {
    TagCounter tagCounter = statistics.computeIfAbsent(wordTag.word, TagCounter::new);
    tagCounter.increment(wordTag.tag);
  }

  public class TagCounter {
    public final String word;
    private final Map<String, Integer> tagCounts = new HashMap<>();
    public int totalTagCount = 0;

    public TagCounter(String word) {
      this.word = word;
      words.add(word);
    }

    public void increment(String tag) {
      int tagCount = tagCounts.getOrDefault(tag, 0);
      tagCount++;
      tagCounts.put(tag, tagCount);
      tags.add(tag);
      totalTagCount++;
    }

    public Map<String, Integer> getTagCounts() {
      return tagCounts;
    }

    public TagCount max() {
      Map.Entry<String, Integer> max = Collections.max(tagCounts.entrySet(), Map.Entry.comparingByValue());
      return new TagCount(max.getKey(), max.getValue());
    }
  }

  public class TagCount {
    public final String tag;
    public final int count;

    public TagCount(String tag, int count) {
      this.tag = tag;
      this.count = count;
    }
  }

  public void buildPB(Perceptron.PerceptronModel.Builder pb) {
    pb.addAllUniqueTag(tags);
    pb.addAllUniqueWord(words);
    for (String word : statistics.keySet()) {
      TagCounter tagCounter = statistics.get(word);
      Perceptron.WordTagFrequency.Builder wtfBuilder = Perceptron.WordTagFrequency.newBuilder();
      wtfBuilder.setWord(word);
      for (String tag : tagCounter.tagCounts.keySet()) {
        Perceptron.TagCount tagCount = Perceptron.TagCount.newBuilder()
            .setTag(tag)
            .setCount(tagCounter.tagCounts.get(tag))
            .build();
        wtfBuilder.addTagCount(tagCount);
      }
      pb.addFreq(wtfBuilder.build());
    }
  }

  public void initFromPB(Perceptron.PerceptronModel model) {
    tags.addAll(model.getUniqueTagList());
    words.addAll(model.getUniqueWordList());
    List<Perceptron.WordTagFrequency> freqList = model.getFreqList();
    for (Perceptron.WordTagFrequency wtf : freqList) {
      String word = wtf.getWord();
      TagCounter tagCounter = statistics.computeIfAbsent(word, TagCounter::new);
      Map<String, Integer> tagCounts = tagCounter.getTagCounts();
      List<Perceptron.TagCount> tagCountList = wtf.getTagCountList();
      for (Perceptron.TagCount tagCount : tagCountList) {
        tagCounts.put(tagCount.getTag(), tagCount.getCount());
      }
    }
  }
}
