The arguments to assertThat method is a constant. It should be a variable or a
method invocation. For eg. switch assertThat(1).isEqualTo(methodCall())
to assertThat(methodCall()).isEqualTo(1).
