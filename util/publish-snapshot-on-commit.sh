# https://github.com/google/dagger/blob/master/util/publish-snapshot-on-commit.sh

if [ "$TRAVIS_REPO_SLUG" == "google/error-prone" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo -e "Publishing maven snapshot...\n"

  mvn clean deploy --settings="util/settings.xml" -DskipTests=true -Dinvoker.skip=true -Dmaven.javadoc.skip=true

  echo -e "Published maven snapshot"
fi

