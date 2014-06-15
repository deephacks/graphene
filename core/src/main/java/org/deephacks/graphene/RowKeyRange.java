package org.deephacks.graphene;

import org.deephacks.graphene.internal.serialization.BytesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RowKeyRange {
  public enum Bound { LOWER, UPPER }

  private static final byte[] DEGENERATE_KEY = new byte[]{1};
  public static final byte[] UNBOUND = new byte[0];

  public static final RowKeyRange EMPTY_RANGE = new RowKeyRange(DEGENERATE_KEY, false, DEGENERATE_KEY, false);
  public static final RowKeyRange EVERYTHING_RANGE = new RowKeyRange(UNBOUND, false, UNBOUND, false);

  public static final Comparator<RowKeyRange> COMPARATOR = (o1, o2) -> {
    Comparator<RowKeyRange> comparator = (k1, k2) -> Comparator.<Boolean>naturalOrder().compare(k2.lowerUnbound(), k1.lowerUnbound());
    comparator.thenComparing((k1, k2) -> BytesUtils.compareTo(k1.getLowerRange(), k2.getLowerRange()));
    comparator.thenComparing((k1, k2) -> Comparator.<Boolean>naturalOrder().compare(k2.isLowerInclusive(), k1.isLowerInclusive()));
    comparator.thenComparing((k1, k2) -> BytesUtils.compareTo(k1.getUpperRange(), k2.getUpperRange()));
    comparator.thenComparing((k1, k2) -> Comparator.<Boolean>naturalOrder().compare(k2.isUpperInclusive(), k1.isUpperInclusive()));
    return comparator.compare(o1, o2);
  };

  private byte[] lowerRange;
  private boolean lowerInclusive;
  private byte[] upperRange;
  private boolean upperInclusive;
  private boolean isSingleKey;

  public static RowKeyRange getKeyRange(byte[] point) {
    return getKeyRange(point, true, point, true);
  }

  public static RowKeyRange getKeyRange(byte[] lowerRange, byte[] upperRange) {
    return getKeyRange(lowerRange, true, upperRange, false);
  }

  public static RowKeyRange getKeyRange(byte[] lowerRange, boolean lowerInclusive,
                                     byte[] upperRange, boolean upperInclusive) {
    if (lowerRange == null || upperRange == null) {
      return EMPTY_RANGE;
    }
    boolean unboundLower = false;
    boolean unboundUpper = false;
    if (lowerRange.length == 0) {
      lowerRange = UNBOUND;
      lowerInclusive = false;
      unboundLower = true;
    }
    if (upperRange.length == 0) {
      upperRange = UNBOUND;
      upperInclusive = false;
      unboundUpper = true;
    }

    if (unboundLower && unboundUpper) {
      return EVERYTHING_RANGE;
    }
    if (!unboundLower && !unboundUpper) {
      int cmp = BytesUtils.compareTo(lowerRange, upperRange);
      if (cmp > 0 || (cmp == 0 && !(lowerInclusive && upperInclusive))) {
        return EMPTY_RANGE;
      }
    }
    return new RowKeyRange(lowerRange, unboundLower ? false : lowerInclusive,
            upperRange, unboundUpper ? false : upperInclusive);
  }

  public RowKeyRange() {
    this.lowerRange = DEGENERATE_KEY;
    this.lowerInclusive = false;
    this.upperRange = DEGENERATE_KEY;
    this.upperInclusive = false;
    this.isSingleKey = false;
  }

  private RowKeyRange(byte[] lowerRange, boolean lowerInclusive, byte[] upperRange, boolean upperInclusive) {
    this.lowerRange = lowerRange;
    this.lowerInclusive = lowerInclusive;
    this.upperRange = upperRange;
    this.upperInclusive = upperInclusive;
    init();
  }

  private void init() {
    this.isSingleKey = lowerRange != UNBOUND && upperRange != UNBOUND
            && lowerInclusive && upperInclusive && BytesUtils.compareTo(lowerRange, upperRange) == 0;
  }

  public byte[] getRange(Bound bound) {
    return bound == Bound.LOWER ? getLowerRange() : getUpperRange();
  }

  public boolean isInclusive(Bound bound) {
    return bound == Bound.LOWER ? isLowerInclusive() : isUpperInclusive();
  }

  public boolean isUnbound(Bound bound) {
    return bound == Bound.LOWER ? lowerUnbound() : upperUnbound();
  }

  public boolean isSingleKey() {
    return isSingleKey;
  }

  /**
   * Compares a lower bound against an upper bound
   *
   * @param b           upper bound byte array
   * @param o           upper bound offset
   * @param l           upper bound length
   * @param isInclusive upper bound inclusive
   * @return -1 if the lower bound is less than the upper bound,
   * 1 if the lower bound is greater than the upper bound,
   * and 0 if they are equal.
   */
  public int compareLowerToUpperBound(byte[] b, int o, int l, boolean isInclusive) {
    if (lowerUnbound() || b == RowKeyRange.UNBOUND) {
      return -1;
    }
    int cmp = BytesUtils.compareTo(lowerRange, 0, lowerRange.length, b, o, l);
    if (cmp > 0) {
      return 1;
    }
    if (cmp < 0) {
      return -1;
    }
    if (lowerInclusive && isInclusive) {
      return 0;
    }
    return 1;
  }

  public int compareUpperToLowerBound(byte[] b, int o, int l) {
    return compareUpperToLowerBound(b, o, l, true);
  }

  public int compareUpperToLowerBound(byte[] b, int o, int l, boolean isInclusive) {
    if (upperUnbound() || b == RowKeyRange.UNBOUND) {
      return 1;
    }
    int cmp = BytesUtils.compareTo(upperRange, 0, upperRange.length, b, o, l);
    if (cmp > 0) {
      return 1;
    }
    if (cmp < 0) {
      return -1;
    }
    if (upperInclusive && isInclusive) {
      return 0;
    }
    return -1;
  }

  public byte[] getLowerRange() {
    return lowerRange;
  }

  public boolean isLowerInclusive() {
    return lowerInclusive;
  }

  public byte[] getUpperRange() {
    return upperRange;
  }

  public boolean isUpperInclusive() {
    return upperInclusive;
  }

  public boolean isUnbound() {
    return lowerUnbound() || upperUnbound();
  }

  public boolean upperUnbound() {
    return upperRange == UNBOUND;
  }

  public boolean lowerUnbound() {
    return lowerRange == UNBOUND;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(lowerRange);
    if (lowerRange != null)
      result = prime * result + (lowerInclusive ? 1231 : 1237);
    result = prime * result + Arrays.hashCode(upperRange);
    if (upperRange != null)
      result = prime * result + (upperInclusive ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof RowKeyRange)) {
      return false;
    }
    RowKeyRange that = (RowKeyRange) o;
    return BytesUtils.compareTo(this.lowerRange, that.lowerRange) == 0 && this.lowerInclusive == that.lowerInclusive &&
            BytesUtils.compareTo(this.upperRange, that.upperRange) == 0 && this.upperInclusive == that.upperInclusive;
  }

  public RowKeyRange intersect(RowKeyRange range) {
    byte[] newLowerRange;
    byte[] newUpperRange;
    boolean newLowerInclusive;
    boolean newUpperInclusive;
    if (lowerUnbound()) {
      newLowerRange = range.lowerRange;
      newLowerInclusive = range.lowerInclusive;
    } else if (range.lowerUnbound()) {
      newLowerRange = lowerRange;
      newLowerInclusive = lowerInclusive;
    } else {
      int cmp = BytesUtils.compareTo(this.lowerRange, range.lowerRange);
      if (cmp != 0 || lowerInclusive == range.lowerInclusive) {
        if (cmp <= 0) {
          newLowerRange = range.lowerRange;
          newLowerInclusive = range.lowerInclusive;
        } else {
          newLowerRange = lowerRange;
          newLowerInclusive = lowerInclusive;
        }
      } else { // Same lower range, but one is not inclusive
        newLowerRange = range.lowerRange;
        newLowerInclusive = false;
      }
    }
    if (upperUnbound()) {
      newUpperRange = range.upperRange;
      newUpperInclusive = range.upperInclusive;
    } else if (range.upperUnbound()) {
      newUpperRange = upperRange;
      newUpperInclusive = upperInclusive;
    } else {
      int cmp = BytesUtils.compareTo(this.upperRange, range.upperRange);
      if (cmp != 0 || upperInclusive == range.upperInclusive) {
        if (cmp >= 0) {
          newUpperRange = range.upperRange;
          newUpperInclusive = range.upperInclusive;
        } else {
          newUpperRange = upperRange;
          newUpperInclusive = upperInclusive;
        }
      } else { // Same upper range, but one is not inclusive
        newUpperRange = range.upperRange;
        newUpperInclusive = false;
      }
    }
    if (newLowerRange == lowerRange && newLowerInclusive == lowerInclusive
            && newUpperRange == upperRange && newUpperInclusive == upperInclusive) {
      return this;
    }
    return getKeyRange(newLowerRange, newLowerInclusive, newUpperRange, newUpperInclusive);
  }

  public RowKeyRange union(RowKeyRange other) {
    if (EMPTY_RANGE == other) return this;
    if (EMPTY_RANGE == this) return other;
    byte[] newLower, newUpper;
    boolean newLowerInclusive, newUpperInclusive;
    if (this.lowerUnbound() || other.lowerUnbound()) {
      newLower = UNBOUND;
      newLowerInclusive = false;
    } else {

      int lowerCmp = BytesUtils.compareTo(this.lowerRange, other.lowerRange);
      if (lowerCmp < 0) {
        newLower = lowerRange;
        newLowerInclusive = lowerInclusive;
      } else if (lowerCmp == 0) {
        newLower = lowerRange;
        newLowerInclusive = this.lowerInclusive || other.lowerInclusive;
      } else {
        newLower = other.lowerRange;
        newLowerInclusive = other.lowerInclusive;
      }
    }

    if (this.upperUnbound() || other.upperUnbound()) {
      newUpper = UNBOUND;
      newUpperInclusive = false;
    } else {
      int upperCmp = BytesUtils.compareTo(this.upperRange, other.upperRange);
      if (upperCmp > 0) {
        newUpper = upperRange;
        newUpperInclusive = this.upperInclusive;
      } else if (upperCmp == 0) {
        newUpper = upperRange;
        newUpperInclusive = this.upperInclusive || other.upperInclusive;
      } else {
        newUpper = other.upperRange;
        newUpperInclusive = other.upperInclusive;
      }
    }
    return RowKeyRange.getKeyRange(newLower, newLowerInclusive, newUpper, newUpperInclusive);
  }

  public static List<RowKeyRange> of(List<byte[]> keys) {
    return keys.stream().map(k -> new RowKeyRange(k, true, k, true)).collect(Collectors.toList());
  }

  public static List<RowKeyRange> intersect(List<RowKeyRange> keyRanges, List<RowKeyRange> keyRanges2) {
    List<RowKeyRange> tmp = new ArrayList<>();
    for (RowKeyRange r1 : keyRanges) {
      for (RowKeyRange r2 : keyRanges2) {
        RowKeyRange r = r1.intersect(r2);
        if (EMPTY_RANGE != r) {
          tmp.add(r);
        }
      }
    }
    if (tmp.size() == 0) {
      return Collections.singletonList(RowKeyRange.EMPTY_RANGE);
    }
    Collections.sort(tmp, RowKeyRange.COMPARATOR);
    List<RowKeyRange> tmp2 = new ArrayList<>();
    RowKeyRange r = tmp.get(0);
    for (int i = 1; i < tmp.size(); i++) {
      if (EMPTY_RANGE == r.intersect(tmp.get(i))) {
        tmp2.add(r);
        r = tmp.get(i);
      } else {
        r = r.intersect(tmp.get(i));
      }
    }
    tmp2.add(r);
    return tmp2;
  }

  /**
   * Fill both upper and lower range of keyRange to keyLength bytes.
   * If the upper bound is inclusive, it must be filled such that an
   * intersection with a longer key would still match if the shorter
   * length matches.  For example: (*,00C] intersected with [00Caaa,00Caaa]
   * should still return [00Caaa,00Caaa] since the 00C matches and is
   * inclusive.
   *
   * @param keyLength
   * @return the newly filled RowKeyRange
   */
  public RowKeyRange fill(int keyLength) {
    byte[] lowerRange = this.getLowerRange();
    byte[] newLowerRange = lowerRange;
    if (!this.lowerUnbound()) {
      // If lower range is inclusive, fill with 0x00 since conceptually these bytes are included in the range
      newLowerRange = fillKey(lowerRange, keyLength);
    }
    byte[] upperRange = this.getUpperRange();
    byte[] newUpperRange = upperRange;
    if (!this.upperUnbound()) {
      // If upper range is inclusive, fill with 0xFF since conceptually these bytes are included in the range
      newUpperRange = fillKey(upperRange, keyLength);
    }
    if (newLowerRange != lowerRange || newUpperRange != upperRange) {
      return RowKeyRange.getKeyRange(newLowerRange, this.isLowerInclusive(), newUpperRange, this.isUpperInclusive());
    }
    return this;
  }

  public static byte[] fillKey(byte[] key, int length) {
    if (key.length > length) {
      throw new IllegalStateException();
    }
    if (key.length == length) {
      return key;
    }
    byte[] newBound = new byte[length];
    System.arraycopy(key, 0, newBound, 0, key.length);
    return newBound;
  }

  public static RowKeyRange getKeyRange(byte[] key, CompareOp op) {
    switch (op) {
      case EQUAL:
        return getNextKeyRange(key, true, key, true);
      case GREATER:
        return getNextKeyRange(key, false, RowKeyRange.UNBOUND, false);
      case GREATER_OR_EQUAL:
        return getNextKeyRange(key, true, RowKeyRange.UNBOUND, false);
      case LESS:
        return getNextKeyRange(RowKeyRange.UNBOUND, false, key, false);
      case LESS_OR_EQUAL:
        return getNextKeyRange(RowKeyRange.UNBOUND, false, key, true);
      default:
        throw new IllegalArgumentException("Unknown operator " + op);
    }
  }

  @Override
  public String toString() {
    if (isSingleKey()) {
      return BytesUtils.toStringBinary(lowerRange);
    }
    return (lowerInclusive ? "[" :
            "(") + (lowerUnbound() ? "*" :
            BytesUtils.toStringBinary(lowerRange)) + " - " + (upperUnbound() ? "*" :
            BytesUtils.toStringBinary(upperRange)) + (upperInclusive ? "]" : ")" );
  }


  public static RowKeyRange getNextKeyRange(byte[] lowerRange, boolean lowerInclusive, byte[] upperRange, boolean upperInclusive) {
        /*
         * Force lower bound to be inclusive for fixed width keys because it makes
         * comparisons less expensive when you can count on one bound or the other
         * being inclusive. Comparing two fixed width exclusive bounds against each
         * other is inherently more expensive, because you need to take into account
         * if the bigger key is equal to the next key after the smaller key. For
         * example:
         *   (A-B] compared against [A-B)
         * An exclusive lower bound A is bigger than an exclusive upper bound B.
         * Forcing a fixed width exclusive lower bound key to be inclusive prevents
         * us from having to do this extra logic in the compare function.
         */
    if (lowerRange != RowKeyRange.UNBOUND && !lowerInclusive) {
      lowerRange = nextKey(lowerRange);
      lowerInclusive = true;
    }
    return getNextKeyRange(lowerRange, lowerInclusive, upperRange, upperInclusive);
  }

  public static byte[] nextKey(byte[] key) {
    byte[] nextStartRow = new byte[key.length];
    System.arraycopy(key, 0, nextStartRow, 0, key.length);
    if (!nextKey(nextStartRow, nextStartRow.length)) {
      return null;
    }
    return nextStartRow;
  }

  public static boolean nextKey(byte[] key, int length) {
    return nextKey(key, 0, length);
  }

  public static boolean nextKey(byte[] key, int offset, int length) {
    if (length == 0) {
      return false;
    }
    int i = offset + length - 1;
    while (key[i] == -1) {
      key[i] = 0;
      i--;
      if (i < offset) {
        // Change bytes back to the way they were
        do {
          key[++i] = -1;
        } while (i < offset + length - 1);
        return false;
      }
    }
    key[i] = (byte)(key[i] + 1);
    return true;
  }

  public enum CompareOp {
    EQUAL, GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL
  }

}