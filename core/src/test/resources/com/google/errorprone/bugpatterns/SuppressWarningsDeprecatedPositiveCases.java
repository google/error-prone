package com.google.errorprone.bugpatterns;


/**
 * Positive cases for {@link SuppressWarningsDeprecated}.
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class SuppressWarningsDeprecatedPositiveCases {

  //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
  @SuppressWarnings("deprecated")
  public static void positiveCase1() {
  }
  
  //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
  @SuppressWarnings({"deprecated"})
  public static void positiveCase2() {
  }
  
  //BUG: Suggestion includes "@SuppressWarnings({"deprecation", "foobarbaz"})"
  @SuppressWarnings({"deprecated", "foobarbaz"})
  public static void positiveCase3() {
  }
  
  public static void positiveCase4() {
    //BUG: Suggestion includes "@SuppressWarnings({"deprecation", "foobarbaz"})"
    @SuppressWarnings({"deprecated", "foobarbaz"})
    int a = 3;
  }
  
  public static void positiveCase5() {
    //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
    @SuppressWarnings("deprecated")
    int a = 3;
  }
  
  public static void positiveCase6() {
    //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
    @SuppressWarnings("deprecated")
    class Foo { };
  }
  
  public static void positiveCase7() {
    //BUG: Suggestion includes "@SuppressWarnings({"deprecation", "foobarbaz"})"
    @SuppressWarnings({"deprecated", "foobarbaz"})
    class Foo { };
  }
  
  //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
  @SuppressWarnings(value = {"deprecated"})
  public static void positiveCase8() {
  }
  
  //BUG: Suggestion includes "@SuppressWarnings("deprecation")"
  @SuppressWarnings(value = "deprecated")
  public static void positiveCase9() {
  }
}
