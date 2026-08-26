#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

if [ -d spec ]; then
  exec bundle exec rspec
fi

if [ -x bin/rails ]; then
  exec bundle exec rails test
fi

if [ -d test ]; then
  exec bundle exec ruby -Itest -e 'Dir["test/**/*_test.rb"].sort.each { |file| require "./#{file}" }'
fi

echo "No spec/ or test/ directory found."
