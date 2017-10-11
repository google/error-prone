package com.google.errorprone.bugpatterns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.errorprone.CompilationTestHelper;

@RunWith(value = JUnit4.class)
public class DateEqualsTest {

    private CompilationTestHelper compilationHelper;

    @Before
    public void setUp() {
        compilationHelper = CompilationTestHelper.newInstance(DateEquals.class, getClass());
    }

    @Test
    public void compareDates() {
        compilationHelper
                .addSourceLines(
                        "NormalDate.java",
                        "import java.util.Date;",
                        "public class NormalDate {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Date();",
                        "    date.compareTo(date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void compareTimestamp() {
        compilationHelper
                .addSourceLines(
                        "TimestampDate.java",
                        "import java.util.Date;",
                        "import java.sql.Timestamp;",
                        "public class TimestampDate {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Timestamp(date.getTime());",
                        "    date.compareTo(date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void instanceEqualUsed() {
        compilationHelper
                .addSourceLines(
                        "DateEquals.java",
                        "import java.util.Date;",
                        "public class DateEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Date();",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    date.equals(date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void objectsEqualUsed() {
        compilationHelper
                .addSourceLines(
                        "DateObjectsEquals.java",
                        "import java.util.Date;",
                        "import java.util.Objects;",
                        "public class DateObjectsEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Date();",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    Objects.equals(date, date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void timestampInstanceEquals() {
        compilationHelper
                .addSourceLines(
                        "TimeStampEquals.java",
                        "import java.util.Date;",
                        "import java.sql.Timestamp;",
                        "public class TimeStampEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Timestamp(date.getTime());",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    date.equals(date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void timestampInstanceEqualsReverse() {
        compilationHelper
                .addSourceLines(
                        "TimeStampEquals.java",
                        "import java.util.Date;",
                        "import java.sql.Timestamp;",
                        "public class TimeStampEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Timestamp(date.getTime());",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    date1.equals(date);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void timestampObjectEquals() {
        compilationHelper
                .addSourceLines(
                        "TimeStampEquals.java",
                        "import java.util.Date;",
                        "import java.sql.Timestamp;",
                        "import java.util.Objects;",
                        "public class TimeStampEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Timestamp(date.getTime());",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    Objects.equals(date, date1);",
                        "  }",
                        "}").doTest();
    }

    @Test
    public void timestampObjectEqualsReverse() {
        compilationHelper
                .addSourceLines(
                        "TimeStampEquals.java",
                        "import java.util.Date;",
                        "import java.sql.Timestamp;",
                        "import java.util.Objects;",
                        "public class TimeStampEquals {",
                        "  public void normal() {",
                        "    Date date = new Date();",
                        "    Date date1 = new Timestamp(date.getTime());",
                        "    // BUG: Diagnostic contains: Equals used to compare dates",
                        "    Objects.equals(date1, date);",
                        "  }",
                        "}").doTest();
    }
}
