package com.google.errorprone;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Abstract base class for compiler integration tests.
 *
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class IntegrationTest {

  /**
   * Constructs the absolute paths to the given files, so they can be passed as arguments to the
   * compiler.
   */
  protected String[] sources(String... files) throws URISyntaxException {
    String[] result = new String[files.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new File(getClass().getResource("/" + files[i]).toURI()).getAbsolutePath();
    }
    return result;
  }

}
