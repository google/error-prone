jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field
must have a @Rule JUnit annotation. If this is not done, then all of your jMock
tests will run and pass, but none of your assertions will actually be evaluated.
Your tests will pass even if they shouldn't.
