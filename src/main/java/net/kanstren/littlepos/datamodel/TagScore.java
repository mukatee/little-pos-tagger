package net.kanstren.littlepos.datamodel;

/**
 * For tracking weights for a tag.
 *
 * @author Teemu Kanstren.
 */
public class TagScore {
  /** Tag name/id. */
  public final String tag;
  /** Score/weight for the tag. */
  public final double score;

  public TagScore(String tag, double score) {
    this.tag = tag;
    this.score = score;
  }
}
