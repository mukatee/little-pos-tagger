package net.kanstren.littlepos.datamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Features the tag prediction is based on.
 *
 * @author Teemu Kanstren
 */
public class Features {
  /** Key = feature id, value = value to add to overall feature weight. Value is practically just 1 now...*/
  private Map<String, Integer> features = new HashMap<>();

  public Features(int i, String word, String[] context, String prev1, String prev2) {
    //for some reason the original author says a constant bias is good. what do i know so just keep it..
    add("bias");
    //suffix of the word being analyzed. by default it is last 3 chars, unless the word is shorter
    //->should probably toss shorter words already before coming here
    if (word.length() > 3)
      add("i-suffix", word.substring(word.length() - 3));
    else
      add("i-suffix", word.substring(word.length() - 1));
    //prefix of the word being analyzed. the first char
    add("i-prefix-1", ""+word.charAt(0));
    //tag given to previous word
    add("prev-tag-1", prev1);
    //tag given to previous word of previous word
    add("prev-tag-2", prev2);
    //pair of previous two tags
    add("prev-tag-1-and-2", prev1 + prev2);
    //word being analyzed
    add("i-word", context[i]);
    //pair of tag for previous word and the current word itself being analyzed
    add("prev-tag-and-word", prev1 + context[i]);
    String prevWord = context[i - 1];
    //same type of features for previous word in sentence and the following word in sentence
    //since the "context" has the prefix and suffix added these indices should always work
    //that is, the context is actuallt START,START,SENTENCEWORDS,END,END so the indices are never out of bounds with -2 or +2
    add("prev-word", prevWord);
    if (prevWord.length() > 3)
      add("prev-word-suffix", prevWord.substring(prevWord.length() - 3));
    else
      add("prev-word-suffix", prevWord.substring(prevWord.length() - 1));
    add("prev-word-2", context[i - 2]);
    String nextWord = context[i + 1];
    add("next-word", nextWord);
    if (nextWord.length() > 3)
      add("next-word-suffix", nextWord.substring(nextWord.length() - 3));
    else
      add("next-word-suffix", nextWord.substring(nextWord.length() - 1));
    add("next-word-2", context[i + 2]);
  }

  private void add(String... names) {
    StringBuilder fullName = new StringBuilder();
    for (String name : names) {
      fullName.append(name);
      fullName.append(" ");
    }
    String key = fullName.toString().trim();
    features.put(key, 1);
  }

  public Collection<String> keys() {
    return features.keySet();
  }

  public int valueFor(String fname) {
    return features.getOrDefault(fname, 0);
  }

}
