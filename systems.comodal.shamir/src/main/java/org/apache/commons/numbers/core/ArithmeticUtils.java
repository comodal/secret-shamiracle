/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.numbers.core;

import java.text.MessageFormat;

/**
 * Some useful, arithmetics related, additions to the built-in functions in
 * {@link Math}.
 */
public final class ArithmeticUtils {

  /**
   * Private constructor.
   */
  private ArithmeticUtils() {
    super();
  }

  /**
   * Computes the greatest common divisor of the absolute value of two
   * numbers, using a modified version of the "binary gcd" method.
   * See Knuth 4.5.2 algorithm B.
   * The algorithm is due to Josef Stein (1961).
   * <br>
   * Special cases:
   * <ul>
   * <li>The invocations
   * {@code gcd(Integer.MIN_VALUE, Integer.MIN_VALUE)},
   * {@code gcd(Integer.MIN_VALUE, 0)} and
   * {@code gcd(0, Integer.MIN_VALUE)} throw an
   * {@code ArithmeticException}, because the result would be 2^31, which
   * is too large for an int value.</li>
   * <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
   * {@code gcd(x, 0)} is the absolute value of {@code x}, except
   * for the special cases above.</li>
   * <li>The invocation {@code gcd(0, 0)} is the only one which returns
   * {@code 0}.</li>
   * </ul>
   *
   * @param p Number.
   * @param q Number.
   * @return the greatest common divisor (never negative).
   * @throws ArithmeticException if the result cannot be represented as
   *                             a non-negative {@code int} value.
   */
  public static int gcd(final int p, final int q) {
    int a = p;
    int b = q;
    if (a == 0 || b == 0) {
      if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE) {
        throw new NumbersArithmeticException("overflow: gcd({0}, {1}) is 2^31", p, q);
      }
      return Math.abs(a + b);
    }

    long al = a;
    long bl = b;
    boolean useLong = false;
    if (a < 0) {
      if (Integer.MIN_VALUE == a) {
        useLong = true;
      } else {
        a = -a;
      }
      al = -al;
    }
    if (b < 0) {
      if (Integer.MIN_VALUE == b) {
        useLong = true;
      } else {
        b = -b;
      }
      bl = -bl;
    }
    if (useLong) {
      if (al == bl) {
        throw new NumbersArithmeticException("overflow: gcd({0}, {1}) is 2^31", p, q);
      }
      long blbu = bl;
      bl = al;
      al = blbu % al;
      if (al == 0) {
        if (bl > Integer.MAX_VALUE) {
          throw new NumbersArithmeticException("overflow: gcd({0}, {1}) is 2^31", p, q);
        }
        return (int) bl;
      }
      blbu = bl;

      // Now "al" and "bl" fit in an "int".
      b = (int) al;
      a = (int) (blbu % al);
    }

    return gcdPositive(a, b);
  }

  /**
   * Computes the greatest common divisor of two <em>positive</em> numbers
   * (this precondition is <em>not</em> checked and the result is undefined
   * if not fulfilled) using the "binary gcd" method which avoids division
   * and modulo operations.
   * See Knuth 4.5.2 algorithm B.
   * The algorithm is due to Josef Stein (1961).
   * <br/>
   * Special cases:
   * <ul>
   * <li>The result of {@code gcd(x, x)}, {@code gcd(0, x)} and
   * {@code gcd(x, 0)} is the value of {@code x}.</li>
   * <li>The invocation {@code gcd(0, 0)} is the only one which returns
   * {@code 0}.</li>
   * </ul>
   *
   * @param a Positive number.
   * @param b Positive number.
   * @return the greatest common divisor.
   */
  private static int gcdPositive(int a, int b) {
    if (a == 0) {
      return b;
    } else if (b == 0) {
      return a;
    }

    // Make "a" and "b" odd, keeping track of common power of 2.
    final int aTwos = Integer.numberOfTrailingZeros(a);
    a >>= aTwos;
    final int bTwos = Integer.numberOfTrailingZeros(b);
    b >>= bTwos;
    final int shift = Math.min(aTwos, bTwos);

    // "a" and "b" are positive.
    // If a > b then "gdc(a, b)" is equal to "gcd(a - b, b)".
    // If a < b then "gcd(a, b)" is equal to "gcd(b - a, a)".
    // Hence, in the successive iterations:
    //  "a" becomes the absolute difference of the current values,
    //  "b" becomes the minimum of the current values.
    while (a != b) {
      final int delta = a - b;
      b = Math.min(a, b);
      a = Math.abs(delta);

      // Remove any power of 2 in "a" ("b" is guaranteed to be odd).
      a >>= Integer.numberOfTrailingZeros(a);
    }

    // Recover the common power of 2.
    return a << shift;
  }


  /**
   * Multiply two long integers, checking for overflow.
   *
   * @param a Factor.
   * @param b Factor.
   * @return the product {@code a * b}.
   * @throws ArithmeticException if the result can not be represented
   *                             as a {@code long}.
   */
  public static long mulAndCheck(final long a, final long b) {
    long ret;
    if (a > b) {
      // use symmetry to reduce boundary cases
      ret = mulAndCheck(b, a);
    } else {
      if (a < 0) {
        if (b < 0) {
          // check for positive overflow with negative a, negative b
          if (a >= Long.MAX_VALUE / b) {
            ret = a * b;
          } else {
            throw new NumbersArithmeticException();
          }
        } else if (b > 0) {
          // check for negative overflow with negative a, positive b
          if (Long.MIN_VALUE / b <= a) {
            ret = a * b;
          } else {
            throw new NumbersArithmeticException();
          }
        } else {
          // assert b == 0
          ret = 0;
        }
      } else if (a > 0) {
        // assert a > 0
        // assert b > 0

        // check for positive overflow with positive a, positive b
        if (a <= Long.MAX_VALUE / b) {
          ret = a * b;
        } else {
          throw new NumbersArithmeticException();
        }
      } else {
        // assert a == 0
        ret = 0;
      }
    }
    return ret;
  }

  /**
   * Exception.
   */
  private static final class NumbersArithmeticException extends ArithmeticException {
    /**
     * Serializable version Id.
     */
    private static final long serialVersionUID = 20180130L;
    /**
     * Argument to construct a message.
     */
    private final Object[] formatArguments;

    /**
     * Default constructor.
     */
    NumbersArithmeticException() {
      this("arithmetic exception");
    }

    /**
     * Constructor with a specific message.
     *
     * @param message Message pattern providing the specific context of
     *                the error.
     * @param args    Arguments.
     */
    NumbersArithmeticException(String message, Object... args) {
      super(message);
      this.formatArguments = args;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
      return MessageFormat.format(super.getMessage(), formatArguments);
    }
  }
}
