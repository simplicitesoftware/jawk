package org.jawk.jrt;

import java.util.Enumeration;

/**
 * Similar to StringTokenizer, except that tokens are characters
 * in the input string themselves.
 * <p>
 * For Jawk, this class is used when NF == "".
 */
public class CharacterTokenizer implements Enumeration<Object> {

  private String input;
  private int idx=0;

  /**
   * Construct a CharacterTokenizer.
   *
   * @param input The input string to tokenize.
   */
  public CharacterTokenizer(String input) {
	this.input = input;
  }

  // to satisfy the Enumeration<Object> interface
  public boolean hasMoreElements() {
	return idx < input.length();
  }

  public Object nextElement() {
	return Character.toString(input.charAt(idx++));
  }
}

