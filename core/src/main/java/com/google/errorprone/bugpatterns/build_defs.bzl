#  Copyright 2021 The Error Prone Authors.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

"""Asserts that `keys` contains no entries not in `files`."""

def assert_no_unexpected_files(keys, files):
    unexpected_tests = difference(keys, files)
    if unexpected_tests:
        fail("Unexpected tests: " + ", ".join(unexpected_tests))

# Returns the difference of two sets.
def difference(set1, set2):
    return [i for i in set1 if i not in set2]
