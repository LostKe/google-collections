/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import junit.framework.TestCase;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Unit test for {@code AbstractIterator}.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings("serial") // No serialization is used in this test
public class AbstractIteratorTest extends TestCase {

  public void testDefaultBehaviorOfNextAndHasNext() {

    // This sample AbstractIterator returns 0 on the first call, 1 on the
    // second, then signals that it's reached the end of the data
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      private int rep;
      @Override public Integer computeNext() {
        switch (rep++) {
          case 0:
            return 0;
          case 1:
            return 1;
          case 2:
            return endOfData();
          default:
            fail("Should not have been invoked again");
            return null;
        }
      }
    };

    assertTrue(iter.hasNext());
    assertEquals(0, (int) iter.next());

    // verify idempotence of hasNext()
    assertTrue(iter.hasNext());
    assertTrue(iter.hasNext());
    assertTrue(iter.hasNext());
    assertEquals(1, (int) iter.next());

    assertFalse(iter.hasNext());

    // Make sure computeNext() doesn't get invoked again
    assertFalse(iter.hasNext());

    try {
      iter.next();
      fail("no exception thrown");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testDefaultBehaviorOfPeek() {
    /*
     * This sample AbstractIterator returns 0 on the first call, 1 on the
     * second, then signals that it's reached the end of the data
     */
    AbstractIterator<Integer> iter = new AbstractIterator<Integer>() {
      private int rep;
      @Override public Integer computeNext() {
        switch (rep++) {
          case 0:
            return 0;
          case 1:
            return 1;
          case 2:
            return endOfData();
          default:
            fail("Should not have been invoked again");
            return null;
        }
      }
    };

    assertEquals(0, (int) iter.peek());
    assertEquals(0, (int) iter.peek());
    assertTrue(iter.hasNext());
    assertEquals(0, (int) iter.peek());
    assertEquals(0, (int) iter.next());

    assertEquals(1, (int) iter.peek());
    assertEquals(1, (int) iter.next());

    try {
      iter.peek();
      fail("peek() should throw NoSuchElementException at end");
    } catch (NoSuchElementException expected) {
    }

    try {
      iter.peek();
      fail("peek() should continue to throw NoSuchElementException at end");
    } catch (NoSuchElementException expected) {
    }

    try {
      iter.next();
      fail("next() should throw NoSuchElementException as usual");
    } catch (NoSuchElementException expected) {
    }

    try {
      iter.peek();
      fail("peek() should still throw NoSuchElementException after next()");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testDefaultBehaviorOfPeekForEmptyIteration() {

    AbstractIterator<Integer> empty = new AbstractIterator<Integer>() {
      private boolean alreadyCalledEndOfData;
      @Override public Integer computeNext() {
        if (alreadyCalledEndOfData) {
          fail("Should not have been invoked again");
        }
        alreadyCalledEndOfData = true;
        return endOfData();
      }
    };

    try {
      empty.peek();
      fail("peek() should throw NoSuchElementException at end");
    } catch (NoSuchElementException expected) {
    }

    try {
      empty.peek();
      fail("peek() should continue to throw NoSuchElementException at end");
    } catch (NoSuchElementException expected) {
    }
  }

  public void testSneakyThrow() throws Exception {
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      boolean haveBeenCalled;
      @Override public Integer computeNext() {
        if (haveBeenCalled) {
          fail("Should not have been called again");
        } else {
          haveBeenCalled = true;
          SneakyThrower.sneakyThrow();
        }
        return null; // never reached
      }
    };

    // The first time, the sneakily-thrown exception comes out
    try {
      iter.hasNext();
      fail("No exception thrown");
    } catch (Exception e) {
      if (!(e instanceof SomeCheckedException)) {
        throw e;
      }
    }

    // But the second time, AbstractIterator itself throws an ISE
    try {
      iter.hasNext();
      fail("No exception thrown");
    } catch (IllegalStateException expected) {
    }
  }

  public void testException() {
    final SomeUncheckedException exception = new SomeUncheckedException();
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      @Override public Integer computeNext() {
        throw exception;
      }
    };

    // It should pass through untouched
    try {
      iter.hasNext();
      fail("No exception thrown");
    } catch (SomeUncheckedException e) {
      assertSame(exception, e);
    }
  }

  public void testExceptionAfterEndOfData() {
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      @Override public Integer computeNext() {
        endOfData();
        throw new SomeUncheckedException();
      }
    };
    try {
      iter.hasNext();
      fail("No exception thrown");
    } catch (SomeUncheckedException expected) {
    }
  }

  public void testCantRemove() {
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      boolean haveBeenCalled;
      @Override public Integer computeNext() {
        if (haveBeenCalled) {
          endOfData();
        }
        haveBeenCalled = true;
        return 0;
      }
    };

    assertEquals(0, (int) iter.next());

    try {
      iter.remove();
      fail("No exception thrown");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testReentrantHasNext() {
    Iterator<Integer> iter = new AbstractIterator<Integer>() {
      @Override protected Integer computeNext() {
        hasNext();
        return null;
      }
    };
    try {
      iter.hasNext();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  // Technically we should test other reentrant scenarios (9 combinations of
  // hasNext/next/peek), but we'll cop out for now, knowing that peek() and
  // next() both start by invoking hasNext() anyway.

  /**
   * Throws a undeclared checked exception.
   */
  private static class SneakyThrower {
    SneakyThrower() throws SomeCheckedException {
      throw new SomeCheckedException();
    }
    private static void sneakyThrow() {
      try {
        SneakyThrower.class.newInstance();
      } catch (IllegalAccessException e) {
      } catch (InstantiationException e) {
      }
    }
  }

  private static class SomeCheckedException extends Exception {
  }

  private static class SomeUncheckedException extends RuntimeException {
  }
}
