# https://github.com/google/dagger/blob/master/util/generate-latest-docs.sh

if [ "$TRAVIS_REPO_SLUG" == "cushon/error-prone" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk7" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo -e "Publishing javadoc...\n"
  mvn clean
  mvn -P run-annotation-processor compile site
  TARGET="$(pwd)/core/target/generated-wiki/"

  cd $HOME
  git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/cushon/error-prone gh-pages > /dev/null
 
  cd gh-pages
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  rm -rf _data/bugpatterns.yaml bugpattern
  rsync -a ${TARGET} .
  git add --all _data bugpattern
  git commit -m "Lastest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
  git push -fq origin gh-pages > /dev/null

  echo -e "Published Javadoc to gh-pages.\n"
fi
